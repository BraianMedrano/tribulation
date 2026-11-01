package com.rfizzle.tribulation.advancement;

import com.rfizzle.tribulation.Tribulation;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * Registry for Tribulation's custom advancement criterion triggers. Each
 * trigger is registered under a {@code tribulation:*} id and fired from the
 * handler that owns the matching state change. {@link #register()} must run in
 * {@code onInitialize} before any advancement referencing these triggers is
 * loaded.
 */
public final class TribulationCriteria {
    public static final TierReachedCriterion TIER_REACHED = new TierReachedCriterion();
    public static final SimplePlayerTrigger SOULBOUND_SURVIVED = new SimplePlayerTrigger();
    public static final SimplePlayerTrigger SHATTER_SHARD_USED = new SimplePlayerTrigger();
    public static final SimplePlayerTrigger ASCENDANT_SHARD_USED = new SimplePlayerTrigger();
    public static final SimplePlayerTrigger HEART_FRAGMENT_USED = new SimplePlayerTrigger();
    public static final SimplePlayerTrigger TIER_FIVE_MOB_KILLED = new SimplePlayerTrigger();

    private TribulationCriteria() {}

    public static void register() {
        Registry.register(BuiltInRegistries.TRIGGER_TYPES, Tribulation.id("tier_reached"), TIER_REACHED);
        Registry.register(BuiltInRegistries.TRIGGER_TYPES, Tribulation.id("soulbound_survived"), SOULBOUND_SURVIVED);
        Registry.register(BuiltInRegistries.TRIGGER_TYPES, Tribulation.id("shatter_shard_used"), SHATTER_SHARD_USED);
        Registry.register(BuiltInRegistries.TRIGGER_TYPES, Tribulation.id("ascendant_shard_used"), ASCENDANT_SHARD_USED);
        Registry.register(BuiltInRegistries.TRIGGER_TYPES, Tribulation.id("heart_fragment_used"), HEART_FRAGMENT_USED);
        Registry.register(BuiltInRegistries.TRIGGER_TYPES, Tribulation.id("tier_five_mob_killed"), TIER_FIVE_MOB_KILLED);
    }
}
