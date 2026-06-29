package com.hologrammenu.particle;

public final class ParticlePresetCategories {
	public static final String NONE = "None";
	public static final String AMBIENT = "Ambient";
	public static final String MAGIC = "Magic";
	public static final String NATURE = "Nature";
	public static final String FIRE = "Fire";
	public static final String MOB = "Mob";

	public static final String[] ALL = {
		NONE,
		AMBIENT,
		MAGIC,
		NATURE,
		FIRE,
		MOB
	};

	private ParticlePresetCategories() {
	}

	public static String defaultCategory() {
		return NONE;
	}

	public static String normalize(String category) {
		if (category == null || category.isBlank()) {
			return defaultCategory();
		}
		for (String value : ALL) {
			if (value.equalsIgnoreCase(category)) {
				return value;
			}
		}
		return defaultCategory();
	}
}
