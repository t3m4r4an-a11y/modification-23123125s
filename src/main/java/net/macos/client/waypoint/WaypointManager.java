package net.macos.client.waypoint;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.macos.client.MacClient;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class WaypointManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File FILE = new File("config/macclient_waypoints.json");
    private static final Type LIST_TYPE = new TypeToken<List<Waypoint>>() {}.getType();

    public static final List<Waypoint> WAYPOINTS = new ArrayList<>();

    public static void load() {
        try {
            if (!FILE.exists()) return;
            FileReader reader = new FileReader(FILE);
            List<Waypoint> loaded = GSON.fromJson(reader, LIST_TYPE);
            reader.close();
            if (loaded != null) {
                WAYPOINTS.clear();
                WAYPOINTS.addAll(loaded);
            }
        } catch (Exception e) {
            MacClient.LOGGER.error("Failed to load waypoints", e);
        }
    }

    public static void save() {
        try {
            FILE.getParentFile().mkdirs();
            FileWriter writer = new FileWriter(FILE);
            GSON.toJson(WAYPOINTS, writer);
            writer.close();
        } catch (Exception e) {
            MacClient.LOGGER.error("Failed to save waypoints", e);
        }
    }

    public static boolean add(String name, double x, double y, double z, String dim, int color) {
        // Удалить старую с тем же именем
        WAYPOINTS.removeIf(w -> w.name.equalsIgnoreCase(name));
        WAYPOINTS.add(new Waypoint(name, x, y, z, dim, color));
        save();
        return true;
    }

    public static boolean remove(String name) {
        boolean removed = WAYPOINTS.removeIf(w -> w.name.equalsIgnoreCase(name));
        if (removed) save();
        return removed;
    }

    public static List<Waypoint> getAll() {
        return WAYPOINTS;
    }
}