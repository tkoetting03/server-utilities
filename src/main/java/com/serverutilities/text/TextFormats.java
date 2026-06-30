package com.serverutilities.text;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.StringUtil;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

public final class TextFormats {
	public record ColorOption(String id, char legacyCode, int previewColor) {
	}

	public record EffectOption(String id, char legacyCode, String label, StyledText.Effect effect) {
	}

	public static final Map<String, ColorOption> COLORS = new LinkedHashMap<>();
	public static final Map<String, EffectOption> EFFECTS = new LinkedHashMap<>();

	static {
		registerColor("black", '0', 0x000000);
		registerColor("dark_blue", '1', 0x0000AA);
		registerColor("dark_green", '2', 0x00AA00);
		registerColor("dark_aqua", '3', 0x00AAAA);
		registerColor("dark_red", '4', 0xAA0000);
		registerColor("dark_purple", '5', 0xAA00AA);
		registerColor("gold", '6', 0xFFAA00);
		registerColor("gray", '7', 0xAAAAAA);
		registerColor("dark_gray", '8', 0x555555);
		registerColor("blue", '9', 0x5555FF);
		registerColor("green", 'a', 0x55FF55);
		registerColor("aqua", 'b', 0x55FFFF);
		registerColor("red", 'c', 0xFF5555);
		registerColor("light_purple", 'd', 0xFF55FF);
		registerColor("yellow", 'e', 0xFFFF55);
		registerColor("white", 'f', 0xFFFFFF);

		registerEffect("obfuscated", 'k', "Obfuscated", StyledText.Effect.OBFUSCATED);
		registerEffect("bold", 'l', "Bold", StyledText.Effect.BOLD);
		registerEffect("strikethrough", 'm', "Strikethrough", StyledText.Effect.STRIKETHROUGH);
		registerEffect("underline", 'n', "Underline", StyledText.Effect.UNDERLINE);
		registerEffect("italic", 'o', "Italic", StyledText.Effect.ITALIC);
	}

	private TextFormats() {
	}

	private static void registerColor(String id, char legacyCode, int previewColor) {
		COLORS.put(id, new ColorOption(id, legacyCode, previewColor));
	}

	private static void registerEffect(String id, char legacyCode, String label, StyledText.Effect effect) {
		EFFECTS.put(id, new EffectOption(id, legacyCode, label, effect));
	}

	public static String normalize(String input) {
		if (input == null || input.isBlank()) {
			return "";
		}
		return parse(input).serialize();
	}

	public static StyledText parse(String input) {
		if (input == null || input.isEmpty()) {
			return StyledText.EMPTY;
		}

		if (input.indexOf('§') >= 0) {
			return fromLegacy(input);
		}

		if (input.length() > 1 && input.charAt(0) == '>' && looksLikeSpanBody(input.charAt(1))) {
			return parseNative(input.substring(1));
		}

		StyledText parsed = parseNative(input);
		if (looksLikeUnparsedFormattedText(parsed, input)) {
			StyledText salvaged = trySalvageFormattedTail(input);
			if (salvaged != null) {
				return salvaged;
			}
		}
		return parsed;
	}

	private static boolean looksLikeUnparsedFormattedText(StyledText parsed, String input) {
		if (parsed.spans().size() != 1) {
			return false;
		}
		StyledSpan span = parsed.spans().get(0);
		return span.color().isEmpty()
			&& !span.isGradient()
			&& span.text().equals(input)
			&& input.indexOf('#') >= 0;
	}

	private static StyledText trySalvageFormattedTail(String input) {
		for (int index = input.length() - 9; index >= 0; index--) {
			if (input.charAt(index) != '#') {
				continue;
			}
			if (index + 7 >= input.length() || input.charAt(index + 7) != '>') {
				continue;
			}
			if (parseHexRgb(input.substring(index + 1, index + 7)) == null) {
				continue;
			}
			StyledText candidate = parseNative(input.substring(index));
			if (!candidate.text().equals(input.substring(index))) {
				return candidate;
			}
		}
		return null;
	}

	private static boolean looksLikeSpanBody(char character) {
		return character == '#' || character == '!' || character == 'g';
	}

	private static boolean looksLikeFormattedNativeBody(String input) {
		if (input.isEmpty()) {
			return false;
		}
		char first = input.charAt(0);
		return first == 'g' || first == '#' || first == '!';
	}

