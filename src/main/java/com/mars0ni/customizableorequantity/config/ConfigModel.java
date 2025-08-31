package com.mars0ni.customizableorequantity.config;

import java.util.Map;

public class ConfigModel {
    public float default_min = 2.0f;
    public float default_max = 5.0f;
    public Map<String, OreRuleJson> ores; // key = loot table id, e.g. "minecraft:blocks/iron_ore"


    public static class OreRuleJson {
        public String drop;       // e.g. "minecraft:raw_iron"
        public Float min;         // optional; falls back to default_min
        public Float max;         // optional; falls back to default_max
        public Boolean fortune;   // optional; default true
    }
}
