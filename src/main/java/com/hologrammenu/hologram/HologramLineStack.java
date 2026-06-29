package com.hologrammenu.hologram;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.hologrammenu.mixin.accessor.TextDisplayAccessor;
import com.hologrammenu.text.TextFormats;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class HologramLineStack {
	public static final int MAX_LINES = 10;
	public static final String GROUP_TAG_PREFIX = "hologrammenu:hologram_group:";
	public static final String LINE_INDEX_TAG_PREFIX = "hologrammenu:hologram_line:";

	private static final Gson GSON = new Gson();
	private static final Type LINE_LIST_TYPE = new TypeToken<List<Line>>() {}.getType();

	public record Line(String text, float scale) {
		public Line {
			text = TextFormats.normalize(text == null ? "" : text);
			scale = HologramScale.clamp(scale);
		}
	}

	private HologramLineStack() {
	}

	public static List<Line> defaults(String text) {
		List<Line> lines = parseLegacyText(text, HologramScale.DEFAULT);
		return lines.isEmpty() ? new ArrayList<>(List.of(new Line("Hologram", HologramScale.DEFAULT))) : lines;
	}

	public static List<Line> parseLegacyText(String text, float scale) {
		List<Line> lines = new ArrayList<>();
		if (text == null || text.isBlank()) {
			return lines;
		}
		for (String line : text.split("\\\\n|\\r?\\n")) {
			if (lines.size() >= MAX_LINES) {
				break;
			}
			lines.add(new Line(line, scale));
		}
		return normalize(lines);
	}

	public static String serialize(List<Line> lines) {
		return GSON.toJson(normalize(lines));
	}

	public static List<Line> deserialize(String json) {
		if (json == null || json.isBlank()) {
			return new ArrayList<>(List.of(new Line("", HologramScale.DEFAULT)));
		}
		try {
			List<Line> lines = GSON.fromJson(json, LINE_LIST_TYPE);
			return normalize(lines == null ? List.of() : lines);
		} catch (RuntimeException ignored) {
			return parseLegacyText(json, HologramScale.DEFAULT);
		}
	}

	public static List<Line> readGroup(List<Display.TextDisplay> displays) {
		return displays.stream()
			.sorted(Comparator.comparingInt(HologramLineStack::lineIndex))
			.map(display -> new Line(
				TextFormats.fromComponent(((TextDisplayAccessor) display).hologrammenu$getText()),
				HologramScale.getScale(display)
			))
			.toList();
	}

	public static UUID groupId(Entity entity) {
		for (String tag : entity.entityTags()) {
			if (tag.startsWith(GROUP_TAG_PREFIX)) {
				try {
					return UUID.fromString(tag.substring(GROUP_TAG_PREFIX.length()));
				} catch (IllegalArgumentException ignored) {
					return null;
				}
			}
		}
		return null;
	}

	public static int lineIndex(Entity entity) {
		for (String tag : entity.entityTags()) {
			if (tag.startsWith(LINE_INDEX_TAG_PREFIX)) {
				try {
					return Integer.parseInt(tag.substring(LINE_INDEX_TAG_PREFIX.length()));
				} catch (NumberFormatException ignored) {
					return 0;
				}
			}
		}
		return 0;
	}

	public static void writeLineTags(Entity entity, UUID groupId, int lineIndex) {
		entity.entityTags().removeIf(tag -> tag.startsWith(GROUP_TAG_PREFIX) || tag.startsWith(LINE_INDEX_TAG_PREFIX));
		entity.addTag(GROUP_TAG_PREFIX + groupId);
		entity.addTag(LINE_INDEX_TAG_PREFIX + lineIndex);
	}

	public static List<Line> normalize(List<Line> source) {
		List<Line> normalized = new ArrayList<>();
		if (source == null) {
			source = List.of();
		}
		for (Line line : source) {
			if (normalized.size() >= MAX_LINES) {
				break;
			}
			normalized.add(new Line(line == null ? "" : line.text(), line == null ? HologramScale.DEFAULT : line.scale()));
		}
		if (normalized.isEmpty()) {
			normalized.add(new Line("", HologramScale.DEFAULT));
		}
		return normalized;
	}
}
