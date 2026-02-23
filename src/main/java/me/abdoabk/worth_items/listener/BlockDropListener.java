package me.abdoabk.worth_items.listener;

import me.abdoabk.worth_items.pricing.PriceService;
import me.abdoabk.worth_items.util.ConfigUtil;
import me.abdoabk.worth_items.util.ItemUtil;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.inventory.ItemStack;

/**
 * BlockDropListener — injects worth lore into items when blocks are broken.
 *
 * NOTE: This listener was previously declared but never registered in Worth_items.
 * It is now registered in Worth_items#registerListeners() — see that class.
 */
public class BlockDropListener implements Listener {

    private final PriceService prices;
    private final ConfigUtil   config;

    public BlockDropListener(PriceService prices, ConfigUtil config) {
        this.prices = prices;
        this.config = config;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockDrop(BlockDropItemEvent event) {
        for (Item entity : event.getItems()) {
            ItemStack stack = entity.getItemStack();

            if (ItemUtil.hasWorth(stack)) continue;
            if (config.isBlacklisted(stack.getType().name())) continue;

            int price = prices.getPrice(stack.getType());
            if (price <= 0) continue;

            ItemUtil.applyPriceLore(stack, price, config.getCurrencySymbol());
            entity.setItemStack(stack); // push the mutation back to the entity
        }
    }
}