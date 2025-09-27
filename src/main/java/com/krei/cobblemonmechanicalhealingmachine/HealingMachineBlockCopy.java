package com.krei.cobblemonmechanicalhealingmachine;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.api.text.TextKt;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.util.LocalizationUtilsKt;
import com.cobblemon.mod.common.util.MiscUtilsKt;
import com.cobblemon.mod.common.util.PlayerExtensionsKt;
import com.mojang.serialization.MapCodec;
import kotlin.collections.CollectionsKt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

// Dev Note: I have to rewrite the whole thing in java because I don't want to use kotlin
// when working with Create and the class is not open
public class HealingMachineBlockCopy extends BaseEntityBlock {

    public static final MapCodec<HealingMachineBlockCopy> CODEC = simpleCodec(HealingMachineBlockCopy::new);

    private VoxelShape NORTH_SOUTH_AABB = Shapes.or(
            Shapes.box(0.0, 0.0, 0.0, 1.0, 0.625, 1.0),
            Shapes.box(0.0625, 0.625, 0.0, 0.9375, 0.875, 0.125),
            Shapes.box(0.0625, 0.625, 0.875, 0.9375, 0.875, 1.0),
            Shapes.box(0.0625, 0.625, 0.125, 0.1875, 0.75, 0.875),
            Shapes.box(0.8125, 0.625, 0.125, 0.9375, 0.75, 0.875),
            Shapes.box(0.1875, 0.625, 0.125, 0.8125, 0.6875, 0.875)
    );

    private VoxelShape WEST_EAST_AABB = Shapes.or(
            Shapes.box(0.0, 0.0, 0.0, 1.0, 0.625, 1.0),
            Shapes.box(0.875, 0.625, 0.0625, 1.0, 0.875, 0.9375),
            Shapes.box(0.0, 0.625, 0.0625, 0.125, 0.875, 0.9375),
            Shapes.box(0.125, 0.625, 0.0625, 0.875, 0.75, 0.1875),
            Shapes.box(0.125, 0.625, 0.8125, 0.875, 0.75, 0.9375),
            Shapes.box(0.125, 0.625, 0.1875, 0.875, 0.6875, 0.8125)
    );

    public static int MAX_CHARGE_LEVEL = 5;
    public static final IntegerProperty CHARGE_LEVEL = IntegerProperty.create("charge", 0, MAX_CHARGE_LEVEL + 1);
    public static final BooleanProperty NATURAL = BooleanProperty.create("natural");

    public HealingMachineBlockCopy(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH)
                .setValue(NATURAL, false)
                .setValue(CHARGE_LEVEL, 0));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        if (facing == Direction.WEST || facing == Direction.EAST) {
            return WEST_EAST_AABB;
        } else {
            return NORTH_SOUTH_AABB;
        }
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HealingMachineBlockEntityCopy(pos, state);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, context.getHorizontalDirection());
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HorizontalDirectionalBlock.FACING, NATURAL);
        builder.add(CHARGE_LEVEL);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(HorizontalDirectionalBlock.FACING,
                rotation.rotate(state.getValue(HorizontalDirectionalBlock.FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(HorizontalDirectionalBlock.FACING)));
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            super.onRemove(state, level, pos, newState, movedByPiston);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof HealingMachineBlockEntityCopy hmbEntity)) {
            return InteractionResult.SUCCESS;
        }

        ServerPlayer serverPlayerEntity;
        if (player instanceof ServerPlayer sp) {
            serverPlayerEntity = sp;
        } else {
            return InteractionResult.SUCCESS; // just in case
        }

        if (hmbEntity.isInUse()) {
            serverPlayerEntity.sendSystemMessage(TextKt.red(LocalizationUtilsKt.lang("healingmachine.alreadyinuse")), true);
            return InteractionResult.SUCCESS;
        }

        if (PlayerExtensionsKt.isInBattle(serverPlayerEntity)) {
            serverPlayerEntity.sendSystemMessage(TextKt.red(LocalizationUtilsKt.lang("healingmachine.inbattle")), true);
            return InteractionResult.SUCCESS;
        }

        PlayerPartyStore party = PlayerExtensionsKt.party(serverPlayerEntity);
        if (CollectionsKt.none(party)) {
            serverPlayerEntity.sendSystemMessage(TextKt.red(LocalizationUtilsKt.lang("healingmachine.nopokemon")), true);
            return InteractionResult.SUCCESS;
        }

        if (CollectionsKt.none(party, Pokemon::canBeHealed)) {
            serverPlayerEntity.sendSystemMessage(TextKt.red(LocalizationUtilsKt.lang("healingmachine.nopokemon")), true);
            return InteractionResult.SUCCESS;
        }

        if (HealingMachineBlockEntityCopy.Companion.isUsingHealer(player)) {
            serverPlayerEntity.sendSystemMessage(TextKt.red(LocalizationUtilsKt.lang("healingmachine.alreadyhealing")), true);
            return InteractionResult.SUCCESS;
        }

        if (hmbEntity.canHeal(party)) {
            hmbEntity.activate(player.getUUID(), party);
            serverPlayerEntity.sendSystemMessage(TextKt.green(LocalizationUtilsKt.lang("healingmachine.healing")), true);
        } else {
            float neededCharge = PlayerExtensionsKt.party(serverPlayerEntity).getHealingRemainderPercent() - hmbEntity.getHealingCharge();
            serverPlayerEntity.sendSystemMessage(TextKt.red(LocalizationUtilsKt.lang("healingmachine.notenoughcharge", neededCharge + "%")), true);
        }

        // party.forEach { it.tryRecallWithAnimation() }
        for (Pokemon pokemon : party) {
            pokemon.tryRecallWithAnimation();
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);

        if (!level.isClientSide() && placer instanceof ServerPlayer serverPlayer && serverPlayer.isCreative()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (!(blockEntity instanceof HealingMachineBlockEntityCopy healingMachine)) {
                return;
            }
            healingMachine.setInfinite(true);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof HealingMachineBlockEntityCopy healingMachine)) {
            return;
        }

        if (random.nextInt(2) == 0 && healingMachine.getHealTimeLeft() > 0) {
            double posX = pos.getX() + 0.5 + ((random.nextFloat() * 0.3F) * (random.nextInt(2) > 0 ? 1 : -1));
            double posY = pos.getY() + 0.9;
            double posZ = pos.getZ() + 0.5 + ((random.nextFloat() * 0.3F) * (random.nextInt(2) > 0 ? 1 : -1));
            level.addParticle(ParticleTypes.HAPPY_VILLAGER, posX, posY, posZ, 0.0, 0.0, 0.0);
        }
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof HealingMachineBlockEntityCopy healingMachine) {
            return healingMachine.getCurrentSignal();
        }
        return 0;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState blockState, BlockEntityType<T> blockWithEntityType) {
        return createTickerHelper(blockWithEntityType, MechanicalHealingMachine.HEALING_MACHINE_BLOCK_ENTITY.get(), HealingMachineBlockEntityCopy.Companion.TICKER);
    }

    @Override
    public RenderShape getRenderShape(BlockState blockState) {
        return RenderShape.MODEL;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag options) {
        tooltip.add(TextKt.gray(MiscUtilsKt.asTranslated("block." + Cobblemon.MODID + ".healing_machine.tooltip1")));
        tooltip.add(TextKt.gray(MiscUtilsKt.asTranslated("block." + Cobblemon.MODID + ".healing_machine.tooltip2")));
    }
}
