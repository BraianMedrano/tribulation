package com.rfizzle.tribulation.item;

import com.rfizzle.tribulation.Tribulation;
import com.rfizzle.tribulation.api.TribulationLevelCallback;
import com.rfizzle.tribulation.compat.common.AscendantInfoFormatter;
import com.rfizzle.tribulation.compat.common.TooltipDetailProvider;
import com.rfizzle.tribulation.config.TribulationConfig;
import com.rfizzle.tribulation.data.PlayerDifficultyState;
import com.rfizzle.tribulation.stat.TribulationStats;
import com.rfizzle.tribulation.network.TribulationNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/**
 * Consumable shard that raises the using player's difficulty level by
 * {@link TribulationConfig.Ascension#raisePower}, capped at
 * {@link TribulationConfig.General#maxLevel} — the dark twin of the Shatter
 * Shard. The raised level (tougher mobs, champion spawns, richer XP/loot) is
 * the reward; consumption is costless by default. When
 * {@code ascension.sideEffects} is enabled the user also gains a short
 * Strength II / Resistance II buff window. The stack is consumed in
 * survival/adventure (creative keeps the item).
 */
public class AscendantShardItem extends Item {
    public static final int SIDE_EFFECT_DURATION_TICKS = 200;
    public static final int SIDE_EFFECT_AMPLIFIER = 1;

    public AscendantShardItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag tooltipFlag) {
        tooltip.accept(Component.translatable("item.tribulation.ascendant_shard.tooltip"));
        if (TooltipDetailProvider.isShiftHeld()) {
            tooltip.accept(Component.empty());
            for (Component line : AscendantInfoFormatter.infoLines(TooltipDetailProvider.effectiveConfig())) {
                tooltip.accept(line.copy().withStyle(ChatFormatting.GRAY));
            }
        } else {
            tooltip.accept(Component.translatable("item.tribulation.ascendant_shard.info.hold_shift")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide()) {
            // Let the client animate the swing; all state mutation happens server-side.
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        TribulationConfig cfg = Tribulation.getConfig();
        if (cfg == null || !cfg.ascension.enabled) {
            return InteractionResult.PASS;
        }

        MinecraftServer server = serverPlayer.level().getServer();
        if (server == null) {
            return InteractionResult.PASS;
        }

        try {
            PlayerDifficultyState state = PlayerDifficultyState.getOrCreate(server);
            int before = state.getLevel(serverPlayer.getUUID());
            int after = state.raisePlayerLevel(
                    serverPlayer.getUUID(),
                    cfg.ascension.raisePower,
                    cfg.general.maxLevel
            );

            if (after <= before) {
                // No upward movement — already at (or above) the ceiling, or a
                // zero raise. Never consume the item or push the level down: an
                // ascendant shard must only ever raise. Keep the item and warn.
                serverPlayer.sendOverlayMessage(
                        Component.translatable("item.tribulation.ascendant_shard.at_ceiling", before));
                return InteractionResult.PASS;
            }

            if (cfg.ascension.sideEffects) {
                applySideEffects(serverPlayer);
            }

            serverPlayer.awardStat(TribulationStats.ASCENDANT_SHARDS_USED);
            stack.consume(1, serverPlayer);
            com.rfizzle.tribulation.advancement.TribulationCriteria.ASCENDANT_SHARD_USED.trigger(serverPlayer);

            level.playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                    SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 0.8f, 0.8f);

            TribulationNetworking.syncLevel(serverPlayer);

            TribulationLevelCallback.EVENT.invoker().onLevelChanged(serverPlayer, before, after);
            serverPlayer.sendOverlayMessage(
                    Component.translatable("item.tribulation.ascendant_shard.used", before, after));
            Tribulation.LOGGER.debug(
                    "Ascendant shard used: {} raised from level {} to {}",
                    serverPlayer.getGameProfile().name(), before, after
            );
        } catch (Exception e) {
            Tribulation.LOGGER.error("Failed to apply ascendant shard for {}", player, e);
            return InteractionResult.FAIL;
        }

        return InteractionResult.CONSUME;
    }

    private static void applySideEffects(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.STRENGTH,
                SIDE_EFFECT_DURATION_TICKS, SIDE_EFFECT_AMPLIFIER, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE,
                SIDE_EFFECT_DURATION_TICKS, SIDE_EFFECT_AMPLIFIER, false, true, true));
    }
}
