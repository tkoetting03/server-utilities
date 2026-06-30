package com.serverutilities.client.screen.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class TextStylePanelLayout {
	public static final int PANEL_WIDTH = ModPanelLayout.PANEL_WIDTH;
	public static final int PANEL_PADDING = ModPanelLayout.PANEL_PADDING;
	public static final int CONTENT_LEFT = PANEL_PADDING;
	public static final int CONTENT_WIDTH = ModPanelLayout.CONTENT_WIDTH;

	public static final int CLASSIC_COLUMNS = 8;
	public static final int CLASSIC_ROWS = 2;
	public static final int SECTION_LABEL_HEIGHT = UiSectionSeparator.HEIGHT;
	public static final int BUTTON_ROW_GAP = UiScale.s(1);
	public static final int EFFECT_GRID_GAP = 0;
	public static final int SECTION_GAP = ModPanelLayout.SECTION_GAP;
	public static final int SECTION_LABEL_GAP = ModPanelLayout.SECTION_LABEL_GAP;
	public static final int FOOTER_SECTION_GAP = ModPanelLayout.SECTION_GAP;
	public static final int BUTTON_EXTRA_VERTICAL_PAD = UiScale.s(4);
	public static final int EFFECT_EXTRA_VERTICAL_PAD = UiScale.s(2);
	public static final int MIN_BUTTON_HEIGHT = UiScale.s(15);
	public static final double PICKER_SCALE = 0.90D;
	public static final int EFFECT_COLUMNS = 3;

	private static final Component SOLID = Component.translatable("screen.serverutilities.text_style.apply_color");
	private static final Component GRADIENT = Component.translatable("screen.serverutilities.text_style.apply_gradient");
	private static final Component RESET = Component.translatable("screen.serverutilities.text_style.reset");
	private static final Component DONE = Component.translatable("gui.done");

	private TextStylePanelLayout() {
	}

	public record Metrics(
		boolean gradientExpanded,
		boolean partsCollapsed,
		int contentTopOffset,
		int buttonHeight,
		int effectButtonHeight,
		int buttonRowGap,
		int effectGridGap,
		int classicSwatch,
		int classicGap,
		int classicBlockHeight,
		int pickerWidth,
		int pickerHeight,
		int gradientPreviewHeight,
		List<Component[]> effectRows,
		List<Component[]> footerRows
	) {
		public int partTop() {
			return ModPanelLayout.CONTENT_TOP + contentTopOffset;
		}

		public int partRowHeight() {
			return LabeledFieldLayout.FIELD_HEIGHT + BUTTON_ROW_GAP;
		}

		public boolean hasPartToggle(int partCount) {
			return partCount > 1;
		}

		public int partListTop(int partCount) {
			if (!hasPartToggle(partCount)) {
				return partTop();
			}
			return partTop() + buttonHeight + BUTTON_ROW_GAP;
		}

		public int visiblePartRows(int partCount) {
			if (partsCollapsed && hasPartToggle(partCount)) {
				return 1;
			}
			return Math.max(1, partCount);
		}

		public int partsSectionHeight(int partCount) {
			int toggle = hasPartToggle(partCount) ? buttonHeight + BUTTON_ROW_GAP : 0;
			int rows = visiblePartRows(partCount);
			int addButton = partCount < 6 ? buttonHeight + BUTTON_ROW_GAP : 0;
			return toggle + rows * partRowHeight() + addButton;
		}

		public int colorsLabelTop(int partCount) {
			return partTop() + partsSectionHeight(partCount) + SECTION_GAP;
		}

		public int classicTop(int partCount) {
			return colorsLabelTop(partCount) + SECTION_LABEL_HEIGHT + BUTTON_ROW_GAP;
		}

		public int customTop(int partCount) {
			return classicTop(partCount) + classicBlockHeight + BUTTON_ROW_GAP;
		}

		public int pickerLeft(int panelLeft) {
			return panelLeft + CONTENT_LEFT + (CONTENT_WIDTH - pickerWidth) / 2;
		}

		public int colorModeTop(int partCount) {
			return customTop(partCount) + pickerHeight + BUTTON_ROW_GAP;
		}

		public int gradientPreviewTop(int partCount) {
			return colorModeTop(partCount) + buttonHeight + BUTTON_ROW_GAP;
		}

		public int gradientTargetTop(int partCount) {
			return gradientPreviewTop(partCount) + gradientPreviewHeight + BUTTON_ROW_GAP;
		}

		public int colorsSectionEnd(int partCount) {
			int end = colorModeTop(partCount) + buttonHeight;
			if (gradientExpanded) {
				end = gradientTargetTop(partCount) + buttonHeight;
			}
			return end;
		}

		public int effectsLabelTop(int partCount) {
			return colorsSectionEnd(partCount) + SECTION_GAP;
		}

		public int effectsTop(int partCount) {
			return effectsLabelTop(partCount) + SECTION_LABEL_HEIGHT + BUTTON_ROW_GAP;
		}

		public int effectsBlockHeight() {
			return effectRows.size() * effectButtonHeight + (effectRows.size() - 1) * effectGridGap;
		}

		public int footerTop(int partCount) {
			return effectsTop(partCount) + effectsBlockHeight() + FOOTER_SECTION_GAP;
		}

		public int panelHeight(int partCount) {
			return footerTop(partCount) + rowStackHeight(footerRows.size(), buttonHeight, buttonRowGap) + PANEL_PADDING;
		}

		public int classicGridLeft(int panelLeft) {
			int gridWidth = CLASSIC_COLUMNS * classicSwatch + (CLASSIC_COLUMNS - 1) * classicGap;
			return panelLeft + CONTENT_LEFT + (CONTENT_WIDTH - gridWidth) / 2;
		}

		private int rowStackHeight(int rowCount, int rowHeight, int rowGap) {
			if (rowCount <= 0) {
				return 0;
			}
			return rowCount * rowHeight + (rowCount - 1) * rowGap;
		}
	}

	public static Metrics metrics(int partCount, int contentTopOffset) {
		return metrics(partCount, contentTopOffset, false);
	}

	public static Metrics metrics(int partCount, int contentTopOffset, boolean gradientExpanded) {
		return metrics(partCount, contentTopOffset, gradientExpanded, false);
	}

	public static Metrics metrics(int partCount, int contentTopOffset, boolean gradientExpanded, boolean partsCollapsed) {
		Font font = Minecraft.getInstance().font;
		int buttonHeight = Math.max(
			MIN_BUTTON_HEIGHT,
			UiLayoutHelper.buttonHeight(font, BUTTON_EXTRA_VERTICAL_PAD)
		);
		int effectButtonHeight = Math.max(
			buttonHeight,
			UiLayoutHelper.buttonHeight(font, BUTTON_EXTRA_VERTICAL_PAD + EFFECT_EXTRA_VERTICAL_PAD)
		);
		int classicGap = BUTTON_ROW_GAP;
		int classicSwatch = (CONTENT_WIDTH - classicGap * (CLASSIC_COLUMNS - 1)) / CLASSIC_COLUMNS;
		int classicBlockHeight = CLASSIC_ROWS * classicSwatch + (CLASSIC_ROWS - 1) * classicGap;
		int pickerWidth = pickerContentWidth();
		int pickerHeight = RgbColorPickerWidget.layoutHeight(PICKER_SCALE);
		int gradientPreviewHeight = UiScale.s(12);

		List<Component[]> effectRows = List.of(
			new Component[] {
				Component.translatable("screen.serverutilities.text_style.effect.bold"),
				Component.translatable("screen.serverutilities.text_style.effect.underline"),
				Component.translatable("screen.serverutilities.text_style.effect.italic")
			},
			new Component[] {
				Component.translatable("screen.serverutilities.text_style.effect.strikethrough"),
				Component.translatable("screen.serverutilities.text_style.effect.obfuscated")
			}
		);
		List<Component[]> footerRows = List.of(new Component[][] {
			{ RESET, DONE }
		});

		return new Metrics(
			gradientExpanded,
			partsCollapsed,
			contentTopOffset,
			buttonHeight,
			effectButtonHeight,
			BUTTON_ROW_GAP,
			EFFECT_GRID_GAP,
			classicSwatch,
			classicGap,
			classicBlockHeight,
			pickerWidth,
			pickerHeight,
			gradientPreviewHeight,
			effectRows,
			footerRows
		);
	}

	public static int pickerContentWidth() {
		return Math.max(UiScale.s(40), (int) Math.round(CONTENT_WIDTH * PICKER_SCALE));
	}

	public static Component solidModeLabel() {
		return SOLID;
	}

	public static Component gradientModeLabel() {
		return GRADIENT;
	}

	public static int partFieldWidth(boolean removable, int buttonHeight) {
		if (!removable) {
			return CONTENT_WIDTH;
		}
		return CONTENT_WIDTH - buttonHeight - BUTTON_ROW_GAP;
	}
}
