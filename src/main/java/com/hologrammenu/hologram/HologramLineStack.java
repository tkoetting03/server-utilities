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
	public static final String GROUP_HEIGHT_TAG_PREFIX = "hologrammenu:hologram_group_height:";
	public static final String LINE_HEIGHT_TAG_PREFIX = "hologrammenu:hologram_line_height:";
	public static final float MIN_HEIGHT_OFFSET = -20.0F;
	public static final float MAX_HEIGHT_OFFSET = 20.0F;
	public static final double LINE_SPACING = 0.28D;

	private static final Gson GSON = new Gson();
	private static final Type LINE_LIST_TYPE = new TypeToken<List<Line>>() {}.getType();

	public record Line(String text, float scale, float heightOffset, boolean seeThroughWalls) {
		public Line {
			text = TextFormats.normalize(text == null ? "" : text);
			scale = HologramScale.clamp(scale);
			heightOffset = clampHeightOffset(heightOffset);
		}

		public Line(String text, float scale, float heightOffset) {
			this(text, scale, heightOffset, true);
		}

		public Line(String text, float scale) {
			this(text, scale, 0.0F, true);
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
			List<Line> normalized = normalize(lines == null ? List.of() : lines);
			if (!json.contains("\"seeThroughWalls\"")) {
				return normalized.stream()
					.map(line -> new Line(line.text(), line.scale(), line.heightOffset(), true))
					.toList();
			}
			return normalized;
		} catch (RuntimeException ignored) {
			return parseLegacyText(json, HologramScale.DEFAULT);
		}
	}

	public static List<Line> readGroup(List<Display.TextDisplay> displays) {
		List<Display.TextDisplay> sorted = displays.stream()
			.sorted(Comparator.comparingInt(HologramLineStack::lineIndex))
			.toList();
		if (sorted.isEmpty()) {
			return List.of();
		}
		double anchorY = sorted.getLast().getY();
		float groupHeightOffset = groupHeightOffset(sorted.getLast());
		List<Line> baseLines = sorted.stream()
			.map(display -> new Line(
				TextFormats.fromComponent(((TextDisplayAccessor) display).hologrammenu$getText()),
				HologramScale.getScale(display),
				0.0F,
				isSeeThrough(display)
			))
			.toList();
		List<Line> lines = new ArrayList<>(sorted.size());
		for (Display.TextDisplay display : sorted) {
			int index = Math.max(0, Math.min(sorted.size() - 1, lineIndex(display)));
			double expectedY = anchorY - groupHeightOffset + stackOffsetForLine(baseLines, index);
			lines.add(new Line(
				TextFormats.fromComponent(((TextDisplayAccessor) display).hologrammenu$getText()),
				HologramScale.getScale(display),
				lineHeightOffset(display).orElse((float) (display.getY() - expectedY)),
				isSeeThrough(display)
			));
		}
		return lines;
	}

	public static double stackOffsetForLine(List<Line> lines, int index) {
		List<Line> normalized = normalize(lines);
		int clampedIndex = Math.max(0, Math.min(normalized.size() - 1, index));
		double offset = 0.0D;
		for (int below = clampedIndex + 1; below < normalized.size(); below++) {
			offset += lineSpacingAbove(normalized.get(below));
		}
		return offset;
	}

	private static double lineSpacingAbove(Line line) {
		float scale = line == null ? HologramScale.DEFAULT : HologramScale.clamp(line.scale());
		return LINE_SPACING * scale;
	}

	public static boolean isSeeThrough(Display.TextDisplay display) {
		return (display.getFlags() & Display.TextDisplay.FLAG_SEE_THROUGH) != 0;
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
		writeLineTags(entity, groupId, lineIndex, 0.0F, 0.0F);
	}

	public static void writeLineTags(Entity entity, UUID groupId, int lineIndex, float groupHeightOffset, float lineHeightOffset) {
		entity.entityTags().removeIf(tag -> tag.startsWith(GROUP_TAG_PREFIX)
			|| tag.startsWith(LINE_INDEX_TAG_PREFIX)
			|| tag.startsWith(GROUP_HEIGHT_TAG_PREFIX)
			|| tag.startsWith(LINE_HEIGHT_TAG_PREFIX));
		entity.addTag(GROUP_TAG_PREFIX + groupId);
		entity.addTag(LINE_INDEX_TAG_PREFIX + lineIndex);
		entity.addTag(GROUP_HEIGHT_TAG_PREFIX + String.format(java.util.Locale.ROOT, "%.4f", clampHeightOffset(groupHeightOffset)));
		entity.addTag(LINE_HEIGHT_TAG_PREFIX + String.format(java.util.Locale.ROOT, "%.4f", clampHeightOffset(lineHeightOffset)));
	}

	public static float groupHeightOffset(Entity entity) {
		for (String tag : entity.entityTags()) {
			if (tag.startsWith(GROUP_HEIGHT_TAG_PREFIX)) {
				try {
					return clampHeightOffset(Float.parseFloat(tag.substring(GROUP_HEIGHT_TAG_PREFIX.length())));
				} catch (NumberFormatException ignored) {
					return 0.0F;
				}
			}
		}
		return 0.0F;
	}

	private static java.util.Optional<Float> lineHeightOffset(Entity entity) {
		for (String tag : entity.entityTags()) {
			if (tag.startsWith(LINE_HEIGHT_TAG_PREFIX)) {
				try {
					return java.util.Optional.of(clampHeightOffset(Float.parseFloat(tag.substring(LINE_HEIGHT_TAG_PREFIX.length()))));
				} catch (NumberFormatException ignored) {
					return java.util.Optional.empty();
				}
			}
		}
		return java.util.Optional.empty();
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
			normalized.add(new Line(
				line == null ? "" : line.text(),
				line == null ? HologramScale.DEFAULT : line.scale(),
				line == null ? 0.0F : line.heightOffset(),
				line == null || line.seeThroughWalls()
			));
		}
		if (normalized.isEmpty()) {
			normalized.add(new Line("", HologramScale.DEFAULT));
		}
		return normalized;
	}

	public static float clampHeightOffset(float offset) {
		if (Float.isNaN(offset)) {
			return 0.0F;
		}
		return Math.max(MIN_HEIGHT_OFFSET, Math.min(MAX_HEIGHT_OFFSET, offset));
	}
}
