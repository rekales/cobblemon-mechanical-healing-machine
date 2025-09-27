package com.krei.cobblemonmechanicalhealingmachine;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.CobblemonSounds;
import com.cobblemon.mod.common.api.pokeball.PokeBalls;
import com.cobblemon.mod.common.api.storage.party.PartyStore;
import com.cobblemon.mod.common.api.text.TextKt;
import com.cobblemon.mod.common.entity.npc.NPCEntity;
import com.cobblemon.mod.common.pokeball.PokeBall;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.*;

// Dev Note: I have to rewrite the whole thing in java because I don't want to use kotlin
// when working with Create and the class is not open
public class HealingMachineBlockEntityCopy extends BlockEntity {

    private UUID currentUser = null;
    private int healTimeLeft = 0;
    private float healingCharge = 0.0F;
    private boolean infinite = false;
    private int currentSignal = 0;
    private float maxCharge = 6F;

    private DataSnapshot dataSnapshot = null;

    private final Map<Integer, PokeBall> pokeBalls = new HashMap<>();

    public HealingMachineBlockEntityCopy(BlockPos blockPos, BlockState blockState) {
        super(MechanicalHealingMachine.HEALING_MACHINE_BLOCK_ENTITY.get(), blockPos, blockState);
        this.maxCharge = Math.max(Cobblemon.config.getMaxHealerCharge(), 6F);
        this.updateRedstoneSignal();
        this.updateBlockChargeLevel();
    }

    // Some getter boilerplate that's automatically made by kotlin
    public int getCurrentSignal() {
        return currentSignal;
    }

    public int getHealTimeLeft() {
        return healTimeLeft;
    }

    public float getHealingCharge() {
        return healingCharge;
    }

    public void setInfinite(boolean infinite) {
        this.infinite = infinite;
    }

    public boolean isInUse() {
        return currentUser != null;
    }

    public Map<Integer, PokeBall> pokeBalls() {
        return this.pokeBalls;
    }

    public void setUser(UUID user, PartyStore party) {
        this.clearData();

        this.pokeBalls.clear();
        List<Pokemon> partyList = party.toGappyList();
        for (int index = 0; index < partyList.size(); index++) {
            Pokemon pokemon = partyList.get(index);
            if (pokemon != null) {
                this.pokeBalls.put(index, pokemon.getCaughtBall());
            }
        }

        this.currentUser = user;
        this.healTimeLeft = 24;

        markUpdated();
    }

    public boolean canHeal(PartyStore party) {
        if (Cobblemon.config.getInfiniteHealerCharge() || this.infinite) {
            return true;
        }
        float neededHealthPercent = party.getHealingRemainderPercent();
        return this.healingCharge >= neededHealthPercent;
    }