	public static String applyClassicColor(String text, int rgb) {
		return parse(text).withColor(rgb).serialize();
	}

	public static String applyRgbColor(String text, int rgb) {
		return applyClassicColor(text, rgb);
	}

	public static String toggleEffect(String text, StyledText.Effect effect) {
		return parse(text).toggleEffect(effect).serialize();
	}

	public static String stripFormatting(String input) {
		return parse(input).text();
	}

	public static OptionalInt extractColor(String text) {
		return parse(text).color();
	}

	public static String preview(String text) {
		String plain = stripFormatting(text);
		return plain.isEmpty() ? "Preview" : plain;
	}

	public static String filterAnvilName(String input) {
		StyledText styled = parse(input);
		String filtered = filterPlainText(styled.text());
		return styled.withText(filtered).serialize();
	}

	public static MutableComponent toComponent(String value) {
		return parse(value).toComponent();
	}

	public static FormattedCharSequence editBoxFormat(String serialized, String visible, int visibleStart) {
		if (visible == null || visible.isEmpty()) {
			return FormattedCharSequence.EMPTY;
		}
		StyledText styled = parse(serialized);
		String plain = styled.text();
		int end = visibleStart + visible.length();
		if (visibleStart < 0 || end > plain.length() || !plain.regionMatches(visibleStart, visible, 0, visible.length())) {
			return FormattedCharSequence.forward(visible, Style.EMPTY);
		}
		return styled.formattedPlainWindow(visibleStart, end);
	}

	public static String fromComponent(Component component) {
		List<StyledSpan> spans = new ArrayList<>();
		flattenComponent(component, spans);
		if (spans.isEmpty()) {
			return "";
		}
		return new StyledText(EnumSet.noneOf(StyledText.Effect.class), spans).serialize();
	}

	private static void flattenComponent(Component component, List<StyledSpan> spans) {
		String direct = directLiteralText(component);
		if (!direct.isEmpty()) {
			spans.add(componentStyleToSpan(component.getStyle(), direct));
		}
		for (Component sibling : component.getSiblings()) {
			flattenComponent(sibling, spans);
		}
	}

	private static String directLiteralText(Component component) {
		if (component.getContents() instanceof net.minecraft.network.chat.contents.PlainTextContents contents) {
			return contents.text();
		}
		return "";
	}

	private static StyledSpan componentStyleToSpan(Style style, String text) {
		OptionalInt color = style.getColor() != null
			? OptionalInt.of(style.getColor().getValue())
			: OptionalInt.empty();
		EnumSet<StyledText.Effect> effects = effectsFromStyle(style);
		StyledSpan span = color.isPresent() ? StyledSpan.solid(color.getAsInt(), text) : StyledSpan.plain(text);
		return effects.isEmpty() ? span : span.withEffects(effects);
	}

	private static EnumSet<StyledText.Effect> effectsFromStyle(Style style) {
		EnumSet<StyledText.Effect> effects = EnumSet.noneOf(StyledText.Effect.class);
		if (Boolean.TRUE.equals(style.isBold())) {
			effects.add(StyledText.Effect.BOLD);
		}
		if (Boolean.TRUE.equals(style.isItalic())) {
			effects.add(StyledText.Effect.ITALIC);
		}
		if (Boolean.TRUE.equals(style.isUnderlined())) {
			effects.add(StyledText.Effect.UNDERLINE);
		}
		if (Boolean.TRUE.equals(style.isStrikethrough())) {
			effects.add(StyledText.Effect.STRIKETHROUGH);
		}
		if (Boolean.TRUE.equals(style.isObfuscated())) {
			effects.add(StyledText.Effect.OBFUSCATED);
		}
		return effects;
	}

	public static String plainTextForLengthCheck(String value) {
		return parse(value).text();
	}

