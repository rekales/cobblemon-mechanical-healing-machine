package com.krei.cobblemonmechanicalhealingmachine;

import com.cobblemon.mod.common.CobblemonItems;
import com.cobblemon.mod.common.CobblemonSounds;
import com.cobblemon.mod.common.CobblemonTradeOffers;
import com.google.common.collect.ImmutableSet;
import com.krei.cobblemonmechanicalhealingmachine.ponder.PonderScenes;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.*;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.BuiltInRegistries;
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

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@EventBusSubscriber
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

    public static final ResourceLocation NURSE_COPY_RL = ResourceLocation.fromNamespaceAndPath(MOD_ID, "nurse_copy");

    public static final DeferredRegister<PoiType> POINTS_OF_INTERESTS = DeferredRegister.create(
            BuiltInRegistries.POINT_OF_INTEREST_TYPE, MechanicalHealingMachine.MOD_ID
    );

    public static final DeferredRegister<VillagerProfession> PROFESSIONS = DeferredRegister.create(
            BuiltInRegistries.VILLAGER_PROFESSION, MechanicalHealingMachine.MOD_ID
    );

    public static final Holder<PoiType> NURSE_COPY_POI = POINTS_OF_INTERESTS.register(
            "nurse_copy", () -> new PoiType(ImmutableSet.copyOf(
                    HEALING_MACHINE_BLOCK.get().getStateDefinition().getPossibleStates()), 1, 1)
    );

    public static final Holder<VillagerProfession> NURSE_COPY_PROF = PROFESSIONS.register(
            NURSE_COPY_RL.getPath(), () -> {
                ResourceKey<PoiType> poiName = NURSE_COPY_POI.unwrapKey().orElseThrow();
                return new VillagerProfession(
                        NURSE_COPY_RL.toString(),
                        holder -> holder.is(poiName),
                        holder -> holder.is(poiName),
                        ImmutableSet.of(),
                        ImmutableSet.of(),
                        CobblemonSounds.VILLAGER_WORK_NURSE
                );
            }
    );

    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(
            Registries.SOUND_EVENT, MOD_ID
    );

    public static final Holder<SoundEvent> MHM_SHOOP = SOUNDS.register(
            "healing_machine_active_shoop",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(MOD_ID,
                    "healing_machine_active_shoop"))
    );

    public static final Holder<SoundEvent> MHM_TUNE = SOUNDS.register(
            "healing_machine_active_tune",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(MOD_ID,
                    "healing_machine_active_tune"))
    );


    public MechanicalHealingMachine(IEventBus modEventBus, ModContainer modContainer) {
        BLOCKS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
        ITEMS.register(modEventBus);
        POINTS_OF_INTERESTS.register(modEventBus);
        PROFESSIONS.register(modEventBus);
        SOUNDS.register(modEventBus);
        modContainer.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);
        modEventBus.addListener(MechanicalHealingMachine::buildContents);
        modEventBus.addListener(MechanicalHealingMachine::onBuildCreativeTab);
        modEventBus.addListener(MechanicalHealingMachine::clientInit);
        modEventBus.addListener(ServerConfig::onLoad);
        modEventBus.addListener(ServerConfig::onReload);
    }

    public static void buildContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(HEALING_MACHINE_BLOCK_ITEM.get());
        }
    }

    public static void clientInit(final FMLClientSetupEvent event) {
        PonderIndex.addPlugin(new PonderScenes());
        BlockEntityRenderers.register(
                HEALING_MACHINE_BLOCK_ENTITY.get(),
                MechHealingMachineRenderer::new
        );
    }

    public static void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        event.remove(CobblemonItems.HEALING_MACHINE.getDefaultInstance(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
    }

    @SubscribeEvent
    public static void registerTrades(VillagerTradesEvent event) {
        Int2ObjectMap<List<VillagerTrades.ItemListing>> trades = event.getTrades();
        final ResourceLocation typeName = ResourceLocation.parse(event.getType().name());
        if (NURSE_COPY_RL.equals(typeName)) {
            trades.get(1).add(new VillagerTrades.ItemsForEmeralds(CobblemonItems.ORAN_BERRY, 4, 1, 16, 1));
            trades.get(1).add(new VillagerTrades.ItemsForEmeralds(CobblemonItems.PERSIM_BERRY, 4, 1, 16, 1));
            trades.get(1).add(new VillagerTrades.ItemsForEmeralds(CobblemonItems.ENERGY_ROOT, 4, 1, 12, 1));
            trades.get(1).add(new VillagerTrades.EmeraldForItems(CobblemonItems.MEDICINAL_LEEK, 24, 12, 2, 1));

            trades.get(2).add(new CobblemonTradeOffers.TradeOffer(new ItemCost(Items.EMERALD, 8), new ItemStack(CobblemonItems.MEDICINAL_BREW), 8, 5, Optional.of(new ItemCost(Items.GLASS_BOTTLE)), 0.05F));
            trades.get(2).add(new VillagerTrades.ItemsForEmeralds(CobblemonItems.BLUE_MINT_LEAF, 2, 1, 8, 5));
            trades.get(2).add(new VillagerTrades.ItemsForEmeralds(CobblemonItems.CYAN_MINT_LEAF, 2, 1, 8, 5));
            trades.get(2).add(new VillagerTrades.ItemsForEmeralds(CobblemonItems.GREEN_MINT_LEAF, 2, 1, 8, 5));
            trades.get(2).add(new VillagerTrades.ItemsForEmeralds(CobblemonItems.PINK_MINT_LEAF, 2, 1, 8, 5));
            trades.get(2).add(new VillagerTrades.ItemsForEmeralds(CobblemonItems.RED_MINT_LEAF, 2, 1, 8, 5));
            trades.get(2).add(new VillagerTrades.ItemsForEmeralds(CobblemonItems.WHITE_MINT_LEAF, 2, 1, 8, 5));

            trades.get(3).add(new VillagerTrades.EmeraldForItems(CobblemonItems.MULCH_BASE, 4, 16, 20, 2));
            trades.get(3).add(new VillagerTrades.ItemsForEmeralds(Items.GLASS_BOTTLE, 1, 1, 16, 10));

            trades.get(4).add(new VillagerTrades.ItemsForEmeralds(CobblemonItems.REVIVAL_HERB, 8, 1, 16, 15));
            trades.get(4).add(new VillagerTrades.ItemsForEmeralds(CobblemonItems.VIVICHOKE_SEEDS, 24, 1, 4, 15));

            trades.get(5).add(new CobblemonTradeOffers.TradeOffer(new ItemCost(Items.EMERALD, 8), new ItemStack(CobblemonItems.ANTIDOTE, 1), 12, 30, Optional.of(new ItemCost(Items.GLASS_BOTTLE)), 0.05F));
            trades.get(5).add(new CobblemonTradeOffers.TradeOffer(new ItemCost(Items.EMERALD, 8), new ItemStack(CobblemonItems.AWAKENING, 1), 12, 30, Optional.of(new ItemCost(Items.GLASS_BOTTLE)), 0.05F));
            trades.get(5).add(new CobblemonTradeOffers.TradeOffer(new ItemCost(Items.EMERALD, 8), new ItemStack(CobblemonItems.BURN_HEAL, 1), 12, 30, Optional.of(new ItemCost(Items.GLASS_BOTTLE)), 0.05F));
            trades.get(5).add(new CobblemonTradeOffers.TradeOffer(new ItemCost(Items.EMERALD, 8), new ItemStack(CobblemonItems.ICE_HEAL, 1), 12, 30, Optional.of(new ItemCost(Items.GLASS_BOTTLE)), 0.05F));
            trades.get(5).add(new CobblemonTradeOffers.TradeOffer(new ItemCost(Items.EMERALD, 8), new ItemStack(CobblemonItems.PARALYZE_HEAL, 1), 12, 30, Optional.of(new ItemCost(Items.GLASS_BOTTLE)), 0.05F));
            trades.get(5).add(new CobblemonTradeOffers.TradeOffer(new ItemCost(Items.EMERALD, 8), new ItemStack(CobblemonItems.VIVICHOKE_DIP, 1), 12, 30, Optional.of(new ItemCost(Items.GLASS_BOTTLE)), 0.05F));
        }
    }

    // TODO: Intercept battle start when pokemons are healing
    // NOTE: Maybe no longer necessary
}
