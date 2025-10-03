package com.krei.cobblemonmechanicalhealingmachine;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.CobblemonSounds;
import com.cobblemon.mod.common.api.pokeball.PokeBalls;
import com.cobblemon.mod.common.api.storage.party.PartyStore;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.api.text.TextKt;
import com.cobblemon.mod.common.entity.npc.NPCEntity;
import com.cobblemon.mod.common.pokeball.PokeBall;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.util.*;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import kotlin.collections.CollectionsKt;
import kotlin.ranges.RangesKt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.*;

// Dev Note: I have to rewrite the whole thing in java because I don't want to use kotlin
// when working with the Create mod and the original class is not extendable.
// NOTE: All references to HealingMachineBlock is replaced by the mechanical variant
// NOTE: Companion object turned into usual static variables. Breaks slight parity but makes it more Java-like
/*  NOTE: Ended up with a complete overhaul anyway
    - Heal by a set amount while the pokemons are in the mhm.
    - Pokemons stay until retrieved by owner.
    - Removed pokemon storage variables, relying on the player reference instead. Maybe I'll add a per 5 tick updated cache
        for rendering though I think it would be unnecessary since in practice only a few will exist.
    - Removed healing charge and other related variables
    - Removed infinite checks, unnecessary and let the natural variant have that feature
    - Removed a lot of unnecessary functions
    - Removed Data Snapshot, I don't really understand the need for this. I'll just save a player entity reference if needed.
*/
public class MechHealingMachineBlockEntity extends KineticBlockEntity {

    private UUID currentUser = null;
    private int currentSignal = 0;
    protected boolean active = false;  // Not on original
    private Map<Integer, PokeBall> pokeBallsCache = new HashMap<>();

    public MechHealingMachineBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(MechanicalHealingMachine.HEALING_MACHINE_BLOCK_ENTITY.get(), blockPos, blockState);
        this.updateRedstoneSignal();
        this.updateBlockChargeLevel();
    }

    public void updatePokeBallsCache() {
        if (currentUser != null) {
            ServerPlayer serverPlayer = PlayerExtensionsKt.getPlayer(currentUser);
            if (serverPlayer != null) {
                PlayerPartyStore party = PlayerExtensionsKt.party(serverPlayer);
                List<Pokemon> partyList = party.toGappyList();
                for (int index = 0; index < partyList.size(); index++) {
                    Pokemon pokemon = partyList.get(index);
                    if (pokemon != null) {
                        pokeBallsCache.put(index, pokemon.getCaughtBall());
                    }
                }
            } else {
                // TODO: Account for NPC Entities. Or not, we could just simply not bother
            }
        }
        pokeBalls().clear();
    }

    public Map<Integer, PokeBall> pokeBalls() {
        return pokeBallsCache;
    }

    public void setUser(UUID user, PartyStore party) {
        this.clearData();
        this.currentUser = user;
        markUpdated();
    }

    // Keeping method name parity
    public void activate(UUID user, PartyStore party) {
        this.setUser(user, party);
        alreadyHealing.add(user);
        updateBlockChargeLevel();
        updatePokeBallsCache();

        if (level == null || level.isClientSide()) return;
        WorldExtensionsKt.playSoundServer(
                level,
                BlockPosExtensionsKt.toVec3d(getBlockPos()),
                CobblemonSounds.HEALING_MACHINE_ACTIVE,
                SoundSource.NEUTRAL,
                1f,
                1f
        );
    }

    public void deactivate() {
        clearData();
        updateBlockChargeLevel();
        updatePokeBallsCache();
    }

    // Used parent read methods instead of readAdditional
    @Override
    public void read(CompoundTag compoundTag, HolderLookup.Provider registryLookup, boolean clientPacket) {
        super.read(compoundTag, registryLookup, clientPacket);

        if (compoundTag.hasUUID(DataKeys.HEALER_MACHINE_USER)) {
            this.currentUser = compoundTag.getUUID(DataKeys.HEALER_MACHINE_USER);
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
                    this.pokeBallsCache.put(actualIndex, pokeBall);
                }
                index++;
            }
        }
    }

    // Used parent write methods instead of saveAdditional
    @Override
    public void write(CompoundTag compoundTag, HolderLookup.Provider registryLookup, boolean clientPackets) {
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

        super.write(compoundTag, registryLookup, clientPackets);
    }

    // No need for this, Create's BEs automatically handles this.
//    @Override
//    public Packet<ClientGamePacketListener> getUpdatePacket() {
//        return ClientboundBlockEntityDataPacket.create(this);
//    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        return super.saveWithFullMetadata(provider);
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
                // TODO: Rotation speed calculations
                chargeLevel = MechHealingMachineBlock.MAX_CHARGE_LEVEL;
            }

            if (world.getGameTime()%20 == 0) {
//                MechanicalHealingMachine.LOGGER.debug(chargeLevel+"");
//                MechanicalHealingMachine.LOGGER.debug(state.getValue(MechHealingMachineBlock.CHARGE_LEVEL)+"");
//                MechanicalHealingMachine.LOGGER.debug(this.pokeBalls().entrySet()+"");
            }

            int currentCharge = state.getValue(MechHealingMachineBlock.CHARGE_LEVEL);
            if (chargeLevel != currentCharge) {
                world.setBlockAndUpdate(getBlockPos(), state.setValue(MechHealingMachineBlock.CHARGE_LEVEL, chargeLevel));
                this.markUpdated();
            }
        }
    }

    private void markUpdated() {
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    private void clearData() {
        if (this.currentUser != null) {
            alreadyHealing.remove(this.currentUser);
        }
        this.currentUser = null;
        markUpdated();
    }

    // Some getter setter boilerplate that's automatically handled by kotlin

    public int getCurrentSignal() {
        return currentSignal;
    }

    public UUID getCurrentUser() {
        return currentUser;
    }

    public boolean isInUse() {
        return currentUser != null;
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

        this.active = ServerConfig.minActivationSpeed <= Math.abs(this.getSpeed());
        if (this.active && world.getGameTime()%20 == 0) {
            if (currentUser != null) {  // Is in use
                ServerPlayer serverPlayer = PlayerExtensionsKt.getPlayer(currentUser);
                if (serverPlayer != null) {
                    PartyStore party = PlayerExtensionsKt.party(serverPlayer);

                    float rotSpeed = Math.abs(this.getSpeed());
                    double healRate = Math.min(1, Math.max(0, rotSpeed/(float)ServerConfig.maxHealRotSpeed)) * ServerConfig.maxHealRate;
                    int numHealRate = (int) healRate;
                    // Chance to heal more, cuz no decimal health.
                    numHealRate = numHealRate + (world.random.nextDouble() < healRate-numHealRate ? 1 : 0);

                    for(Pokemon pokemon : party) {
                        int health = pokemon.getCurrentHealth();
                        health = Math.min(pokemon.getMaxHealth(), health+numHealRate);
                        pokemon.setCurrentHealth(health);
                    }

                    if (CollectionsKt.none(party, Pokemon::canBeHealed)) {  // Completely Healed, ned notif. TODO: Send once, maybe highlight block
                        serverPlayer.sendSystemMessage(TextKt.red(LocalizationUtilsKt.lang("healingmachine.alreadyhealed")), true);
                        this.deactivate();
                    }
                }
            } else {
                // TODO: Handle NPC Entities here instead
//                clearData();
            }
        }
        this.updateRedstoneSignal();
        this.updateBlockChargeLevel();
        this.sendData();
    }
}