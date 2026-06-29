package com.hologrammenu.client.screen.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class NpcStackDragHandle extends AbstractWidget {
	public interface Listener {
		void onDragStart(int rowIndex);

		void onDragMove(int mouseY);

		void onDragEnd();
	}

	private final int rowIndex;
	private final Listener listener;
	private boolean dragging;

	public NpcStackDragHandle(int x, int y, int size, int rowIndex, Listener listener) {
		super(x, y, size, size, Component.empty());
		this.rowIndex = rowIndex;
		this.listener = listener;
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		boolean hovered = isHovered() || dragging;
		graphics.fill(getX(), getY(), getX() + width, getY() + height, hovered ? 0x30FFFFFF : 0x00000000);
		String grip = "≡";
		int gripWidth = UiScaleText.width(Minecraft.getInstance().font, grip);
		UiScaleText.draw(
			graphics,
			Minecraft.getInstance().font,
			grip,
			getX() + (width - gripWidth) / 2,
			getY() + Math.max(1, (height - UiScaleText.lineHeight(Minecraft.getInstance().font)) / 2),
			hovered ? 0xFFFFFF : 0xA0A0A0
		);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() != 0 || !isMouseOver(event.x(), event.y())) {
			return false;
		}
		dragging = true;
		listener.onDragStart(rowIndex);
		return true;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (!dragging) {
			return false;
		}
		listener.onDragMove((int) Math.round(event.y()));
		return true;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (!dragging) {
			return false;
		}
		dragging = false;
		listener.onDragEnd();
		return true;
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		defaultButtonNarrationText(output);
	}
}
