package net.macos.client.utils;

import net.macos.client.MacClient;
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

    public static final Map<String, SoundEvent> HIT_EVENTS  = new HashMap<>();
    public static final Map<String, SoundEvent> KILL_EVENTS = new HashMap<>();

    public static void init() {
        for (String path : HIT_SOUNDS) {
            Identifier id  = new Identifier("macclient", path);
            SoundEvent event = SoundEvent.of(id);
            Registry.register(Registries.SOUND_EVENT, id, event);

            String key = path.substring(path.indexOf('.') + 1);
            HIT_EVENTS.put(key, event);
            KILL_EVENTS.put(key, event);
        }

        Identifier rustId   = new Identifier("macclient", "kill.rust");
        SoundEvent rustEvent = SoundEvent.of(rustId);
        Registry.register(Registries.SOUND_EVENT, rustId, rustEvent);
        KILL_EVENTS.put("rust", rustEvent);

        MacClient.LOGGER.info("SoundRegistry initialized: {} hit, {} kill",
                HIT_EVENTS.size(), KILL_EVENTS.size());
    }
}