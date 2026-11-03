package com.rfizzle.tribulation.compat.jade;

import com.rfizzle.tribulation.Tribulation;
import com.rfizzle.tribulation.compat.common.MobScalingDataCollector;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IServerDataProvider;

/**
 * Server half of the Jade mob-scaling tooltip: writes the scaling data the client half
 * ({@code MobScalingJadeComponent}, client source set) reads back under the same {@link #UID}.
 *
 * <p>The halves are split because Jade 26.x's {@code ITooltip} extends a client-only Minecraft
 * type, so the tooltip component cannot compile in this common source set.
 */
public final class MobScalingJadeProvider implements IServerDataProvider<EntityAccessor> {

    public static final MobScalingJadeProvider INSTANCE = new MobScalingJadeProvider();

    public static final Identifier UID = Identifier.fromNamespaceAndPath(
            Tribulation.MOD_ID, JadeProviderIds.MOB_SCALING);

    private MobScalingJadeProvider() {}

    @Override
    public void appendServerData(CompoundTag data, EntityAccessor accessor) {
        if (accessor.getEntity() instanceof Mob mob) {
            MobScalingDataCollector.collect(mob, data);
        }
    }

    @Override
    public Identifier getUid() {
        return UID;
    }
}
