package com.krei.cobblemonmechanicalhealingmachine;

import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Neo's config APIs
public class ServerConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.DoubleValue STRESS_IMPACT = BUILDER
            .comment("Stress impact per unit of speed")
            .defineInRange("mhmStressImpact", 8f, 0, Double.MAX_VALUE);

    public static final ModConfigSpec.DoubleValue MAX_CHARGE_ROT_SPEED = BUILDER
            .comment("At what speed should the maximum charge rate should be")
            .defineInRange("maxChargeRotSpeed", 256, 0, Double.MAX_VALUE);

    public static final ModConfigSpec.DoubleValue MIN_ACTIVATION_SPEED = BUILDER
            .comment("Minimum speed required for the MHM to activate")
            .defineInRange("minActivationSpeed", 16, 0, Double.MAX_VALUE);

    // TODO: Implement
    public static final ModConfigSpec.BooleanValue HM_DROPS_MHM = BUILDER
            .comment("Should the healing machine drop the mhm")
            .define("hmDropsMhm", true);


    static final ModConfigSpec SPEC = BUILDER.build();

    public static double stressImpact;
    public static double maxChargeRotSpeed;
    public static double minActivationSpeed;

    static void onLoad(final ModConfigEvent event) {
        stressImpact = STRESS_IMPACT.get();
        maxChargeRotSpeed = MAX_CHARGE_ROT_SPEED.get();
        minActivationSpeed = MIN_ACTIVATION_SPEED.get();
    }

    static void onReload(final ModConfigEvent event) {
        stressImpact = STRESS_IMPACT.get();
        maxChargeRotSpeed = MAX_CHARGE_ROT_SPEED.get();
        minActivationSpeed = MIN_ACTIVATION_SPEED.get();
    }
}
