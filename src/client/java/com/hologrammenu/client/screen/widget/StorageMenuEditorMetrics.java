package com.hologrammenu.client.screen.widget;

public final class StorageMenuEditorMetrics {
	public static final int PANEL_WIDTH = ModPanelLayout.PANEL_WIDTH;
	public static final int PANEL_PADDING = ModPanelLayout.PANEL_PADDING;
	public static final int CONTENT_WIDTH = ModPanelLayout.CONTENT_WIDTH;
	public static final int HEADER_TOP = UiScale.s(14);
	public static final int TAB_ROW_HEIGHT = UiScale.s(20);
	public static final int CONTENT_TOP = HEADER_TOP + TAB_ROW_HEIGHT;
	public static final int SLOT_SIZE = UiScale.s(UiScale.SLOT_BASE);
	public static final int SLOT_STEP = SLOT_SIZE;
	public static final int SECTION_GAP = UiScale.s(3);
	public static final int BUTTON_HEIGHT = UiLayoutHelper.defaultButtonHeight();
	public static final int FOOTER_BUTTON_HEIGHT = UiLayoutHelper.defaultButtonHeight();
	public static final int FOOTER_ROW_GAP = ModPanelLayout.ROW_GAP;
	public static final int FOOTER_SECTION_GAP = ModPanelLayout.SECTION_GAP;
	public static final int ACTION_ROW_GAP = FOOTER_ROW_GAP;
	public static final int OPTION_ROW_GAP = FOOTER_ROW_GAP;
	public static final int TREE_GAP = UiScale.s(4);
	public static final int HINT_HEIGHT = UiScale.s(10);
	public static final int INVENTORY_NUMBER_ROW_HEIGHT = StorageMenuNumberBox.SIZE + UiScale.s(5);

	private StorageMenuEditorMetrics() {
	}
}
