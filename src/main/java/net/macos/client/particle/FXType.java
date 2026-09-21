package net.macos.client.particle;

public enum FXType {
    SPARK("spark"),
    STAR("star"),
    POINT("point"),
    RHOMBUS("rhombus"),
    SNOWFLAKE("snowflake"),
    CROWN("crown"),
    HEART("heart"),
    DOLLAR("dollar"),
    GLOW("glow"),
    LIGHTNING("lightning");

    public final String texture;

    FXType(String texture) {
        this.texture = texture;
    }

    public static FXType fromName(String name) {
        if (name == null) return SPARK;
        for (FXType t : values()) {
            if (t.texture.equalsIgnoreCase(name)) return t;
        }
        return SPARK;
    }
}