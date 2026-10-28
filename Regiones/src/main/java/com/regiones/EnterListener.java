package com.regiones;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Avisa al entrar/salir de una región y si hay PVP o no. */
public class EnterListener implements Listener {
    private final Regiones plugin;
    private final Map<UUID, Set<String>> inside = new HashMap<>();

    public EnterListener(Regiones plugin) { this.plugin = plugin; }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        Location from = e.getFrom(), to = e.getTo();
        if (to == null) return;
        if (from.getWorld() == to.getWorld()
                && from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) return;
        update(e.getPlayer(), to);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) { update(e.getPlayer(), e.getPlayer().getLocation()); }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) { inside.remove(e.getPlayer().getUniqueId()); }

    private void update(Player p, Location to) {
        Set<String> now = new HashSet<>();
        for (Region r : plugin.regions().at(to)) now.add(r.name);
        Set<String> before = inside.getOrDefault(p.getUniqueId(), Set.of());

        for (String name : now) {
            if (before.contains(name)) continue;
            Region r = plugin.regions().get(name);
            if (r == null || !r.flag(Flag.NOTIFY)) continue;
            boolean pvp = plugin.regions().isAllowed(to, Flag.PVP);
            Component sub = pvp
                    ? Component.text("⚠ PVP ACTIVADO", NamedTextColor.RED)
                    : Component.text("✔ PVP desactivado", NamedTextColor.GREEN);
            p.showTitle(Title.title(Component.text(r.name, NamedTextColor.YELLOW), sub,
                    Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(2), Duration.ofMillis(500))));
        }
        for (String name : before) {
            if (now.contains(name)) continue;
            Region r = plugin.regions().get(name);
            if (r == null || !r.flag(Flag.NOTIFY)) continue;
            p.sendActionBar(Component.text("Sales de " + r.name, NamedTextColor.GRAY));
        }
        if (now.isEmpty()) inside.remove(p.getUniqueId());
        else inside.put(p.getUniqueId(), now);
    }
}
