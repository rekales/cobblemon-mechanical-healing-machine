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

    public static final ModConfigSpec.DoubleValue MAX_HEAL_RATE = BUILDER
            .comment("Amount healed every tick per rpm")
            .defineInRange("maxHealRate", 0.05, 0.001, Double.MAX_VALUE);

    public static final ModConfigSpec.DoubleValue MAX_HEAL_ROT_SPEED = BUILDER
            .comment("At what speed should the maximum healing rate should be")
            .defineInRange("maxHealRotSpeed", 256, 0, Double.MAX_VALUE);

    public static final ModConfigSpec.DoubleValue MIN_ACTIVATION_SPEED = BUILDER
            .comment("Minimum speed required for the MHM to activate")
            .defineInRange("minActivationSpeed", 16, 0, Double.MAX_VALUE);


    static final ModConfigSpec SPEC = BUILDER.build();

    public static double stressImpact;
    public static double maxHealRotSpeed;
    public static double minActivationSpeed;
    public static double maxHealRate;

    static void onLoad(final ModConfigEvent event) {
        stressImpact = STRESS_IMPACT.get();
        maxHealRotSpeed = MAX_HEAL_ROT_SPEED.get();
        minActivationSpeed = MIN_ACTIVATION_SPEED.get();
        maxHealRate = MAX_HEAL_RATE.get();
    }

    static void onReload(final ModConfigEvent event) {
        stressImpact = STRESS_IMPACT.get();
        maxHealRotSpeed = MAX_HEAL_ROT_SPEED.get();
        minActivationSpeed = MIN_ACTIVATION_SPEED.get();
        maxHealRate = MAX_HEAL_RATE.get();
    }
}
