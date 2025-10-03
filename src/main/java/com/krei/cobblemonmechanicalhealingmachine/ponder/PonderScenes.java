package com.krei.cobblemonmechanicalhealingmachine.ponder;

import com.cobblemon.mod.common.CobblemonItems;
import com.krei.cobblemonmechanicalhealingmachine.MechHealingMachineBlock;
import com.krei.cobblemonmechanicalhealingmachine.MechHealingMachineBlockEntity;
import com.krei.cobblemonmechanicalhealingmachine.MechanicalHealingMachine;
import com.simibubi.create.content.contraptions.piston.MechanicalPistonHeadBlock;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import com.tterrag.registrate.util.entry.ItemProviderEntry;
import com.tterrag.registrate.util.entry.RegistryEntry;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

public class PonderScenes implements PonderPlugin {
    @Override
    public String getModId() {
        return MechanicalHealingMachine.MOD_ID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        helper.addStoryBoard(
                ResourceLocation.fromNamespaceAndPath(MechanicalHealingMachine.MOD_ID, "mechanical_healing_machine"),
                ResourceLocation.fromNamespaceAndPath(MechanicalHealingMachine.MOD_ID, "mechanical_healing_machine"),
                PonderScenes::mechanicalHealingMachine);
    }

    public static void mechanicalHealingMachine(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("mechanical_healing_machine", "Mechanical Healing Machine");
        scene.configureBasePlate(0,0,5);

        List<BlockPos> placeList = List.of(new BlockPos[]{
                new BlockPos(3, 1, 1),
                new BlockPos(3, 1, 2),
                new BlockPos(3, 1, 3),
                new BlockPos(2, 1, 1),
                new BlockPos(2, 1, 2),
                new BlockPos(2, 1, 3),
                new BlockPos(1, 1, 1),
                new BlockPos(1, 1, 2),
                new BlockPos(1, 1, 3),
                new BlockPos(0, 1, 2),
        });

        scene.world().setBlocks(util.select().position(2,1,2), MechanicalHealingMachine.HEALING_MACHINE_BLOCK.get()
                .defaultBlockState().setValue(MechHealingMachineBlock.CHARGE_LEVEL, MechHealingMachineBlock.MAX_CHARGE_LEVEL+2), false);
        scene.world().setBlocks(util.select().position(0,0,2), Blocks.WHITE_CONCRETE.defaultBlockState(), false);
        scene.world().showSection(util.select().layer(0), Direction.DOWN);
        scene.world().showSection(util.select().position(2,1,2), Direction.DOWN);
        scene.idle(20);

        scene.overlay().showText(90)
                .placeNearTarget()
                .pointAt(util.select().position(2,1,2).getCenter())
                .text("This mechanical healing machine is currently unpowered and requires rotational energy from the bottom");
        scene.idle(100);

        scene.world().hideSection(util.select().position(2,1,2), Direction.UP);
        scene.idle(30);

        scene.world().restoreBlocks(util.select().everywhere());
        scene.idle(6);
        for (BlockPos pos : placeList) {
            scene.world().showSection(util.select().position(pos), Direction.DOWN);
            scene.idle(2);
        }
        scene.idle(10);
        scene.world().showSection(util.select().position(2,2,2), Direction.DOWN);
        scene.idle(15);
        scene.addKeyframe();

        scene.world().setKineticSpeed(util.select().everywhere(), 4);
        scene.overlay().showText(80)
                .placeNearTarget()
                .pointAt(util.select().position(2,2,2).getCenter())
                .text("Red status indicator means it doesn't have enough rotational speed");
        scene.idle(90);

        scene.world().setKineticSpeed(util.select().everywhere(), 32);
        scene.world().modifyBlock(
                new BlockPos(2,2,2),
                state -> state.setValue(MechHealingMachineBlock.CHARGE_LEVEL, MechHealingMachineBlock.MAX_CHARGE_LEVEL),
                false
        );
        scene.idle(10);
        scene.overlay().showText(60)
                .placeNearTarget()
                .pointAt(util.select().position(2,2,2).getCenter())
                .text("Blue means its ready for use");
        scene.idle(70);

        // NOTE: Seems unnecessary
//        scene.world().modifyBlockEntity(
//                new BlockPos(2,2,2),
//                MechHealingMachineBlockEntity.class,
//
//                );
//        scene.world().modifyBlock(
//                new BlockPos(2,2,2),
//                state -> state.setValue(MechHealingMachineBlock.CHARGE_LEVEL, MechHealingMachineBlock.MAX_CHARGE_LEVEL+1),
//                false
//        );
//        scene.idle(5);
//        scene.overlay().showText(60)
//                .placeNearTarget()
//                .pointAt(util.select().position(2,2,2).getCenter())
//                .text("And green means it's currently healing pokemons");
//        scene.idle(70);

        scene.overlay().showText(80)
                .attachKeyFrame()
                .placeNearTarget()
                .text("The speed of healing depends on how fast the rotational speed is");
        scene.idle(60);
    }
}
