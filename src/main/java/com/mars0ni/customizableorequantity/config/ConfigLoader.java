package com.mars0ni.customizableorequantity.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mars0ni.customizableorequantity.CustomizableOreQuantity;
import com.mars0ni.customizableorequantity.loot.OreDropRule;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;

public final class ConfigLoader {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir().resolve(CustomizableOreQuantity.MODID + ".json");

    private ConfigLoader() {}

    public static ConfigModel loadOrCreate() {
        try {
            if (!Files.exists(CONFIG_PATH)) {
                ConfigModel def = defaultConfig();
                try (BufferedWriter w = Files.newBufferedWriter(CONFIG_PATH)) {
                    GSON.toJson(def, w);
                }
                CustomizableOreQuantity.LOG.info("[{}] Created default config at {}", CustomizableOreQuantity.MODID, CONFIG_PATH);
                return def;
            }

            try (BufferedReader r = Files.newBufferedReader(CONFIG_PATH)) {
                ConfigModel cfg = GSON.fromJson(r, ConfigModel.class);
                if (cfg == null) cfg = defaultConfig();
                CustomizableOreQuantity.LOG.info("[{}] Loaded config from {}", CustomizableOreQuantity.MODID, CONFIG_PATH);
                return cfg;
            }
        } catch (Exception e) {
            CustomizableOreQuantity.LOG.error("[{}] Failed to read config: {}", CustomizableOreQuantity.MODID, e.getMessage(), e);
            return defaultConfig();
        }
    }

    /** Apply a ConfigModel into CustomizableOreQuantity.ORE_RULES */
    public static void apply(ConfigModel cfg) {
        CustomizableOreQuantity.ORE_RULES.clear();

        float defMin = cfg.default_min;
        float defMax = cfg.default_max;

        if (cfg.ores == null || cfg.ores.isEmpty()) {
            CustomizableOreQuantity.LOG.warn("[{}] No ores configured; using defaults only.", CustomizableOreQuantity.MODID);
            return;
        }

        cfg.ores.forEach((lootIdStr, ruleJson) -> {
            try {
                Identifier lootId = new Identifier(lootIdStr);
                if (ruleJson == null || ruleJson.drop == null) {
                    CustomizableOreQuantity.LOG.warn("[{}] Skipping {}: missing 'drop'.", CustomizableOreQuantity.MODID, lootIdStr);
                    return;
                }
                Item drop = Registries.ITEM.get(new Identifier(ruleJson.drop));
                if (drop == Items.AIR) {
                    CustomizableOreQuantity.LOG.warn("[{}] Skipping {}: unknown drop item id {}", CustomizableOreQuantity.MODID, lootIdStr, ruleJson.drop);
                    return;
                }

                float min = ruleJson.min != null ? ruleJson.min : defMin;
                float max = ruleJson.max != null ? ruleJson.max : defMax;
                boolean fortune = ruleJson.fortune == null || ruleJson.fortune; // default true

                if (min > max) {
                    float t = min; min = max; max = t;
                    CustomizableOreQuantity.LOG.warn("[{}] Swapped min/max for {} to keep min<=max.", CustomizableOreQuantity.MODID, lootId);
                }

                CustomizableOreQuantity.ORE_RULES.put(lootId,
                        OreDropRule.withOreDrops(drop, min, max).withFortune(fortune));
            } catch (Exception ex) {
                CustomizableOreQuantity.LOG.warn("[{}] Failed to parse ore rule {}: {}", CustomizableOreQuantity.MODID, lootIdStr, ex.toString());
            }
        });

        CustomizableOreQuantity.LOG.info("[{}] Applied {} ore rule(s).",
                CustomizableOreQuantity.MODID, CustomizableOreQuantity.ORE_RULES.size());
    }

    // ===== Default config with all vanilla ores (vanilla-style counts) =====
    private static ConfigModel defaultConfig() {
        ConfigModel m = new ConfigModel();
        m.default_min = 2.0f;
        m.default_max = 5.0f;
        m.ores = new LinkedHashMap<>();

        // === Overworld raw ores === (vanilla: 1 each; Copper 2–5)
        m.ores.put("minecraft:blocks/iron_ore",              entry("minecraft:raw_iron", 1f, 1f, true)); // vanilla: 1
        m.ores.put("minecraft:blocks/deepslate_iron_ore",    entry("minecraft:raw_iron", 1f, 1f, true)); // vanilla: 1
        m.ores.put("minecraft:blocks/gold_ore",              entry("minecraft:raw_gold", 1f, 1f, true)); // vanilla: 1
        m.ores.put("minecraft:blocks/deepslate_gold_ore",    entry("minecraft:raw_gold", 1f, 1f, true)); // vanilla: 1
        m.ores.put("minecraft:blocks/copper_ore",            entry("minecraft:raw_copper", 2f, 5f, true)); // vanilla: 2–5
        m.ores.put("minecraft:blocks/deepslate_copper_ore",  entry("minecraft:raw_copper", 2f, 5f, true));

        // === Overworld gems / items === (vanilla: 1 each)
        m.ores.put("minecraft:blocks/diamond_ore",           entry("minecraft:diamond", 1f, 1f, true)); // vanilla: 1
        m.ores.put("minecraft:blocks/deepslate_diamond_ore", entry("minecraft:diamond", 1f, 1f, true)); // vanilla: 1
        m.ores.put("minecraft:blocks/emerald_ore",           entry("minecraft:emerald", 1f, 1f, true)); // vanilla: 1
        m.ores.put("minecraft:blocks/deepslate_emerald_ore", entry("minecraft:emerald", 1f, 1f, true)); // vanilla: 1
        m.ores.put("minecraft:blocks/coal_ore",              entry("minecraft:coal", 1f, 1f, true)); // vanilla: 1
        m.ores.put("minecraft:blocks/deepslate_coal_ore",    entry("minecraft:coal", 1f, 1f, true)); // vanilla: 1

        // === Special-count overworld ores ===
        m.ores.put("minecraft:blocks/lapis_ore",              entry("minecraft:lapis_lazuli",4f, 9f, true)); // vanilla: 4–9
        m.ores.put("minecraft:blocks/deepslate_lapis_ore",    entry("minecraft:lapis_lazuli",4f, 9f, true));
        m.ores.put("minecraft:blocks/redstone_ore",           entry("minecraft:redstone",    4f, 5f, true)); // vanilla: 4–5
        m.ores.put("minecraft:blocks/deepslate_redstone_ore", entry("minecraft:redstone",    4f, 5f, true));

        // === Nether ores ===
        m.ores.put("minecraft:blocks/nether_quartz_ore",     entry("minecraft:quartz", 2f, 4f, true)); // vanilla: 2-4
        m.ores.put("minecraft:blocks/nether_gold_ore",       entry("minecraft:gold_nugget", 2f, 6f, true)); // vanilla: 2–6

        // === Ancient debris (special case) ===
        m.ores.put("minecraft:blocks/ancient_debris",         entry("minecraft:ancient_debris", 1f, 1f, false)); // vanilla: 1, no Fortune

        return m;
    }

    private static ConfigModel.OreRuleJson entry(String drop, Float min, Float max, Boolean fortune) {
        ConfigModel.OreRuleJson e = new ConfigModel.OreRuleJson();
        e.drop = drop; e.min = min; e.max = max; e.fortune = fortune;
        return e;
    }
}
