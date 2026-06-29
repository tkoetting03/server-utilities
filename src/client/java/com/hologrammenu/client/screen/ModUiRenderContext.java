package com.hologrammenu.client.screen;

import com.hologrammenu.client.screen.widget.AnvilEditorPanelWidget;
import com.hologrammenu.client.screen.widget.AnvilStyleTabWidget;
import com.hologrammenu.client.screen.widget.PresetPickerPanelWidget;
import com.hologrammenu.client.screen.widget.ShopItemPickerGridWidget;
import com.hologrammenu.client.screen.widget.ShopItemPickerPanelWidget;
import com.hologrammenu.client.screen.widget.StorageMenuEditorPanelWidget;
import com.hologrammenu.client.screen.widget.StorageMenuInventoryNumberBadge;
import com.hologrammenu.client.screen.widget.StorageMenuInventoryTreeWidget;
import com.hologrammenu.client.screen.widget.StorageMenuNamePanelWidget;
import com.hologrammenu.client.screen.widget.StorageMenuSlotButton;
import com.hologrammenu.client.screen.widget.StorageMenuTabWidget;
import com.hologrammenu.client.screen.widget.StorageMenuTreeNodeWidget;
import com.hologrammenu.client.screen.widget.TextStylePanelWidget;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractStringWidget;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;

import java.util.IdentityHashMap;
import java.util.Map;

public final class ModUiRenderContext {
	private static final Map<AbstractWidget, Boolean> INTERACTIVE_WIDGETS = new IdentityHashMap<>();

	private ModUiRenderContext() {
	}

	public static void markInteractive(AbstractWidget widget) {
		INTERACTIVE_WIDGETS.put(widget, Boolean.TRUE);
		if (widget instanceof AbstractButton button) {
			button.setOverrideRenderHighlightedSprite(button::isHovered);
		}
	}

	public static void markIfInteractive(AbstractWidget widget) {
		if (widget instanceof Button || widget instanceof EditBox || widget instanceof AbstractStringWidget) {
			markInteractive(widget);
		}
	}

	public static void clearScreen(Screen screen) {
		INTERACTIVE_WIDGETS.clear();
	}

	public static boolean isInteractive(AbstractWidget widget) {
		return INTERACTIVE_WIDGETS.containsKey(widget);
	}

	public static boolean shouldScaleWidgetRender(AbstractWidget widget) {
		if (INTERACTIVE_WIDGETS.containsKey(widget)) {
			return true;
		}
		if (!widget.getClass().getPackageName().startsWith("com.hologrammenu.client.screen.widget.")) {
			return false;
		}
		return !(widget instanceof StorageMenuEditorPanelWidget
			|| widget instanceof TextStylePanelWidget
			|| widget instanceof StorageMenuInventoryTreeWidget
			|| widget instanceof StorageMenuNamePanelWidget
			|| widget instanceof StorageMenuInventoryNumberBadge
			|| widget instanceof StorageMenuTreeNodeWidget
			|| widget instanceof StorageMenuSlotButton
			|| widget instanceof ShopItemPickerGridWidget
			|| widget instanceof ShopItemPickerPanelWidget
			|| widget instanceof PresetPickerPanelWidget
			|| widget instanceof StorageMenuTabWidget
			|| widget instanceof AnvilStyleTabWidget
			|| widget instanceof AnvilEditorPanelWidget);
	}
}
