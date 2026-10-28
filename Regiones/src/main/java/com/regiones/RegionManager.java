package com.regiones;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RegionManager {
    private final Regiones plugin;
    private final File file;
    private final Map<String, Region> regions = new LinkedHashMap<>();

    public RegionManager(Regiones plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "regions.yml");
    }

    public void load() {
        regions.clear();
        if (!file.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection sec = y.getConfigurationSection("regions");
        if (sec == null) return;
        for (String key : sec.getKeys(false)) {
            ConfigurationSection s = sec.getConfigurationSection(key);
            if (s == null) continue;
            Region r = new Region(key, s.getString("world", "world"),
                    s.getInt("min.x"), s.getInt("min.y"), s.getInt("min.z"),
                    s.getInt("max.x"), s.getInt("max.y"), s.getInt("max.z"));
            ConfigurationSection f = s.getConfigurationSection("flags");
            if (f != null) {
                for (String id : f.getKeys(false)) r.flags.put(id, f.getBoolean(id));
            }
            regions.put(key, r);
        }
    }

    public void save() {
        YamlConfiguration y = new YamlConfiguration();
        for (Region r : regions.values()) {
            String p = "regions." + r.name + ".";
            y.set(p + "world", r.world);
            y.set(p + "min.x", r.minX); y.set(p + "min.y", r.minY); y.set(p + "min.z", r.minZ);
            y.set(p + "max.x", r.maxX); y.set(p + "max.y", r.maxY); y.set(p + "max.z", r.maxZ);
            for (Map.Entry<String, Boolean> e : r.flags.entrySet()) {
                y.set(p + "flags." + e.getKey(), e.getValue());
            }
        }
        try {
            plugin.getDataFolder().mkdirs();
            y.save(file);
        } catch (IOException ex) {
            plugin.getLogger().severe("No se pudo guardar regions.yml: " + ex.getMessage());
        }
    }

    public void add(Region r) { regions.put(r.name, r); save(); }
    public boolean remove(String name) {
        boolean ok = regions.remove(name.toLowerCase()) != null;
        if (ok) save();
        return ok;
    }
    public Region get(String name) { return regions.get(name.toLowerCase()); }
    public Collection<Region> all() { return regions.values(); }

    public List<Region> at(Location l) {
        List<Region> out = new ArrayList<>();
        for (Region r : regions.values()) {
            if (r.contains(l)) out.add(r);
        }
        return out;
    }

    /** Permitido salvo que alguna región en esa posición tenga la flag en deny. */
    public boolean isAllowed(Location l, Flag f) {
        for (Region r : regions.values()) {
            if (r.contains(l) && !r.flag(f)) return false;
        }
        return true;
    }
}
