package com.hologrammenu.client.screen;

import com.hologrammenu.client.mixin.accessor.AbstractContainerScreenAccessor;
import com.hologrammenu.client.screen.widget.ModPanelLayout;
import com.hologrammenu.client.screen.widget.ShopItemPickerPanelWidget;
import net.minecraft.client.gui.screens.Screen;

public final class ShopItemPickerPanelPositions {
	private static final int SCREEN_EDGE_GAP = ModPanelLayout.PANEL_PADDING;

	private ShopItemPickerPanelPositions() {
	}

	public static int[] rightOfContainer(AbstractContainerScreenAccessor layout, Screen screen) {
		int panelX = layout.hologrammenu$getLeftPos() + layout.hologrammenu$getImageWidth() + ModPanelLayout.PANEL_HORIZONTAL_GAP;
		int panelY = layout.hologrammenu$getTopPos() + ModPanelLayout.PANEL_VERTICAL_OFFSET;
		return clamp(screen, panelX, panelY);
	}

	public static int[] clamp(Screen screen, int panelX, int panelY) {
		int panelWidth = ShopItemPickerPanelWidget.panelWidth();
		int panelHeight = ShopItemPickerPanelWidget.panelHeight();
		int maxX = Math.max(SCREEN_EDGE_GAP, screen.width - panelWidth - SCREEN_EDGE_GAP);
		int maxY = Math.max(SCREEN_EDGE_GAP, screen.height - panelHeight - SCREEN_EDGE_GAP);
		return new int[] {
			Math.max(SCREEN_EDGE_GAP, Math.min(panelX, maxX)),
			Math.max(SCREEN_EDGE_GAP, Math.min(panelY, maxY))
		};
	}
}
