package com.rfizzle.tribulation.mixin;

import com.rfizzle.tribulation.ability.AbilityManager;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lights every arrow loosed by a skeleton carrying
 * {@link AbilityManager#TAG_FLAME_ARROWS} (tier 4+), the same way vanilla's
 * {@code WitherSkeleton#getArrow} lights its own: a burning arrow sets its
 * target on fire on hit. The skeleton itself is never set alight.
 *
 * <p>Targets {@link AbstractSkeleton#getArrow} because subclasses that override
 * it (Stray, Bogged, Parched) still call {@code super}, so one injection covers
 * every shooter; the tag check keeps it to mobs the ability was applied to.
 */
@Mixin(AbstractSkeleton.class)
public abstract class SkeletonFlameArrowsMixin {

    /** Burn duration given to the arrow, matching vanilla's wither skeleton. */
    private static final float TRIBULATION$ARROW_FIRE_SECONDS = 100.0F;

    @Inject(method = "getArrow", at = @At("RETURN"))
    private void tribulation$igniteArrow(ItemStack projectile, float power, @Nullable ItemStack firingWeapon,
                                         CallbackInfoReturnable<AbstractArrow> cir) {
        AbstractSkeleton self = (AbstractSkeleton) (Object) this;
        if (!self.entityTags().contains(AbilityManager.TAG_FLAME_ARROWS)) return;
        AbstractArrow arrow = cir.getReturnValue();
        if (arrow != null) {
            arrow.igniteForSeconds(TRIBULATION$ARROW_FIRE_SECONDS);
        }
    }
}
