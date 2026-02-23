package me.abdoabk.worth_items.util;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * ItemUtil
 *
 * Duplicate-safe worth lore injection.
 * Uses BOTH PDC (for identity) AND lore text scan (for stacked items
 * whose PDC was lost during merge) to prevent double "Worth:" lines.
 */
public final class ItemUtil {

    private static final NumberFormat NUMBER_FORMAT =
            NumberFormat.getInstance(Locale.US);

    private static final String WORTH_PREFIX = "Worth: ";
    private static NamespacedKey WORTH_KEY;

    private ItemUtil() {}

    /** MUST be called once in onEnable(). */
    public static void init(JavaPlugin plugin) {
        WORTH_KEY = new NamespacedKey(plugin, "worth");
    }

    /**
     * Returns true if this item already has a worth lore line OR PDC tag.
     * Checking both prevents duplicates when items stack and PDC is lost.
     */
    public static boolean hasWorth(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();

        // 1. PDC check (fast path)
        if (meta.getPersistentDataContainer().has(WORTH_KEY, PersistentDataType.INTEGER)) {
            return true;
        }

        // 2. Lore text check (fallback for stacked items that lost PDC on merge)
        if (meta.hasLore()) {
            for (String line : meta.getLore()) {
                if (ChatColor.stripColor(line).startsWith(WORTH_PREFIX)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Applies worth lore and stores price in PDC.
     * Safe to call multiple times — will not duplicate.
     */
    public static void applyPriceLore(ItemStack item, int price, String currency) {
        if (item == null || item.getType() == Material.AIR) return;
        if (price <= 0) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        if (hasWorth(item)) return;

        // Store value in PDC for fast future reads
        meta.getPersistentDataContainer()
                .set(WORTH_KEY, PersistentDataType.INTEGER, price);

        // Build lore line
        List<String> lore = meta.hasLore()
                ? new ArrayList<>(meta.getLore())
                : new ArrayList<>();

        lore.add(ChatColor.GRAY + WORTH_PREFIX + ChatColor.GREEN
                + currency + NUMBER_FORMAT.format(price));

        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    /**
     * Reads worth value from PDC.
     * Returns 0 if item has no worth stored.
     */
    public static int getWorth(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        Integer value = item.getItemMeta()
                .getPersistentDataContainer()
                .get(WORTH_KEY, PersistentDataType.INTEGER);
        return value != null ? value : 0;
    }

    /**
     * Human-readable Material name.
     * Example: WHITE_WOOL → "White Wool"
     */
    public static String formatName(Material material) {
        String[] words = material.name().split("_");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            sb.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1).toLowerCase())
                    .append(" ");
        }
        return sb.toString().trim();
    }
}