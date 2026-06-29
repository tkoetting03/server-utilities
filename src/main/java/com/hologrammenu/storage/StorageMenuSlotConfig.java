package com.hologrammenu.storage;

import net.minecraft.world.item.ItemStack;

public record StorageMenuSlotConfig(int index, StorageMenuSlotType type, ItemStack displayStack, String command, String subMenuId) {
	public static final String NO_SUB_MENU = "";

	public StorageMenuSlotConfig(int index, StorageMenuSlotType type, ItemStack displayStack, String command) {
		this(index, type, displayStack, command, NO_SUB_MENU);
	}

	public static StorageMenuSlotConfig empty(int index) {
		return new StorageMenuSlotConfig(index, StorageMenuSlotType.EMPTY, ItemStack.EMPTY, "", NO_SUB_MENU);
	}

	public StorageMenuSlotConfig withType(StorageMenuSlotType newType) {
		return new StorageMenuSlotConfig(index, newType, displayStack, command, subMenuId);
	}

	public StorageMenuSlotConfig withDisplayStack(ItemStack stack) {
		return new StorageMenuSlotConfig(index, type, stack.copy(), command, subMenuId);
	}

	public StorageMenuSlotConfig withCommand(String newCommand) {
		return new StorageMenuSlotConfig(index, type, displayStack, newCommand == null ? "" : newCommand, subMenuId);
	}

	public StorageMenuSlotConfig withSubMenuId(String newSubMenuId) {
		return new StorageMenuSlotConfig(index, type, displayStack, command, newSubMenuId == null ? NO_SUB_MENU : newSubMenuId);
	}

	public boolean hasSubMenu() {
		return subMenuId != null && !subMenuId.isBlank();
	}

	public boolean blocksInteraction() {
		return type == StorageMenuSlotType.FILLER
			|| type == StorageMenuSlotType.COMMAND
			|| type == StorageMenuSlotType.LINK
			|| type == StorageMenuSlotType.BACK
			|| type == StorageMenuSlotType.CLOSE;
	}

	public boolean isProtectedFromFill() {
		return type != StorageMenuSlotType.EMPTY;
	}

	public boolean isTrivial() {
		if (type == StorageMenuSlotType.LINK || type == StorageMenuSlotType.SHOP_ITEM) {
			return false;
		}
		return type == StorageMenuSlotType.EMPTY
			&& displayStack.isEmpty()
			&& command.isBlank()
			&& !hasSubMenu();
	}

	public boolean isPersistable() {
		if (type == StorageMenuSlotType.LINK) {
			return hasSubMenu();
		}
		if (type == StorageMenuSlotType.SHOP_ITEM) {
			return !displayStack.isEmpty();
		}
		return !isTrivial();
	}
}
