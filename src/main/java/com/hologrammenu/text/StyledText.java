package com.hologrammenu.text;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.OptionalInt;

public final class StyledText {
	public enum Effect {
		BOLD('b'),
		ITALIC('i'),
		UNDERLINE('u'),
		STRIKETHROUGH('s'),
		OBFUSCATED('o');

		private final char code;

		Effect(char code) {
			this.code = code;
		}

		public char code() {
			return code;
		}

		public static Effect fromCode(char value) {
			for (Effect effect : values()) {
				if (effect.code == value) {
					return effect;
				}
			}
			return null;
		}
	}

	public static final StyledText EMPTY = new StyledText(EnumSet.noneOf(Effect.class), List.of(StyledSpan.plain("")));

	private final EnumSet<Effect> effects;
	private final List<StyledSpan> spans;

	public StyledText(EnumSet<Effect> effects, List<StyledSpan> spans) {
		this.effects = effects.isEmpty() ? EnumSet.noneOf(Effect.class) : EnumSet.copyOf(effects);
		if (spans == null || spans.isEmpty()) {
			this.spans = List.of(StyledSpan.plain(""));
		} else {
			this.spans = List.copyOf(spans);
		}
	}

	public static StyledText of(OptionalInt color, EnumSet<Effect> effects, String text) {
		StyledSpan span = color.isPresent() ? StyledSpan.solid(color.getAsInt(), text) : StyledSpan.plain(text);
		return new StyledText(effects, List.of(span));
	}

	public EnumSet<Effect> effects() {
		return EnumSet.copyOf(effects);
	}

	public List<StyledSpan> spans() {
		return spans;
	}

	public String text() {
		StringBuilder builder = new StringBuilder();
		for (StyledSpan span : spans) {
			builder.append(span.text());
		}
		return builder.toString();
	}

	public OptionalInt color() {
		for (StyledSpan span : spans) {
			if (span.color().isPresent()) {
				return span.color();
			}
		}
		return OptionalInt.empty();
	}

	public StyledText withColor(int rgb) {
		List<StyledSpan> updated = new ArrayList<>();
		for (StyledSpan span : spans) {
			updated.add(span.text().isEmpty() ? span : span.withSolidColor(rgb));
		}
		return new StyledText(effects, updated);
	}

	public StyledText withoutColor() {
		List<StyledSpan> updated = new ArrayList<>();
		for (StyledSpan span : spans) {
			updated.add(StyledSpan.plain(span.text()));
		}
		return new StyledText(effects, updated);
	}

	public StyledText withText(String value) {
		String newText = value == null ? "" : value;
		String oldText = text();
		if (oldText.equals(newText)) {
			return this;
		}
		if (spans.size() <= 1) {
			StyledSpan span = spans.get(0);
			return new StyledText(effects, List.of(span.withText(newText)));
		}
		return remapSpans(newText);
	}

	public StyledText toggleEffect(Effect effect) {
		EnumSet<Effect> updated = effects.isEmpty() ? EnumSet.noneOf(Effect.class) : EnumSet.copyOf(effects);
		if (updated.contains(effect)) {
			updated.remove(effect);
		} else {
			updated.add(effect);
		}
		return new StyledText(updated, spans);
	}

	public StyledText toggleEffect(int start, int end, Effect effect) {
		int length = text().length();
		start = Math.max(0, Math.min(start, length));
		end = Math.max(start, Math.min(end, length));
		if (start >= end) {
			return toggleEffect(effect);
		}
		boolean remove = sliceSpans(start, end).stream()
			.filter(span -> !span.text().isEmpty())
			.allMatch(span -> effectiveEffects(span).contains(effect));
		List<StyledSpan> rebuilt = new ArrayList<>(sliceSpans(0, start));
		for (StyledSpan span : sliceSpans(start, end)) {
			EnumSet<Effect> updated = effectiveEffects(span);
			if (remove) {
				updated.remove(effect);
			} else {
				updated.add(effect);
			}
			rebuilt.add(span.withEffects(updated));
		}
		rebuilt.addAll(sliceSpans(end, length));
		return new StyledText(effects, rebuilt);
	}

	public StyledText clearStyle() {
		return new StyledText(EnumSet.noneOf(Effect.class), List.of(StyledSpan.plain(text())));
	}

