package com.serverutilities.client.screen.widget;

/**
 * Shared layout constants and helpers for mod GUI panels and centered screens.
 */
public final class ModPanelLayout {
	public static final int PANEL_WIDTH = UiScale.s(260);
	public static final int PANEL_PADDING = UiScale.s(6);
	public static final int CONTENT_WIDTH = PANEL_WIDTH - PANEL_PADDING * 2;
	public static final int TITLE_BAR_HEIGHT = UiScale.s(14);
	public static final int CONTENT_TOP = TITLE_BAR_HEIGHT + UiScale.s(4);
	public static final int TITLE_TEXT_TOP = UiScale.s(4);
	public static final int SECTION_LABEL_GAP = UiScale.s(10);

	public static final int PANEL_HORIZONTAL_GAP = UiScale.s(6);
	public static final int PANEL_VERTICAL_OFFSET = UiScale.s(-50);

	public static final int ROW_GAP = UiScale.s(3);
	public static final int SECTION_GAP = UiScale.s(4);
	public static final int ROW_HEIGHT = UiLayoutHelper.defaultButtonHeight();

	public static final int SCREEN_MARGIN = UiScale.s(24);
	public static final int SCREEN_CONTENT_MAX = UiScale.s(320);
	public static final int TITLE_BLOCK_HEIGHT = UiScale.s(22);

	private ModPanelLayout() {
	}

	public static int screenContentWidth(int screenWidth) {
		return Math.min(SCREEN_CONTENT_MAX, Math.max(UiScale.s(160), screenWidth - SCREEN_MARGIN));
	}

	public static int centeredX(int screenWidth, int width) {
		return screenWidth / 2 - width / 2;
	}

	public static int centeredContentTop(int screenHeight, int contentHeight) {
		int blockHeight = contentHeight + TITLE_BLOCK_HEIGHT;
		return Math.max(UiScale.s(32), (screenHeight - blockHeight) / 2 + TITLE_BLOCK_HEIGHT);
	}

	public static int titleY(int contentTop) {
		return contentTop - UiScale.s(14);
	}

	public static int hintY(int contentTop) {
		return contentTop - UiScale.s(4);
	}

	public static int columnWidth(int contentWidth, int columns, int gap) {
		if (columns <= 0) {
			return contentWidth;
		}
		return (contentWidth - gap * (columns - 1)) / columns;
	}

	public static int stackHeight(int rowCount, int rowHeight, int rowGap) {
		if (rowCount <= 0) {
			return 0;
		}
		return rowCount * rowHeight + (rowCount - 1) * rowGap;
	}
}
