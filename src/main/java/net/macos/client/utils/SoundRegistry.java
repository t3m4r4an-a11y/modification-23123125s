package net.macos.client.utils;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SoundRegistry {

    private static final List<String> HIT_SOUNDS = List.of(
        "hit.aimbooster", "hit.applepay", "hit.bonk", "hit.boykisser",
        "hit.brick", "hit.bring", "hit.bump", "hit.click", "hit.coin",
        "hit.glass", "hit.hitsound", "hit.magicsquash", "hit.meow",
        "hit.moan", "hit.nya", "hit.osu", "hit.pop", "hit.schoolboy",
        "hit.skeet", "hit.slap", "hit.soft", "hit.squash", "hit.tf2crit",
        "hit.tung", "hit.uwu"
    );

    private static final List<String> KILL_SOUNDS = List.of(
        "kill.rust"
    );

    public static final Map<String, SoundEvent> HIT_EVENTS = new HashMap<>();
    public static final Map<String, SoundEvent> KILL_EVENTS = new HashMap<>();

    public static void init() {
        System.out.println("[SoundRegistry] init START, hit count=" + HIT_SOUNDS.size());

        for (String path : HIT_SOUNDS) {
            register(path, HIT_EVENTS);
        }
        System.out.println("[SoundRegistry] HIT_EVENTS size=" + HIT_EVENTS.size());

        for (String path : HIT_SOUNDS) {
            String key = path.substring(path.indexOf('.') + 1);
            KILL_EVENTS.put(key, HIT_EVENTS.get(key));
        }

        // Rust headshot
        Identifier rustId = new Identifier("macclient", "kill.rust");
        SoundEvent rustEvent = SoundEvent.of(rustId);
        Registry.register(Registries.SOUND_EVENT, rustId, rustEvent);
        KILL_EVENTS.put("rust", rustEvent);

        System.out.println("[SoundRegistry] KILL_EVENTS size=" + KILL_EVENTS.size());
    }

    private static void register(String path, Map<String, SoundEvent> map) {
        Identifier id = new Identifier("macclient", path);
        SoundEvent event = SoundEvent.of(id);
        Registry.register(Registries.SOUND_EVENT, id, event);
        String key = path.substring(path.indexOf('.') + 1);
        map.put(key, event);
    }
}