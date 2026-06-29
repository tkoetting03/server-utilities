package com.hologrammenu.head;

import com.hologrammenu.network.ModPackets;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public final class HeadPresetService {
	public static final int PAGE_SIZE = 54;

	private HeadPresetService() {
	}

	public static void handleListRequest(ServerPlayer player, ModPackets.HeadPresetListRequestPayload payload) {
		if (HeadPresetCatalog.isLoading()) {
			sendResponse(player, false, "hud.hologrammenu.head_presets.loading", payload, List.of(), 0);
			return;
		}
		if (!HeadPresetCatalog.isAvailable()) {
			if (!HeadPresetCatalog.hasFailed()) {
				HeadPresetCatalog.initialize();
				sendResponse(player, false, "hud.hologrammenu.head_presets.loading", payload, List.of(), 0);
				return;
			}
			sendResponse(player, false, "hud.hologrammenu.head_presets.unavailable", payload, List.of(), 0);
			return;
		}
		String category = payload.category() == null || payload.category().isBlank()
			? HeadPresetCatalog.browseCategories()[0]
			: payload.category();
		String query = payload.query() == null ? "" : payload.query();
		List<HeadPresetEntry> entries = HeadPresetCatalog.list(category, query, Math.max(0, payload.page()), PAGE_SIZE);
		int totalCount = HeadPresetCatalog.count(category, query);
		sendResponse(player, true, "", payload, entries, totalCount);
	}

	public static String[] browseCategories() {
		return HeadPresetCatalog.browseCategories();
	}

	private static void sendResponse(
		ServerPlayer player,
		boolean available,
		String messageKey,
		ModPackets.HeadPresetListRequestPayload request,
		List<HeadPresetEntry> entries,
		int totalCount
	) {
		ServerPlayNetworking.send(player, new ModPackets.HeadPresetListResponsePayload(
			available,
			messageKey,
			request.category(),
			request.query(),
			request.page(),
			totalCount,
			entries
		));
	}
}
