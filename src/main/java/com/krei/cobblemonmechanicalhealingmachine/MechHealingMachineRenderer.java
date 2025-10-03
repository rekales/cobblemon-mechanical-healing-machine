package com.krei.cobblemonmechanicalhealingmachine;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

import static com.simibubi.create.content.equipment.armor.BacktankRenderer.getShaftModel;

// Note: Did a rewrite instead, class is small enough that I don't need to mention what and which is copied
public class MechHealingMachineRenderer extends KineticBlockEntityRenderer<MechHealingMachineBlockEntity> {

    private static final List<double[]> OFFSETS = List.of(
            new double[]{0.2, 0.385},
            new double[]{-0.2, 0.385},
            new double[]{0.2, 0.0},
            new double[]{-0.2, 0.0},
            new double[]{0.2, -0.385},
            new double[]{-0.2, -0.385}
    );

    public MechHealingMachineRenderer(BlockEntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void renderSafe(MechHealingMachineBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();

        final Direction.Axis axis = Direction.Axis.Y;
        final BlockPos pos = blockEntity.getBlockPos();
        float time = AnimationTickHolder.getRenderTime(blockEntity.getLevel());

        SuperByteBuffer shaft = CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, blockEntity.getBlockState(), Direction.DOWN);
        float rotOffset = getRotationOffsetForPosition(blockEntity, pos, axis);
        float angle = (time * blockEntity.getSpeed() * 3f / 10) % 360;
        angle += rotOffset;
        angle = angle / 180f * (float) Math.PI;

        kineticRotationTransform(shaft, blockEntity, axis, angle, packedLight);
        shaft.renderInto(poseStack, bufferSource.getBuffer(RenderType.solid()));


        BlockState blockState;
        if (blockEntity.getLevel() != null) {
            blockState = blockEntity.getBlockState();
        } else {
            blockState = MechanicalHealingMachine.HEALING_MACHINE_BLOCK.get().defaultBlockState()
                    .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH);
        }

        float yRot = blockState.getValue(HorizontalDirectionalBlock.FACING).toYRot();

        // Position Poké Balls
        poseStack.translate(0.5, 0.5, 0.5);

        poseStack.mulPose(Axis.YP.rotationDegrees(-yRot));
        poseStack.scale(0.65F, 0.65F, 0.65F);


        if (blockEntity.getLevel().getGameTime()%20==0)
            MechanicalHealingMachine.LOGGER.debug(blockEntity.pokeBalls()+"");

        int index = 0;
        for (var entry : blockEntity.pokeBalls().entrySet()) {
            poseStack.pushPose();
            double[] offset = OFFSETS.get(index);
            poseStack.translate(offset[0], 0.4, offset[1]);
            Minecraft.getInstance().getItemRenderer().renderStatic(
                    entry.getValue().stack(1),  // Kotlin doesn't automatically provide empty overloads
                    ItemDisplayContext.GROUND,
                    packedLight,
                    packedOverlay,
                    poseStack,
                    bufferSource,
                    blockEntity.getLevel(),
                    0
            );
            poseStack.popPose();
            index++;
        }
        poseStack.popPose();
    }

    @Override
    protected SuperByteBuffer getRotatedModel(MechHealingMachineBlockEntity be, BlockState state) {
        return CachedBuffers.partial(getShaftModel(state), state);
    }
}
