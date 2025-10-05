/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Modified and translated from Kotlin to Java by Krei, 2025.
 */

package com.krei.cobblemonmechanicalhealingmachine;

import com.cobblemon.mod.common.api.pokeball.PokeBalls;
import com.cobblemon.mod.common.api.storage.party.PartyStore;
import com.cobblemon.mod.common.pokeball.PokeBall;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.util.*;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import kotlin.collections.CollectionsKt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

// Dev Note: I have to rewrite the whole thing in java because I don't want to use kotlin
// when working with the Create mod and the original class is not extendable.
// NOTE: All references to HealingMachineBlock is replaced by the mechanical variant
// NOTE: Companion object turned into usual static variables. Breaks slight parity but makes it more Java-like
/*  NOTE: Ended up with a complete overhaul anyway
    - Heal by a set amount while the pokemons are in the mhm.
    - Pokemons stay until retrieved by owner.
    - Removed healing charge and other related variables
    - Removed infinite checks, unnecessary and let the natural variant have that feature
    - Removed a lot of unnecessary functions
    - Removed Data Snapshot, I don't really understand the need for this. I'll just save a player entity reference if needed.
*/
public class MechHealingMachineBlockEntity extends KineticBlockEntity {

    private UUID currentUser = null;
    private int currentSignal = 0;
    protected boolean active = false;  // Not on original
    private Map<Integer, PokeBall> pokeBalls = new HashMap<>();
    private double healedAmountFixed = 0;  // No need to sync
    private double healedAmountPercentMult = 0;  // No need to sync
    private int ticksHealing = 0;  // No need to sync

