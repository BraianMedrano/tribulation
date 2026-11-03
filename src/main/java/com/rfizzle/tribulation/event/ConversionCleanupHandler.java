package com.rfizzle.tribulation.event;

import com.rfizzle.tribulation.champion.ChampionManager;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;

import java.util.ArrayList;
import java.util.List;

/**
 * Keeps a hostile that converts into a non-hostile mob (a cured zombie villager)
 * from looking scaled.
 *
 * <p>Vanilla conversion copies scoreboard tags and the custom name but not
 * attribute modifiers, so the cured villager already has vanilla stats. Left
 * alone, it would still carry {@code tribulation_*} tags (Jade then reports it as
 * scaled) and, for a champion, the generated champion name. Hostile-to-hostile
 * conversions (zombie to drowned, piglin to zombified piglin) are left as vanilla
 * makes them.
 */
public final class ConversionCleanupHandler {

    static final String TAG_PREFIX = "tribulation_";

    private ConversionCleanupHandler() {}

    public static void register() {
        ServerLivingEntityEvents.MOB_CONVERSION.register((previous, converted, params) -> onConversion(previous, converted));
    }

    static void onConversion(Mob previous, Mob converted) {
        if (converted instanceof Enemy) return;

        for (String tag : tribulationTags(converted.entityTags())) {
            converted.removeTag(tag);
        }
        if (ChampionManager.isChampion(previous)) {
            converted.setCustomName(null);
            converted.setCustomNameVisible(false);
        }
    }

    /** The Tribulation-owned tags in {@code tags}, copied so the caller can remove them while iterating. */
    static List<String> tribulationTags(Iterable<String> tags) {
        List<String> owned = new ArrayList<>();
        for (String tag : tags) {
            if (tag.startsWith(TAG_PREFIX)) owned.add(tag);
        }
        return owned;
    }
}
