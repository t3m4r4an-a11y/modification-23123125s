package net.macos.client.particle;

public enum KillEffectType {
    RING("ring"),
    LIGHTNING("lightning"),
    SPIRAL("spiral"),
    GHOST("ghost"),
    NONE("none");

    public final String id;

    KillEffectType(String id) {
        this.id = id;
    }

    public static KillEffectType fromName(String name) {
        if (name == null) return RING;
        for (KillEffectType t : values()) {
            if (t.id.equalsIgnoreCase(name)) return t;
        }
        return RING;
    }
}