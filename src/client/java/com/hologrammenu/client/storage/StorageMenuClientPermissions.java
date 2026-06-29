package com.hologrammenu.client.storage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.DispenserScreen;
import net.minecraft.client.gui.screens.inventory.HopperScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.server.permissions.Permissions;

public final class StorageMenuClientPermissions {
	private StorageMenuClientPermissions() {
	}

	public static boolean canEdit() {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null) {
			return false;
		}
		if (client.player.getAbilities().instabuild) {
			return true;
		}
		if (client.player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
			return true;
		}
		return client.isLocalServer();
	}

	public static boolean isStorageEditorScreen(AbstractContainerScreen<?> screen) {
		if (screen instanceof CreativeModeInventoryScreen) {
			return false;
		}
		return screen instanceof ContainerScreen
			|| screen instanceof HopperScreen
			|| screen instanceof ShulkerBoxScreen
			|| screen instanceof DispenserScreen;
	}
}
