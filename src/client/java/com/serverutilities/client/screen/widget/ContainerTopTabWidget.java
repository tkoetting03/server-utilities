package com.serverutilities.client.screen.widget;

import com.serverutilities.client.mixin.accessor.AbstractContainerScreenAccessor;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

final class ContainerTopTabWidget {
	static final int SPRITE_WIDTH = 26;
	static final int SPRITE_HEIGHT = 32;
	static final int ICON_SIZE = 16;
	static final int TAB_Y_NUDGE = 5;

	private ContainerTopTabWidget() {
	}

	static int topY(AbstractContainerScreenAccessor layout) {
		return layout.serverutilities$getTopPos() - SPRITE_HEIGHT;
	}

	static void renderTab(
		GuiGraphicsExtractor graphics,
		int widgetX,
		int widgetY,
		Identifier sprite,
		ItemStack icon
	) {
		graphics.blitSprite(
			RenderPipelines.GUI_TEXTURED,
			sprite,
			widgetX,
			widgetY,
			SPRITE_WIDTH,
			SPRITE_HEIGHT
		);
		graphics.item(icon, widgetX + 5, widgetY + 6);
	}
}
