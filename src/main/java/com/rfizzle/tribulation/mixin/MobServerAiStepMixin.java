package com.rfizzle.tribulation.mixin;

import com.rfizzle.tribulation.ability.SkeletonWeaponSwitcher;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Runs the {@code skeleton_sword_switch} weapon swap at the end of
 * {@link Mob#serverAiStep}, after the goal selector has finished iterating its
 * goals. Swapping from inside a goal made vanilla's {@code reassessWeaponGoal}
 * edit the goal set mid-iteration and crashed the server; here the edit lands
 * between AI steps.
 *
 * <p>The target is {@link Mob} because {@code serverAiStep} is declared (final)
 * there and no skeleton overrides it; the {@code instanceof} narrows at runtime.
 */
@Mixin(Mob.class)
public abstract class MobServerAiStepMixin {

    @Inject(method = "serverAiStep", at = @At("TAIL"))
    private void tribulation$switchSkeletonWeapon(CallbackInfo ci) {
        if ((Object) this instanceof AbstractSkeleton skeleton) {
            SkeletonWeaponSwitcher.tick(skeleton);
        }
    }
}
