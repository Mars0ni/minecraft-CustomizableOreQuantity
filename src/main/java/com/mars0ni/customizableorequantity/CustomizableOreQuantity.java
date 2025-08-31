package com.mars0ni.customizableorequantity;

import com.mojang.brigadier.Command;
import com.mars0ni.customizableorequantity.config.ConfigLoader;
import com.mars0ni.customizableorequantity.config.ConfigModel;
import com.mars0ni.customizableorequantity.loot.OreDropRule;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.loot.v2.LootTableEvents;

import net.minecraft.block.Blocks;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.loot.*;
import net.minecraft.loot.condition.InvertedLootCondition;
import net.minecraft.loot.condition.MatchToolLootCondition;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.function.ApplyBonusLootFunction;
import net.minecraft.loot.function.ExplosionDecayLootFunction;
import net.minecraft.loot.function.SetCountLootFunction;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.loot.provider.number.UniformLootNumberProvider;
import net.minecraft.predicate.NumberRange;
import net.minecraft.predicate.item.EnchantmentPredicate;
import net.minecraft.predicate.item.ItemPredicate;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static net.minecraft.server.command.CommandManager.literal;

public class CustomizableOreQuantity implements ModInitializer {
    public static final String MODID = "customizableorequantity";
    public static final Logger LOG = LoggerFactory.getLogger(MODID);

    // Public so ConfigLoader can fill it:
    public static final Map<Identifier, OreDropRule> ORE_RULES = new HashMap<>();

    // Ores that use the special "uniform bonus count" Fortune formula in vanilla:
    private static final Set<Identifier> UNIFORM_FORTUNE_TABLES = Set.of(
            Blocks.LAPIS_ORE.getLootTableId(),
            Blocks.DEEPSLATE_LAPIS_ORE.getLootTableId(),
            Blocks.REDSTONE_ORE.getLootTableId(),
            Blocks.DEEPSLATE_REDSTONE_ORE.getLootTableId(),
            Blocks.NETHER_GOLD_ORE.getLootTableId(),
            Blocks.GLOWSTONE.getLootTableId() // not exactly an ore, but same pattern
    );

    @Override
    public void onInitialize() {
        // 1) Load and apply config
        ConfigModel cfg = ConfigLoader.loadOrCreate();
        ConfigLoader.apply(cfg);

        // 2) Register loot replacement using ORE_RULES
        LootTableEvents.REPLACE.register((resourceManager, lootManager, id, original, source) -> {
            OreDropRule rule = ORE_RULES.get(id);
            if (rule == null) return null;

            Identifier blockId = new Identifier(id.getNamespace(), id.getPath().replace("blocks/", ""));
            var block = Registries.BLOCK.get(blockId);
            if (block == null) return null;

            LootPool.Builder silkTouchPool = LootPool.builder()
                    .rolls(ConstantLootNumberProvider.create(1))
                    .with(ItemEntry.builder(block.asItem()))
                    .conditionally(MatchToolLootCondition.builder(
                            ItemPredicate.Builder.create()
                                    .enchantment(new EnchantmentPredicate(
                                            Enchantments.SILK_TOUCH, NumberRange.IntRange.atLeast(1)))));

            var normalEntry = ItemEntry.builder(rule.dropItem())
                    .apply(SetCountLootFunction.builder(
                            UniformLootNumberProvider.create(rule.min(), rule.max())));

            // Apply Fortune if enabled for this ore
            if (rule.useOreDropsBonus()) {
                if (UNIFORM_FORTUNE_TABLES.contains(id)) {
                    normalEntry.apply(ApplyBonusLootFunction.uniformBonusCount(Enchantments.FORTUNE));
                } else {
                    normalEntry.apply(ApplyBonusLootFunction.oreDrops(Enchantments.FORTUNE));
                }
            }

            LootPool.Builder normalPool = LootPool.builder()
                    .rolls(ConstantLootNumberProvider.create(1))
                    .with(normalEntry)
                    .apply(ExplosionDecayLootFunction.builder())
                    .conditionally(InvertedLootCondition.builder(
                            MatchToolLootCondition.builder(
                                    ItemPredicate.Builder.create()
                                            .enchantment(new EnchantmentPredicate(
                                                    Enchantments.SILK_TOUCH, NumberRange.IntRange.atLeast(1))))));

            return LootTable.builder().pool(silkTouchPool).pool(normalPool).build();
        });

        // 3) /customizableorequantity_reload command (ops only) to re-read config at runtime
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(literal(MODID + "_reload")
                    .requires(src -> src.hasPermissionLevel(2))
                    .executes(ctx -> {
                        ConfigLoader.apply(ConfigLoader.loadOrCreate());
                        ctx.getSource().sendFeedback(() -> Text.literal("[" + MODID + "] Config reloaded."), true);
                        return Command.SINGLE_SUCCESS;
                    }));
        });
    }
}
