package net.arathain.charter;

import net.arathain.charter.block.*;
import net.arathain.charter.block.entity.CharterStoneEntity;
import net.arathain.charter.block.entity.PactPressBlockEntity;
import net.arathain.charter.block.entity.WaystoneEntity;
import net.arathain.charter.components.CharterComponents;
import net.arathain.charter.components.CharterWorldComponent;
import net.arathain.charter.components.packet.UpdateShadePacket;
import net.arathain.charter.item.ContractItem;
import net.arathain.charter.item.EternalSealItem;
import net.arathain.charter.item.MerchantCrestItem;
import net.arathain.charter.util.CharterEventHandlers;
import net.arathain.charter.util.CharterUtil;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.EntityElytraEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.loot.v2.LootTableEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTables;
import net.minecraft.loot.entry.LootTableEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import net.minecraft.util.registry.Registry;
import software.bernie.geckolib3.GeckoLib;

import java.util.function.ToIntFunction;

public class Charter implements ModInitializer {
    public static String MODID = "charter";
    public static final Item CONTRACT = new ContractItem(new Item.Settings().maxCount(1).rarity(Rarity.RARE).group(ItemGroup.COMBAT));
    public static final Item MERCHANT_CREST = new MerchantCrestItem(new Item.Settings().maxCount(1).rarity(Rarity.EPIC).group(ItemGroup.COMBAT));
    public static final Item ETERNAL_SEAL = new EternalSealItem(new Item.Settings().maxCount(1).rarity(Rarity.EPIC).group(ItemGroup.COMBAT));
    public static final StatusEffect ETERNAL_DEBT = new CharterStatusEffect(StatusEffectCategory.NEUTRAL, 0x4bf1f7);
    public static final StatusEffect SOUL_STRAIN = new CharterStatusEffect(StatusEffectCategory.NEUTRAL, 0x6cf5f5);
    public static final Block PACT_PRESS = new PactPressBlock(FabricBlockSettings.copyOf(Blocks.CHISELED_DEEPSLATE).requiresTool().luminance(createLightLevelFromLitBlockState(10)).ticksRandomly());
    public static final Block WAYSTONE = new WaystoneBlock(FabricBlockSettings.copyOf(Blocks.CHISELED_DEEPSLATE).requiresTool().luminance(3).nonOpaque());
    public static final Block CHARTER_VESSEL = new CharterVesselBlock(FabricBlockSettings.copyOf(Blocks.QUARTZ_BLOCK).requiresTool().nonOpaque().ticksRandomly());
    public static final Block BROKEN_WAYSTONE = new BrokenWaystoneBlock(FabricBlockSettings.copyOf(Blocks.OBSIDIAN).requiresTool().luminance(0));
    public static final Block SWAPPER = new SwapperBlock(FabricBlockSettings.copyOf(Blocks.CHISELED_DEEPSLATE).requiresTool().luminance(createLightLevelFromPoweredBlockState(10)));
    public static final Block CHARTER_STONE = new CharterStoneBlock(FabricBlockSettings.copyOf(Blocks.BEDROCK).luminance(7).nonOpaque());
    public static final Block BROKEN_CHARTER_STONE = new BrokenCharterStoneBlock(FabricBlockSettings.copyOf(Blocks.DEEPSLATE).requiresTool().luminance(0).ticksRandomly());
    public static BlockEntityType<PactPressBlockEntity> PACT_PRESS_ENTITY;
    public static BlockEntityType<CharterStoneEntity> CHARTER_STONE_ENTITY;
    public static BlockEntityType<WaystoneEntity> WAYSTONE_ENTITY;
    public static final int CHARTER_COUNT_LIMIT = 10;

