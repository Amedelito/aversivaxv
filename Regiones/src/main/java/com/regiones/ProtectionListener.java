package com.regiones;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.weather.LightningStrikeEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ProtectionListener implements Listener {
    private final Regiones plugin;
    private final Map<UUID, Long> cooldown = new HashMap<>();

    public ProtectionListener(Regiones plugin) { this.plugin = plugin; }

    private boolean allowed(Location l, Flag f) { return plugin.regions().isAllowed(l, f); }

    private void warn(Player p, String msg) {
        long now = System.currentTimeMillis();
        Long last = cooldown.get(p.getUniqueId());
        if (last != null && now - last < 1000) return;
        cooldown.put(p.getUniqueId(), now);
        p.sendActionBar(Component.text(msg, NamedTextColor.RED));
    }

    // ---------- Romper / poner ----------

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        if (plugin.hasBypass(p)) return;
        if (!allowed(e.getBlock().getLocation(), Flag.BUILD)) {
            e.setCancelled(true);
            warn(p, "No puedes romper bloques en esta zona.");
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onPlace(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        if (plugin.hasBypass(p)) return;
        if (!allowed(e.getBlockPlaced().getLocation(), Flag.PLACE)) {
            e.setCancelled(true);
            warn(p, "No puedes colocar bloques en esta zona.");
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onBucketEmpty(PlayerBucketEmptyEvent e) {
        Player p = e.getPlayer();
        if (plugin.hasBypass(p)) return;
        if (!allowed(e.getBlock().getLocation(), Flag.PLACE)) {
            e.setCancelled(true);
            warn(p, "No puedes colocar líquidos en esta zona.");
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onBucketFill(PlayerBucketFillEvent e) {
        Player p = e.getPlayer();
        if (plugin.hasBypass(p)) return;
        if (!allowed(e.getBlock().getLocation(), Flag.BUILD)) {
            e.setCancelled(true);
            warn(p, "No puedes recoger líquidos en esta zona.");
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onHangingPlace(HangingPlaceEvent e) {
        Player p = e.getPlayer();
        if (p == null || plugin.hasBypass(p)) return;
        if (!allowed(e.getEntity().getLocation(), Flag.PLACE)) {
            e.setCancelled(true);
            warn(p, "No puedes colocar esto en esta zona.");
        }
    }

    // Cuadros / marcos rotos por explosión
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onHangingBreak(HangingBreakEvent e) {
        if (e.getCause() == HangingBreakEvent.RemoveCause.EXPLOSION
                && !allowed(e.getEntity().getLocation(), Flag.EXPLOSIONS)) {
            e.setCancelled(true);
        }
    }

    // Cuadros / marcos rotos por un jugador
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onHangingBreakByEntity(HangingBreakByEntityEvent e) {
        if (!(e.getRemover() instanceof Player p)) return;
        if (plugin.hasBypass(p)) return;
        if (!allowed(e.getEntity().getLocation(), Flag.BUILD)) {
            e.setCancelled(true);
            warn(p, "No puedes romper esto en esta zona.");
        }
    }

    // ---------- Interactuar ----------

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (plugin.hasBypass(p)) return;
        Block b = e.getClickedBlock();
        if (b == null) return;
        if (allowed(b.getLocation(), Flag.INTERACT)) return;

        if (e.getAction() == Action.PHYSICAL) {
            e.setCancelled(true); // placas de presión, cultivos pisados, tripwire...
            return;
        }
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK && b.getType().isInteractable()) {
            boolean placing = p.isSneaking() && e.getItem() != null;
            if (!placing) {
                e.setUseInteractedBlock(Event.Result.DENY);
                warn(p, "No puedes interactuar en esta zona.");
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onInteractEntity(PlayerInteractEntityEvent e) {
        Player p = e.getPlayer();
        if (plugin.hasBypass(p)) return;
        if (!allowed(e.getRightClicked().getLocation(), Flag.INTERACT)) {
            e.setCancelled(true);
            warn(p, "No puedes interactuar en esta zona.");
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onArmorStand(PlayerArmorStandManipulateEvent e) {
        Player p = e.getPlayer();
        if (plugin.hasBypass(p)) return;
        if (!allowed(e.getRightClicked().getLocation(), Flag.INTERACT)) {
            e.setCancelled(true);
            warn(p, "No puedes interactuar en esta zona.");
        }
    }

    // Mobs pisando cultivos, etc.
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onEntityInteract(EntityInteractEvent e) {
        if (!allowed(e.getBlock().getLocation(), Flag.INTERACT)) e.setCancelled(true);
    }

    // ---------- PVP ----------

    private Player resolveAttacker(Entity damager) {
        if (damager instanceof Player p) return p;
        if (damager instanceof Projectile proj) {
            ProjectileSource src = proj.getShooter();
            if (src instanceof Player p) return p;
        }
        if (damager instanceof TNTPrimed tnt && tnt.getSource() instanceof Player p) return p;
        return null;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onDamage(EntityDamageByEntityEvent e) {
        Entity victim = e.getEntity();
        Player attacker = resolveAttacker(e.getDamager());

        if (victim instanceof Player v) {
            if (attacker == null || attacker.equals(v)) return;
            if (!allowed(v.getLocation(), Flag.PVP) || !allowed(attacker.getLocation(), Flag.PVP)) {
                e.setCancelled(true);
                warn(attacker, "El PVP está desactivado en esta zona.");
            }
            return;
        }

        // Marcos y soportes de armadura golpeados por jugadores
        if (attacker != null && (victim instanceof Hanging || victim instanceof ArmorStand)) {
            if (plugin.hasBypass(attacker)) return;
            if (!allowed(victim.getLocation(), Flag.BUILD)) {
                e.setCancelled(true);
                warn(attacker, "No puedes romper esto en esta zona.");
            }
        }
    }

    // Soportes de armadura destruidos por explosiones
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onArmorStandExplosion(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof ArmorStand)) return;
        EntityDamageEvent.DamageCause c = e.getCause();
        if ((c == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION || c == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION)
                && !allowed(e.getEntity().getLocation(), Flag.EXPLOSIONS)) {
            e.setCancelled(true);
        }
    }

    // ---------- Explosiones / rayos / fuego ----------

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onEntityExplode(EntityExplodeEvent e) {
        e.blockList().removeIf(b -> !allowed(b.getLocation(), Flag.EXPLOSIONS));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onBlockExplode(BlockExplodeEvent e) {
        e.blockList().removeIf(b -> !allowed(b.getLocation(), Flag.EXPLOSIONS));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onLightning(LightningStrikeEvent e) {
        if (!allowed(e.getLightning().getLocation(), Flag.LIGHTNING)) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onIgnite(BlockIgniteEvent e) {
        BlockIgniteEvent.IgniteCause c = e.getCause();
        if (c == BlockIgniteEvent.IgniteCause.SPREAD
                || c == BlockIgniteEvent.IgniteCause.LAVA
                || c == BlockIgniteEvent.IgniteCause.LIGHTNING) {
            if (!allowed(e.getBlock().getLocation(), Flag.FIRE_SPREAD)) e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onBurn(BlockBurnEvent e) {
        if (!allowed(e.getBlock().getLocation(), Flag.FIRE_SPREAD)) e.setCancelled(true);
    }

    // ---------- Mobs ----------

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onSpawn(CreatureSpawnEvent e) {
        switch (e.getSpawnReason()) {
            case COMMAND, CUSTOM, SPAWNER_EGG, DISPENSE_EGG -> { return; }
            default -> { }
        }
        if (!allowed(e.getLocation(), Flag.MOB_SPAWN)) e.setCancelled(true);
    }

    // Endermans, withers, etc. cambiando bloques
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onEntityChangeBlock(EntityChangeBlockEvent e) {
        if (e.getEntity() instanceof Player || e.getEntity() instanceof FallingBlock) return;
        if (!allowed(e.getBlock().getLocation(), Flag.BUILD)) e.setCancelled(true);
    }
}
