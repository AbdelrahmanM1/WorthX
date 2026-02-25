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
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public class InventoryListener implements Listener {

    private final Plugin plugin;
    private final PriceService priceService;
    private final ConfigUtil configUtil;

    public InventoryListener(Plugin plugin, PriceService priceService, ConfigUtil configUtil) {
        this.plugin = plugin;
        this.priceService = priceService;
        this.configUtil = configUtil;
    }

    /* ───────────────────────── PICKUP ───────────────────────── */

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        ItemStack item = event.getItem().getItemStack();
        applyWorth(item);
        event.getItem().setItemStack(item);

        scheduleSync(player, item.getType());
    }

    /* ───────────────────────── CRAFT ───────────────────────── */

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack result = event.getInventory().getResult();
        Material type = result != null ? result.getType() : null;

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            tagAll(player);
            if (type != null) consolidate(player, type);
        });
    }

    /* ───────────────────────── CREATIVE MODE FIX ───────────────────────── */

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCreativeClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (player.getGameMode() != GameMode.CREATIVE) return;

        InventoryAction action = event.getAction();

        switch (action) {
            case PLACE_ALL:
            case PLACE_ONE:
            case PLACE_SOME:
            case SWAP_WITH_CURSOR:
            case HOTBAR_SWAP:
            case HOTBAR_MOVE_AND_READD:
                break;
            default:
                return;
        }

        // Wait 1 tick until Bukkit finishes cloning the creative item
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            tagAll(player);
        });
    }

    /* ───────────────────────── CONTAINERS ───────────────────────── */

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onContainerClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (event.getClickedInventory() == null) return;
        if (event.getClickedInventory().getType() == InventoryType.PLAYER) return;

        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType().isAir()) return;

        applyWorth(item);
        scheduleSync(player, item.getType());
    }

    /* ───────────────────────── SLOT SWITCH ───────────────────────── */

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItem(event.getNewSlot());
        applyWorth(item);

        plugin.getServer().getScheduler().runTask(plugin, () -> tagAll(player));
    }

    /* ───────────────────────── HELPERS ───────────────────────── */

    private void scheduleSync(Player player, Material type) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            tagAll(player);
            if (type != null) consolidate(player, type);
        });
    }

    private void tagAll(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            applyWorth(item);
        }
    }

    private void consolidate(Player player, Material material) {
        ItemStack[] contents = player.getInventory().getContents();
        int total = 0;
        ItemStack template = null;

        for (ItemStack item : contents) {
            if (item != null && item.getType() == material) {
                total += item.getAmount();
                if (template == null && ItemUtil.hasWorth(item)) {
                    template = item.clone();
                    template.setAmount(1);
                }
            }
        }

        if (template == null || total == 0) return;

        player.getInventory().remove(material);

        int max = material.getMaxStackSize();
        while (total > 0) {
            ItemStack stack = template.clone();
            stack.setAmount(Math.min(max, total));
            player.getInventory().addItem(stack);
            total -= stack.getAmount();
        }
    }

    private void applyWorth(ItemStack item) {
        if (item == null || item.getType().isAir()) return;
        if (!configUtil.isLoreEnabled()) return;
        if (configUtil.isBlacklisted(item.getType().name())) return;
        if (ItemUtil.hasWorth(item)) return;

        int price = priceService.getPrice(item.getType());
        if (price <= 0) return;

        ItemUtil.applyPriceLore(item, price, configUtil.getCurrencySymbol());
    }
}
