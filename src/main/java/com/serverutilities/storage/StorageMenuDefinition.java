package com.serverutilities.storage;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class StorageMenuDefinition {
	private final int containerSize;
	private final boolean enabled;
	private final String title;
	private final Map<Integer, StorageMenuSlotConfig> slots;

	public StorageMenuDefinition(int containerSize, boolean enabled, String title, Map<Integer, StorageMenuSlotConfig> slots) {
		this.containerSize = containerSize;
		this.enabled = enabled;
		this.title = title == null ? "" : title;
		this.slots = Map.copyOf(slots);
	}

	public static StorageMenuDefinition empty(int containerSize) {
		return new StorageMenuDefinition(containerSize, false, "", Map.of());
	}

	public int containerSize() {
		return containerSize;
	}

	public boolean enabled() {
		return enabled;
	}

	public String title() {
		return title;
	}

	public Optional<Component> displayTitle() {
		if (title.isBlank()) {
			return Optional.empty();
		}
		return Optional.of(com.serverutilities.text.TextFormats.toComponent(title));
	}

	public Map<Integer, StorageMenuSlotConfig> slots() {
		return slots;
	}

	public StorageMenuSlotConfig slot(int index) {
		return slots.getOrDefault(index, StorageMenuSlotConfig.empty(index));
	}

	public ItemStack displayStack(int index) {
		StorageMenuSlotConfig config = slot(index);
		if (config.type() == StorageMenuSlotType.EMPTY) {
			return ItemStack.EMPTY;
		}
		return config.displayStack().copy();
	}

	public StorageMenuDefinition withEnabled(boolean newEnabled) {
		return new StorageMenuDefinition(containerSize, newEnabled, title, slots);
	}

	public StorageMenuDefinition withTitle(String newTitle) {
		return new StorageMenuDefinition(containerSize, enabled, newTitle == null ? "" : newTitle, slots);
	}

	public StorageMenuDefinition withSlot(StorageMenuSlotConfig config) {
		Map<Integer, StorageMenuSlotConfig> updated = new HashMap<>(slots);
		if (config.isTrivial()) {
			updated.remove(config.index());
		} else {
			updated.put(config.index(), config);
		}
		return new StorageMenuDefinition(containerSize, enabled, title, updated);
	}

	public StorageMenuDefinition withSlots(List<StorageMenuSlotConfig> slotList) {
		Map<Integer, StorageMenuSlotConfig> updated = new HashMap<>();
		for (StorageMenuSlotConfig config : slotList) {
			if (config.index() < 0 || config.index() >= containerSize) {
				continue;
			}
			if (!config.isPersistable()) {
				continue;
			}
			if (config.isTrivial()) {
				continue;
			}
			updated.put(config.index(), config);
		}
		return new StorageMenuDefinition(containerSize, enabled, title, updated);
	}

	public List<StorageMenuSlotConfig> asList() {
		List<StorageMenuSlotConfig> list = new ArrayList<>();
		for (int index = 0; index < containerSize; index++) {
			StorageMenuSlotConfig config = slot(index);
			if (!config.isTrivial()) {
				list.add(config);
			}
		}
		return list;
	}

	public boolean hasConfiguredSlots() {
		return !title.isBlank() || !slots.isEmpty();
	}

	public StorageMenuDefinition fillFiller(ItemStack fillerStack) {
		Map<Integer, StorageMenuSlotConfig> updated = new HashMap<>(slots);
		ItemStack stack = fillerStack.isEmpty() ? StorageMenuFillerItems.defaultFiller() : fillerStack.copy();
		for (int index = 0; index < containerSize; index++) {
			if (StorageMenuChrome.isReservedIndex(containerSize, index)) {
				continue;
			}
			StorageMenuSlotConfig existing = slot(index);
			if (existing.type() == StorageMenuSlotType.EMPTY) {
				updated.put(index, new StorageMenuSlotConfig(index, StorageMenuSlotType.FILLER, stack.copy(), "", StorageMenuSlotConfig.NO_SUB_MENU));
			}
		}
		return new StorageMenuDefinition(containerSize, enabled, title, updated);
	}
}
