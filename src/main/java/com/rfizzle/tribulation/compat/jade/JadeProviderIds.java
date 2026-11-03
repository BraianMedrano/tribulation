package com.rfizzle.tribulation.compat.jade;

/**
 * UID paths of this mod's Jade providers, under the {@code tribulation}
 * namespace. Kept free of Jade types so plain JUnit tests, which run without
 * Jade on the classpath, can derive the keys Jade looks up from them.
 */
public final class JadeProviderIds {

    /** Path of {@link MobScalingJadeProvider#UID}. */
    public static final String MOB_SCALING = "mob_scaling";

    private JadeProviderIds() {}
}