    public MechHealingMachineBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(MechanicalHealingMachine.HEALING_MACHINE_BLOCK_ENTITY.get(), blockPos, blockState);
        this.updateRedstoneSignal();
        this.updateBlockChargeLevel();
    }

    public Map<Integer, PokeBall> pokeBalls() {
        return pokeBalls;
    }

    public void setUser(UUID user, PartyStore party) {
        this.clearData();
        this.currentUser = user;

        List<Pokemon> partyList = party.toGappyList();
        for (int index = 0; index < partyList.size(); index++) {
            Pokemon pokemon = partyList.get(index);
            if (pokemon != null) {
                pokeBalls.put(index, pokemon.getCaughtBall());
            }
        }
    }

    // Keeping method name parity
    public void activate(UUID user, PartyStore party) {
        this.setUser(user, party);
        alreadyHealing.add(user);
        updateBlockChargeLevel();

        double minPercentHealth = 1;
        double minFixedHealth = 10000;
        List<Pokemon> partyList = party.toGappyList();
        for (int index = 0; index < partyList.size(); index++) {
            Pokemon pokemon = partyList.get(index);
            if (pokemon != null) {
                pokeBalls.put(index, pokemon.getCaughtBall());
                minPercentHealth = Math.min(minPercentHealth, (double) pokemon.getCurrentHealth()/pokemon.getMaxHealth());
                minFixedHealth = Math.min(minFixedHealth, pokemon.getCurrentHealth());
            }
        }
        this.healedAmountPercentMult = minPercentHealth;
        this.healedAmountFixed = minFixedHealth;

        if (!active || level == null || level.isClientSide()) return;
        WorldExtensionsKt.playSoundServer(
                level,
                BlockPosExtensionsKt.toVec3d(getBlockPos()),
                MechanicalHealingMachine.MHM_TUNE.value(),
                SoundSource.NEUTRAL,
                1f,
                1f
        );
    }

    public void deactivate() {
        clearData();
        updateBlockChargeLevel();
        if (level == null || level.isClientSide()) return;
        WorldExtensionsKt.playSoundServer(
                level,
                BlockPosExtensionsKt.toVec3d(getBlockPos()),
                MechanicalHealingMachine.MHM_SHOOP.value(),
                SoundSource.NEUTRAL,
                1f,
                1f
        );
    }

    // Used parent read methods instead of readAdditional
    @Override
    public void read(CompoundTag compoundTag, HolderLookup.Provider registryLookup, boolean clientPacket) {
        super.read(compoundTag, registryLookup, clientPacket);

        if (compoundTag.hasUUID(DataKeys.HEALER_MACHINE_USER)) {
            this.currentUser = compoundTag.getUUID(DataKeys.HEALER_MACHINE_USER);
        } else {
            this.currentUser = null;
        }

        if (compoundTag.contains(DataKeys.HEALER_MACHINE_POKEBALLS)) {
            CompoundTag pokeBallsTag = compoundTag.getCompound(DataKeys.HEALER_MACHINE_POKEBALLS);
            int index = 0;
            for (String key : pokeBallsTag.getAllKeys()) {
                String pokeBallId = pokeBallsTag.getString(key);
                if (pokeBallId.isEmpty()) continue;

                int actualIndex;
                try {
                    actualIndex = Integer.parseInt(key);
                } catch (NumberFormatException e) {
                    actualIndex = index;
                }

                PokeBall pokeBall = PokeBalls.INSTANCE.getPokeBall(ResourceLocation.parse(pokeBallId));
                if (pokeBall != null) {
                    this.pokeBalls.put(actualIndex, pokeBall);
                }
                index++;
            }
        } else {
            this.pokeBalls.clear();
        }
    }

    // Used parent write methods instead of saveAdditional
    @Override
    public void write(CompoundTag compoundTag, HolderLookup.Provider registryLookup, boolean clientPackets) {
        super.write(compoundTag, registryLookup, clientPackets);

        if (this.currentUser != null) {
            compoundTag.putUUID(DataKeys.HEALER_MACHINE_USER, this.currentUser);
        } else {
            compoundTag.remove(DataKeys.HEALER_MACHINE_USER);
        }

        if (!this.pokeBalls().isEmpty()) {
            CompoundTag pokeBallsTag = new CompoundTag();
            for (Map.Entry<Integer, PokeBall> entry : this.pokeBalls().entrySet()) {
                pokeBallsTag.putString(entry.getKey().toString(), entry.getValue().getName().toString());
            }
            compoundTag.put(DataKeys.HEALER_MACHINE_POKEBALLS, pokeBallsTag);
        } else {
            compoundTag.remove(DataKeys.HEALER_MACHINE_POKEBALLS);
        }
    }

    private void updateRedstoneSignal() {
        float rotSpeed = Math.abs(this.getSpeed());
        int signal = (int) (Math.min(1, Math.max(0, rotSpeed/(float)ServerConfig.maxHealRotSpeed)) * MAX_REDSTONE_SIGNAL);
        this.currentSignal = Math.min(signal, MAX_REDSTONE_SIGNAL);
    }

    private void updateBlockChargeLevel() {
        Level world = this.level;
        if (world == null || world.isClientSide()) return;

        BlockState state = world.getBlockState(this.getBlockPos());
        if (state.getBlock() instanceof MechHealingMachineBlock) {
            int chargeLevel;
            if (!this.active) {
                chargeLevel = MechHealingMachineBlock.MAX_CHARGE_LEVEL + 2;
            } else if (this.isInUse()) {
                chargeLevel = MechHealingMachineBlock.MAX_CHARGE_LEVEL + 1;
            } else {
                float rotSpeed = Math.abs(this.getSpeed());
                chargeLevel = (int) (Math.min(1, Math.max(0,
                        (rotSpeed-ServerConfig.minActivationSpeed)/(ServerConfig.maxHealRotSpeed-ServerConfig.minActivationSpeed))) *
                                MechHealingMachineBlock.MAX_CHARGE_LEVEL);
            }

            int currentCharge = state.getValue(MechHealingMachineBlock.CHARGE_LEVEL);
            if (chargeLevel != currentCharge) {
                world.setBlockAndUpdate(getBlockPos(), state.setValue(MechHealingMachineBlock.CHARGE_LEVEL, chargeLevel));
            }
        }
    }

    private void clearData() {
        if (this.currentUser != null) {
            alreadyHealing.remove(this.currentUser);
        }
        this.currentUser = null;
        this.pokeBalls.clear();
        this.healedAmountFixed = 0;
        this.healedAmountPercentMult = 0;
        this.ticksHealing = 0;
    }

    // Some getter setter boilerplate that's automatically handled by kotlin

    public int getCurrentSignal() {
        return currentSignal;
    }

    public UUID getCurrentUser() {
        return currentUser;
    }

    public boolean isInUse() {
        return currentUser != null && !pokeBalls.isEmpty();
    }


    // Kotlin companion turned into static variables

    private static final Set<UUID> alreadyHealing = new HashSet<>();
    public static final int MAX_REDSTONE_SIGNAL = 10;

    public static boolean isUsingHealer(Player player) {
        return alreadyHealing.contains(player.getUUID());
    }


    // Not on original from this point downwards

    public boolean isActive() {
        return active;
    }

    @Override
    public float calculateStressApplied() {
        float impact = (float)ServerConfig.stressImpact;
        this.lastStressApplied = impact;
        return impact;
    }

    // NOTE: Ditched the ticker in favor of using the tick() method for Create compatibility.
    @Override
    public void tick() {
        super.tick();

        Level world = this.getLevel();
        if (world == null || world.isClientSide()) {
            this.sendData();
            return;
        }

        boolean wasActive = this.active;
        this.active = ServerConfig.minActivationSpeed <= Math.abs(this.getSpeed());

        if (currentUser != null) {  // Is in use

            if (this.active) {
                if (!wasActive) {  // Sound when activated while pokemons are inside
                    WorldExtensionsKt.playSoundServer(
                            level,
                            BlockPosExtensionsKt.toVec3d(getBlockPos()),
                            MechanicalHealingMachine.MHM_TUNE.value(),
                            SoundSource.NEUTRAL,
                            1f,
                            1f
                    );
                }

                ServerPlayer serverPlayer = PlayerExtensionsKt.getPlayer(currentUser);
                if (serverPlayer != null) {
                    PartyStore party = PlayerExtensionsKt.party(serverPlayer);

                    double rotSpeed = Math.abs(this.getSpeed());
                    double phSteepness = ServerConfig.healRatePercentSteepness;
                    double percentHealMult = ServerConfig.maxHealRatePercent
                            * ((Math.exp(phSteepness*(rotSpeed/ServerConfig.maxHealRotSpeed))-1)/(Math.exp(phSteepness)-1));
                    double fixedHeal = ServerConfig.maxHealRateFixed * Math.min(1, Math.max(0, rotSpeed/ServerConfig.maxHealRotSpeed));

                    healedAmountPercentMult += percentHealMult;
                    healedAmountFixed += fixedHeal;
                    ticksHealing++;

                    for(Pokemon pokemon : party) {
                        if (pokemon.isFainted()) {
                            pokemon.setFaintedTimer(-1);
                        }

                        int pokemonHealth = (int) Math.max(healedAmountFixed, healedAmountPercentMult*pokemon.getMaxHealth());
                        if (pokemonHealth > pokemon.getMaxHealth()) {
                            pokemon.heal();
                        } else if (pokemon.getCurrentHealth() < pokemonHealth) {
                            pokemon.setCurrentHealth(pokemonHealth);
                        }
                    }

                    if (CollectionsKt.none(party, Pokemon::canBeHealed) && ticksHealing > 24) {
                        this.deactivate();  // Completely healed, return to party
                    }
                }
            }
        } else {
            // TODO: Handle NPC Entities here instead
            clearData();
        }
        this.updateRedstoneSignal();
        this.updateBlockChargeLevel();
        this.sendData();
    }
}