	private static StyledText parseNative(String input) {
		if (containsUnescapedPipe(input)) {
			return parsePipedNative(input);
		}

		int index = 0;
		OptionalInt globalColor = OptionalInt.empty();
		EnumSet<StyledText.Effect> effects = EnumSet.noneOf(StyledText.Effect.class);
		boolean foundMetadata = false;

		if (input.length() >= 7 && input.charAt(0) == '#') {
			Integer rgb = parseHexRgb(input.substring(1, 7));
			if (rgb != null) {
				int next = 7;
				if (next >= input.length() || input.charAt(next) == '!' || input.charAt(next) == '>') {
					globalColor = OptionalInt.of(rgb);
					index = 7;
					foundMetadata = true;
				}
			}
		}

		while (index + 1 < input.length() && input.charAt(index) == '!') {
			StyledText.Effect effect = StyledText.Effect.fromCode(input.charAt(index + 1));
			if (effect == null) {
				break;
			}
			effects.add(effect);
			index += 2;
			foundMetadata = true;
		}

		if (foundMetadata) {
			if (index < input.length() && input.charAt(index) == '>') {
				index++;
				return parseBody(input.substring(index), effects, globalColor);
			}
			return parseBody(input.substring(index), effects, globalColor);
		}

		if (looksLikeFormattedNativeBody(input)) {
			return parseBody(input, effects, globalColor);
		}
		return new StyledText(EnumSet.noneOf(StyledText.Effect.class), List.of(StyledSpan.plain(input)));
	}

	private static StyledText parsePipedNative(String input) {
		int index = 0;
		EnumSet<StyledText.Effect> effects = EnumSet.noneOf(StyledText.Effect.class);
		while (index + 1 < input.length() && input.charAt(index) == '!') {
			StyledText.Effect effect = StyledText.Effect.fromCode(input.charAt(index + 1));
			if (effect == null) {
				break;
			}
			effects.add(effect);
			index += 2;
		}
		if (!effects.isEmpty() && index < input.length() && input.charAt(index) == '>') {
			return parseBody(input.substring(index + 1), effects, OptionalInt.empty());
		}
		return parseBody(input, EnumSet.noneOf(StyledText.Effect.class), OptionalInt.empty());
	}

	private static StyledText parseBody(String body, EnumSet<StyledText.Effect> effects, OptionalInt globalColor) {
		List<StyledSpan> spans = new ArrayList<>();
		for (String segment : splitSegments(body)) {
			StyledSpan span = parseSpan(segment);
			if (!span.text().isEmpty()) {
				spans.add(span);
			}
		}

		if (spans.isEmpty()) {
			StyledSpan span = globalColor.isPresent()
				? StyledSpan.solid(globalColor.getAsInt(), "")
				: StyledSpan.plain("");
			spans.add(span);
		} else if (globalColor.isPresent() && spans.size() == 1 && spans.get(0).color().isEmpty()) {
			spans.set(0, spans.get(0).withSolidColor(globalColor.getAsInt()));
		}

		return new StyledText(effects, spans);
	}

	private static StyledSpan parseSpan(String segment) {
		int index = 0;
		EnumSet<StyledText.Effect> effects = EnumSet.noneOf(StyledText.Effect.class);
		while (index + 1 < segment.length() && segment.charAt(index) == '!') {
			StyledText.Effect effect = StyledText.Effect.fromCode(segment.charAt(index + 1));
			if (effect == null) {
				break;
			}
			effects.add(effect);
			index += 2;
		}

		StyledSpan span = parseSpanBody(segment.substring(index));
		return effects.isEmpty() ? span : span.withEffects(effects);
	}

	private static StyledSpan parseSpanBody(String segment) {
		if (segment.startsWith("g#") && segment.length() >= 16) {
			Integer start = parseHexRgb(segment.substring(2, 8));
			if (start != null && segment.charAt(8) == '#') {
				Integer end = parseHexRgb(segment.substring(9, 15));
				if (end != null && segment.charAt(15) == '>') {
					return StyledSpan.gradient(start, end, StyledSpan.unescape(segment.substring(16)));
				}
			}
		}

		if (segment.length() >= 8 && segment.charAt(0) == '#') {
			Integer rgb = parseHexRgb(segment.substring(1, 7));
			if (rgb != null && segment.charAt(7) == '>') {
				return StyledSpan.solid(rgb, StyledSpan.unescape(segment.substring(8)));
			}
		}

		return StyledSpan.plain(StyledSpan.unescape(segment));
	}

