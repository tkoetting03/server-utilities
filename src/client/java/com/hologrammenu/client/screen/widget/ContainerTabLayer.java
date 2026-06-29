package com.hologrammenu.client.screen.widget;

import net.minecraft.client.gui.GuiGraphicsExtractor;

public interface ContainerTabLayer {
	boolean isTabSelected();

	void extractUnselectedTabIcon(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick);
}
