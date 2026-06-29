package com.hologrammenu.storage;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class StorageMenuChrome {
	private StorageMenuChrome() {
	}

	public static int backSlotIndex(int containerSize) {
		if (containerSize <= 0) {
			return 0;
		}
		if (containerSize == 5) {
			return 0;
		}
		if (containerSize % 9 == 0) {
			return containerSize == 9 ? 0 : containerSize - 9;
		}
		return 0;
	}

	public static int closeSlotIndex(int containerSize) {
		if (containerSize <= 0) {
			return 0;
		}
		return containerSize - 1;
	}

	public static boolean isReservedIndex(int containerSize, int index, boolean subMenu) {
		if (index == closeSlotIndex(containerSize)) {
			return true;
		}
		return subMenu && index == backSlotIndex(containerSize);
	}

	public static boolean isReservedIndex(int containerSize, int index) {
		return isReservedIndex(containerSize, index, true);
	}

	public static StorageMenuDefinition applyRuntimeChrome(StorageMenuDefinition definition, ServerPlayer player) {
		int size = definition.containerSize();
		int closeIndex = closeSlotIndex(size);
		StorageMenuDefinition updated = definition.withSlot(new StorageMenuSlotConfig(
			closeIndex,
			StorageMenuSlotType.CLOSE,
			closeButton(),
			"",
			StorageMenuSlotConfig.NO_SUB_MENU
		));

		if (StorageMenuNavigation.hasParent(player)) {
			int backIndex = backSlotIndex(size);
			updated = updated.withSlot(new StorageMenuSlotConfig(
				backIndex,
				StorageMenuSlotType.BACK,
				StorageMenuFillerItems.backButton(),
				"",
				StorageMenuSlotConfig.NO_SUB_MENU
			));
		}

		return updated;
	}

	public static StorageMenuDefinition stripRuntimeChrome(StorageMenuDefinition definition) {
		int size = definition.containerSize();
		StorageMenuDefinition updated = definition;
		for (int index : new int[] { backSlotIndex(size), closeSlotIndex(size) }) {
			StorageMenuSlotConfig config = updated.slot(index);
			if (config.type() == StorageMenuSlotType.BACK || config.type() == StorageMenuSlotType.CLOSE) {
				updated = updated.withSlot(StorageMenuSlotConfig.empty(index));
			}
		}
		return updated;
	}

	public static ItemStack closeButton() {
		ItemStack stack = new ItemStack(Items.BARRIER);
		stack.set(DataComponents.CUSTOM_NAME, Component.literal("Close").withStyle(ChatFormatting.RED));
		return stack;
	}

	public static boolean isChromeSlot(VirtualStorageContainer container, int slotIndex) {
		StorageMenuSlotType type = container.definition().slot(slotIndex).type();
		return type == StorageMenuSlotType.BACK || type == StorageMenuSlotType.CLOSE;
	}
}
