package com.serverutilities.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class ShopPersistence {
	private ShopPersistence() {
	}

	public record ShopEntry(boolean enabled, List<ListingEntry> listings) {
		public static final Codec<ShopEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.BOOL.fieldOf("enabled").forGetter(ShopEntry::enabled),
			ListingEntry.CODEC.listOf().fieldOf("listings").forGetter(ShopEntry::listings)
		).apply(instance, ShopEntry::new));

		public static ShopEntry fromDefinition(ShopDefinition definition) {
			List<ListingEntry> listings = definition.asList().stream()
				.filter(ShopListing::isConfigured)
				.map(ListingEntry::fromListing)
				.toList();
			return new ShopEntry(definition.enabled(), listings);
		}

		public ShopDefinition toDefinition() {
			java.util.Map<Integer, ShopListing> map = new java.util.HashMap<>();
			for (ListingEntry entry : listings) {
				ShopListing listing = entry.toListing();
				if (listing.isConfigured()) {
					map.put(listing.slotIndex(), listing);
				}
			}
			return new ShopDefinition(enabled, map);
		}
	}

	public record ListingEntry(int slot, ItemStack product, ItemStack cost, int stock) {
		public static final Codec<ListingEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.INT.fieldOf("slot").forGetter(ListingEntry::slot),
			ItemStack.CODEC.fieldOf("product").forGetter(ListingEntry::product),
			ItemStack.CODEC.fieldOf("cost").forGetter(ListingEntry::cost),
			Codec.INT.fieldOf("stock").forGetter(ListingEntry::stock)
		).apply(instance, (slot, product, cost, stock) -> new ListingEntry(slot, product, cost, stock)));

		public static ListingEntry fromListing(ShopListing listing) {
			return new ListingEntry(listing.slotIndex(), listing.product(), listing.cost(), listing.stock());
		}

		public ShopListing toListing() {
			return new ShopListing(slot, product, cost, stock);
		}
	}
}
