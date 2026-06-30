package com.serverutilities.itemstyler;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class ItemStylerMenu extends AbstractContainerMenu {
	public static final int STYLE_SLOT = 0;
	private static final int PLAYER_INVENTORY_START = 1;

	private final ItemStylerContainer container;

	public ItemStylerMenu(int syncId, Inventory playerInventory) {
		this(syncId, playerInventory, new ItemStylerContainer());
	}

	public ItemStylerMenu(int syncId, Inventory playerInventory, ItemStylerContainer container) {
		super(ModMenuTypes.ITEM_STYLER, syncId);
		this.container = container;
		addSlot(new Slot(container, ItemStylerContainer.STYLE_SLOT, 80, 18));
		addStandardInventorySlots(playerInventory, 8, 59);
	}

	public ItemStack styledStack() {
		return container.getItem(ItemStylerContainer.STYLE_SLOT);
	}

	public void setStyledStack(ItemStack stack) {
		container.setItem(ItemStylerContainer.STYLE_SLOT, stack);
		broadcastChanges();
	}

	@Override
	public ItemStack quickMoveStack(Player player, int slotIndex) {
		if (slotIndex < 0 || slotIndex >= slots.size()) {
			return ItemStack.EMPTY;
		}
		Slot slot = slots.get(slotIndex);
		if (!slot.hasItem()) {
			return ItemStack.EMPTY;
		}

		ItemStack stack = slot.getItem();
		ItemStack original = stack.copy();
		if (slotIndex == STYLE_SLOT) {
			if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, slots.size(), true)) {
				return ItemStack.EMPTY;
			}
		} else if (!moveItemStackTo(stack, STYLE_SLOT, STYLE_SLOT + 1, false)) {
			return ItemStack.EMPTY;
		}

		if (stack.isEmpty()) {
			slot.setByPlayer(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}
		return original;
	}

	@Override
	public boolean stillValid(Player player) {
		return container.stillValid(player);
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		container.stopOpen(player);
		if (player instanceof ServerPlayer serverPlayer) {
			ItemStylerSessions.clear(serverPlayer);
		}
	}
}
