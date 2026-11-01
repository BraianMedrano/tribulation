package com.rfizzle.tribulation.mixin.client;

import com.rfizzle.tribulation.client.BloodMoonClientEffects;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.world.level.material.FogType;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tints the atmosphere fog toward blood red while a Blood Moon is active. Injects at the tail of
 * {@code computeFogColor}, after the void, boss and night-vision adjustments, where the 1.21.1
 * mixin sat before the final {@code clearColor}. Submerged cameras keep vanilla fog.
 */
@Mixin(FogRenderer.class)
public abstract class BloodMoonFogMixin {

    @Inject(method = "computeFogColor", at = @At("TAIL"))
    private void tribulation$tintFog(Camera camera, float partialTicks, ClientLevel level, int renderDistance,
                                     float darkenWorldAmount, Vector4f dest, CallbackInfo ci) {
        if (camera.getFluidInCamera() != FogType.NONE) return;
        if (!BloodMoonClientEffects.isTintActive(level)) return;
        float[] tinted = BloodMoonClientEffects.tintFog(dest.x, dest.y, dest.z);
        dest.set(tinted[0], tinted[1], tinted[2], dest.w);
    }
}