    public void activate(UUID user, PartyStore party) {
        if (!Cobblemon.config.getInfiniteHealerCharge() && this.healingCharge != maxCharge) {
            float neededHealthPercent = party.getHealingRemainderPercent();
            this.healingCharge = Math.max(0, Math.min(this.healingCharge - neededHealthPercent, maxCharge));
            this.updateRedstoneSignal();
        }

        this.setUser(user, party);
        Companion.alreadyHealing.add(user);
        updateBlockChargeLevel(HealingMachineBlockCopy.MAX_CHARGE_LEVEL + 1);
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

    public void completeHealing() {
        if (currentUser == null) {
            clearData();
            return;
        }

        ServerPlayer serverPlayer = PlayerExtensionsKt.getPlayer(currentUser); // Extension function translated to Java method
        if (serverPlayer != null) {
            PartyStore party = PlayerExtensionsKt.party(serverPlayer);
            party.heal();
            serverPlayer.sendSystemMessage(TextKt.green(LocalizationUtilsKt.lang("healingmachine.healed")), true);
        } else {
            List<Entity> entities = level.getEntities((Entity) null, AABB.ofSize(BlockPosExtensionsKt.toVec3d(getBlockPos()), 10.0, 10.0, 10.0),
                    entity -> entity.getUUID().equals(currentUser) && entity instanceof NPCEntity);

            NPCEntity npc = entities.isEmpty() ? null : (NPCEntity) entities.get(0);
            if (npc != null && npc.getParty() != null) {
                npc.getParty().heal();
                npc.sendSystemMessage(TextKt.green(LocalizationUtilsKt.lang("healingmachine.healed")));
            }
        }
        updateBlockChargeLevel();
        clearData();
    }

    @Override
    public void loadAdditional(CompoundTag compoundTag, HolderLookup.Provider registryLookup) {
        super.loadAdditional(compoundTag, registryLookup);

        this.pokeBalls.clear();

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
                    this.pokeBalls.put(actualIndex, pokeBall);
                }
                index++;
            }
        }

        if (compoundTag.contains(DataKeys.HEALER_MACHINE_TIME_LEFT)) {
            this.healTimeLeft = compoundTag.getInt(DataKeys.HEALER_MACHINE_TIME_LEFT);
        }

        if (compoundTag.contains(DataKeys.HEALER_MACHINE_CHARGE)) {
            this.healingCharge = Math.min(Math.max(0f, compoundTag.getFloat(DataKeys.HEALER_MACHINE_CHARGE)), maxCharge);
        }

        if (compoundTag.contains(DataKeys.HEALER_MACHINE_INFINITE)) {
            this.infinite = compoundTag.getBoolean(DataKeys.HEALER_MACHINE_INFINITE);
        }
    }

    @Override
    public void saveAdditional(CompoundTag compoundTag, HolderLookup.Provider registryLookup) {
        super.saveAdditional(compoundTag, registryLookup);

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

        compoundTag.putInt(DataKeys.HEALER_MACHINE_TIME_LEFT, this.healTimeLeft);
        compoundTag.putFloat(DataKeys.HEALER_MACHINE_CHARGE, this.healingCharge);
        compoundTag.putBoolean(DataKeys.HEALER_MACHINE_INFINITE, this.infinite);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        return super.saveWithFullMetadata(provider);
    }

    @Override
    public void setRemoved() {
        this.snapshotAndClearData();
        super.setRemoved();
    }

    @Override
    public void clearRemoved() {
        this.restoreSnapshot();
        super.clearRemoved();
    }

    private void updateRedstoneSignal() {
        if (Cobblemon.config.getInfiniteHealerCharge() || this.infinite) {
            this.currentSignal = Companion.MAX_REDSTONE_SIGNAL;
        } else {
            int remainder = (int)((this.healingCharge / maxCharge) * 100) / 10;
            this.currentSignal = Math.min(remainder, Companion.MAX_REDSTONE_SIGNAL);
        }
    }

    private void updateBlockChargeLevel(Integer level) {
        Level world = this.level;
        if (world == null || world.isClientSide()) return;

        int chargeLevel;
        if (level != null) {
            chargeLevel = level;
        } else if (Cobblemon.config.getInfiniteHealerCharge() || this.infinite) {
            chargeLevel = HealingMachineBlockCopy.MAX_CHARGE_LEVEL;
        } else {
            chargeLevel = (int)Math.floor((healingCharge / maxCharge) * HealingMachineBlockCopy.MAX_CHARGE_LEVEL);
        }

        chargeLevel = Math.max(0, Math.min(chargeLevel, HealingMachineBlockCopy.MAX_CHARGE_LEVEL + 1));

        BlockState state = world.getBlockState(getBlockPos());
        if (state.getBlock() instanceof HealingMachineBlockCopy) {
            int currentCharge = state.getValue(HealingMachineBlockCopy.CHARGE_LEVEL);
            if (chargeLevel != currentCharge) {
                world.setBlockAndUpdate(getBlockPos(), state.setValue(HealingMachineBlockCopy.CHARGE_LEVEL, chargeLevel));
            }
        }
    }

    // Overload for Kotlin default argument
    private void updateBlockChargeLevel() {
        updateBlockChargeLevel(null);
    }

    private void markUpdated() {
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    private void snapshotAndClearData() {
        this.dataSnapshot = new DataSnapshot(
                this.currentUser,
                new HashMap<>(this.pokeBalls),
                this.healTimeLeft
        );
        this.clearData();
    }

    private void clearData() {
        if (this.currentUser != null) {
            Companion.alreadyHealing.remove(this.currentUser);
        }
        this.currentUser = null;
        this.pokeBalls.clear();
        this.healTimeLeft = 0;
        markUpdated();
    }

    private void restoreSnapshot() {
        if (this.dataSnapshot != null) {
            this.pokeBalls.clear();
            this.currentUser = this.dataSnapshot.currentUser;
            this.pokeBalls.putAll(this.dataSnapshot.pokeBalls);
            this.healTimeLeft = this.dataSnapshot.healTimeLeft;
        }
    }

    private record DataSnapshot(UUID currentUser, Map<Integer, PokeBall> pokeBalls, int healTimeLeft) { }

    public static class Companion {
        private static final Set<UUID> alreadyHealing = new HashSet<>();
        public static final int MAX_REDSTONE_SIGNAL = 10;

        public static final BlockEntityTicker<HealingMachineBlockEntityCopy> TICKER =
                (world, pos, state, blockEntity) -> {
                    if (world.isClientSide()) return;

                    if (blockEntity.isInUse()) {
                        if (blockEntity.healTimeLeft > 0) {
                            blockEntity.healTimeLeft--;
                        } else {
                            blockEntity.completeHealing();
                        }
                    } else {
                        if (blockEntity.healingCharge < blockEntity.maxCharge) {
                            float chargePerTick = Math.max(0f, Cobblemon.config.getChargeGainedPerTick());
                            blockEntity.healingCharge = Math.min(
                                    blockEntity.maxCharge,
                                    Math.max(0f, blockEntity.healingCharge + chargePerTick)
                            );
                            blockEntity.updateBlockChargeLevel();
                            blockEntity.updateRedstoneSignal();
                            blockEntity.markUpdated();
                        }
                    }
                };

        public static boolean isUsingHealer(Player player) {
            return alreadyHealing.contains(player.getUUID());
        }
    }
}
