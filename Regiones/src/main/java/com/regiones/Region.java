package com.regiones;

import org.bukkit.Location;

import java.util.LinkedHashMap;
import java.util.Map;

public class Region {
    public final String name;
    public final String world;
    public final int minX, minY, minZ, maxX, maxY, maxZ;
    // id de flag -> true (allow) / false (deny). Si no está, se permite por defecto.
    public final Map<String, Boolean> flags = new LinkedHashMap<>();

    public Region(String name, String world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        this.name = name;
        this.world = world;
        this.minX = minX; this.minY = minY; this.minZ = minZ;
        this.maxX = maxX; this.maxY = maxY; this.maxZ = maxZ;
    }

    public boolean contains(Location l) {
        if (l == null || l.getWorld() == null || !l.getWorld().getName().equals(world)) return false;
        int x = l.getBlockX(), y = l.getBlockY(), z = l.getBlockZ();
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    public boolean flag(Flag f) {
        return flags.getOrDefault(f.id(), true);
    }

    public long volume() {
        return (long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
    }
}
