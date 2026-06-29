package com.hologrammenu.client.screen.widget;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import com.hologrammenu.client.mixin.accessor.AbstractContainerScreenAccessor;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.function.BooleanSupplier;

public class StorageMenuTabWidget extends AbstractWidget implements ContainerTabLayer {
	public static final int TAB_WIDTH = ContainerTopTabWidget.SPRITE_WIDTH;
	public static final int TAB_HEIGHT = ContainerTopTabWidget.SPRITE_HEIGHT;

	private static final Identifier SELECTED_TAB = Identifier.withDefaultNamespace("container/creative_inventory/tab_top_selected_1");
	private static final Identifier UNSELECTED_TAB = Identifier.withDefaultNamespace("container/creative_inventory/tab_top_unselected_1");
	private static final ItemStack TAB_ICON = new ItemStack(Items.CHEST);

	private final BooleanSupplier selected;
	private final Runnable onPress;

	public StorageMenuTabWidget(int x, int y, BooleanSupplier selected, Runnable onPress) {
		super(x, y, TAB_WIDTH, TAB_HEIGHT, Component.translatable("screen.hologrammenu.storage_menu.tab"));
		this.selected = selected;
		this.onPress = onPress;
		setTooltip(Tooltip.create(getMessage()));
	}

	public static StorageMenuTabWidget forContainer(AbstractContainerScreenAccessor layout, BooleanSupplier selected, Runnable onPress) {
		return new StorageMenuTabWidget(
			layoutPositionX(layout),
			layoutPositionY(layout),
			selected,
			onPress
		);
	}

	public void reposition(AbstractContainerScreenAccessor layout) {
		setX(layoutPositionX(layout));
		setY(layoutPositionY(layout));
	}

	private static int layoutPositionX(AbstractContainerScreenAccessor layout) {
		return layout.hologrammenu$getLeftPos();
	}

	private static int layoutPositionY(AbstractContainerScreenAccessor layout) {
		return ContainerTopTabWidget.topY(layout) + 5;
	}

	@Override
	public boolean isTabSelected() {
		return selected.getAsBoolean();
	}

	@Override
	public void extractUnselectedTabIcon(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		ContainerTopTabWidget.renderTab(graphics, getX(), getY(), UNSELECTED_TAB, TAB_ICON);
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		if (isHovered()) {
			graphics.requestCursor(CursorTypes.POINTING_HAND);
		}
		if (isTabSelected()) {
			ContainerTopTabWidget.renderTab(graphics, getX(), getY(), SELECTED_TAB, TAB_ICON);
		}
	}

	@Override
	public void onClick(MouseButtonEvent event, boolean doubleClick) {
		onPress.run();
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		defaultButtonNarrationText(output);
	}
}
