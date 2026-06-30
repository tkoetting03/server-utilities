package com.serverutilities.client.screen;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

public final class ModUiSelectionState {
	private static final Set<AbstractWidget> SELECTED = Collections.newSetFromMap(new IdentityHashMap<>());
	private static final Set<AbstractWidget> EFFECT_BUTTONS = Collections.newSetFromMap(new IdentityHashMap<>());

	private ModUiSelectionState() {
	}

	public static void clear() {
		SELECTED.clear();
		EFFECT_BUTTONS.clear();
	}

	public static void registerEffectButton(AbstractWidget widget) {
		if (widget != null) {
			EFFECT_BUTTONS.add(widget);
		}
	}

	public static boolean isEffectButton(AbstractWidget widget) {
		return EFFECT_BUTTONS.contains(widget);
	}

	public static void clearScreen(Screen screen) {
		clear();
	}

	public static void markSelected(AbstractWidget widget) {
		if (widget != null) {
			SELECTED.add(widget);
		}
	}

	public static void unmarkSelected(AbstractWidget widget) {
		SELECTED.remove(widget);
	}

	public static boolean isSelected(AbstractWidget widget) {
		return SELECTED.contains(widget);
	}
}
