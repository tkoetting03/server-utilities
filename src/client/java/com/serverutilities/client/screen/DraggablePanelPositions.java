package com.serverutilities.client.screen;

import net.minecraft.client.gui.screens.Screen;

import java.util.HashMap;
import java.util.Map;

public final class DraggablePanelPositions {
	private record Key(int screenId, String panelId) {
	}

	private static final Map<Key, int[]> SAVED = new HashMap<>();

	private DraggablePanelPositions() {
	}

	public static int[] resolve(Screen screen, String panelId, int defaultX, int defaultY, int width, int height) {
		Key key = new Key(System.identityHashCode(screen), panelId);
		int[] saved = SAVED.get(key);
		if (saved != null) {
			return clamp(screen, saved[0], saved[1], width, height);
		}
		return clamp(screen, defaultX, defaultY, width, height);
	}

	public static void save(Screen screen, String panelId, int x, int y) {
		SAVED.put(new Key(System.identityHashCode(screen), panelId), new int[] { x, y });
	}

	public static int[] clamp(Screen screen, int x, int y, int width, int height) {
		int maxX = Math.max(4, screen.width - width - 4);
		int maxY = Math.max(4, screen.height - height - 4);
		return new int[] {
			Math.max(4, Math.min(x, maxX)),
			Math.max(4, Math.min(y, maxY))
		};
	}

	public static void clearScreen(Screen screen) {
		int screenId = System.identityHashCode(screen);
		SAVED.keySet().removeIf(key -> key.screenId() == screenId);
	}
}
