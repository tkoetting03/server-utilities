package com.hologrammenu.client.screen.widget;

import com.hologrammenu.storage.StorageMenuItemLore;
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
			+ ModPanelLayout.stackHeight(2, UiLayoutHelper.defaultButtonHeight(), ModPanelLayout.ROW_GAP)
			+ ModPanelLayout.SECTION_GAP
			+ UiLayoutHelper.defaultButtonHeight()
			+ ModPanelLayout.ROW_GAP
			+ UiLayoutHelper.defaultButtonHeight()
			+ ModPanelLayout.SECTION_GAP
			+ ModPanelLayout.stackHeight(2, UiLayoutHelper.defaultButtonHeight(), ModPanelLayout.ROW_GAP)
			+ ModPanelLayout.SECTION_GAP
			+ UiLayoutHelper.defaultButtonHeight();
	}

	public static int loreParagraphHeight(int lineCount) {
		int lines = Math.max(3, Math.min(StorageMenuItemLore.MAX_LINES, lineCount));
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

	public static int effectsGridTop() {
		return tabContentTop() + ModPanelLayout.SECTION_LABEL_GAP + UiLayoutHelper.defaultButtonHeight() + ModPanelLayout.ROW_GAP;
	}

	public static int effectsLevelRowTop() {
		int rows = 4;
		return effectsGridTop() + rows * UiLayoutHelper.defaultButtonHeight() + rows * ModPanelLayout.ROW_GAP + ModPanelLayout.SECTION_GAP;
	}

	public static int effectsPageRowTop() {
		return effectsLevelRowTop();
	}

	public static int effectsEnchantLevelRowTop() {
		return effectsPageRowTop() + UiLayoutHelper.defaultButtonHeight() + ModPanelLayout.ROW_GAP;
	}

	public static int effectsEnchantActionRowTop() {
		return effectsEnchantLevelRowTop() + UiLayoutHelper.defaultButtonHeight() + ModPanelLayout.ROW_GAP;
	}

	public static int effectsEnchantFooterTop() {
		return effectsEnchantActionRowTop() + UiLayoutHelper.defaultButtonHeight() + ModPanelLayout.SECTION_GAP;
	}

	public static int effectsActionRowTop() {
		return effectsLevelRowTop() + UiLayoutHelper.defaultButtonHeight() + ModPanelLayout.ROW_GAP;
	}

	public static int effectsFooterTop() {
		return effectsActionRowTop() + UiLayoutHelper.defaultButtonHeight() + ModPanelLayout.SECTION_GAP;
	}

	public static int effectsPanelHeight() {
		return effectsEnchantFooterTop() + UiLayoutHelper.defaultButtonHeight() + ModPanelLayout.PANEL_PADDING;
	}
}
