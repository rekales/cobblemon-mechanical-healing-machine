package com.krei.cobblemonmechanicalhealingmachine;

import com.simibubi.create.AllCreativeModeTabs;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.PushReaction;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
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
            () -> new HealingMachineBlockCopy(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .sound(SoundType.METAL)
                    .pushReaction(PushReaction.BLOCK)
                    .strength(2f)
                    .noOcclusion()
                    .lightLevel(state -> state.getValue(HealingMachineBlockCopy.CHARGE_LEVEL) >= HealingMachineBlockCopy.MAX_CHARGE_LEVEL ? 7 : 2)
            ));

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            MechanicalHealingMachine.MOD_ID
    );

    public static final Supplier<BlockEntityType<HealingMachineBlockEntityCopy>> HEALING_MACHINE_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
            "mechanical_healing_machine",
            () -> BlockEntityType.Builder.of(
                            HealingMachineBlockEntityCopy::new,
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
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        modEventBus.addListener(MechanicalHealingMachine::buildContents);
    }

    @SubscribeEvent // on the mod event bus
    public static void buildContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(HEALING_MACHINE_BLOCK_ITEM.get());
        }
    }
}
