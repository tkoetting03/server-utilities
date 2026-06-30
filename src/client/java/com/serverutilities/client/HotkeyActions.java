package com.serverutilities.client;

import com.serverutilities.client.config.ClientConfig;
import com.serverutilities.client.config.ClientSettings;
import com.serverutilities.client.storage.StorageMenuClientInteractions;
import com.serverutilities.network.ModPackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class HotkeyActions {
	private HotkeyActions() {
	}

	public static void toggleHologramPlacement(Minecraft client) {
		boolean enabled = !ClientSettings.placementModeEnabled;
		if (enabled) {
			ClientPlacementModes.disableAll(client);
		}
		ClientConfig.setPlacementModeEnabled(enabled);
		ClientPlayNetworking.send(new ModPackets.SetPlacementModePayload(enabled));
		if (client.player != null) {
			client.player.sendOverlayMessage(
				enabled
					? Component.translatable("hud.serverutilities.placement.enabled")
					: Component.translatable("hud.serverutilities.placement.disabled")
			);
		}
	}

	public static void toggleStoragePlacement(Minecraft client) {
		boolean enabled = !ClientSettings.storagePlacementModeEnabled;
		if (enabled) {
			ClientPlacementModes.disableAll(client);
			ClientSettings.storagePlacementModeEnabled = true;
		} else {
			ClientSettings.storagePlacementModeEnabled = false;
		}
		StorageMenuClientInteractions.handlePlacementToggle(client, enabled);
	}

	public static void toggleNpcPlacement(Minecraft client) {
		ClientPlacementModes.setNpcPlacement(client, !ClientSettings.npcPlacementModeEnabled);
	}

	public static void toggleNpcEdit(Minecraft client) {
		ClientPlacementModes.setNpcEdit(client, !ClientSettings.npcEditModeEnabled);
	}

	public static void toggleHologramEdit(Minecraft client) {
		ClientPlacementModes.setHologramEdit(client, !ClientSettings.hologramEditModeEnabled);
	}
}
