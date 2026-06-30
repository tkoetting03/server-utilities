package com.serverutilities.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.DropperBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;

public final class StorageMenuFactory {
	private StorageMenuFactory() {
	}

	public static AbstractContainerMenu create(
		ServerLevel level,
		StorageMenuViewContext viewContext,
		StorageMenuDefinition definition,
		int syncId,
		Inventory playerInventory,
		Container validitySource,
		BlockEntity typeSource
	) {
		return create(level, viewContext, definition, ShopDefinition.EMPTY, syncId, playerInventory, validitySource, typeSource);
	}

	public static AbstractContainerMenu create(
		ServerLevel level,
		StorageMenuViewContext viewContext,
		StorageMenuDefinition definition,
		ShopDefinition shop,
		int syncId,
		Inventory playerInventory,
		Container validitySource,
		BlockEntity typeSource
	) {
		VirtualStorageContainer container = new VirtualStorageContainer(level, viewContext, definition, shop, validitySource);

		if (typeSource instanceof HopperBlockEntity) {
			return new HopperMenu(syncId, playerInventory, container);
		}
		if (typeSource instanceof DispenserBlockEntity || typeSource instanceof DropperBlockEntity) {
			return new DispenserMenu(syncId, playerInventory, container);
		}
		if (typeSource instanceof ShulkerBoxBlockEntity) {
			return new ShulkerBoxMenu(syncId, playerInventory, container);
		}
		if (typeSource instanceof ChestBlockEntity || typeSource instanceof BarrelBlockEntity || typeSource == null) {
			int rows = Math.max(1, definition.containerSize() / 9);
			return new ChestMenu(menuTypeForRows(rows), syncId, playerInventory, container, rows);
		}

		return null;
	}

	private static MenuType<?> menuTypeForRows(int rows) {
		return switch (rows) {
			case 1 -> MenuType.GENERIC_9x1;
			case 2 -> MenuType.GENERIC_9x2;
			case 3 -> MenuType.GENERIC_9x3;
			case 4 -> MenuType.GENERIC_9x4;
			case 5 -> MenuType.GENERIC_9x5;
			case 6 -> MenuType.GENERIC_9x6;
			default -> MenuType.GENERIC_9x3;
		};
	}
}
