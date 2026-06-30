package com.serverutilities.storage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ShopDefinition {
	public static final ShopDefinition EMPTY = new ShopDefinition(false, Map.of());

	private final boolean enabled;
	private final Map<Integer, ShopListing> listings;

	public ShopDefinition(boolean enabled, Map<Integer, ShopListing> listings) {
		this.enabled = enabled;
		if (listings == null || listings.isEmpty()) {
			this.listings = Map.of();
		} else {
			this.listings = Map.copyOf(listings);
		}
	}

	public boolean enabled() {
		return enabled;
	}

	public Map<Integer, ShopListing> listings() {
		return listings;
	}

	public Optional<ShopListing> listing(int slotIndex) {
		return Optional.ofNullable(listings.get(slotIndex));
	}

	public List<ShopListing> asList() {
		List<ShopListing> values = new ArrayList<>(listings.values());
		values.sort((left, right) -> Integer.compare(left.slotIndex(), right.slotIndex()));
		return values;
	}

	public ShopDefinition withEnabled(boolean value) {
		return new ShopDefinition(value, listings);
	}

	public ShopDefinition withListing(ShopListing listing) {
		if (listing == null) {
			return this;
		}
		Map<Integer, ShopListing> updated = new HashMap<>(listings);
		if (!listing.isConfigured()) {
			updated.remove(listing.slotIndex());
		} else {
			updated.put(listing.slotIndex(), listing);
		}
		return new ShopDefinition(enabled, updated);
	}

	public ShopDefinition withoutListing(int slotIndex) {
		if (!listings.containsKey(slotIndex)) {
			return this;
		}
		Map<Integer, ShopListing> updated = new HashMap<>(listings);
		updated.remove(slotIndex);
		return new ShopDefinition(enabled, Collections.unmodifiableMap(updated));
	}

	public ShopDefinition clearListings() {
		return new ShopDefinition(enabled, Map.of());
	}
}