	public StyledText applySolidColor(int start, int end, int rgb) {
		int length = text().length();
		start = Math.max(0, Math.min(start, length));
		end = Math.max(start, Math.min(end, length));
		if (start >= end) {
			return withColor(rgb);
		}
		List<StyledSpan> rebuilt = new ArrayList<>(sliceSpans(0, start));
		for (StyledSpan span : sliceSpans(start, end)) {
			rebuilt.add(span.withSolidColor(rgb));
		}
		rebuilt.addAll(sliceSpans(end, length));
		return new StyledText(effects, rebuilt);
	}

	public StyledText applyGradient(int start, int end, int startRgb, int endRgb) {
		int length = text().length();
		start = Math.max(0, Math.min(start, length));
		end = Math.max(start, Math.min(end, length));
		if (start >= end) {
			return withColor(startRgb);
		}
		return spliceSpans(start, end, StyledSpan.gradient(startRgb, endRgb, text().substring(start, end)));
	}

	public StyledText splitAndColorFrom(int position, int rgb) {
		position = Math.max(0, Math.min(position, text().length()));
		List<CharacterStyle> characters = flatten();
		for (int index = position; index < characters.size(); index++) {
			characters.set(index, CharacterStyle.solid(rgb));
		}
		return fromCharacters(characters);
	}

	public StyledText slice(int start, int end) {
		int length = text().length();
		start = Math.max(0, Math.min(start, length));
		end = Math.max(start, Math.min(end, length));
		return new StyledText(effects, sliceSpans(start, end));
	}

	public String serialize() {
		boolean perSpanEffects = spans.stream().anyMatch(span -> !span.effects().isEmpty());
		if (!perSpanEffects
			&& effects.isEmpty()
			&& spans.size() == 1
			&& spans.get(0).color().isEmpty()
			&& !spans.get(0).isGradient()) {
			return spans.get(0).text();
		}

		StringBuilder builder = new StringBuilder();

		if (!perSpanEffects) {
			for (Effect effect : Effect.values()) {
				if (effects.contains(effect)) {
					builder.append('!').append(effect.code());
				}
			}
		}
		if (!perSpanEffects && !builder.isEmpty()) {
			builder.append('>');
		}

		boolean first = true;
		for (StyledSpan span : spans) {
			String encoded = span.serialize();
			if (encoded.isEmpty()) {
				continue;
			}
			if (!first) {
				builder.append('|');
			}
			builder.append(encoded);
			first = false;
		}
		return builder.toString();
	}

	public MutableComponent toComponent() {
		MutableComponent result = Component.empty();
		for (StyledSpan span : spans) {
			if (span.text().isEmpty()) {
				continue;
			}
			if (span.isGradient()) {
				appendGradient(result, span);
			} else {
				result.append(Component.literal(span.text()).withStyle(styleFor(span, span.color())));
			}
		}
		return result;
	}

	public FormattedCharSequence formattedPlainWindow(int start, int end) {
		String plain = text();
		start = Math.max(0, Math.min(start, plain.length()));
		end = Math.max(start, Math.min(end, plain.length()));
		if (start >= end) {
			return FormattedCharSequence.EMPTY;
		}

		List<FormattedCharSequence> parts = new ArrayList<>(end - start);
		int index = 0;
		for (StyledSpan span : spans) {
			if (span.isGradient()) {
				int startRgb = span.color().orElse(0xFFFFFF);
				int endRgb = span.gradientEnd().orElse(startRgb);
				int spanLength = span.text().length();
				for (int local = 0; local < spanLength; local++, index++) {
					if (index < start || index >= end) {
						continue;
					}
					float t = spanLength == 1 ? 0.0F : local / (float) (spanLength - 1);
					int rgb = lerpRgb(startRgb, endRgb, t);
					parts.add(FormattedCharSequence.codepoint(plain.charAt(index), styleFor(span, OptionalInt.of(rgb))));
				}
			} else {
				for (int local = 0; local < span.text().length(); local++, index++) {
					if (index < start || index >= end) {
						continue;
					}
					parts.add(FormattedCharSequence.codepoint(plain.charAt(index), styleFor(span, span.color())));
				}
			}
		}
		return parts.isEmpty() ? FormattedCharSequence.EMPTY : FormattedCharSequence.composite(parts);
	}

	private void appendGradient(MutableComponent result, StyledSpan span) {
		int start = span.color().orElse(0xFFFFFF);
		int end = span.gradientEnd().orElse(start);
		String value = span.text();
		int length = value.length();
		for (int index = 0; index < length; index++) {
			float t = length == 1 ? 0.0F : index / (float) (length - 1);
			int rgb = lerpRgb(start, end, t);
			result.append(Component.literal(String.valueOf(value.charAt(index))).withStyle(styleFor(span, OptionalInt.of(rgb))));
		}
	}

