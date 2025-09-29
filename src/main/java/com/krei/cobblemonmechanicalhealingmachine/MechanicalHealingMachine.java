package com.krei.cobblemonmechanicalhealingmachine;

import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(MechanicalHealingMachine.MOD_ID)
public class MechanicalHealingMachine {
    public static final String MOD_ID = "cobblemonmechanicalhealingmachine";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(
            MechanicalHealingMachine.MOD_ID
    );

    public static final DeferredHolder<Block, Block> HEALING_MACHINE_BLOCK = BLOCKS.register(
            "mechanical_healing_machine",
            () -> new MechHealingMachineBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .sound(SoundType.METAL)
                    .pushReaction(PushReaction.BLOCK)
                    .strength(2f)
                    .noOcclusion()
                    .lightLevel(state -> {
                        int charge = state.getValue(MechHealingMachineBlock.CHARGE_LEVEL);
                        if (charge == MechHealingMachineBlock.MAX_CHARGE_LEVEL + 2) {
                            return 2;
                        } else if (charge >= MechHealingMachineBlock.MAX_CHARGE_LEVEL) {
                            return 7;
                        } else {
                            return 2;
                        }
                    })
            ));

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            MechanicalHealingMachine.MOD_ID
    );

    public static final Supplier<BlockEntityType<MechHealingMachineBlockEntity>> HEALING_MACHINE_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
            "mechanical_healing_machine",
            () -> BlockEntityType.Builder.of(
                            MechHealingMachineBlockEntity::new,
                            HEALING_MACHINE_BLOCK.get()
                    ).build(null)
    );

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(
            MechanicalHealingMachine.MOD_ID
    );

    public static final Supplier<BlockItem> HEALING_MACHINE_BLOCK_ITEM = ITEMS.registerSimpleBlockItem(
            HEALING_MACHINE_BLOCK
    );

    public MechanicalHealingMachine(IEventBus modEventBus, ModContainer modContainer) {
        BLOCKS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
        ITEMS.register(modEventBus);
//        modContainer.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);
        modContainer.registerConfig(ModConfig.Type.COMMON, ServerConfig.SPEC);
        modEventBus.addListener(MechanicalHealingMachine::buildContents);
        modEventBus.addListener(MechanicalHealingMachine::clientInit);
        modEventBus.addListener(ServerConfig::onLoad);
        modEventBus.addListener(ServerConfig::onReload);
    }

    @SubscribeEvent // on the mod event bus
    public static void buildContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(HEALING_MACHINE_BLOCK_ITEM.get());
        }
    }

    public static void clientInit(final FMLClientSetupEvent event) {
        BlockEntityRenderers.register(
                HEALING_MACHINE_BLOCK_ENTITY.get(),
                MechHealingMachineRenderer::new
        );
    }
}
