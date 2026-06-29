package com.hologrammenu.rpg;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public final class RpgSkillStore {
	public static final int MIN_LEVEL = 1;
	public static final int MAX_LEVEL = 255;
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir()
		.resolve("hologrammenu")
		.resolve("rpg-skills.json");
	private static final List<Skill> SKILLS = new ArrayList<>();

	static {
		loadDefaults();
		load();
	}

	private RpgSkillStore() {
	}

	public static List<Skill> skills() {
		return List.copyOf(SKILLS);
	}

	public static Skill first() {
		return SKILLS.get(0);
	}

	public static Optional<Skill> find(String id) {
		return SKILLS.stream()
			.filter(skill -> skill.id().equals(id))
			.findFirst();
	}

	public static Skill addOrUpdate(
		String id,
		String name,
		String description,
		String effect,
		String bonus,
		String colorCode,
		int maxLevel,
		int baseBonus,
		int bonusPerLevel
	) {
		String skillId = cleanId(id, name);
		Skill skill = new Skill(
			skillId,
			clean(name, "Skill"),
			clean(description, "Custom skill."),
			clean(effect, "Passive"),
			clean(bonus, "Bonus"),
			cleanColor(colorCode),
			clampLevel(maxLevel),
			Math.max(0, baseBonus),
			Math.max(0, bonusPerLevel)
		);
		SKILLS.removeIf(existing -> existing.id().equals(skill.id()));
		SKILLS.add(skill);
		save();
		return skill;
	}

	public static void remove(String id) {
		SKILLS.removeIf(skill -> skill.id().equals(id));
		if (SKILLS.isEmpty()) {
			loadDefaults();
		}
		save();
	}

	public static int clampLevel(int level) {
		return Math.max(MIN_LEVEL, Math.min(MAX_LEVEL, level));
	}

	private static void loadDefaults() {
		SKILLS.clear();
		SKILLS.add(new Skill("mining", "Mining", "Break stone and ore more efficiently.", "Haste", "Ore Yield", "&7", 100, 1, 1));
		SKILLS.add(new Skill("fishing", "Fishing", "Improve catches from water.", "Lure", "Treasure Chance", "&b", 100, 1, 1));
		SKILLS.add(new Skill("woodcutting", "Woodcutting", "Gather logs and forest resources.", "Efficiency", "Log Yield", "&2", 100, 1, 1));
		SKILLS.add(new Skill("farming", "Farming", "Harvest crops and natural food.", "Growth", "Crop Yield", "&a", 100, 1, 1));
		SKILLS.add(new Skill("combat", "Combat", "Improve weapon and armor performance.", "Might", "Damage", "&c", 100, 1, 1));
		SKILLS.add(new Skill("excavation", "Excavation", "Dig earth, sand, and gravel.", "Momentum", "Drop Yield", "&6", 100, 1, 1));
		SKILLS.add(new Skill("alchemy", "Alchemy", "Brew and empower potion work.", "Catalyst", "Brew Strength", "&d", 100, 1, 1));
		SKILLS.add(new Skill("foraging", "Foraging", "Collect plants, mushrooms, and wild resources.", "Keen Eye", "Rare Finds", "&e", 100, 1, 1));
	}

	private static void load() {
		if (!Files.exists(CONFIG_PATH)) {
			return;
		}
		try {
			Skill[] loaded = GSON.fromJson(Files.readString(CONFIG_PATH), Skill[].class);
			if (loaded == null || loaded.length == 0) {
				return;
			}
			SKILLS.clear();
			Arrays.stream(loaded)
				.filter(skill -> skill != null && skill.id() != null && !skill.id().isBlank())
				.map(skill -> new Skill(
					cleanId(skill.id(), skill.name()),
					clean(skill.name(), "Skill"),
					clean(skill.description(), "Custom skill."),
					clean(skill.effect(), "Passive"),
					clean(skill.bonus(), "Bonus"),
					cleanColor(skill.colorCode()),
					clampLevel(skill.maxLevel()),
					Math.max(0, skill.baseBonus()),
					Math.max(0, skill.bonusPerLevel())
				))
				.forEach(SKILLS::add);
			if (SKILLS.isEmpty()) {
				loadDefaults();
			}
		} catch (IOException | JsonSyntaxException ignored) {
		}
	}

	private static void save() {
		try {
			Files.createDirectories(CONFIG_PATH.getParent());
			Files.writeString(CONFIG_PATH, GSON.toJson(SKILLS));
		} catch (IOException ignored) {
		}
	}

	private static String clean(String value, String fallback) {
		if (value == null || value.isBlank()) {
			return fallback;
		}
		return value.trim();
	}

	private static String cleanId(String id, String name) {
		String source = id == null || id.isBlank() ? clean(name, "skill") : id;
		String cleaned = source.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9_\\-:]+", "_");
		return cleaned.isBlank() ? "skill" : cleaned;
	}

	private static String cleanColor(String value) {
		String color = clean(value, "&f");
		return color.matches("&[0-9a-fk-orA-FK-OR]") ? color : "&f";
	}

	public record Skill(
		String id,
		String name,
		String description,
		String effect,
		String bonus,
		String colorCode,
		int maxLevel,
		int baseBonus,
		int bonusPerLevel
	) {
		public String preview(int level) {
			int clamped = Math.min(RpgSkillStore.clampLevel(level), maxLevel);
			return name + " " + RpgCustomEffectStore.levelText(clamped) + " - " + effect + ": +" + value(clamped) + " " + bonus;
		}

		public int value(int level) {
			return baseBonus + Math.max(0, level - 1) * bonusPerLevel;
		}
	}
}
