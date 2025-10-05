package com.krei.cobblemonmechanicalhealingmachine;

import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Neo's config APIs
public class ServerConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.DoubleValue MIN_ACTIVATION_SPEED = BUILDER
            .comment("Minimum speed required for the MHM to activate")
            .defineInRange("minActivationSpeed", 16, 0, Double.MAX_VALUE);

    public static final ModConfigSpec.DoubleValue STRESS_IMPACT = BUILDER
            .comment("Stress impact per unit of speed")
            .defineInRange("mhmStressImpact", 8f, 0, Double.MAX_VALUE);

    public static final ModConfigSpec.DoubleValue MAX_HEAL_ROT_SPEED = BUILDER
            .comment("At what speed should the maximum healing rate should be")
            .defineInRange("maxHealRotSpeed", 256, 0, Double.MAX_VALUE);

    public static final ModConfigSpec.DoubleValue MAX_HEAL_RATE_FIXED = BUILDER
            .comment("Amount healed every tick per rpm")
            .defineInRange("maxHealRateFixed", 0.025, 0.0001, Double.MAX_VALUE);

    public static final ModConfigSpec.DoubleValue MAX_HEAL_RATE_PERCENT = BUILDER
            .comment("Percent healed every tick per rpm")
            .comment("Heals whichever is higher between the fixed and percent values")
            .defineInRange("maxHealRatePercent", 0.04, 0.0001, Double.MAX_VALUE);

    public static final ModConfigSpec.DoubleValue HEAL_RATE_PERCENT_STEEPNESS = BUILDER
            .comment("ADVANCED: The percent heal rate is calculated with a exponential function. Adjust the steepness with this value.")
            .defineInRange("healRatePercentSteepness", 2.5, 0.01, Double.MAX_VALUE);


    static final ModConfigSpec SPEC = BUILDER.build();

    public static double stressImpact;
    public static double maxHealRotSpeed;
    public static double minActivationSpeed;
    public static double maxHealRateFixed;
    public static double maxHealRatePercent;
    public static double healRatePercentSteepness;

    static void onLoad(final ModConfigEvent.Loading event) {
        stressImpact = STRESS_IMPACT.get();
        maxHealRotSpeed = MAX_HEAL_ROT_SPEED.get();
        minActivationSpeed = MIN_ACTIVATION_SPEED.get();
        maxHealRateFixed = MAX_HEAL_RATE_FIXED.get();
        maxHealRatePercent = MAX_HEAL_RATE_PERCENT.get();
        healRatePercentSteepness = HEAL_RATE_PERCENT_STEEPNESS.get();
    }

    static void onReload(final ModConfigEvent.Reloading event) {
        stressImpact = STRESS_IMPACT.get();
        maxHealRotSpeed = MAX_HEAL_ROT_SPEED.get();
        minActivationSpeed = MIN_ACTIVATION_SPEED.get();
        maxHealRateFixed = MAX_HEAL_RATE_FIXED.get();
        maxHealRatePercent = MAX_HEAL_RATE_PERCENT.get();
        healRatePercentSteepness = HEAL_RATE_PERCENT_STEEPNESS.get();
    }
}
