package com.rfizzle.tribulation.mixin;

import com.rfizzle.tribulation.Tribulation;
import com.rfizzle.tribulation.ability.SkeletonWeaponSwitchGoal;
import com.rfizzle.tribulation.config.TribulationConfig;
import com.rfizzle.tribulation.config.TribulationConfig.SpecialSkeletons;
import com.rfizzle.tribulation.event.SkeletonVariantHandler;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Overrides the bow draw cadence for Deadeye/Brute skeleton variants. Vanilla
 * {@code reassessWeaponGoal()} hardcodes the bow goal's attack interval
 * ({@code getAttackInterval()} = 40 on Easy/Normal, {@code getHardAttackInterval()}
 * = 20 on Hard); no attribute drives it in 1.21.1.
 *
 * <p>Injecting at {@code RETURN} runs after vanilla has (re)configured the goal,
 * so the override always wins — including when the {@code skeletonSwordSwitch}
 * ability or a later weapon swap triggers another reassess. When the skeleton
 * carries a variant tag and is holding a bow (the bow goal is the active goal),
 * the configured per-variant interval replaces vanilla's value. The 20-tick
 * minimum draw still floors shots at ~1/sec, so only the gap between shots moves.
 */
@Mixin(AbstractSkeleton.class)
public abstract class AbstractSkeletonMixin {

    /**
     * Registers the {@code skeleton_sword_switch} weapon swap on every skeleton.
     * The goal is inert unless the ability stashed a second weapon, and it
     * declares no flags, so its priority only orders it ahead of vanilla's
     * attack goals (4) within a tick; it never blocks them.
     */
    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void tribulation$addWeaponSwitchGoal(CallbackInfo ci) {
        AbstractSkeleton self = (AbstractSkeleton) (Object) this;
        ((MobGoalSelectorAccessor) self).tribulation$getGoalSelector()
                .addGoal(1, new SkeletonWeaponSwitchGoal(self));
    }

    @Inject(method = "reassessWeaponGoal", at = @At("RETURN"))
    private void tribulation$applyVariantBowInterval(CallbackInfo ci) {
        AbstractSkeleton self = (AbstractSkeleton) (Object) this;

        // The bow goal is only the active goal when a bow is held; otherwise the
        // melee goal was added and the interval override is meaningless.
        if (!self.isHolding(Items.BOW)) return;

        TribulationConfig cfg = Tribulation.getConfig();
        if (cfg == null) return;
        SpecialSkeletons sk = cfg.specialSkeletons;
        if (sk == null || !sk.enabled) return;

        Integer interval = null;
        if (self.entityTags().contains(SkeletonVariantHandler.DEADEYE_TAG)) {
            interval = sk.deadeyeSkeletonAttackInterval;
        } else if (self.entityTags().contains(SkeletonVariantHandler.BRUTE_TAG)) {
            interval = sk.bruteSkeletonAttackInterval;
        }
        if (interval == null) return;

        RangedBowAttackGoal<AbstractSkeleton> bowGoal =
                ((AbstractSkeletonAccessor) self).tribulation$getBowGoal();
        if (bowGoal != null) {
            bowGoal.setMinAttackInterval(interval);
        }
    }
}
