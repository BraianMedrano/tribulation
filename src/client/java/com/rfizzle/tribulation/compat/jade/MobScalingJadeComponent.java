package com.rfizzle.tribulation.compat.jade;

import com.rfizzle.tribulation.compat.common.TribulationTooltipFormatter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.config.IPluginConfig;

/**
 * Client half of the Jade mob-scaling tooltip: renders the data {@link MobScalingJadeProvider}
 * wrote on the server, paired with it through the shared {@link MobScalingJadeProvider#UID}.
 * Registered by {@code JadeTribulationPlugin#registerClient}, which reaches {@link #register} by
 * name because the plugin compiles in the common source set.
 */
public final class MobScalingJadeComponent implements IEntityComponentProvider {

    private static final MobScalingJadeComponent INSTANCE = new MobScalingJadeComponent();

    private MobScalingJadeComponent() {}

    public static void register(IWailaClientRegistration registration) {
        registration.registerEntityComponent(INSTANCE, Mob.class);
    }

    @Override
    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        for (String line : TribulationTooltipFormatter.format(data)) {
            tooltip.add(Component.literal(line));
        }
    }

    @Override
    public Identifier getUid() {
        return MobScalingJadeProvider.UID;
    }
}