	private Style styleFor(StyledSpan span, OptionalInt rgb) {
		EnumSet<Effect> active = effectiveEffects(span);
		Style style = Style.EMPTY;
		if (rgb.isPresent()) {
			style = style.withColor(TextColor.fromRgb(rgb.getAsInt()));
		}
		if (active.contains(Effect.BOLD)) {
			style = style.withBold(true);
		}
		if (active.contains(Effect.ITALIC)) {
			style = style.withItalic(true);
		}
		if (active.contains(Effect.UNDERLINE)) {
			style = style.withUnderlined(true);
		}
		if (active.contains(Effect.STRIKETHROUGH)) {
			style = style.withStrikethrough(true);
		}
		if (active.contains(Effect.OBFUSCATED)) {
			style = style.withObfuscated(true);
		}
		return style;
	}

	private EnumSet<Effect> effectiveEffects(StyledSpan span) {
		return span.effects().isEmpty()
			? (effects.isEmpty() ? EnumSet.noneOf(Effect.class) : EnumSet.copyOf(effects))
			: EnumSet.copyOf(span.effects());
	}

	private StyledText remapSpans(String newText) {
		String oldText = text();
		if (oldText.isEmpty()) {
			return new StyledText(effects, List.of(StyledSpan.plain(newText)));
		}

		List<StyledSpan> remapped = new ArrayList<>();
		int oldPos = 0;
		int newLength = newText.length();
		for (StyledSpan span : spans) {
			int spanLength = span.text().length();
			if (spanLength <= 0) {
				continue;
			}
			int newStart = Math.min(newLength, (int) ((long) oldPos * newLength / oldText.length()));
			int newEnd = Math.min(newLength, (int) ((long) (oldPos + spanLength) * newLength / oldText.length()));
			oldPos += spanLength;
			if (newEnd > newStart) {
				remapped.add(span.withText(newText.substring(newStart, newEnd)));
			}
		}
		if (remapped.isEmpty()) {
			remapped.add(StyledSpan.plain(newText));
		}
		return new StyledText(effects, remapped);
	}

	private StyledText spliceSpans(int start, int end, StyledSpan inserted) {
		List<StyledSpan> rebuilt = new ArrayList<>(sliceSpans(0, start));
		if (!inserted.text().isEmpty()) {
			rebuilt.add(inserted);
		}
		rebuilt.addAll(sliceSpans(end, text().length()));
		return new StyledText(effects, rebuilt);
	}

	private List<StyledSpan> sliceSpans(int from, int to) {
		if (from >= to) {
			return List.of();
		}

		List<StyledSpan> pieces = new ArrayList<>();
		int position = 0;
		for (StyledSpan span : spans) {
			int spanStart = position;
			int spanEnd = position + span.text().length();
			position = spanEnd;
			if (spanEnd <= from || spanStart >= to) {
				continue;
			}

			int localFrom = Math.max(0, from - spanStart);
			int localTo = Math.min(span.text().length(), to - spanStart);
			String piece = span.text().substring(localFrom, localTo);
			if (piece.isEmpty()) {
				continue;
			}

			if (span.isGradient()) {
				int startRgb = span.color().orElse(0xFFFFFF);
				int endRgb = span.gradientEnd().orElse(startRgb);
				int fullLength = span.text().length();
				float startT = fullLength == 1 ? 0.0F : localFrom / (float) (fullLength - 1);
				float endT = fullLength == 1 ? 0.0F : (localTo - 1) / (float) (fullLength - 1);
				pieces.add(StyledSpan.gradient(
					lerpRgb(startRgb, endRgb, startT),
					lerpRgb(startRgb, endRgb, endT),
					piece
				).withEffects(span.effects()));
			} else if (span.color().isPresent()) {
				pieces.add(StyledSpan.solid(span.color().getAsInt(), piece).withEffects(span.effects()));
			} else {
				pieces.add(StyledSpan.plain(piece).withEffects(span.effects()));
			}
		}
		return pieces;
	}

	private List<CharacterStyle> flatten() {
		List<CharacterStyle> characters = new ArrayList<>();
		for (StyledSpan span : spans) {
			if (span.isGradient()) {
				int start = span.color().orElse(0xFFFFFF);
				int end = span.gradientEnd().orElse(start);
				int length = span.text().length();
				for (int index = 0; index < length; index++) {
					float t = length == 1 ? 0.0F : index / (float) (length - 1);
					characters.add(CharacterStyle.solid(lerpRgb(start, end, t)));
				}
			} else if (span.color().isPresent()) {
				int rgb = span.color().getAsInt();
				for (int index = 0; index < span.text().length(); index++) {
					characters.add(CharacterStyle.solid(rgb));
				}
			} else {
				for (int index = 0; index < span.text().length(); index++) {
					characters.add(CharacterStyle.plain());
				}
			}
		}
		return characters;
	}

