package com.hologrammenu.storage;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;

public final class ShopDescriptions {
	private ShopDescriptions() {
	}

	public static Component priceTooltip(ShopListing listing) {
		if (!listing.isConfigured()) {
			return Component.empty();
		}
		ItemStack cost = listing.cost();
		int amount = Math.max(1, cost.getCount());
		return Component.translatable(
			"screen.hologrammenu.shop.price_tooltip",
			amount,
			cost.getHoverName()
		);
	}

	public static ShopListing withProductCostDescription(ShopListing listing) {
		ItemStack product = listing.product();
		if (product.isEmpty()) {
			return listing;
		}
		ItemStack described = withCostDescription(product, listing.isConfigured() ? priceLore(listing) : Component.empty());
		return listing.withProduct(described);
	}

	private static Component priceLore(ShopListing listing) {
		ItemStack cost = listing.cost();
		return Component.translatable(
			"screen.hologrammenu.shop.price_lore",
			Math.max(1, cost.getCount()),
			cost.getHoverName()
		);
	}

	private static ItemStack withCostDescription(ItemStack product, Component price) {
		ItemStack copy = product.copy();
		List<Component> lines = new ArrayList<>();
		ItemLore lore = copy.get(DataComponents.LORE);
		if (lore != null) {
			for (Component line : lore.lines()) {
				if (!isGeneratedCostLine(line)) {
					lines.add(line);
				}
			}
		}
		if (price != null && !price.getString().isBlank()) {
			lines.add(price.copy().withStyle(style -> style.withItalic(false).withColor(ChatFormatting.GRAY)));
		}
		if (lines.isEmpty()) {
			copy.remove(DataComponents.LORE);
		} else {
			copy.set(DataComponents.LORE, new ItemLore(lines));
		}
		return copy;
	}

	private static boolean isGeneratedCostLine(Component line) {
		return line != null
			&& (line.getString().startsWith("Cost: ") || line.getString().startsWith("Cost per item: "));
	}
}
