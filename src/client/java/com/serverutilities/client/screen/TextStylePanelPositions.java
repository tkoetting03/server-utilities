package com.serverutilities.client.screen;

import com.serverutilities.client.mixin.accessor.AbstractContainerScreenAccessor;
import com.serverutilities.client.screen.widget.ModPanelLayout;
import com.serverutilities.client.screen.widget.TextStylePanelWidget;
import net.minecraft.client.gui.screens.Screen;

public final class TextStylePanelPositions {
	private TextStylePanelPositions() {
	}

	public static int[] besideContainer(AbstractContainerScreenAccessor layout, Screen screen) {
		int panelX = layout.serverutilities$getLeftPos() + layout.serverutilities$getImageWidth() + ModPanelLayout.PANEL_HORIZONTAL_GAP;
		int panelY = layout.serverutilities$getTopPos() + ModPanelLayout.PANEL_VERTICAL_OFFSET;
		return TextStyleOverlay.clampPanelPosition(screen, panelX, panelY);
	}

	public static int[] leftOfContainer(AbstractContainerScreenAccessor layout, Screen screen) {
		int panelX = layout.serverutilities$getLeftPos() - TextStylePanelWidget.PANEL_WIDTH - ModPanelLayout.PANEL_HORIZONTAL_GAP;
		int panelY = layout.serverutilities$getTopPos() + ModPanelLayout.PANEL_VERTICAL_OFFSET;
		return TextStyleOverlay.clampPanelPosition(screen, panelX, panelY);
	}

	public static int[] besideField(Screen screen, int fieldX, int fieldWidth, int fieldY) {
		int panelX = fieldX + fieldWidth + ModPanelLayout.PANEL_HORIZONTAL_GAP;
		int panelY = fieldY + ModPanelLayout.PANEL_VERTICAL_OFFSET;
		return TextStyleOverlay.clampPanelPosition(screen, panelX, panelY);
	}

	public static int[] belowPanel(Screen screen, int panelX, int panelY, int panelHeight) {
		int styleX = panelX;
		int styleY = panelY + panelHeight + ModPanelLayout.ROW_GAP;
		return TextStyleOverlay.clampPanelPosition(screen, styleX, styleY);
	}

	public static int[] rightOfPanel(Screen screen, int panelX, int panelY, int panelWidth) {
		int styleX = panelX + panelWidth + ModPanelLayout.PANEL_HORIZONTAL_GAP;
		return TextStyleOverlay.clampPanelPosition(screen, styleX, panelY);
	}

	public static int[] leftOfPanel(Screen screen, int panelX, int panelY, int panelWidth) {
		int styleX = panelX - TextStylePanelWidget.PANEL_WIDTH - ModPanelLayout.PANEL_HORIZONTAL_GAP;
		return TextStyleOverlay.clampPanelPosition(screen, styleX, panelY);
	}
}
