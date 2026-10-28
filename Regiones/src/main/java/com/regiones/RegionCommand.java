package com.regiones;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class RegionCommand implements CommandExecutor, TabCompleter {
    private static final List<String> SUBS = List.of(
            "wand", "pos1", "pos2", "create", "delete", "list", "info", "flag", "flags", "bypass", "reload");

    private final Regiones plugin;

    public RegionCommand(Regiones plugin) { this.plugin = plugin; }

    private Player player(CommandSender s) {
        if (s instanceof Player p) return p;
        Regiones.send(s, "&cEste subcomando es solo para jugadores.");
        return null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("regiones.admin")) {
            Regiones.send(sender, "&cNo tienes permiso.");
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            help(sender, label);
            return true;
        }

        RegionManager rm = plugin.regions();
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "wand" -> {
                Player p = player(sender);
                if (p == null) return true;
                p.getInventory().addItem(WandListener.createWand(plugin));
                Regiones.send(p, "&6[Regiones] &fTienes la varita. Click izq = pos1, click der = pos2.");
            }
            case "pos1", "pos2" -> {
                Player p = player(sender);
                if (p == null) return true;
                int idx = args[0].equalsIgnoreCase("pos1") ? 0 : 1;
                Location l = p.getLocation().getBlock().getLocation();
                plugin.selection(p.getUniqueId())[idx] = l;
                Regiones.send(p, "&6[Regiones] &fPosición " + (idx + 1) + " = &e"
                        + l.getBlockX() + ", " + l.getBlockY() + ", " + l.getBlockZ());
            }
            case "create" -> {
                Player p = player(sender);
                if (p == null) return true;
                if (args.length < 2) {
                    Regiones.send(p, "&cUso: /" + label + " create <nombre> [3d]");
                    return true;
                }
                String name = args[1].toLowerCase(Locale.ROOT);
                if (!name.matches("[a-z0-9_-]+")) {
                    Regiones.send(p, "&cNombre no válido (usa letras, números, _ y -).");
                    return true;
                }
                if (rm.get(name) != null) {
                    Regiones.send(p, "&cYa existe una región con ese nombre.");
                    return true;
                }
                Location[] sel = plugin.selection(p.getUniqueId());
                if (sel[0] == null || sel[1] == null) {
                    Regiones.send(p, "&cPrimero selecciona pos1 y pos2 (/" + label + " wand).");
                    return true;
                }
                if (!sel[0].getWorld().equals(sel[1].getWorld())) {
                    Regiones.send(p, "&cLas dos posiciones deben estar en el mismo mundo.");
                    return true;
                }
                World w = sel[0].getWorld();
                boolean full = !(args.length >= 3 && args[2].equalsIgnoreCase("3d"));
                int minY = full ? w.getMinHeight() : Math.min(sel[0].getBlockY(), sel[1].getBlockY());
                int maxY = full ? w.getMaxHeight() - 1 : Math.max(sel[0].getBlockY(), sel[1].getBlockY());
                Region r = new Region(name, w.getName(),
                        Math.min(sel[0].getBlockX(), sel[1].getBlockX()), minY,
                        Math.min(sel[0].getBlockZ(), sel[1].getBlockZ()),
                        Math.max(sel[0].getBlockX(), sel[1].getBlockX()), maxY,
                        Math.max(sel[0].getBlockZ(), sel[1].getBlockZ()));
                rm.add(r);
                Regiones.send(p, "&6[Regiones] &aRegión &e" + name + " &acreada ("
                        + r.volume() + " bloques" + (full ? ", altura completa" : "") + ").");
            }
            case "delete" -> {
                if (args.length < 2) {
                    Regiones.send(sender, "&cUso: /" + label + " delete <nombre>");
                    return true;
                }
                if (rm.remove(args[1])) Regiones.send(sender, "&6[Regiones] &aRegión eliminada.");
                else Regiones.send(sender, "&cNo existe esa región.");
            }
            case "list" -> {
                if (rm.all().isEmpty()) {
                    Regiones.send(sender, "&6[Regiones] &fNo hay regiones.");
                    return true;
                }
                Regiones.send(sender, "&6[Regiones] &fRegiones:");
                for (Region r : rm.all()) {
                    Regiones.send(sender, " &e" + r.name + " &7(" + r.world + ")");
                }
            }
            case "info" -> {
                Region r;
                if (args.length >= 2) {
                    r = rm.get(args[1]);
                } else {
                    Player p = player(sender);
                    if (p == null) return true;
                    List<Region> here = rm.at(p.getLocation());
                    r = here.isEmpty() ? null : here.get(0);
                }
                if (r == null) {
                    Regiones.send(sender, "&cNo se encontró la región.");
                    return true;
                }
                Regiones.send(sender, "&6[Regiones] &e" + r.name + " &7(" + r.world + ")");
                Regiones.send(sender, " &fDe &7" + r.minX + "," + r.minY + "," + r.minZ
                        + " &fa &7" + r.maxX + "," + r.maxY + "," + r.maxZ);
                for (Flag f : Flag.values()) {
                    boolean v = r.flag(f);
                    boolean explicit = r.flags.containsKey(f.id());
                    Regiones.send(sender, " &f" + f.id() + ": " + (v ? "&aallow" : "&cdeny")
                            + (explicit ? "" : " &8(por defecto)"));
                }
            }
            case "flag" -> {
                if (args.length < 4) {
                    Regiones.send(sender, "&cUso: /" + label + " flag <región> <flag> <allow|deny|reset>");
                    return true;
                }
                Region r = rm.get(args[1]);
                if (r == null) {
                    Regiones.send(sender, "&cNo existe esa región.");
                    return true;
                }
                Flag f = Flag.byId(args[2]);
                if (f == null) {
                    Regiones.send(sender, "&cFlag desconocida. Usa /" + label + " flags");
                    return true;
                }
                switch (args[3].toLowerCase(Locale.ROOT)) {
                    case "allow" -> r.flags.put(f.id(), true);
                    case "deny" -> r.flags.put(f.id(), false);
                    case "reset" -> r.flags.remove(f.id());
                    default -> {
                        Regiones.send(sender, "&cUsa allow, deny o reset.");
                        return true;
                    }
                }
                rm.save();
                Regiones.send(sender, "&6[Regiones] &e" + r.name + "&f: " + f.id() + " = "
                        + (r.flag(f) ? "&aallow" : "&cdeny"));
            }
            case "flags" -> {
                Regiones.send(sender, "&6[Regiones] &fFlags disponibles (por defecto todas en allow):");
                for (Flag f : Flag.values()) {
                    Regiones.send(sender, " &e" + f.id() + " &7- " + f.description());
                }
            }
            case "bypass" -> {
                Player p = player(sender);
                if (p == null) return true;
                boolean on = plugin.toggleBypass(p);
                Regiones.send(p, "&6[Regiones] &fBypass " + (on ? "&aactivado" : "&cdesactivado")
                        + "&f (afecta a romper/poner/interactuar).");
            }
            case "reload" -> {
                rm.load();
                Regiones.send(sender, "&6[Regiones] &aregions.yml recargado (" + rm.all().size() + " regiones).");
            }
            default -> help(sender, label);
        }
        return true;
    }

    private void help(CommandSender s, String label) {
        Regiones.send(s, "&6&lRegiones &7- comandos:");
        Regiones.send(s, " &e/" + label + " wand &7- varita de selección");
        Regiones.send(s, " &e/" + label + " pos1 | pos2 &7- selecciona donde estás");
        Regiones.send(s, " &e/" + label + " create <nombre> [3d] &7- crea la región (altura completa salvo 3d)");
        Regiones.send(s, " &e/" + label + " delete <nombre>");
        Regiones.send(s, " &e/" + label + " list | info [nombre]");
        Regiones.send(s, " &e/" + label + " flag <región> <flag> <allow|deny|reset>");
        Regiones.send(s, " &e/" + label + " flags &7- lista de flags");
        Regiones.send(s, " &e/" + label + " bypass &7- activa/desactiva tu inmunidad de op");
        Regiones.send(s, " &e/" + label + " reload");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!sender.hasPermission("regiones.admin")) return List.of();
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            out.addAll(SUBS);
        } else if (args.length == 2 && List.of("delete", "info", "flag").contains(args[0].toLowerCase(Locale.ROOT))) {
            plugin.regions().all().forEach(r -> out.add(r.name));
        } else if (args.length == 3 && args[0].equalsIgnoreCase("flag")) {
            for (Flag f : Flag.values()) out.add(f.id());
        } else if (args.length == 4 && args[0].equalsIgnoreCase("flag")) {
            out.addAll(List.of("allow", "deny", "reset"));
        } else if (args.length == 3 && args[0].equalsIgnoreCase("create")) {
            out.add("3d");
        }
        String last = args[args.length - 1].toLowerCase(Locale.ROOT);
        return out.stream().filter(s -> s.toLowerCase(Locale.ROOT).startsWith(last)).collect(Collectors.toList());
    }
}