	private StyledText fromCharacters(List<CharacterStyle> characters) {
		if (characters.isEmpty()) {
			return new StyledText(effects, List.of(StyledSpan.plain("")));
		}

		List<StyledSpan> rebuilt = new ArrayList<>();
		int runStart = 0;
		while (runStart < characters.size()) {
			int runEnd = runStart + 1;
			while (runEnd < characters.size() && sharesStyleRun(characters, runStart, runEnd)) {
				runEnd++;
			}
			rebuilt.add(toSpan(characters, runStart, runEnd, text().substring(runStart, runEnd)));
			runStart = runEnd;
		}
		return new StyledText(effects, rebuilt);
	}

	private static boolean sharesStyleRun(List<CharacterStyle> characters, int runStart, int runEnd) {
		CharacterStyle previous = characters.get(runEnd - 1);
		CharacterStyle next = characters.get(runEnd);
		if (previous.isPlain() && next.isPlain()) {
			return true;
		}
		if (previous.isPlain() || next.isPlain()) {
			return false;
		}
		if (previous.color().equals(next.color())) {
			return true;
		}
		int runLength = runEnd - runStart;
		int totalLength = runLength + 1;
		int startRgb = characters.get(runStart).color().orElse(0xFFFFFF);
		int endRgb = characters.get(runEnd).color().orElse(startRgb);
		float tPrevious = runLength == 1 ? 0.0F : (runLength - 1) / (float) (totalLength - 1);
		float tNext = runLength / (float) (totalLength - 1);
		return lerpRgb(startRgb, endRgb, tPrevious) == previous.color().orElse(0xFFFFFF)
			&& lerpRgb(startRgb, endRgb, tNext) == endRgb;
	}

	private static StyledSpan toSpan(List<CharacterStyle> characters, int start, int end, String value) {
		if (value.isEmpty()) {
			return StyledSpan.plain("");
		}
		CharacterStyle first = characters.get(start);
		if (first.isPlain()) {
			return StyledSpan.plain(value);
		}
		int startRgb = first.color().orElse(0xFFFFFF);
		int endRgb = characters.get(end - 1).color().orElse(startRgb);
		if (startRgb == endRgb && allSameColor(characters, start, end, startRgb)) {
			return StyledSpan.solid(startRgb, value);
		}
		if (isLinearGradient(characters, start, end, startRgb, endRgb)) {
			return StyledSpan.gradient(startRgb, endRgb, value);
		}
		return StyledSpan.solid(startRgb, value);
	}

	private static boolean allSameColor(List<CharacterStyle> characters, int start, int end, int rgb) {
		for (int index = start; index < end; index++) {
			if (characters.get(index).color().orElse(-1) != rgb) {
				return false;
			}
		}
		return true;
	}

	private static boolean isLinearGradient(List<CharacterStyle> characters, int start, int end, int startRgb, int endRgb) {
		int length = end - start;
		if (length <= 1) {
			return startRgb != endRgb;
		}
		for (int index = start; index < end; index++) {
			float t = (index - start) / (float) (length - 1);
			int expected = lerpRgb(startRgb, endRgb, t);
			if (characters.get(index).color().orElse(-1) != expected) {
				return false;
			}
		}
		return true;
	}

	private static int lerpRgb(int start, int end, float t) {
		int sr = (start >> 16) & 0xFF;
		int sg = (start >> 8) & 0xFF;
		int sb = start & 0xFF;
		int er = (end >> 16) & 0xFF;
		int eg = (end >> 8) & 0xFF;
		int eb = end & 0xFF;
		int r = Math.round(sr + (er - sr) * t);
		int g = Math.round(sg + (eg - sg) * t);
		int b = Math.round(sb + (eb - sb) * t);
		return (r << 16) | (g << 8) | b;
	}

	private record CharacterStyle(OptionalInt color) {
		static CharacterStyle plain() {
			return new CharacterStyle(OptionalInt.empty());
		}

		static CharacterStyle solid(int rgb) {
			return new CharacterStyle(OptionalInt.of(rgb & 0xFFFFFF));
		}

		boolean isPlain() {
			return color.isEmpty();
		}
	}
}
