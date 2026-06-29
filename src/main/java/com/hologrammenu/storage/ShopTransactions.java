package com.hologrammenu.storage;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public final class ShopTransactions {
	private ShopTransactions() {
	}

	public static boolean tryPurchase(
		ServerPlayer player,
		ServerLevel level,
		VirtualStorageContainer container,
		int slotIndex
	) {
		if (!container.viewContext().isRoot()) {
			return false;
		}

		Optional<StorageMenuBlockData> blockData = StorageMenuBlockStore.get(level, container.blockPos());
		if (blockData.isEmpty()) {
			return false;
		}

		ShopDefinition shop = blockData.get().shop();
		if (!shop.enabled()) {
			return false;
		}

		StorageMenuSlotConfig config = container.definition().slot(slotIndex);
		if (config.type() != StorageMenuSlotType.SHOP_ITEM) {
			return false;
		}

		Optional<ShopListing> listing = shop.listing(slotIndex);
		if (listing.isEmpty() || !listing.get().isConfigured()) {
			return false;
		}

		return executePurchase(player, level, container.blockPos(), listing.get(), shop);
	}

	private static boolean executePurchase(
		ServerPlayer player,
		ServerLevel level,
		net.minecraft.core.BlockPos pos,
		ShopListing listing,
		ShopDefinition shop
	) {
		if (!listing.hasStock()) {
			StorageMenuInteractions.sendFeedback(player, "screen.hologrammenu.shop.out_of_stock");
			return true;
		}

		ItemStack cost = listing.cost();
		if (!removeCost(player, cost)) {
			StorageMenuInteractions.sendFeedback(player, "screen.hologrammenu.shop.cannot_afford");
			return true;
		}

		ItemStack product = listing.product().copy();
		if (!player.getInventory().add(product)) {
			player.drop(product, false);
		}

		ShopDefinition updatedShop = shop;
		if (listing.stock() != ShopListing.UNLIMITED_STOCK) {
			int remaining = listing.stock() - 1;
			if (remaining <= 0) {
				updatedShop = shop.withoutListing(listing.slotIndex());
			} else {
				updatedShop = shop.withListing(listing.withStock(remaining));
			}
		}

		StorageMenuManager.saveShop(level, pos, updatedShop);
		StorageMenuBlockData data = blockData(level, pos);
		StorageMenuLiveRefresh.refreshViewers(level, pos, data.definition());
		StorageMenuInteractions.sendFeedback(player, "screen.hologrammenu.shop.purchased");
		return true;
	}

	private static StorageMenuBlockData blockData(ServerLevel level, net.minecraft.core.BlockPos pos) {
		return StorageMenuBlockStore.get(level, pos).orElse(
			new StorageMenuBlockData(StorageMenuDefinition.empty(StorageMenuSizes.SINGLE_CHEST), false, false, ShopDefinition.EMPTY)
		);
	}

	private static boolean removeCost(ServerPlayer player, ItemStack cost) {
		int required = cost.getCount();
		if (required <= 0) {
			return true;
		}

		Inventory inventory = player.getInventory();
		int available = 0;
		for (int index = 0; index < inventory.getContainerSize(); index++) {
			ItemStack stack = inventory.getItem(index);
			if (ItemStack.isSameItemSameComponents(stack, cost)) {
				available += stack.getCount();
			}
		}

		if (available < required) {
			return false;
		}

		int remaining = required;
		for (int index = 0; index < inventory.getContainerSize() && remaining > 0; index++) {
			ItemStack stack = inventory.getItem(index);
			if (!ItemStack.isSameItemSameComponents(stack, cost)) {
				continue;
			}
			int remove = Math.min(remaining, stack.getCount());
			stack.shrink(remove);
			remaining -= remove;
		}
		return remaining <= 0;
	}
}
