package com.hologrammenu.client.screen;

import com.hologrammenu.client.screen.widget.DraggableTitleBarWidget;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class DraggablePanelGroup {
	private final Screen screen;
	private final String panelId;
	private final List<AbstractWidget> tracked = new ArrayList<>();
	private int anchorX;
	private int anchorY;

	public DraggablePanelGroup(Screen screen, String panelId) {
		this.screen = screen;
		this.panelId = panelId;
	}

	public int[] resolvePosition(int defaultX, int defaultY, int width, int height) {
		int[] resolved = DraggablePanelPositions.resolve(screen, panelId, defaultX, defaultY, width, height);
		anchorX = resolved[0];
		anchorY = resolved[1];
		return resolved;
	}

	public int anchorX() {
		return anchorX;
	}

	public int anchorY() {
		return anchorY;
	}

	public void track(AbstractWidget widget) {
		tracked.add(widget);
	}

	public DraggableTitleBarWidget createTitleBar(Component title, int titleBarWidth, int titleBarHeight) {
		DraggableTitleBarWidget titleBar = new DraggableTitleBarWidget(
			anchorX,
			anchorY,
			titleBarWidth,
			titleBarHeight,
			title,
			this::moveTo
		);
		track(titleBar);
		return titleBar;
	}

	public void moveTo(int newAnchorX, int newAnchorY) {
		int deltaX = newAnchorX - anchorX;
		int deltaY = newAnchorY - anchorY;
		shiftBy(deltaX, deltaY);
	}

	public void shiftBy(int deltaX, int deltaY) {
		if (deltaX == 0 && deltaY == 0) {
			return;
		}
		anchorX += deltaX;
		anchorY += deltaY;
		for (AbstractWidget widget : tracked) {
			widget.setX(widget.getX() + deltaX);
			widget.setY(widget.getY() + deltaY);
		}
		DraggablePanelPositions.save(screen, panelId, anchorX, anchorY);
	}
}
