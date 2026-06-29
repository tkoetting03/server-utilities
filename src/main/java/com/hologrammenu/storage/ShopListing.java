package com.hologrammenu.storage;

import net.minecraft.world.item.ItemStack;

public record ShopListing(int slotIndex, ItemStack product, ItemStack cost, int stock) {
	public static final int UNLIMITED_STOCK = -1;

	public ShopListing {
		product = product == null ? ItemStack.EMPTY : product;
		cost = cost == null ? ItemStack.EMPTY : cost;
	}

	public static ShopListing empty(int slotIndex) {
		return new ShopListing(slotIndex, ItemStack.EMPTY, ItemStack.EMPTY, UNLIMITED_STOCK);
	}

	public boolean isConfigured() {
		return !product.isEmpty() && !cost.isEmpty();
	}

	public boolean hasStock() {
		return stock == UNLIMITED_STOCK || stock > 0;
	}

	public ShopListing withProduct(ItemStack value) {
		return new ShopListing(slotIndex, value == null ? ItemStack.EMPTY : value, cost, stock);
	}

	public ShopListing withCost(ItemStack value) {
		return new ShopListing(slotIndex, product, value == null ? ItemStack.EMPTY : value, stock);
	}

	public ShopListing withStock(int value) {
		return new ShopListing(slotIndex, product, cost, value);
	}
}
