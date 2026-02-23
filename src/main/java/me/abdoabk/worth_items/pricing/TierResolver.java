package me.abdoabk.worth_items.pricing;

import org.bukkit.Material;

import java.util.EnumSet;
import java.util.Set;

/**
 * TierResolver — maps a Material to its pricing Tier.
 *
 * FIX: NETHERITE_SPEAR removed from the hardcoded set because it does not exist
 *      in stable Minecraft releases (only in pre-release snapshots). It is now
 *      handled safely by the name-pattern fallback ("NETHERITE" → EPIC) so no
 *      compile-time or runtime error occurs regardless of server version.
 */
public class TierResolver {

    private static final Set<Material> LEGENDARY_ITEMS = EnumSet.of(
            Material.DRAGON_EGG,
            Material.NETHER_STAR,
            Material.BEACON,
            Material.END_CRYSTAL,
            Material.HEART_OF_THE_SEA
    );

    private static final Set<Material> EPIC_ITEMS = EnumSet.of(
            Material.ELYTRA,
            Material.TOTEM_OF_UNDYING,
            Material.ENCHANTED_GOLDEN_APPLE,
            Material.TRIDENT,
            Material.NETHERITE_INGOT,
            Material.NETHERITE_SCRAP,
            Material.NETHERITE_SWORD,
            Material.NETHERITE_PICKAXE,
            Material.NETHERITE_AXE,
            Material.NETHERITE_SHOVEL,
            Material.NETHERITE_HOE,
            Material.NETHERITE_HELMET,
            Material.NETHERITE_CHESTPLATE,
            Material.NETHERITE_LEGGINGS,
            Material.NETHERITE_BOOTS,
            Material.NETHERITE_BLOCK
            // NOTE: NETHERITE_SPEAR intentionally omitted — not present in all MC versions.
            // The name-pattern check below ("NETHERITE" → EPIC) covers it if it ever exists.
    );

    private static final Set<Material> JUNK_ITEMS = EnumSet.of(
            Material.DIRT,
            Material.GRASS_BLOCK,
            Material.SAND,
            Material.COBBLESTONE,
            Material.NETHERRACK,
            Material.ROTTEN_FLESH,
            Material.BONE,
            Material.STRING,
            Material.FEATHER,
            Material.LEATHER,
            Material.GLASS,
            Material.GLASS_PANE,
            Material.POISONOUS_POTATO,
            Material.SPIDER_EYE,
            Material.COBWEB,
            Material.DEAD_BUSH,
            Material.GRAVEL
    );

    public Tier resolve(Material material) {

        // Explicit set checks first (fast, unambiguous)
        if (LEGENDARY_ITEMS.contains(material)) return Tier.LEGENDARY;
        if (EPIC_ITEMS.contains(material))      return Tier.EPIC;
        if (JUNK_ITEMS.contains(material))      return Tier.JUNK;

        String name = material.name();

        // ── Tool / armour material tiers ────────────────────────────────────
        // NETHERITE must come before DIAMOND to prevent "DIAMOND" matching
        // netherite-adjacent future names.
        if (name.contains("NETHERITE"))       return Tier.EPIC;
        if (name.contains("DIAMOND"))         return Tier.RARE;
        if (name.contains("GOLDEN") || name.contains("GOLD")) return Tier.UNCOMMON;
        if (name.contains("IRON"))            return Tier.UNCOMMON;
        if (name.contains("CHAINMAIL"))       return Tier.UNCOMMON;
        if (name.startsWith("WOODEN_") || name.startsWith("STONE_") || name.startsWith("LEATHER_"))
            return Tier.COMMON;

        // ── Ores ────────────────────────────────────────────────────────────
        if (name.contains("ANCIENT_DEBRIS"))  return Tier.RARE;
        if (name.contains("EMERALD"))         return Tier.RARE;
        if (matchesAny(name, "GOLD_ORE", "IRON_ORE", "LAPIS_ORE",
                "REDSTONE_ORE", "COPPER_ORE")) return Tier.COMMON;

        // ── Storage blocks ───────────────────────────────────────────────────
        if (name.contains("DIAMOND_BLOCK"))   return Tier.RARE;
        if (name.contains("IRON_BLOCK"))      return Tier.UNCOMMON;
        if (name.contains("GOLD_BLOCK"))      return Tier.UNCOMMON;

        // ── Food ────────────────────────────────────────────────────────────
        if (matchesAny(name, "COOKED_", "STEAK", "BREAD", "CAKE",
                "GOLDEN_CARROT", "MUSHROOM_STEW", "RABBIT_STEW",
                "SUSPICIOUS_STEW", "PUMPKIN_PIE")) return Tier.UNCOMMON;
        if (matchesAny(name, "APPLE", "CARROT", "POTATO", "BEETROOT",
                "MELON", "PUMPKIN", "WHEAT", "SUGAR")) return Tier.COMMON;

        // ── Natural / building blocks ────────────────────────────────────────
        if (matchesAny(name, "_WOOL", "_TULIP", "_ORCHID", "_DAISY",
                "DANDELION", "POPPY", "_SAPLING",
                "_LOG", "_PLANKS", "_LEAVES")) return Tier.COMMON;

        // ── Remaining dirt/gravel type ───────────────────────────────────────
        if (matchesAny(name, "CLAY")) return Tier.JUNK;

        // Default
        return Tier.COMMON;
    }

    private boolean matchesAny(String name, String... patterns) {
        for (String pattern : patterns) {
            if (name.contains(pattern)) return true;
        }
        return false;
    }
}