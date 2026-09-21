package net.macos.client.waypoint;

public class Waypoint {
    public String name;
    public double x, y, z;
    public String dimension;
    public int color;

    public Waypoint() {} // для GSON

    public Waypoint(String name, double x, double y, double z, String dimension, int color) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.z = z;
        this.dimension = dimension;
        this.color = color;
    }

    public double distanceTo(double px, double py, double pz) {
        double dx = x - px;
        double dy = y - py;
        double dz = z - pz;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}