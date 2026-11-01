package com.rfizzle.tribulation.mixin.client;

import com.rfizzle.tribulation.client.BloodMoonClientEffects;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.attribute.EnvironmentAttributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Pulls the sky color toward blood red while a Blood Moon is active. Since 26.x the sky color is
 * the {@code SKY_COLOR} environment attribute instead of {@code ClientLevel#getSkyColor}, so the
 * tint is one more layer appended after the client's own layers (the lightning flash included),
 * the same place the old RETURN inject sat. The sky renderer and the atmospheric fog both read
 * the tinted value.
 */
@Mixin(ClientLevel.class)
public abstract class BloodMoonSkyColorMixin {

    @Inject(method = "addEnvironmentAttributeLayers", at = @At("RETURN"))
    private void tribulation$addBloodMoonSkyLayer(EnvironmentAttributeSystem.Builder builder,
                                                  CallbackInfoReturnable<EnvironmentAttributeSystem.Builder> cir) {
        ClientLevel self = (ClientLevel) (Object) this;
        cir.getReturnValue().addTimeBasedLayer(EnvironmentAttributes.SKY_COLOR, (skyColor, cacheTickId) ->
                BloodMoonClientEffects.isTintActive(self) ? BloodMoonClientEffects.tintSky(skyColor) : skyColor);
    }
}