	private static List<String> splitSegments(String body) {
		List<String> segments = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		boolean escape = false;
		for (int index = 0; index < body.length(); index++) {
			char character = body.charAt(index);
			if (escape) {
				current.append(character);
				escape = false;
				continue;
			}
			if (character == '\\') {
				escape = true;
				continue;
			}
			if (character == '|') {
				segments.add(current.toString());
				current.setLength(0);
				continue;
			}
			current.append(character);
		}
		if (escape) {
			current.append('\\');
		}
		segments.add(current.toString());
		return segments;
	}

	private static boolean containsUnescapedPipe(String value) {
		boolean escape = false;
		for (int index = 0; index < value.length(); index++) {
			char character = value.charAt(index);
			if (escape) {
				escape = false;
				continue;
			}
			if (character == '\\') {
				escape = true;
				continue;
			}
			if (character == '|') {
				return true;
			}
		}
		return false;
	}

	private static StyledText fromLegacy(String input) {
		Style style = Style.EMPTY;
		StringBuilder plain = new StringBuilder();
		char[] chars = input.toCharArray();

		for (int index = 0; index < chars.length; index++) {
			char character = chars[index];
			if (character == ChatFormatting.PREFIX_CODE && index + 1 < chars.length) {
				if (chars[index + 1] == 'x') {
					Integer rgb = parseLegacyHexAt(chars, index);
					if (rgb != null) {
						style = style.withColor(TextColor.fromRgb(rgb));
						index += 13;
						continue;
					}
				}

				ChatFormatting formatting = ChatFormatting.getByCode(chars[index + 1]);
				if (formatting != null) {
					style = formatting == ChatFormatting.RESET ? Style.EMPTY : style.applyLegacyFormat(formatting);
					index++;
					continue;
				}
			}

			plain.append(character);
		}

		return styledFromStyle(style, plain.toString());
	}

	private static StyledText styledFromStyle(Style style, String text) {
		OptionalInt color = style.getColor() != null
			? OptionalInt.of(style.getColor().getValue())
			: OptionalInt.empty();
		EnumSet<StyledText.Effect> effects = EnumSet.noneOf(StyledText.Effect.class);
		if (Boolean.TRUE.equals(style.isBold())) {
			effects.add(StyledText.Effect.BOLD);
		}
		if (Boolean.TRUE.equals(style.isItalic())) {
			effects.add(StyledText.Effect.ITALIC);
		}
		if (Boolean.TRUE.equals(style.isUnderlined())) {
			effects.add(StyledText.Effect.UNDERLINE);
		}
		if (Boolean.TRUE.equals(style.isStrikethrough())) {
			effects.add(StyledText.Effect.STRIKETHROUGH);
		}
		if (Boolean.TRUE.equals(style.isObfuscated())) {
			effects.add(StyledText.Effect.OBFUSCATED);
		}
		return StyledText.of(color, effects, text);
	}

	private static String filterPlainText(String input) {
		StringBuilder filtered = new StringBuilder();
		for (char character : input.toCharArray()) {
			if (StringUtil.isAllowedChatCharacter(character)) {
				filtered.append(character);
			}
		}
		return filtered.toString();
	}

	private static Integer parseHexRgb(String hex) {
		if (hex.length() != 6) {
			return null;
		}

		int value = 0;
		for (int index = 0; index < 6; index++) {
			int digit = parseHexDigit(hex.charAt(index));
			if (digit < 0) {
				return null;
			}
			value = (value << 4) | digit;
		}
		return value;
	}

	private static Integer parseLegacyHexAt(char[] chars, int start) {
		if (start + 13 >= chars.length || chars[start] != ChatFormatting.PREFIX_CODE || chars[start + 1] != 'x') {
			return null;
		}

		int value = 0;
		for (int digit = 0; digit < 6; digit++) {
			int markerIndex = start + 2 + digit * 2;
			if (chars[markerIndex] != ChatFormatting.PREFIX_CODE) {
				return null;
			}

			int nibble = parseHexDigit(chars[markerIndex + 1]);
			if (nibble < 0) {
				return null;
			}

			value = (value << 4) | nibble;
		}

		return value;
	}

	private static int parseHexDigit(char character) {
		if (character >= '0' && character <= '9') {
			return character - '0';
		}
		if (character >= 'a' && character <= 'f') {
			return character - 'a' + 10;
		}
		if (character >= 'A' && character <= 'F') {
			return character - 'A' + 10;
		}
		return -1;
	}
}
