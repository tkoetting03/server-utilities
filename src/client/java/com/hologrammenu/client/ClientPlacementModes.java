package com.hologrammenu.client;

import com.hologrammenu.client.config.ClientConfig;
import com.hologrammenu.client.config.ClientSettings;
import com.hologrammenu.client.storage.StorageMenuClientInteractions;
import com.hologrammenu.network.ModPackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class ClientPlacementModes {
	private ClientPlacementModes() {
	}

	public static void disableAll(Minecraft client) {
		if (ClientSettings.placementModeEnabled) {
			ClientConfig.setPlacementModeEnabled(false);
			ClientPlayNetworking.send(new ModPackets.SetPlacementModePayload(false));
		}
		if (ClientSettings.storagePlacementModeEnabled) {
			ClientSettings.storagePlacementModeEnabled = false;
			ClientPlayNetworking.send(new ModPackets.SetStoragePlacementModePayload(false));
		}
		if (ClientSettings.npcPlacementModeEnabled) {
			ClientSettings.npcPlacementModeEnabled = false;
			ClientPlayNetworking.send(new ModPackets.SetNpcPlacementModePayload(false));
		}
		if (ClientSettings.npcEditModeEnabled) {
			ClientSettings.npcEditModeEnabled = false;
			ClientPlayNetworking.send(new ModPackets.SetNpcEditModePayload(false));
		}
		if (ClientSettings.hologramEditModeEnabled) {
			ClientSettings.hologramEditModeEnabled = false;
			ClientPlayNetworking.send(new ModPackets.SetHologramEditModePayload(false));
		}
	}

	public static void setNpcPlacement(Minecraft client, boolean enabled) {
		if (enabled) {
			disableAll(client);
		}
		ClientSettings.npcPlacementModeEnabled = enabled;
		ClientPlayNetworking.send(new ModPackets.SetNpcPlacementModePayload(enabled));
		if (client.player != null) {
			client.player.sendOverlayMessage(
				enabled
					? Component.translatable("hud.hologrammenu.npc.enabled")
					: Component.translatable("hud.hologrammenu.npc.disabled")
			);
		}
	}

	public static void setNpcEdit(Minecraft client, boolean enabled) {
		if (enabled) {
			disableAll(client);
		}
		ClientSettings.npcEditModeEnabled = enabled;
		ClientPlayNetworking.send(new ModPackets.SetNpcEditModePayload(enabled));
		if (client.player != null) {
			client.player.sendOverlayMessage(
				enabled
					? Component.translatable("hud.hologrammenu.npc_edit.enabled")
					: Component.translatable("hud.hologrammenu.npc_edit.disabled")
			);
		}
	}

	public static void setHologramEdit(Minecraft client, boolean enabled) {
		if (enabled) {
			disableAll(client);
		}
		ClientSettings.hologramEditModeEnabled = enabled;
		ClientPlayNetworking.send(new ModPackets.SetHologramEditModePayload(enabled));
		if (client.player != null) {
			client.player.sendOverlayMessage(
				enabled
					? Component.translatable("hud.hologrammenu.hologram_edit.enabled")
					: Component.translatable("hud.hologrammenu.hologram_edit.disabled")
			);
		}
	}
}
