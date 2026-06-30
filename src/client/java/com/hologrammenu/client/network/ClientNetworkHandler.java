package com.hologrammenu.client.network;

import com.hologrammenu.client.hologram.HologramClientRegistry;
import com.hologrammenu.client.npc.NpcClientConfigStore;
import com.hologrammenu.client.npc.NpcClientRegistry;
import com.hologrammenu.client.npc.NpcEditorSessionState;
import com.hologrammenu.client.screen.EditorMousePreservation;
import com.hologrammenu.client.screen.HologramOptionsScreen;
import com.hologrammenu.client.screen.StorageMenuEditorOverlay;
import com.hologrammenu.client.storage.ShopClientState;
import com.hologrammenu.client.storage.StorageMenuAssociatedBlocks;
import com.hologrammenu.client.storage.StorageMenuClientTracker;
import com.hologrammenu.hologram.HologramLineStack;
import com.hologrammenu.network.ModPackets;
import com.hologrammenu.storage.StorageMenuViewContext;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

public final class ClientNetworkHandler {
	private ClientNetworkHandler() {
	}

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(ModPackets.HologramTrackPayload.TYPE, (payload, context) -> {
			Minecraft.getInstance().execute(() -> HologramClientRegistry.track(payload));
		});

		ClientPlayNetworking.registerGlobalReceiver(ModPackets.HologramUntrackPayload.TYPE, (payload, context) -> {
			Minecraft.getInstance().execute(() -> HologramClientRegistry.untrack(payload.entityId()));
		});

		ClientPlayNetworking.registerGlobalReceiver(ModPackets.HologramSyncPayload.TYPE, (payload, context) -> {
			Minecraft.getInstance().execute(() -> HologramClientRegistry.sync(payload.holograms()));
		});

		ClientPlayNetworking.registerGlobalReceiver(ModPackets.HologramOpenScreenPayload.TYPE, (payload, context) -> {
			Minecraft.getInstance().execute(() -> Minecraft.getInstance().setScreen(
				new HologramOptionsScreen(payload.entityId(), HologramLineStack.deserialize(payload.lines()))
			));
		});

		ClientPlayNetworking.registerGlobalReceiver(ModPackets.NpcTrackPayload.TYPE, (payload, context) -> {
			Minecraft.getInstance().execute(() -> NpcClientRegistry.track(payload.entityId()));
		});

		ClientPlayNetworking.registerGlobalReceiver(ModPackets.NpcUntrackPayload.TYPE, (payload, context) -> {
			Minecraft.getInstance().execute(() -> {
				NpcClientRegistry.untrack(payload.entityId());
				NpcClientConfigStore.remove(payload.entityId());
				NpcEditorSessionState.clear(payload.entityId());
			});
		});

		ClientPlayNetworking.registerGlobalReceiver(ModPackets.NpcSyncPayload.TYPE, (payload, context) -> {
			Minecraft.getInstance().execute(() -> NpcClientRegistry.sync(payload.entityIds()));
		});

		ClientPlayNetworking.registerGlobalReceiver(ModPackets.NpcConfigPayload.TYPE, (payload, context) -> {
			Minecraft.getInstance().execute(() -> NpcClientConfigStore.apply(payload));
		});

		ClientPlayNetworking.registerGlobalReceiver(ModPackets.StorageMenuSyncPayload.TYPE, (payload, context) -> {
			Minecraft.getInstance().execute(() -> {
				Minecraft client = Minecraft.getInstance();
				if (client.screen != null && StorageMenuEditorOverlay.getActive(client.screen) != null) {
					EditorMousePreservation.restoreIfPending();
				}
				if (payload.menu().viewContext().isRoot() && !payload.menu().enabled()) {
					StorageMenuAssociatedBlocks.forget(payload.menu().viewContext().anchorPos());
				}
				StorageMenuEditorOverlay.handleSync(payload.menu());
			});
		});

		ClientPlayNetworking.registerGlobalReceiver(ModPackets.StorageMenuContextPayload.TYPE, (payload, context) -> {
			Minecraft.getInstance().execute(() -> {
				EditorMousePreservation.restoreIfPending();
				StorageMenuViewContext viewContext = payload.npcEntityId() >= 0
					? StorageMenuViewContext.forNpc(payload.npcEntityId())
					: StorageMenuViewContext.root(payload.pos());
				if (payload.subMenuId() != null && !payload.subMenuId().isBlank()) {
					viewContext = viewContext.withSubMenu(payload.subMenuId());
				}
				StorageMenuClientTracker.setActiveView(viewContext);
				StorageMenuAssociatedBlocks.remember(viewContext.anchorPos());
				StorageMenuEditorOverlay.handleContext(viewContext);
			});
		});

		ClientPlayNetworking.registerGlobalReceiver(ModPackets.StorageMenuNavigationStatePayload.TYPE, (payload, context) -> {
			Minecraft.getInstance().execute(() -> StorageMenuClientTracker.setShowBackButton(payload.showBack()));
		});

		ClientPlayNetworking.registerGlobalReceiver(ModPackets.ShopStatePayload.TYPE, (payload, context) -> {
			Minecraft.getInstance().execute(() -> {
				java.util.Map<Integer, com.hologrammenu.storage.ShopListing> listings = new java.util.HashMap<>();
				for (com.hologrammenu.storage.StorageMenuNetwork.ShopListingData listing : payload.listings()) {
					com.hologrammenu.storage.ShopListing resolved = listing.toListing();
					if (resolved.isConfigured()) {
						listings.put(resolved.slotIndex(), resolved);
					}
				}
				ShopClientState.set(payload.pos(), new com.hologrammenu.storage.ShopDefinition(payload.shopEnabled(), listings));
			});
		});

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			HologramClientRegistry.clear();
			NpcClientRegistry.clear();
			NpcClientConfigStore.clear();
			NpcEditorSessionState.clearAll();
			StorageMenuClientTracker.clear();
			StorageMenuAssociatedBlocks.clear();
			ShopClientState.clear();
		});
	}
}
