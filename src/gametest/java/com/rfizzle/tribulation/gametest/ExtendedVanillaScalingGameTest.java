package com.rfizzle.tribulation.gametest;

import com.rfizzle.concord.gametest.MockPlayers;
import com.rfizzle.tribulation.Tribulation;
import com.rfizzle.tribulation.config.TribulationConfig;
import com.rfizzle.tribulation.data.PlayerDifficultyState;
import com.rfizzle.tribulation.data.TribulationAttachments;
import com.rfizzle.tribulation.event.MobScalingHandler;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.ArrayList;
import java.util.List;

/**
 * End-to-end checks for the vanilla hostiles added to the per-mob scaling list,
 * and for the cleanup that keeps a cured zombie villager from looking scaled.
 */
public class ExtendedVanillaScalingGameTest {

    private static final int PLAYER_LEVEL = 100;
    private static final BlockPos SPAWN = new BlockPos(1, 2, 1);

    private static final List<EntityType<? extends Mob>> ADDED = List.of(
            EntityTypes.PARCHED, EntityTypes.ZOMBIE_VILLAGER, EntityTypes.PIGLIN_BRUTE,
            EntityTypes.ENDERMAN, EntityTypes.BLAZE, EntityTypes.BREEZE, EntityTypes.EVOKER);

    @GameTest(structure = "tribulation:empty_3x3")
    public void addedHostiles_getTimeAxisHealth(GameTestHelper helper) {
        TribulationConfig cfg = Tribulation.getConfig();
        placePlayerAtLevel(helper, PLAYER_LEVEL, cfg);

        boolean savedDist = cfg.distanceScaling.enabled;
        boolean savedHeight = cfg.heightScaling.enabled;
        boolean savedChampions = cfg.champions.enabled;
        cfg.distanceScaling.enabled = false;
        cfg.heightScaling.enabled = false;
        cfg.champions.enabled = false;

        List<Mob> spawned = new ArrayList<>();
        try {
            // MobScalingHandler runs synchronously on ENTITY_LOAD, so modifiers are
            // frozen on each mob before the config is restored below.
            for (EntityType<? extends Mob> type : ADDED) {
                spawned.add(helper.spawnWithNoFreeWill(type, SPAWN));
            }
        } finally {
            cfg.distanceScaling.enabled = savedDist;
            cfg.heightScaling.enabled = savedHeight;
            cfg.champions.enabled = savedChampions;
        }

        helper.succeedWhen(() -> {
            for (Mob mob : spawned) {
                String key = EntityType.getKey(mob.getType()).getPath();
                helper.assertTrue(mob.entityTags().contains(MobScalingHandler.PROCESSED_TAG),
                        key + " missing " + MobScalingHandler.PROCESSED_TAG);
                TribulationConfig.MobScaling rates = cfg.scaling.get(key);
                double base = mob.getAttribute(Attributes.MAX_HEALTH).getBaseValue();
                double expected = base * (1.0 + Math.min(rates.healthRate * PLAYER_LEVEL, rates.healthCap));
                helper.assertTrue(Math.abs(mob.getMaxHealth() - expected) < 0.01,
                        key + " max health " + mob.getMaxHealth() + ", expected " + expected);
            }
        });
    }

    @GameTest(structure = "tribulation:empty_3x3")
    public void curedZombieVillager_dropsTribulationTagsAndChampionName(GameTestHelper helper) {
        Mob zombieVillager = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE_VILLAGER, SPAWN);
        zombieVillager.addTag(MobScalingHandler.PROCESSED_TAG);
        zombieVillager.setAttached(TribulationAttachments.CHAMPION_AFFIXES, List.of("thorns"));
        zombieVillager.setCustomName(Component.literal("Thorned Zombie Villager Champion"));
        zombieVillager.setCustomNameVisible(true);

        Mob villager = zombieVillager.convertTo(EntityTypes.VILLAGER,
                ConversionParams.single(zombieVillager, false, false), converted -> { });

        helper.succeedWhen(() -> {
            helper.assertTrue(villager != null, "conversion produced no villager");
            for (String tag : villager.entityTags()) {
                helper.assertFalse(tag.startsWith("tribulation_"), "cured villager kept tag " + tag);
            }
            helper.assertTrue(villager.getCustomName() == null,
                    "cured villager kept the champion name " + villager.getCustomName());
            helper.assertFalse(villager.isCustomNameVisible(), "cured villager still shows a name tag");
        });
    }

    @GameTest(structure = "tribulation:empty_3x3")
    public void curedZombieVillager_keepsPlayerNameTag(GameTestHelper helper) {
        // Only a champion's generated name is cleared: a name a player gave with a
        // name tag survives the cure, as in vanilla.
        Mob zombieVillager = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE_VILLAGER, SPAWN);
        zombieVillager.setCustomName(Component.literal("Bob"));

        Mob villager = zombieVillager.convertTo(EntityTypes.VILLAGER,
                ConversionParams.single(zombieVillager, false, false), converted -> { });

        helper.succeedWhen(() -> {
            helper.assertTrue(villager != null, "conversion produced no villager");
            helper.assertTrue(villager.getCustomName() != null
                            && "Bob".equals(villager.getCustomName().getString()),
                    "cured villager lost its player-given name, had " + villager.getCustomName());
        });
    }

    @GameTest(structure = "tribulation:empty_3x3")
    public void zombieToDrowned_keepsTribulationTags(GameTestHelper helper) {
        // Hostile-to-hostile conversions keep vanilla's tag copy untouched.
        Mob zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, SPAWN);
        zombie.addTag(MobScalingHandler.PROCESSED_TAG);

        Mob drowned = zombie.convertTo(EntityTypes.DROWNED,
                ConversionParams.single(zombie, true, true), converted -> { });

        helper.succeedWhen(() -> {
            helper.assertTrue(drowned != null, "conversion produced no drowned");
            helper.assertTrue(drowned.entityTags().contains(MobScalingHandler.PROCESSED_TAG),
                    "drowned lost " + MobScalingHandler.PROCESSED_TAG);
        });
    }

    private static void placePlayerAtLevel(GameTestHelper helper, int level, TribulationConfig cfg) {
        ServerPlayer player = MockPlayers.serverPlayerInLevel(helper);
        BlockPos playerAbs = helper.absolutePos(new BlockPos(0, 2, 1));
        player.teleportTo(playerAbs.getX() + 0.5, playerAbs.getY(), playerAbs.getZ() + 0.5);
        PlayerDifficultyState.getOrCreate(helper.getLevel().getServer())
                .setLevel(player.getUUID(), level, cfg.general.maxLevel);
    }
}
