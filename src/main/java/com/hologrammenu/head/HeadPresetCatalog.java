package com.hologrammenu.head;

import java.util.List;
import java.util.Optional;

public final class HeadPresetCatalog {
	private HeadPresetCatalog() {
	}

	public static void initialize() {
		MinecraftHeadsCatalog.startLoading();
	}

	public static boolean isAvailable() {
		return MinecraftHeadsCatalog.isReady();
	}

	public static boolean isLoading() {
		return MinecraftHeadsCatalog.isLoading();
	}

	public static boolean hasFailed() {
		return MinecraftHeadsCatalog.hasFailed();
	}

	public static List<HeadPresetEntry> list(String category, String query, int page, int pageSize) {
		return MinecraftHeadsCatalog.list(category, query, page, pageSize);
	}

	public static int count(String category, String query) {
		return MinecraftHeadsCatalog.count(category, query);
	}

	public static Optional<String> getBase64(String id) {
		return getEntry(id).map(HeadPresetEntry::base64);
	}

	public static Optional<HeadPresetEntry> getEntry(String id) {
		if (id == null || id.isBlank()) {
			return Optional.empty();
		}
		return MinecraftHeadsCatalog.getEntry(id);
	}

	public static String[] browseCategories() {
		return MinecraftHeadsCatalog.browseCategories();
	}
}
