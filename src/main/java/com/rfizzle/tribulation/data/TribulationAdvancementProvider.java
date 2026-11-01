package com.rfizzle.tribulation.data;

import com.rfizzle.tribulation.Tribulation;
import com.rfizzle.tribulation.advancement.SimplePlayerTrigger;
import com.rfizzle.tribulation.advancement.TierReachedCriterion;
import com.rfizzle.tribulation.advancement.TribulationCriteria;
import com.rfizzle.tribulation.item.TribulationItems;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricAdvancementProvider;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.advancements.triggers.PlayerTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Generates the {@code tribulation} advancement tab: a chained tier ladder
 * (Tier 1 → Tier 5) plus four milestone leaves hanging off the root. Output
 * lands in {@code src/main/generated/data/tribulation/advancement/} and is
 * never hand-edited — rerun {@code ./gradlew runDatagen} after any change.
 */
public class TribulationAdvancementProvider extends FabricAdvancementProvider {
    private static final Identifier BACKGROUND =
            Identifier.withDefaultNamespace("gui/advancements/backgrounds/stone");

    public TribulationAdvancementProvider(FabricPackOutput output,
                                          CompletableFuture<HolderLookup.Provider> registryLookup) {
        super(output, registryLookup);
    }

    @Override
    public void generateAdvancement(HolderLookup.Provider registryLookup, Consumer<AdvancementHolder> consumer) {
        AdvancementHolder root = Advancement.Builder.advancement()
                .rootDisplay(
                        TribulationItems.SHATTER_SHARD,
                        title("root"),
                        description("root"),
                        BACKGROUND,
                        AdvancementType.TASK,
                        // showToast=true so the tick-granted root pops a toast on
                        // first join, acknowledging the mod is installed. Chat
                        // announce and hidden stay off.
                        true, false, false)
                .addCriterion("tick", CriteriaTriggers.TICK.createCriterion(
                        new PlayerTrigger.TriggerInstance(Optional.empty())))
                .build(id("root"));
        consumer.accept(root);

        AdvancementHolder tier1 = tierAdvancement(consumer, root, 1, Items.STONE_SWORD);
        AdvancementHolder tier2 = tierAdvancement(consumer, tier1, 2, Items.IRON_SWORD);
        AdvancementHolder tier3 = tierAdvancement(consumer, tier2, 3, Items.DIAMOND_SWORD);
        AdvancementHolder tier4 = tierAdvancement(consumer, tier3, 4, Items.NETHERITE_SWORD);
        tierAdvancement(consumer, tier4, 5, Items.WITHER_SKELETON_SKULL);

        leaf(consumer, root, "soulbound_survived", Items.SOUL_LANTERN, AdvancementType.GOAL,
                "soulbound_survived",
                TribulationCriteria.SOULBOUND_SURVIVED.createCriterion(simple()));
        leaf(consumer, root, "shatter_shard_used", TribulationItems.SHATTER_SHARD, AdvancementType.TASK,
                "shatter_shard_used",
                TribulationCriteria.SHATTER_SHARD_USED.createCriterion(simple()));
        leaf(consumer, root, "ascendant_shard_used", TribulationItems.ASCENDANT_SHARD, AdvancementType.TASK,
                "ascendant_shard_used",
                TribulationCriteria.ASCENDANT_SHARD_USED.createCriterion(simple()));
        leaf(consumer, root, "heart_fragment_used", TribulationItems.HEART_FRAGMENT, AdvancementType.GOAL,
                "heart_fragment_used",
                TribulationCriteria.HEART_FRAGMENT_USED.createCriterion(simple()));
        leaf(consumer, root, "tier_five_mob_killed", Items.NETHERITE_SWORD, AdvancementType.CHALLENGE,
                "tier_five_mob_killed",
                TribulationCriteria.TIER_FIVE_MOB_KILLED.createCriterion(simple()));
    }

    private static AdvancementHolder tierAdvancement(Consumer<AdvancementHolder> consumer, AdvancementHolder parent,
                                                     int tier, ItemLike icon) {
        String key = "tier_" + tier;
        AdvancementHolder holder = Advancement.Builder.advancement()
                .parent(parent)
                .display(
                        icon.asItem(),
                        title(key),
                        description(key),
                        tier == 5 ? AdvancementType.GOAL : AdvancementType.TASK,
                        true, false, false)
                .addCriterion("reach_tier_" + tier, TribulationCriteria.TIER_REACHED.createCriterion(
                        TierReachedCriterion.TriggerInstance.forTier(tier)))
                .build(id(key));
        consumer.accept(holder);
        return holder;
    }

    private static void leaf(Consumer<AdvancementHolder> consumer, AdvancementHolder parent, String key,
                             ItemLike icon, AdvancementType type, String criterionName,
                             Criterion<?> criterion) {
        consumer.accept(Advancement.Builder.advancement()
                .parent(parent)
                .display(
                        icon.asItem(),
                        title(key),
                        description(key),
                        type,
                        true, false, false)
                .addCriterion(criterionName, criterion)
                .build(id(key)));
    }

    private static SimplePlayerTrigger.TriggerInstance simple() {
        return SimplePlayerTrigger.TriggerInstance.instance();
    }

    private static Component title(String key) {
        return Component.translatable("advancements.tribulation." + key + ".title");
    }

    private static Component description(String key) {
        return Component.translatable("advancements.tribulation." + key + ".description");
    }

    private static Identifier id(String key) {
        return Tribulation.id(key);
    }
}
