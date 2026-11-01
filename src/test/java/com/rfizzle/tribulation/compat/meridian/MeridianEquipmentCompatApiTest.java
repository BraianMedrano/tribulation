package com.rfizzle.tribulation.compat.meridian;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.rfizzle.meridian.api.EnchantmentInfo;
import com.rfizzle.meridian.api.MeridianAPI;
import net.minecraft.core.Holder;
import net.minecraft.world.item.enchantment.Enchantment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Covers the reflective bridge to Meridian's API. Meridian has no 26.x build, so Tribulation
 * reaches it through method handles instead of linking against it; a wrong name, owner, or
 * signature would only surface on a server with Meridian installed. The stand-ins under
 * {@code com.rfizzle.meridian.api} in the test source set mirror meridian-1.0.0, so these tests
 * prove the handles resolve and that the values flow through the {@code invoke} casts.
 */
class MeridianEquipmentCompatApiTest {

    private static final Holder<Enchantment> HOLDER = Holder.direct(null);

    @AfterEach
    void resetStub() {
        MeridianAPI.next = new EnchantmentInfo(1, true);
        MeridianAPI.lastHolder = null;
    }

    @Test
    void enabledMaxLevel_enabledEnchant_returnsMeridianMaxLevel() throws Throwable {
        MeridianAPI.next = new EnchantmentInfo(4, true);

        assertEquals(4, MeridianEquipmentCompat.Api.enabledMaxLevel(HOLDER));
        assertSame(HOLDER, MeridianAPI.lastHolder, "the holder is passed through to Meridian");
    }

    @Test
    void enabledMaxLevel_disabledEnchant_returnsZero() throws Throwable {
        MeridianAPI.next = new EnchantmentInfo(4, false);

        assertEquals(0, MeridianEquipmentCompat.Api.enabledMaxLevel(HOLDER));
    }
}
