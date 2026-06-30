package com.serverutilities.client.screen.widget;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class UiLayoutHelper {
	/** Padding in on-screen pixels between widget content and each edge after text scaling. */
	public static final int VISUAL_PADDING = 4;

	public record ButtonPlacement(int x, int width) {
	}

	private UiLayoutHelper() {
	}

	public static int buttonWidth(Font font, Component label) {
		return buttonWidth(font, label, 0);
	}

	/** Widget width so scaled label text has {@link #VISUAL_PADDING}px on-screen inset on each side. */
	public static int buttonWidth(Font font, Component label, int extraVisualHorizontalPad) {
		int visual = UiScaleText.width(font, label) + horizontalVisualPadding() + extraVisualHorizontalPad;
		return Math.max(defaultMinButtonWidth(), (int) Math.ceil(visual / UiScale.TEXT_SCALE));
	}

	public static int buttonHeight(Font font) {
		return buttonHeight(font, 0);
	}

	public static int buttonHeight(Font font, int extraVisualVerticalPad) {
		int visual = UiScaleText.lineHeight(font) + verticalVisualPadding() + extraVisualVerticalPad;
		return Math.max(minButtonHeight(), (int) Math.ceil(visual / UiScale.TEXT_SCALE));
	}

	/** Approximate button height when a font is unavailable during layout. */
	public static int defaultButtonHeight() {
		int visual = 9 + verticalVisualPadding();
		return Math.max(minButtonHeight(), (int) Math.ceil(visual / UiScale.TEXT_SCALE));
	}

	public static int maxButtonWidth(Font font, Component... labels) {
		int max = defaultMinButtonWidth();
		for (Component label : labels) {
			max = Math.max(max, buttonWidth(font, label));
		}
		return max;
	}

	public static int iconButtonWidth(Font font, Component label) {
		return buttonWidth(font, label, UiScale.s(20));
	}

	public static boolean labelFitsButton(Font font, Component label, int buttonWidth) {
		return buttonWidth(font, label) <= buttonWidth;
	}

	public static boolean labelsFitEqualRow(int rowWidth, int columns, int gap, Font font, Component... labels) {
		if (columns <= 0 || labels.length > columns) {
			return false;
		}
		int columnWidth = equalColumnWidth(rowWidth, columns, gap);
		for (Component label : labels) {
			if (!labelFitsButton(font, label, columnWidth)) {
				return false;
			}
		}
		return true;
	}

	public static int centeredRowWidth(Font font, int gap, Component... labels) {
		if (labels.length == 0) {
			return 0;
		}
		int total = 0;
		for (Component label : labels) {
			total += buttonWidth(font, label);
		}
		return total + gap * (labels.length - 1);
	}

	public static boolean fitsCenteredRow(int rowWidth, int gap, Font font, Component... labels) {
		return centeredRowWidth(font, gap, labels) <= rowWidth;
	}

	public static List<ButtonPlacement> layoutCenteredRow(
		int rowLeft,
		int rowWidth,
		int gap,
		Font font,
		Component... labels
	) {
		List<ButtonPlacement> placements = new ArrayList<>(labels.length);
		if (labels.length == 0) {
			return placements;
		}
		int total = centeredRowWidth(font, gap, labels);
		int x = rowLeft + Math.max(0, (rowWidth - total) / 2);
		for (Component label : labels) {
			int width = buttonWidth(font, label);
			placements.add(new ButtonPlacement(x, width));
			x += width + gap;
		}
		return placements;
	}

	public static int equalColumnWidth(int rowWidth, int columns, int gap) {
		if (columns <= 0) {
			return rowWidth;
		}
		return (rowWidth - gap * (columns - 1)) / columns;
	}

	public static List<ButtonPlacement> layoutEqualRow(int rowLeft, int rowWidth, int gap, int columns) {
		int width = equalColumnWidth(rowWidth, columns, gap);
		List<ButtonPlacement> placements = new ArrayList<>(columns);
		int x = rowLeft;
		for (int index = 0; index < columns; index++) {
			placements.add(new ButtonPlacement(x, width));
			x += width + gap;
		}
		return placements;
	}

	public static List<ButtonPlacement> layoutCenteredEqualButtons(
		int rowLeft,
		int rowWidth,
		int gap,
		int buttonWidth,
		int count
	) {
		List<ButtonPlacement> placements = new ArrayList<>(count);
		if (count <= 0) {
			return placements;
		}
		int total = count * buttonWidth + gap * (count - 1);
		int x = rowLeft + Math.max(0, (rowWidth - total) / 2);
		for (int index = 0; index < count; index++) {
			placements.add(new ButtonPlacement(x, buttonWidth));
			x += buttonWidth + gap;
		}
		return placements;
	}

	public static List<Component[]> splitCenteredRows(int rowWidth, int gap, Font font, Component... labels) {
		List<Component[]> rows = new ArrayList<>();
		List<Component> current = new ArrayList<>();
		for (Component label : labels) {
			current.add(label);
			if (!fitsCenteredRow(rowWidth, gap, font, current.toArray(Component[]::new))) {
				if (current.size() == 1) {
					rows.add(current.toArray(Component[]::new));
					current = new ArrayList<>();
				} else {
					Component last = current.remove(current.size() - 1);
					rows.add(current.toArray(Component[]::new));
					current = new ArrayList<>();
					current.add(last);
				}
			}
		}
		if (!current.isEmpty()) {
			rows.add(current.toArray(Component[]::new));
		}
		return rows;
	}

	public static int horizontalVisualPadding() {
		return VISUAL_PADDING * 2;
	}

	public static int verticalVisualPadding() {
		return VISUAL_PADDING * 2;
	}

	private static int minButtonHeight() {
		return (int) Math.ceil((9 + verticalVisualPadding()) / UiScale.TEXT_SCALE);
	}

	private static int defaultMinButtonWidth() {
		return (int) Math.ceil((8 + horizontalVisualPadding()) / UiScale.TEXT_SCALE);
	}
}
