package com.regiones;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class WandListener implements Listener {
    private final Regiones plugin;

    public WandListener(Regiones plugin) { this.plugin = plugin; }

    public static ItemStack createWand(Regiones plugin) {
        ItemStack item = new ItemStack(Material.GOLDEN_AXE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Varita de Regiones", NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(java.util.List.of(
                Component.text("Click izq: posición 1", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("Click der: posición 2", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
        meta.getPersistentDataContainer().set(plugin.wandKey(), PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    private boolean isWand(ItemStack item) {
        if (item == null || item.getType() != Material.GOLDEN_AXE || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(plugin.wandKey(), PersistentDataType.BYTE);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        Player p = e.getPlayer();
        if (!isWand(e.getItem())) return;
        if (!p.hasPermission("regiones.admin")) return;
        Block b = e.getClickedBlock();
        if (b == null) return;

        Action a = e.getAction();
        if (a == Action.LEFT_CLICK_BLOCK) {
            e.setCancelled(true);
            set(p, 0, b.getLocation());
        } else if (a == Action.RIGHT_CLICK_BLOCK) {
            e.setCancelled(true);
            set(p, 1, b.getLocation());
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBreak(BlockBreakEvent e) {
        if (isWand(e.getPlayer().getInventory().getItemInMainHand())) e.setCancelled(true);
    }

    private void set(Player p, int idx, Location loc) {
        plugin.selection(p.getUniqueId())[idx] = loc;
        Regiones.send(p, "&6[Regiones] &fPosición " + (idx + 1) + " = &e"
                + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
    }
}
