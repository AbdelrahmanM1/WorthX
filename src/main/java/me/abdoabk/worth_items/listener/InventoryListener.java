package me.abdoabk.worth_items.listener;

import me.abdoabk.worth_items.pricing.PriceService;
import me.abdoabk.worth_items.util.ConfigUtil;
import me.abdoabk.worth_items.util.ItemUtil;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/**
 * InventoryListener — injects worth lore into items as players acquire them.
 *
 * STACKING FIX:
 * Items with lore and items without lore are treated as DIFFERENT items by Bukkit,
 * so they refuse to auto-stack. This caused a visible split: crafted items (tagged)
 * wouldn't merge with existing inventory stacks (untagged), and clicking them twice
 * was needed to force a manual merge.
 *
 * Solution: after tagging any item, immediately scan the whole inventory for
 * untagged items of the SAME material and tag them too. Once every copy of a
 * material has identical lore, Bukkit stacks them normally.
 */
public class InventoryListener implements Listener {

    private final Plugin       plugin;
    private final PriceService priceService;
    private final ConfigUtil   configUtil;

    public InventoryListener(Plugin plugin, PriceService priceService, ConfigUtil configUtil) {
        this.plugin       = plugin;
        this.priceService = priceService;
        this.configUtil   = configUtil;
    }

    /** Item picked up from the ground. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        ItemStack item = event.getItem().getItemStack();
        applyWorthLore(item);
        event.getItem().setItemStack(item);

        // Tag existing inventory stacks of the same material so they can merge.
        scheduleInventorySync(player, item.getType());
    }

    /**
     * Item crafted — delayed one tick to avoid the rapid shift+right-click
     * duplication glitch (Bukkit fires this event multiple times before the
     * inventory settles). After the tick, tag all untagged items in the inventory
     * so the new crafted items auto-stack with existing ones.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // Capture the crafted material before the tick delay.
        ItemStack result = event.getInventory().getResult();
        Material craftedType = (result != null && !result.getType().isAir())
                ? result.getType() : null;

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            // Tag everything — covers both the new item and any existing stacks.
            tagAllInInventory(player);

            // Force Bukkit to consolidate stacks of the crafted material.
            if (craftedType != null) {
                consolidateStacks(player, craftedType);
            }
        });
    }

    /**
     * Creative mode / container transfers.
     * Also syncs inventory so newly tagged items stack with existing ones.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (player.getGameMode() == GameMode.CREATIVE) {
            applyWorthLore(event.getCursor());
            applyWorthLore(event.getCurrentItem());
            scheduleInventorySync(player, null);
            return;
        }

        if (event.getClickedInventory() != null
                && event.getClickedInventory().getType() != InventoryType.PLAYER) {
            ItemStack current = event.getCurrentItem();
            if (current != null && !current.getType().isAir()) {
                applyWorthLore(current);
                scheduleInventorySync(player, current.getType());
            }
        }
    }

    /** Player switches held slot — tags legacy items without causing hover animation. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItem(event.getNewSlot());
        if (item != null && !item.getType().isAir()) {
            applyWorthLore(item);
            // Tag all untagged items but skip consolidation to avoid the hover animation.
            plugin.getServer().getScheduler().runTask(plugin, () -> tagAllInInventory(player));
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Schedules a one-tick task that tags all untagged items in the inventory,
     * then consolidates stacks of the given material (null = all materials).
     */
    private void scheduleInventorySync(Player player, Material material) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            tagAllInInventory(player);
            if (material != null) {
                consolidateStacks(player, material);
            }
        });
    }

    /** Tags every untagged, non-blacklisted item in the player's inventory. */
    private void tagAllInInventory(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            applyWorthLore(item);
        }
    }

    /**
     * Consolidates all stacks of the given material by removing them all and
     * re-adding them. Bukkit will then merge them into the fewest slots possible.
     *
     * This is needed because Bukkit only auto-stacks on item ADD — existing split
     * stacks in the inventory don't re-merge on their own even after lore is
     * made uniform.
     */
    private void consolidateStacks(Player player, Material material) {
        ItemStack[] contents = player.getInventory().getContents();
        int totalAmount = 0;
        ItemStack template = null;

        // Count total amount and find a tagged template to clone from.
        for (ItemStack item : contents) {
            if (item != null && item.getType() == material) {
                totalAmount += item.getAmount();
                if (template == null && ItemUtil.hasWorth(item)) {
                    template = item.clone();
                    template.setAmount(1);
                }
            }
        }

        if (template == null || totalAmount == 0) return;

        // Remove all stacks of this material.
        player.getInventory().remove(material);

        // Re-add as consolidated stacks (Bukkit splits into max-stack sizes automatically).
        int maxStack  = material.getMaxStackSize();
        int remaining = totalAmount;

        while (remaining > 0) {
            ItemStack stack = template.clone();
            stack.setAmount(Math.min(remaining, maxStack));
            player.getInventory().addItem(stack);
            remaining -= stack.getAmount();
        }
    }

    private void applyWorthLore(ItemStack item) {
        if (item == null || item.getType().isAir()) return;
        if (!configUtil.isLoreEnabled()) return;
        if (configUtil.isBlacklisted(item.getType().name())) return;
        if (ItemUtil.hasWorth(item)) return;

        int price = priceService.getPrice(item.getType());
        if (price <= 0) return;

        ItemUtil.applyPriceLore(item, price, configUtil.getCurrencySymbol());
    }
}