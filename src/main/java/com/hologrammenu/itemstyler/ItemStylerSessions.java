package com.hologrammenu.itemstyler;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;

public final class ItemStylerSessions {
	public static final Component TITLE = Component.translatable("screen.hologrammenu.item_styler.title");

	private static final Map<UUID, ItemStylerContainer> ACTIVE = new ConcurrentHashMap<>();

	private ItemStylerSessions() {
	}

	public static void open(ServerPlayer player) {
		ItemStylerContainer container = new ItemStylerContainer();
		ACTIVE.put(player.getUUID(), container);
		player.openMenu(new MenuProvider() {
			@Override
			public Component getDisplayName() {
				return TITLE;
			}

			@Override
			public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player menuPlayer) {
				return new ItemStylerMenu(syncId, playerInventory, container);
			}
		});
	}

	public static Optional<ItemStack> stack(ServerPlayer player) {
		ItemStylerContainer container = ACTIVE.get(player.getUUID());
		if (container == null) {
			return Optional.empty();
		}
		ItemStack stack = container.getItem(ItemStylerContainer.STYLE_SLOT);
		return stack.isEmpty() ? Optional.empty() : Optional.of(stack);
	}

	public static boolean update(ServerPlayer player, UnaryOperator<ItemStack> updater) {
		ItemStylerContainer container = ACTIVE.get(player.getUUID());
		if (container == null || !(player.containerMenu instanceof ItemStylerMenu menu)) {
			return false;
		}
		ItemStack stack = container.getItem(ItemStylerContainer.STYLE_SLOT);
		if (stack.isEmpty()) {
			return false;
		}
		container.setItem(ItemStylerContainer.STYLE_SLOT, updater.apply(stack));
		menu.broadcastChanges();
		return true;
	}

	public static void clear(ServerPlayer player) {
		ACTIVE.remove(player.getUUID());
	}
}
