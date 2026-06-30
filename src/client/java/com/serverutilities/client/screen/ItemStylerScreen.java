package com.serverutilities.client.screen;

import com.serverutilities.ServerUtilitiesMod;
import com.serverutilities.itemstyler.ItemStylerMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public final class ItemStylerScreen extends AbstractContainerScreen<ItemStylerMenu> {
	private static final Identifier STYLER_BACKGROUND = ServerUtilitiesMod.id("textures/gui/item_styler.png");
	private static final Identifier INVENTORY_BACKGROUND = Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");
	private static final int STYLER_TOP_HEIGHT = 43;
	private static final int INVENTORY_HEIGHT = 96;
	private static final int IMAGE_HEIGHT = STYLER_TOP_HEIGHT + INVENTORY_HEIGHT;

	public ItemStylerScreen(ItemStylerMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, DEFAULT_IMAGE_WIDTH, IMAGE_HEIGHT);
		this.inventoryLabelY = this.imageHeight - 94;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractBackground(graphics, mouseX, mouseY, partialTick);
		int x = (this.width - this.imageWidth) / 2;
		int y = (this.height - this.imageHeight) / 2;
		graphics.blit(RenderPipelines.GUI_TEXTURED, STYLER_BACKGROUND, x, y, 0.0F, 0.0F, this.imageWidth, STYLER_TOP_HEIGHT, 256, 256);
		graphics.blit(RenderPipelines.GUI_TEXTURED, INVENTORY_BACKGROUND, x, y + STYLER_TOP_HEIGHT, 0.0F, 126.0F, this.imageWidth, INVENTORY_HEIGHT, 256, 256);
	}
}
