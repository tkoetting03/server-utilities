package com.hologrammenu.text;

import java.util.EnumSet;
import java.util.OptionalInt;

public record StyledSpan(OptionalInt color, OptionalInt gradientEnd, String text, EnumSet<StyledText.Effect> effects) {
	public StyledSpan {
		text = text == null ? "" : text;
		effects = effects == null || effects.isEmpty()
			? EnumSet.noneOf(StyledText.Effect.class)
			: EnumSet.copyOf(effects);
	}

	public static StyledSpan plain(String text) {
		return new StyledSpan(OptionalInt.empty(), OptionalInt.empty(), text, EnumSet.noneOf(StyledText.Effect.class));
	}

	public static StyledSpan solid(int rgb, String text) {
		return new StyledSpan(OptionalInt.of(rgb & 0xFFFFFF), OptionalInt.empty(), text, EnumSet.noneOf(StyledText.Effect.class));
	}

	public static StyledSpan gradient(int startRgb, int endRgb, String text) {
		return new StyledSpan(
			OptionalInt.of(startRgb & 0xFFFFFF),
			OptionalInt.of(endRgb & 0xFFFFFF),
			text,
			EnumSet.noneOf(StyledText.Effect.class)
		);
	}

	public boolean isGradient() {
		return color.isPresent() && gradientEnd.isPresent();
	}

	public StyledSpan withText(String value) {
		return new StyledSpan(color, gradientEnd, value == null ? "" : value, effects);
	}

	public StyledSpan withSolidColor(int rgb) {
		return new StyledSpan(OptionalInt.of(rgb & 0xFFFFFF), OptionalInt.empty(), text, effects);
	}

	public StyledSpan withEffects(EnumSet<StyledText.Effect> value) {
		return new StyledSpan(color, gradientEnd, text, value);
	}

	public String serialize() {
		if (text.isEmpty()) {
			return "";
		}

		StringBuilder builder = new StringBuilder();
		for (StyledText.Effect effect : StyledText.Effect.values()) {
			if (effects.contains(effect)) {
				builder.append('!').append(effect.code());
			}
		}
		builder.append(serializeBody());
		return builder.toString();
	}

	private String serializeBody() {
		String escaped = escape(text);
		if (isGradient()) {
			return String.format(
				"g#%06X#%06X>%s",
				color.getAsInt() & 0xFFFFFF,
				gradientEnd.getAsInt() & 0xFFFFFF,
				escaped
			);
		}
		if (color.isPresent()) {
			return String.format("#%06X>%s", color.getAsInt() & 0xFFFFFF, escaped);
		}
		return escaped;
	}

	public static String escape(String value) {
		StringBuilder builder = new StringBuilder(value.length());
		for (int index = 0; index < value.length(); index++) {
			char character = value.charAt(index);
			if (character == '\\' || character == '|' || character == '>') {
				builder.append('\\');
			}
			builder.append(character);
		}
		return builder.toString();
	}

	public static String unescape(String value) {
		StringBuilder builder = new StringBuilder(value.length());
		boolean escape = false;
		for (int index = 0; index < value.length(); index++) {
			char character = value.charAt(index);
			if (escape) {
				builder.append(character);
				escape = false;
				continue;
			}
			if (character == '\\') {
				escape = true;
				continue;
			}
			builder.append(character);
		}
		if (escape) {
			builder.append('\\');
		}
		return builder.toString();
	}
}
