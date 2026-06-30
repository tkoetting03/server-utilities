package com.serverutilities.npc;

import net.minecraft.world.Container;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class NpcEntityValidityContainer implements Container {
	private static final double MAX_DISTANCE = 6.0D;

	private final LivingEntity npc;

	public NpcEntityValidityContainer(LivingEntity npc) {
		this.npc = npc;
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
		return npc.isAlive() && NpcHelper.isNpc(npc) && player.distanceToSqr(npc) <= MAX_DISTANCE * MAX_DISTANCE;
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
