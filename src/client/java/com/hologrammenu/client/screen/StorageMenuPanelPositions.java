package com.hologrammenu.client.screen;

import com.hologrammenu.client.mixin.accessor.AbstractContainerScreenAccessor;
import com.hologrammenu.client.screen.widget.ModPanelLayout;
import com.hologrammenu.client.screen.widget.StorageMenuEditorMetrics;
import com.hologrammenu.client.screen.widget.StorageMenuEditorPanelWidget;
import com.hologrammenu.client.screen.widget.StorageMenuInventoryTreeWidget;
import net.minecraft.client.gui.screens.Screen;

public final class StorageMenuPanelPositions {
	private StorageMenuPanelPositions() {
	}

	public static int[] besideContainer(AbstractContainerScreenAccessor layout, Screen screen, int panelHeight) {
		int panelX = layout.hologrammenu$getLeftPos() + layout.hologrammenu$getImageWidth() + ModPanelLayout.PANEL_HORIZONTAL_GAP;
		int preferredY = layout.hologrammenu$getTopPos() + ModPanelLayout.PANEL_VERTICAL_OFFSET;
		int maxY = Math.max(4, screen.height - panelHeight - 4);
		int panelY = Math.max(4, Math.min(preferredY, maxY));
		return clampPanelPosition(screen, panelX, panelY, panelHeight);
	}

	public static int[] leftOfContainer(AbstractContainerScreenAccessor layout, Screen screen, int panelHeight) {
		int panelX = layout.hologrammenu$getLeftPos() - StorageMenuEditorPanelWidget.PANEL_WIDTH - ModPanelLayout.PANEL_HORIZONTAL_GAP;
		int preferredY = layout.hologrammenu$getTopPos() + ModPanelLayout.PANEL_VERTICAL_OFFSET;
		int maxY = Math.max(4, screen.height - panelHeight - 4);
		int panelY = Math.max(4, Math.min(preferredY, maxY));
		return clampPanelPosition(screen, panelX, panelY, panelHeight);
	}

	public static int[] belowContainer(AbstractContainerScreenAccessor layout, Screen screen, int treeHeight) {
		int containerLeft = layout.hologrammenu$getLeftPos();
		int containerWidth = layout.hologrammenu$getImageWidth();
		int treeX = containerLeft + (containerWidth - StorageMenuInventoryTreeWidget.PANEL_WIDTH) / 2;
		int preferredY = layout.hologrammenu$getTopPos() + layout.hologrammenu$getImageHeight() + StorageMenuEditorMetrics.TREE_GAP;
		int maxY = Math.max(4, screen.height - treeHeight - 4);
		int treeY = Math.max(4, Math.min(preferredY, maxY));
		return clampTreePosition(screen, treeX, treeY, treeHeight);
	}

	private static int[] clampPanelPosition(Screen screen, int panelX, int panelY, int panelHeight) {
		int maxX = Math.max(4, screen.width - StorageMenuEditorPanelWidget.PANEL_WIDTH - 4);
		return new int[] {
			Math.max(4, Math.min(panelX, maxX)),
			panelY
		};
	}

	private static int[] clampTreePosition(Screen screen, int treeX, int treeY, int treeHeight) {
		int maxX = Math.max(4, screen.width - StorageMenuInventoryTreeWidget.PANEL_WIDTH - 4);
		return new int[] {
			Math.max(4, Math.min(treeX, maxX)),
			Math.min(treeY, Math.max(4, screen.height - treeHeight - 4))
		};
	}
}
