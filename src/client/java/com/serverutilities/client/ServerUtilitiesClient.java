package com.serverutilities.client;

import com.serverutilities.ServerUtilitiesMod;
import com.serverutilities.client.config.ClientConfig;
import com.serverutilities.client.config.ClientSettings;
import com.serverutilities.client.hologram.HologramAssociationHighlighter;
import com.serverutilities.client.hologram.HologramClientInteractions;
import com.serverutilities.client.hologram.LateHologramRenderer;
import com.serverutilities.client.screen.EditorMousePreservation;
import com.serverutilities.client.screen.HotkeyHubScreen;
import com.serverutilities.client.screen.ItemStylerScreen;
import com.serverutilities.client.storage.StorageMenuAssociationHighlighter;
import com.serverutilities.client.storage.StorageMenuClientInteractions;
import com.serverutilities.client.network.ClientNetworkHandler;
import com.serverutilities.itemstyler.ModMenuTypes;
import com.serverutilities.network.ModPackets;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.resources.Identifier;

public class ServerUtilitiesClient implements ClientModInitializer {
	public static KeyMapping OPEN_HUB_KEY;
	public static KeyMapping OPEN_ITEM_STYLER_KEY;
	public static KeyMapping TOGGLE_PLACEMENT_KEY;
	public static KeyMapping TOGGLE_STORAGE_PLACEMENT_KEY;

	@Override
	public void onInitializeClient() {
		ClientConfig.load();
		ClientNetworkHandler.register();
		HologramClientInteractions.register();
		HologramAssociationHighlighter.register();
		StorageMenuAssociationHighlighter.register();
		LateHologramRenderer.register();
		com.serverutilities.client.hologram.HologramClientEditInteractions.register();
		com.serverutilities.client.npc.NpcClientInteractions.register();
		com.serverutilities.client.npc.NpcClientEditInteractions.register();
		StorageMenuClientInteractions.register();
		MenuScreens.register(ModMenuTypes.ITEM_STYLER, ItemStylerScreen::new);

		KeyMapping.Category category = KeyMapping.Category.register(
			Identifier.fromNamespaceAndPath(ServerUtilitiesMod.MOD_ID, "main")
		);

		OPEN_HUB_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.serverutilities.open_hub",
			InputConstants.Type.KEYSYM,
			InputConstants.KEY_H,
			category
		));

		OPEN_ITEM_STYLER_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.serverutilities.open_item_styler",
			InputConstants.Type.KEYSYM,
			InputConstants.UNKNOWN.getValue(),
			category
		));

		TOGGLE_PLACEMENT_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.serverutilities.toggle_placement",
			InputConstants.Type.KEYSYM,
			InputConstants.UNKNOWN.getValue(),
			category
		));

		TOGGLE_STORAGE_PLACEMENT_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.serverutilities.toggle_storage_placement",
			InputConstants.Type.KEYSYM,
			InputConstants.UNKNOWN.getValue(),
			category
		));

		ClientTickEvents.END_CLIENT_TICK.register(this::handleKeybinds);

		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			if (ClientSettings.placementModeEnabled) {
				ClientPlayNetworking.send(new ModPackets.SetPlacementModePayload(true));
			}
			if (ClientSettings.storagePlacementModeEnabled) {
				ClientPlayNetworking.send(new ModPackets.SetStoragePlacementModePayload(true));
			}
			if (ClientSettings.npcPlacementModeEnabled) {
				ClientPlayNetworking.send(new ModPackets.SetNpcPlacementModePayload(true));
			}
			if (ClientSettings.npcEditModeEnabled) {
				ClientPlayNetworking.send(new ModPackets.SetNpcEditModePayload(true));
			}
			if (ClientSettings.hologramEditModeEnabled) {
				ClientPlayNetworking.send(new ModPackets.SetHologramEditModePayload(true));
			}
		});
	}

	private void handleKeybinds(Minecraft client) {
		EditorMousePreservation.tick();

		while (OPEN_HUB_KEY.consumeClick()) {
			if (client.player != null) {
				client.setScreen(new HotkeyHubScreen());
			}
		}

		while (OPEN_ITEM_STYLER_KEY.consumeClick()) {
			if (client.player != null && client.screen == null) {
				ClientPlayNetworking.send(new ModPackets.ItemStylerOpenPayload());
			}
		}

		while (TOGGLE_PLACEMENT_KEY.consumeClick()) {
			HotkeyActions.toggleHologramPlacement(client);
		}

		while (TOGGLE_STORAGE_PLACEMENT_KEY.consumeClick()) {
			HotkeyActions.toggleStoragePlacement(client);
		}
	}
}
