package com.rfizzle.tribulation.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.rfizzle.tribulation.scaling.RaidCreationContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Publishes the raid's {@link ServerLevel} while a live {@link Raid} is constructed, so
 * {@link RaidScalingMixin} can resolve the targeted players' tier on the constructor's
 * {@code getNumGroups} call. {@code getOrCreateRaid} is the only place vanilla builds a live
 * raid; the NBT codec path restores {@code numGroups} without calling {@code getNumGroups}.
 */
@Mixin(Raids.class)
public abstract class RaidsMixin {

    @WrapOperation(
            method = "getOrCreateRaid",
            at = @At(
                    value = "NEW",
                    target = "(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/Difficulty;)Lnet/minecraft/world/entity/raid/Raid;"
            )
    )
    private Raid tribulation$publishLevel(BlockPos pos, Difficulty difficulty, Operation<Raid> original,
                                          ServerLevel level, BlockPos raidPos) {
        return RaidCreationContext.withLevel(level, () -> original.call(pos, difficulty));
    }
}
