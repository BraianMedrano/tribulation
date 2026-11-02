package com.rfizzle.tribulation.compat.jade;

import com.rfizzle.tribulation.Tribulation;
import net.minecraft.world.entity.Mob;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

@WailaPlugin("tribulation")
public final class JadeTribulationPlugin implements IWailaPlugin {

    /**
     * The tooltip component lives in the client source set: Jade 26.x's {@code ITooltip} extends a
     * client-only Minecraft type, so it cannot compile here. Jade calls {@link #registerClient}
     * only on a client, where this class exists, so it is reached by name. Both names are
     * constants so {@code ManifestEntrypointsCompiledTest} can read them from this class file and
     * check that the target is a {@code public static void register(IWailaClientRegistration)}.
     */
    private static final String CLIENT_COMPONENT = "com.rfizzle.tribulation.compat.jade.MobScalingJadeComponent";
    private static final String CLIENT_REGISTER_METHOD = "register";

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerEntityDataProvider(MobScalingJadeProvider.INSTANCE, Mob.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        try {
            MethodHandles.publicLookup()
                    .findStatic(Class.forName(CLIENT_COMPONENT), CLIENT_REGISTER_METHOD,
                            MethodType.methodType(void.class, IWailaClientRegistration.class))
                    .invoke(registration);
        } catch (Throwable t) {
            // A failure here only loses the mob-scaling tooltip; never take Jade's plugin load down.
            Tribulation.LOGGER.warn("Tribulation Jade tooltip component {} unavailable; tooltip disabled",
                    CLIENT_COMPONENT, t);
        }
    }
}
