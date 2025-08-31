package com.mars0ni.customizableorequantity.loot;

import net.minecraft.item.Item;

public record OreDropRule(Item dropItem, float min, float max, boolean useOreDropsBonus) {
    public static OreDropRule withOreDrops(Item dropItem, float min, float max) {
        return new OreDropRule(dropItem, min, max, true);
    }

    public static OreDropRule noBonus(Item dropItem, float min, float max) {
        return new OreDropRule(dropItem, min, max, false);
    }

    public OreDropRule withFortune(boolean enabled) {
        return new OreDropRule(dropItem, min, max, enabled);
    }
}
