package com.rfizzle.tribulation.mixin.client;

import com.rfizzle.tribulation.client.BloodMoonClientEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SkyRenderer;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Tints the celestial bodies red while a Blood Moon is active. In 1.21.1 the sun and the moon
 * shared one shader color; 26.x passes a per-body color modulator to {@code writeTransform}, so
 * both draws are targeted to keep that. Stars keep their vanilla color, and the alpha (rain fade)
 * is left untouched.
 */
@Mixin(SkyRenderer.class)
public abstract class BloodMoonMoonTintMixin {

    @ModifyArg(
            method = {"renderSun", "renderMoon"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/DynamicGpuData;writeTransform(Lorg/joml/Matrix4f;Lorg/joml/Vector4f;)Lcom/mojang/renderpearl/api/buffers/GpuBufferSlice;"),
            index = 1,
            require = 2)
    private Vector4f tribulation$tintCelestialBody(Vector4f color) {
        if (!BloodMoonClientEffects.isTintActive(Minecraft.getInstance().level)) return color;
        // The modulator is a fresh vector built for this single draw.
        color.y = BloodMoonClientEffects.tintMoonGreen(color.y);
        color.z = BloodMoonClientEffects.tintMoonBlue(color.z);
        return color;
    }
}