    @Override
    public void onInitialize() {
        Registry.register(Registry.ITEM, new Identifier(MODID, "contract"), CONTRACT);
        Registry.register(Registry.ITEM, new Identifier(MODID, "merchant_crest"), MERCHANT_CREST);
        Registry.register(Registry.ITEM, new Identifier(MODID, "eternal_seal"), ETERNAL_SEAL);
        Registry.register(Registry.ITEM, new Identifier(MODID, "pact_press"), new BlockItem(PACT_PRESS, new FabricItemSettings().group(ItemGroup.COMBAT)));
        Registry.register(Registry.BLOCK, new Identifier(MODID, "pact_press"), PACT_PRESS);
        Registry.register(Registry.BLOCK, new Identifier(MODID, "waystone"), WAYSTONE);
        Registry.register(Registry.BLOCK, new Identifier(MODID, "charter_vessel"), CHARTER_VESSEL);
        Registry.register(Registry.ITEM, new Identifier(MODID, "charter_vessel"), new BlockItem(CHARTER_VESSEL, new FabricItemSettings().group(ItemGroup.COMBAT)));
        Registry.register(Registry.ITEM, new Identifier(MODID, "waystone"), new BlockItem(WAYSTONE, new FabricItemSettings().group(ItemGroup.COMBAT)));
        Registry.register(Registry.ITEM, new Identifier(MODID, "swapper"), new BlockItem(SWAPPER, new FabricItemSettings().group(ItemGroup.REDSTONE)));
        Registry.register(Registry.BLOCK, new Identifier(MODID, "swapper"), SWAPPER);
        PACT_PRESS_ENTITY = Registry.register(Registry.BLOCK_ENTITY_TYPE, new Identifier(MODID, "pact_press"), FabricBlockEntityTypeBuilder.create(PactPressBlockEntity::new, PACT_PRESS).build(null));
        CHARTER_STONE_ENTITY = Registry.register(Registry.BLOCK_ENTITY_TYPE, new Identifier(MODID, "charter_stone"), FabricBlockEntityTypeBuilder.create(CharterStoneEntity::new, CHARTER_STONE).build(null));
        WAYSTONE_ENTITY = Registry.register(Registry.BLOCK_ENTITY_TYPE, new Identifier(MODID, "waystone"), FabricBlockEntityTypeBuilder.create(WaystoneEntity::new, WAYSTONE).build(null));
        Registry.register(Registry.STATUS_EFFECT, new Identifier(MODID, "eternal_debt"), ETERNAL_DEBT);
        Registry.register(Registry.STATUS_EFFECT, new Identifier(MODID, "soul_strain"), SOUL_STRAIN);
        GeckoLib.initialize();
        LootTableEvents.MODIFY.register((resourceManager, lootManager, identifier, builder, source) -> {
            Identifier bastion_treasure = new Identifier(MODID, "inject/bastion_treasure");
            if (LootTables.BASTION_TREASURE_CHEST.equals(identifier)) {
                builder.pool(LootPool.builder().with(LootTableEntry.builder(bastion_treasure).weight(2)).build());
            }
        });
        Registry.register(Registry.ITEM, new Identifier(MODID, "charter_stone"), new BlockItem(CHARTER_STONE, new FabricItemSettings().group(ItemGroup.COMBAT)));
        Registry.register(Registry.ITEM, new Identifier(MODID, "apex_focus"), new Item(new FabricItemSettings().group(ItemGroup.MISC)));
        Registry.register(Registry.BLOCK, new Identifier(MODID, "charter_stone"), CHARTER_STONE);
        Registry.register(Registry.BLOCK, new Identifier(MODID, "broken_charter_stone"), BROKEN_CHARTER_STONE);
        Registry.register(Registry.BLOCK, new Identifier(MODID, "broken_waystone"), BROKEN_WAYSTONE);
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerWorld world : server.getWorlds()) {
                CharterWorldComponent charterWorldComponent = CharterComponents.CHARTERS.get(world);

                charterWorldComponent.tick();
            }
        });
        EntityElytraEvents.CUSTOM.register(((entity, tickElytra) -> CharterUtil.ownsCharter(entity, entity.world)));
        CharterEventHandlers.init();
        ServerPlayNetworking.registerGlobalReceiver(UpdateShadePacket.ID, UpdateShadePacket::handle);

    }

    private static ToIntFunction<BlockState> createLightLevelFromLitBlockState(int litLevel) {
        return (state) -> (Boolean) state.get(Properties.LIT) ? litLevel : 0;
    }

    private static ToIntFunction<BlockState> createLightLevelFromPoweredBlockState(int litLevel) {
        return (state) -> (Boolean) state.get(Properties.POWERED) ? litLevel : 0;
    }


}
