package com.serverutilities.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class StorageMenuBlockValidityContainer implements Container {
	private static final double MAX_DISTANCE = 12.0D;

	private final ServerLevel level;
	private final BlockPos pos;

	public StorageMenuBlockValidityContainer(ServerLevel level, BlockPos pos) {
		this.level = level;
		this.pos = pos.immutable();
	}

	@Override
	public int getContainerSize() {
		return 0;
	}

	@Override
	public boolean isEmpty() {
		return true;
	}

	@Override
	public ItemStack getItem(int slot) {
		return ItemStack.EMPTY;
	}

	@Override
	public ItemStack removeItem(int slot, int amount) {
		return ItemStack.EMPTY;
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		return ItemStack.EMPTY;
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
	}

	@Override
	public void setChanged() {
	}

	@Override
	public boolean stillValid(Player player) {
		if (level.getBlockState(pos).isAir()) {
			return false;
		}
		return player.blockPosition().distSqr(pos) <= MAX_DISTANCE * MAX_DISTANCE;
	}

	@Override
	public void startOpen(ContainerUser user) {
	}

	@Override
	public void stopOpen(ContainerUser user) {
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return false;
	}

	@Override
	public void clearContent() {
	}
}
