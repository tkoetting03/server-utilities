package com.hologrammenu.itemstyler;

import net.minecraft.world.Container;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class ItemStylerContainer implements Container {
	public static final int SIZE = 1;
	public static final int STYLE_SLOT = 0;

	private ItemStack stack = ItemStack.EMPTY;

	@Override
	public int getContainerSize() {
		return SIZE;
	}

	@Override
	public boolean isEmpty() {
		return stack.isEmpty();
	}

	@Override
	public ItemStack getItem(int slot) {
		return slot == STYLE_SLOT ? stack : ItemStack.EMPTY;
	}

	@Override
	public ItemStack removeItem(int slot, int amount) {
		if (slot != STYLE_SLOT || stack.isEmpty() || amount <= 0) {
			return ItemStack.EMPTY;
		}
		ItemStack removed = stack.split(amount);
		if (stack.isEmpty()) {
			stack = ItemStack.EMPTY;
		}
		setChanged();
		return removed;
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		if (slot != STYLE_SLOT) {
			return ItemStack.EMPTY;
		}
		ItemStack removed = stack;
		stack = ItemStack.EMPTY;
		return removed;
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		if (slot == STYLE_SLOT) {
			this.stack = stack == null ? ItemStack.EMPTY : stack;
			setChanged();
		}
	}

	@Override
	public void setChanged() {
	}

	@Override
	public boolean stillValid(Player player) {
		return true;
	}

	@Override
	public void clearContent() {
		stack = ItemStack.EMPTY;
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return slot == STYLE_SLOT;
	}

	@Override
	public void stopOpen(ContainerUser user) {
		if (user instanceof Player player && !stack.isEmpty()) {
			ItemStack returned = stack;
			stack = ItemStack.EMPTY;
			if (!player.getInventory().add(returned)) {
				player.drop(returned, false);
			}
		}
	}
}
