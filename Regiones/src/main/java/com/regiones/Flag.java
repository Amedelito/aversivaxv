package com.regiones;

public enum Flag {
    BUILD("build", "Romper bloques"),
    PLACE("place", "Colocar bloques / cubos / cuadros"),
    INTERACT("interact", "Interactuar (puertas, cofres, botones, aldeanos, pisar placas...)"),
    PVP("pvp", "Daño entre jugadores (incluye flechas y TNT lanzado por jugadores)"),
    MOB_SPAWN("mob-spawn", "Aparición de mobs"),
    EXPLOSIONS("explosions", "Explosiones rompen bloques (creeper, TNT, cama, cristal...)"),
    FIRE_SPREAD("fire-spread", "Fuego (propagación, lava, rayo prendiendo bloques)"),
    LIGHTNING("lightning", "Caída de rayos"),
    NOTIFY("notify", "Aviso al entrar/salir (con estado del PVP)");

    private final String id;
    private final String description;

    Flag(String id, String description) {
        this.id = id;
        this.description = description;
    }

    public String id() { return id; }
    public String description() { return description; }

    public static Flag byId(String s) {
        for (Flag f : values()) {
            if (f.id.equalsIgnoreCase(s)) return f;
        }
        return null;
    }
}
