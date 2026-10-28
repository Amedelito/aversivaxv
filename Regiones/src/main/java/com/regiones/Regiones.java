package com.regiones;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class Regiones extends JavaPlugin {
    private RegionManager regionManager;
    private NamespacedKey wandKey;
    private final Map<UUID, Location[]> selections = new HashMap<>();
    private final Set<UUID> bypassOff = new HashSet<>();

    @Override
    public void onEnable() {
        wandKey = new NamespacedKey(this, "wand");
        regionManager = new RegionManager(this);
        regionManager.load();

        getServer().getPluginManager().registerEvents(new WandListener(this), this);
        getServer().getPluginManager().registerEvents(new ProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new EnterListener(this), this);

        RegionCommand cmd = new RegionCommand(this);
        getCommand("region").setExecutor(cmd);
        getCommand("region").setTabCompleter(cmd);
        getLogger().info("Regiones cargadas: " + regionManager.all().size());
    }

    @Override
    public void onDisable() {
        if (regionManager != null) regionManager.save();
    }

    public RegionManager regions() { return regionManager; }
    public NamespacedKey wandKey() { return wandKey; }

    public Location[] selection(UUID id) {
        return selections.computeIfAbsent(id, k -> new Location[2]);
    }

    public boolean hasBypass(Player p) {
        return p.hasPermission("regiones.bypass") && !bypassOff.contains(p.getUniqueId());
    }

    /** Devuelve true si ahora tiene el bypass activado. */
    public boolean toggleBypass(Player p) {
        if (bypassOff.remove(p.getUniqueId())) return true;
        bypassOff.add(p.getUniqueId());
        return false;
    }

    public static void send(CommandSender s, String legacy) {
        s.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(legacy));
    }
}
