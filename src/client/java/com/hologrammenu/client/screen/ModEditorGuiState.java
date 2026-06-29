package com.hologrammenu.client.screen;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;

public final class ModEditorGuiState {
	private ModEditorGuiState() {
	}

	public static boolean isModPanelOpen(Screen screen) {
		StorageMenuEditorOverlay storage = StorageMenuEditorOverlay.getActive(screen);
		if (storage != null && storage.hasOpenModPanel()) {
			return true;
		}
		TextStyleOverlay style = TextStyleOverlay.getActive(screen);
		return style != null && style.isOpen();
	}

	public static boolean shouldSuppressInventoryClose(AbstractContainerScreen<?> screen) {
		return isModPanelOpen(screen) && (screen.getFocused() instanceof EditBox || screen.getFocused() instanceof MultiLineEditBox);
	}

	public static boolean shouldDeferAnvilNameKeyRouting(Screen screen) {
		if (!(screen instanceof AnvilScreen)) {
			return false;
		}
		TextStyleOverlay overlay = TextStyleOverlay.getActive(screen);
		if (overlay == null || !overlay.isOpen()) {
			return false;
		}
		GuiEventListener focused = screen.getFocused();
		if (!(focused instanceof EditBox) && !(focused instanceof MultiLineEditBox)) {
			return false;
		}
		return overlay.isOverlayTextInput(focused);
	}
}
