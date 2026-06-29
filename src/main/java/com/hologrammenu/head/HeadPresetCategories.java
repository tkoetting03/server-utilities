package com.hologrammenu.head;

import java.util.Locale;

public final class HeadPresetCategories {
	public static final String[] PLUGIN = {
		"ALPHABET",
		"ANIMALS",
		"BLOCKS",
		"DECORATION",
		"FOOD_DRINKS",
		"HUMANS",
		"HUMANOID",
		"MISCELLANEOUS",
		"MONSTERS",
		"PLANTS",
		"CUSTOM",
		"CUSTOM2",
		"CUSTOM3",
		"CUSTOM4",
		"CUSTOM5"
	};

	public static final String[] PUBLIC = {
		"ALPHABET",
		"ANIMALS",
		"BLOCKS",
		"DECORATION",
		"FOOD_DRINKS",
		"HUMANS",
		"HUMANOID",
		"MISCELLANEOUS",
		"MONSTERS",
		"PLANTS"
	};

	private HeadPresetCategories() {
	}

	public static String defaultCategory() {
		return PUBLIC[0];
	}

	public static String normalize(String category) {
		if (category == null || category.isBlank()) {
			return defaultCategory();
		}
		return category.trim().toUpperCase(Locale.ROOT);
	}

	public static String fromJsonCategory(String jsonCategory) {
		if (jsonCategory == null || jsonCategory.isBlank()) {
			return "MISCELLANEOUS";
		}
		return jsonCategory.trim()
			.toUpperCase(Locale.ROOT)
			.replace(" & ", "_")
			.replace(' ', '_');
	}
}
