package com.rfizzle.tribulation.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.rfizzle.tribulation.event.PackTacticsHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Enlarges natural spawn groups of pack-tactics-eligible mob types above the
 * configured tier threshold. Vanilla rolls the group size once per group as
 * {@code spawnerData.count().sample(random)}; adding the bonus to that single
 * sample shifts the whole group size by exactly the bonus, the same result the
 * pre-26.x {@code minCount}/{@code maxCount} redirects produced. That is the
 * only {@code IntProvider.sample} call in the method, so no other logic is
 * affected.
 */
@Mixin(NaturalSpawner.class)
public abstract class NaturalSpawnerGroupSizeMixin {

    @ModifyExpressionValue(
            method = "spawnCategoryForPosition(Lnet/minecraft/world/entity/MobCategory;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/NaturalSpawner$SpawnPredicate;Lnet/minecraft/world/level/NaturalSpawner$AfterSpawnCallback;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/valueproviders/IntProvider;sample(Lnet/minecraft/util/RandomSource;)I"))
    private static int tribulation$boostGroupSize(int original,
                                                  MobCategory category, ServerLevel level, ChunkAccess chunk, BlockPos pos,
                                                  NaturalSpawner.SpawnPredicate filter, NaturalSpawner.AfterSpawnCallback callback,
                                                  @Local MobSpawnSettings.SpawnerData data) {
        return original + PackTacticsHandler.spawnGroupBonus(data.type(), level, pos);
    }
}
