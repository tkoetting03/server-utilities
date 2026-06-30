package com.serverutilities.client.screen;

import com.serverutilities.client.mixin.accessor.EditBoxAccessor;
import com.serverutilities.text.StyledText;
import com.serverutilities.text.TextFormats;
import net.minecraft.client.gui.components.EditBox;

import java.util.function.Consumer;
import java.util.function.Supplier;

@FunctionalInterface
public interface TextStyleTarget {
	void applyStyledText(String serialized);

	default int[] selectionRange(Supplier<String> plainText) {
		int end = plainText.get().length();
		return new int[] {end, end};
	}

	static TextStyleTarget plainField(Supplier<String> plainText, Consumer<String> plainTextWriter, Consumer<String> styledWriter) {
		return serialized -> {
			String normalized = TextFormats.normalize(serialized);
			styledWriter.accept(normalized);
			String plain = TextFormats.parse(normalized).text();
			if (!plain.equals(plainText.get())) {
				plainTextWriter.accept(plain);
			}
		};
	}

	static TextStyleTarget editBox(EditBox field, Consumer<String> styledWriter) {
		return new TextStyleTarget() {
			@Override
			public void applyStyledText(String serialized) {
				String normalized = TextFormats.normalize(serialized);
				styledWriter.accept(normalized);
				String plain = TextFormats.parse(normalized).text();
				if (!plain.equals(field.getValue())) {
					field.setValue(plain);
				}
			}

			@Override
			public int[] selectionRange(Supplier<String> plainText) {
				int cursor = field.getCursorPosition();
				int highlight = ((EditBoxAccessor) field).serverutilities$getHighlightPos();
				return new int[] {Math.min(cursor, highlight), Math.max(cursor, highlight)};
			}
		};
	}

	default String readSerialized(String storedSerialized, Supplier<String> plainText) {
		return TextFormats.parse(storedSerialized).withText(plainText.get()).serialize();
	}
}
