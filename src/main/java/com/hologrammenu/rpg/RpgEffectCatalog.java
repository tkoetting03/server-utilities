package com.hologrammenu.rpg;

import com.hologrammenu.text.TextFormats;

import java.util.List;

public final class RpgEffectCatalog {
	public static final int MIN_LEVEL = 1;
	public static final int MAX_LEVEL = 5;
	private static final String LEGACY_LORE_PREFIX = "RPG: ";

	private static final List<Entry> EFFECTS = List.of(
		new Entry("flame_brand", "Flame Brand", "Weapon", "Adds a burning strike effect.", "&c"),
		new Entry("frost_guard", "Frost Guard", "Armor", "Adds a chilling defensive effect.", "&b"),
		new Entry("thunder_strike", "Thunder Strike", "Weapon", "Adds a charged burst effect.", "&e"),
		new Entry("vampiric_edge", "Vampiric Edge", "Weapon", "Adds a life-steal style effect.", "&4"),
		new Entry("guardian_ward", "Guardian Ward", "Armor", "Adds a protective ward effect.", "&9"),
		new Entry("swift_step", "Swift Step", "Boots", "Adds a movement effect.", "&a"),
		new Entry("harvest_luck", "Harvest Luck", "Tool", "Adds a gathering luck effect.", "&2"),
		new Entry("arcane_focus", "Arcane Focus", "Gear", "Adds a magic focus effect.", "&d")
	);

	private RpgEffectCatalog() {
	}

	public static List<Entry> effects() {
		return EFFECTS;
	}

	public static Entry first() {
		return EFFECTS.get(0);
	}

	public static Entry byId(String id) {
		for (Entry effect : EFFECTS) {
			if (effect.id().equals(id)) {
				return effect;
			}
		}
		return first();
	}

	public static int clampLevel(int level) {
		return Math.max(MIN_LEVEL, Math.min(MAX_LEVEL, level));
	}

	public static boolean isEffectLore(String serializedOrPlain) {
		for (Entry effect : EFFECTS) {
			if (isEffectLoreFor(serializedOrPlain, effect)) {
				return true;
			}
		}
		return false;
	}

	public static boolean isEffectLoreFor(String serializedOrPlain, Entry effect) {
		String plain = plainText(serializedOrPlain);
		return plain.startsWith(effect.name()) || plain.startsWith(LEGACY_LORE_PREFIX + effect.name());
	}

	private static String plainText(String value) {
		if (value == null) {
			return "";
		}
		return TextFormats.parse(translateAmpersandCodes(value)).text();
	}

	private static String translateAmpersandCodes(String value) {
		StringBuilder translated = new StringBuilder(value.length());
		for (int index = 0; index < value.length(); index++) {
			char character = value.charAt(index);
			if (character == '&' && index + 1 < value.length() && isLegacyCode(value.charAt(index + 1))) {
				translated.append('§').append(Character.toLowerCase(value.charAt(index + 1)));
				index++;
			} else {
				translated.append(character);
			}
		}
		return translated.toString();
	}

	private static boolean isLegacyCode(char value) {
		return (value >= '0' && value <= '9')
			|| (value >= 'a' && value <= 'f')
			|| (value >= 'A' && value <= 'F')
			|| "kKlLmMnNoOrR".indexOf(value) >= 0;
	}

	public record Entry(String id, String name, String category, String description, String colorCode) {
		public String loreLine(int level) {
			return translateAmpersandCodes(colorCode + name + " " + roman(clampLevel(level)));
		}

		public String preview(int level) {
			return name + " " + roman(clampLevel(level));
		}
	}

	private static String roman(int value) {
		return switch (clampLevel(value)) {
			case 2 -> "II";
			case 3 -> "III";
			case 4 -> "IV";
			case 5 -> "V";
			default -> "I";
		};
	}
}
