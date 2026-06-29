package com.hologrammenu.client.screen.widget;

public final class LabeledFieldLayout {
	public static final int LABEL_HEIGHT = UiScale.s(8);
	public static final int FIELD_HEIGHT = UiLayoutHelper.defaultButtonHeight();
	public static final int LABEL_GAP = UiScale.s(1);
	public static final int ROW_GAP = ModPanelLayout.ROW_GAP;

	private LabeledFieldLayout() {
	}

	public static int labeledRowHeight() {
		return LABEL_HEIGHT + LABEL_GAP + FIELD_HEIGHT + ROW_GAP;
	}
}
