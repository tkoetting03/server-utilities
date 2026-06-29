package com.hologrammenu.storage;

import net.minecraft.server.level.ServerLevel;

import java.util.Optional;
import java.util.UUID;

public final class StorageSubMenuManager {
	private StorageSubMenuManager() {
	}

	public static String createId() {
		return UUID.randomUUID().toString();
	}

	public static Optional<StorageMenuDefinition> get(ServerLevel level, String subMenuId) {
		if (subMenuId == null || subMenuId.isBlank()) {
			return Optional.empty();
		}
		return storage(level).get(subMenuId);
	}

	public static void save(ServerLevel level, String subMenuId, StorageMenuDefinition definition) {
		if (subMenuId == null || subMenuId.isBlank()) {
			return;
		}
		storage(level).put(subMenuId, definition);
	}

	public static void clear(ServerLevel level, String subMenuId) {
		if (subMenuId == null || subMenuId.isBlank()) {
			return;
		}
		storage(level).remove(subMenuId);
	}

	public static void ensureExists(ServerLevel level, String subMenuId, int containerSize) {
		if (subMenuId == null || subMenuId.isBlank() || containerSize <= 0) {
			return;
		}
		if (get(level, subMenuId).isEmpty()) {
			save(level, subMenuId, StorageMenuDefinition.empty(containerSize).withEnabled(true));
		}
	}

	private static StorageSubMenuSavedData storage(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(StorageSubMenuSavedData.TYPE);
	}
}
