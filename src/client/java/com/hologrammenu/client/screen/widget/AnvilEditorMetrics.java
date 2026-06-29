package com.hologrammenu.client.screen.widget;

import com.hologrammenu.storage.StorageMenuItemLore;
import com.hologrammenu.text.TextFormats;
import net.minecraft.client.Minecraft;

public final class AnvilEditorMetrics {
	public static final int PANEL_WIDTH = TextStylePanelLayout.PANEL_WIDTH;
	public static final int TAB_BUTTON_EXTRA_VERTICAL_PAD = UiScale.s(8);

	private AnvilEditorMetrics() {
	}

	public static int tabButtonHeight() {
		return Math.max(
			TextStylePanelLayout.MIN_BUTTON_HEIGHT,
			UiLayoutHelper.buttonHeight(Minecraft.getInstance().font, TAB_BUTTON_EXTRA_VERTICAL_PAD)
		);
	}

	public static int tabRowHeight() {
		return tabButtonHeight() + UiScale.s(2);
	}

	public static int tabContentTop() {
		return ModPanelLayout.CONTENT_TOP + tabRowHeight();
	}

	public static int stylePanelHeight(int partCount) {
		return stylePanelHeight(partCount, false);
	}

	public static int stylePanelHeight(int partCount, boolean gradientExpanded) {
		return TextStylePanelWidget.panelHeight(partCount, tabRowHeight(), gradientExpanded);
	}

	public static int loreLinesSectionHeight(int lineCount) {
		return loreParagraphHeight(lineCount)
			+ ModPanelLayout.ROW_GAP
			+ UiLayoutHelper.defaultButtonHeight()
			+ ModPanelLayout.ROW_GAP
			+ UiLayoutHelper.defaultButtonHeight()
			+ ModPanelLayout.SECTION_GAP
			+ ModPanelLayout.stackHeight(2, UiLayoutHelper.defaultButtonHeight(), ModPanelLayout.ROW_GAP)
			+ ModPanelLayout.SECTION_GAP
			+ UiLayoutHelper.defaultButtonHeight();
	}

	public static int loreParagraphHeight(int lineCount) {
		int lines = Math.max(5, Math.min(StorageMenuItemLore.MAX_LINES, lineCount));
		return UiScale.s(10) + lines * UiScale.s(9);
	}

	public static int loreFooterTop(int lineCount) {
		return tabContentTop()
			+ ModPanelLayout.SECTION_LABEL_GAP
			+ loreLinesSectionHeight(lineCount)
			+ ModPanelLayout.SECTION_GAP
			+ UiLayoutHelper.defaultButtonHeight()
			+ ModPanelLayout.SECTION_GAP;
	}

	public static int lorePanelHeight(int lineCount) {
		return loreFooterTop(lineCount) + UiLayoutHelper.defaultButtonHeight() + ModPanelLayout.PANEL_PADDING;
	}

	public static int lorePanelHeight(int lineCount, boolean colorTableOpen, boolean gradientExpanded) {
		if (!colorTableOpen) {
			return lorePanelHeight(lineCount);
		}
		return tabContentTop()
			+ ModPanelLayout.SECTION_LABEL_GAP
			+ loreParagraphHeight(lineCount)
			+ ModPanelLayout.ROW_GAP
			+ loreColorTableHeight(gradientExpanded)
			+ ModPanelLayout.PANEL_PADDING;
	}

	public static int loreColorTableHeight(boolean gradientExpanded) {
		int buttonHeight = UiLayoutHelper.defaultButtonHeight();
		int rowGap = ModPanelLayout.ROW_GAP;
		int colorColumns = Math.max(1, TextFormats.COLORS.size() / 2);
		int colorRows = (int) Math.ceil(TextFormats.COLORS.size() / (double) colorColumns);
		int swatch = (ModPanelLayout.CONTENT_WIDTH - rowGap * (colorColumns - 1)) / colorColumns;
		int height = colorRows * swatch + Math.max(0, colorRows - 1) * rowGap + ModPanelLayout.SECTION_GAP
			+ RgbColorPickerWidget.layoutHeight(TextStylePanelLayout.PICKER_SCALE) + rowGap
			+ buttonHeight + rowGap;
		if (gradientExpanded) {
			height += TextStylePanelLayout.metrics(1, 0, true).gradientPreviewHeight() + rowGap
				+ buttonHeight + rowGap;
		}
		return height + buttonHeight;
	}

}
