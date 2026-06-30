package com.serverutilities.client.screen;

import com.serverutilities.client.mixin.accessor.AbstractContainerScreenAccessor;
import com.serverutilities.client.screen.widget.ModPanelLayout;
import com.serverutilities.client.screen.widget.StorageMenuNamePanelWidget;
import net.minecraft.client.gui.screens.Screen;

public final class StorageMenuNamePanelPositions {
	private static final int SCREEN_EDGE_GAP = ModPanelLayout.PANEL_PADDING;

	private StorageMenuNamePanelPositions() {
	}

	public static int[] rightOfContainer(AbstractContainerScreenAccessor layout, Screen screen) {
		int panelX = layout.serverutilities$getLeftPos() + layout.serverutilities$getImageWidth() + ModPanelLayout.PANEL_HORIZONTAL_GAP;
		int panelY = layout.serverutilities$getTopPos() + ModPanelLayout.PANEL_VERTICAL_OFFSET;
		return clamp(screen, panelX, panelY);
	}

	public static int[] onLeftSide(Screen screen) {
		int panelX = SCREEN_EDGE_GAP;
		int panelY = Math.max(SCREEN_EDGE_GAP, (screen.height - StorageMenuNamePanelWidget.panelHeight()) / 2);
		return new int[] { panelX, panelY };
	}

	public static int[] atTopOfScreen(Screen screen) {
		int panelX = SCREEN_EDGE_GAP;
		int panelY = SCREEN_EDGE_GAP;
		return clamp(screen, panelX, panelY);
	}

	public static int[] clamp(Screen screen, int panelX, int panelY) {
		int panelHeight = StorageMenuNamePanelWidget.panelHeight();
		int maxX = Math.max(SCREEN_EDGE_GAP, screen.width - StorageMenuNamePanelWidget.PANEL_WIDTH - SCREEN_EDGE_GAP);
		int maxY = Math.max(SCREEN_EDGE_GAP, screen.height - panelHeight - SCREEN_EDGE_GAP);
		return new int[] {
			Math.max(SCREEN_EDGE_GAP, Math.min(panelX, maxX)),
			Math.max(SCREEN_EDGE_GAP, Math.min(panelY, maxY))
		};
	}
}
