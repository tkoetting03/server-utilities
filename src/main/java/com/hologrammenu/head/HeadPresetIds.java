package com.hologrammenu.head;

import java.util.Optional;

public final class HeadPresetIds {
	public static final String PREFIX = "hdb:";

	private HeadPresetIds() {
	}

	public static boolean isReference(String value) {
		return value != null && value.startsWith(PREFIX);
	}

	public static Optional<String> parseId(String value) {
		if (!isReference(value)) {
			return Optional.empty();
		}
		String id = value.substring(PREFIX.length()).trim();
		return id.isEmpty() ? Optional.empty() : Optional.of(id);
	}

	public static String encode(String headDatabaseId) {
		return PREFIX + headDatabaseId;
	}
}
