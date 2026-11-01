package com.rfizzle.meridian.api;

import net.minecraft.core.Holder;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * Test stand-in for Meridian's {@code MeridianAPI}, mirroring only the member Tribulation reaches
 * by reflection: {@code public static EnchantmentInfo getEnchantmentInfo(Holder<Enchantment>)},
 * as in meridian-1.0.0. Tests set {@link #next} to choose what the lookup returns.
 */
public final class MeridianAPI {

    /** What {@link #getEnchantmentInfo} returns next. */
    public static EnchantmentInfo next = new EnchantmentInfo(1, true);

    /** The holder passed to the most recent {@link #getEnchantmentInfo} call. */
    public static Holder<Enchantment> lastHolder;

    private MeridianAPI() {
    }

    public static EnchantmentInfo getEnchantmentInfo(Holder<Enchantment> holder) {
        lastHolder = holder;
        return next;
    }
}
