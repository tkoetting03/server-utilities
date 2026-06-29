package com.hologrammenu.rpg;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.hologrammenu.text.TextFormats;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public final class RpgCustomEffectStore {
	public static final int MIN_LEVEL = 1;
	public static final int MAX_LEVEL = 255;
	private static final String LEGACY_LORE_PREFIX = "RPG: ";
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir()
		.resolve("hologrammenu")
		.resolve("custom-rpg-effects.json");
	private static final List<CustomEffect> CUSTOM_EFFECTS = new ArrayList<>();
	private static int nextId = 1;

	static {
		load();
	}

	private RpgCustomEffectStore() {
	}

	public static List<CustomEffect> effects() {
		return List.copyOf(CUSTOM_EFFECTS);
	}

	public static Optional<CustomEffect> find(String id) {
		return CUSTOM_EFFECTS.stream()
			.filter(effect -> effect.id().equals(id))
			.findFirst();
	}

	public static CustomEffect addOrUpdate(
		String id,
		String name,
		String category,
		String description,
		String colorCode,
		int maxLevel,
		int basePower,
		int powerPerLevel
	) {
		CustomEffect effect = new CustomEffect(
			id == null || id.isBlank() ? "custom:" + nextId++ : id,
			clean(name, "Custom Effect"),
			clean(category, "Gear"),
			clean(description, "Custom RPG effect."),
			cleanColor(colorCode),
			clampLevel(maxLevel),
			Math.max(0, basePower),
			Math.max(0, powerPerLevel)
		);
		CUSTOM_EFFECTS.removeIf(existing -> existing.id().equals(effect.id()));
		CUSTOM_EFFECTS.add(effect);
		nextId = Math.max(nextId, nextCustomId(effect.id()) + 1);
		save();
		return effect;
	}

	public static void remove(String id) {
		CUSTOM_EFFECTS.removeIf(effect -> effect.id().equals(id));
		save();
	}

	public static int clampLevel(int level) {
		return Math.max(MIN_LEVEL, Math.min(MAX_LEVEL, level));
	}

	public static boolean isEffectLoreFor(String serializedOrPlain, String effectName) {
		String cleanName = clean(effectName, "");
		String plain = plainText(serializedOrPlain);
		return plain.startsWith(cleanName) || plain.startsWith(LEGACY_LORE_PREFIX + cleanName);
	}

	private static String clean(String value, String fallback) {
		if (value == null || value.isBlank()) {
			return fallback;
		}
		return value.trim();
	}

	private static String cleanColor(String value) {
		String color = clean(value, "&d");
		return color.matches("&[0-9a-fk-orA-FK-OR]") ? color : "&d";
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

	private static void load() {
		if (!Files.exists(CONFIG_PATH)) {
			return;
		}
		try {
			CustomEffect[] loaded = GSON.fromJson(Files.readString(CONFIG_PATH), CustomEffect[].class);
			if (loaded == null) {
				return;
			}
			CUSTOM_EFFECTS.clear();
			Arrays.stream(loaded)
				.filter(effect -> effect != null && effect.id() != null && !effect.id().isBlank())
				.map(effect -> new CustomEffect(
					effect.id(),
					clean(effect.name(), "Custom Effect"),
					clean(effect.category(), "Gear"),
					clean(effect.description(), "Custom RPG effect."),
					cleanColor(effect.colorCode()),
					clampLevel(effect.maxLevel()),
					Math.max(0, effect.basePower()),
					Math.max(0, effect.powerPerLevel())
				))
				.forEach(effect -> {
					CUSTOM_EFFECTS.add(effect);
					nextId = Math.max(nextId, nextCustomId(effect.id()) + 1);
				});
		} catch (IOException | JsonSyntaxException ignored) {
		}
	}

	private static void save() {
		try {
			Files.createDirectories(CONFIG_PATH.getParent());
			Files.writeString(CONFIG_PATH, GSON.toJson(CUSTOM_EFFECTS));
		} catch (IOException ignored) {
		}
	}

	private static int nextCustomId(String id) {
		if (id == null || !id.startsWith("custom:")) {
			return 0;
		}
		try {
			return Integer.parseInt(id.substring("custom:".length()));
		} catch (NumberFormatException ignored) {
			return 0;
		}
	}

	public record CustomEffect(
		String id,
		String name,
		String category,
		String description,
		String colorCode,
		int maxLevel,
		int basePower,
		int powerPerLevel
	) {
		public String loreLine(int level) {
			int clamped = Math.min(RpgCustomEffectStore.clampLevel(level), maxLevel);
			return translateAmpersandCodes(colorCode + name + " " + levelText(clamped) + " +" + power(clamped));
		}

		public String preview(int level) {
			int clamped = Math.min(RpgCustomEffectStore.clampLevel(level), maxLevel);
			return name + " " + levelText(clamped) + " +" + power(clamped);
		}

		public int power(int level) {
			return basePower + Math.max(0, level - 1) * powerPerLevel;
		}
	}

	public static String levelText(int value) {
		if (value <= 10) {
			return switch (value) {
				case 2 -> "II";
				case 3 -> "III";
				case 4 -> "IV";
				case 5 -> "V";
				case 6 -> "VI";
				case 7 -> "VII";
				case 8 -> "VIII";
				case 9 -> "IX";
				case 10 -> "X";
				default -> "I";
			};
		}
		return Integer.toString(value);
	}
}
