package com.hologrammenu.client.screen.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import java.util.function.BiConsumer;

public final class DraggableTitleBarWidget extends AbstractWidget {
	private final BiConsumer<Integer, Integer> moveTo;
	private boolean dragging;
	private double grabOffsetX;
	private double grabOffsetY;

	public DraggableTitleBarWidget(
		int x,
		int y,
		int width,
		int height,
		Component title,
		BiConsumer<Integer, Integer> moveTo
	) {
		super(x, y, width, height, title);
		this.moveTo = moveTo;
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		boolean hovered = isHovered() || dragging;
		graphics.fill(getX(), getY(), getX() + width, getY() + height, hovered ? 0x30FFFFFF : 0x00000000);
		String grip = "::";
		int gripWidth = UiScaleText.width(Minecraft.getInstance().font, grip);
		UiScaleText.draw(
			graphics,
			Minecraft.getInstance().font,
			grip,
			getX() + width - gripWidth - UiScale.s(4),
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
		grabOffsetX = event.x() - getX();
		grabOffsetY = event.y() - getY();
		return true;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (!dragging) {
			return false;
		}
		int targetX = (int) Math.round(event.x() - grabOffsetX);
		int targetY = (int) Math.round(event.y() - grabOffsetY);
		moveTo.accept(targetX, targetY);
		return true;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (!dragging) {
			return false;
		}
		dragging = false;
		return true;
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		defaultButtonNarrationText(output);
	}
}
