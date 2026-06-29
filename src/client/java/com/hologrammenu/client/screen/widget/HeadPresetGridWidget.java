package com.hologrammenu.client.screen.widget;

import com.hologrammenu.head.HeadPresetEntry;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public final class HeadPresetGridWidget extends AbstractWidget {
	private static final int SLOT_SIZE = HeadPresetPickerPanelWidget.slotSize();
	private static final int COLS = HeadPresetPickerPanelWidget.COLS;

	private final Supplier<List<HeadPresetEntry>> visibleEntries;
	private final Supplier<List<ItemStack>> visibleStacks;
	private final IntSupplier scrollRow;
	private final IntSupplier selectedIndex;
	private final IntConsumer onSelectIndex;

	public HeadPresetGridWidget(
		int x,
		int y,
		int width,
		int height,
		Supplier<List<HeadPresetEntry>> visibleEntries,
		Supplier<List<ItemStack>> visibleStacks,
		IntSupplier scrollRow,
		IntSupplier selectedIndex,
		IntConsumer onSelectIndex
	) {
		super(x, y, width, height, Component.empty());
		this.visibleEntries = visibleEntries;
		this.visibleStacks = visibleStacks;
		this.scrollRow = scrollRow;
		this.selectedIndex = selectedIndex;
		this.onSelectIndex = onSelectIndex;
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		List<HeadPresetEntry> entries = visibleEntries.get();
		List<ItemStack> stacks = visibleStacks.get();
		int startRow = scrollRow.getAsInt();
		int selected = selectedIndex.getAsInt();
		int visibleRows = height / SLOT_SIZE;

		for (int row = 0; row < visibleRows; row++) {
			for (int col = 0; col < COLS; col++) {
				int index = (startRow + row) * COLS + col;
				if (index >= entries.size()) {
					continue;
				}
				int slotX = getX() + col * SLOT_SIZE;
				int slotY = getY() + row * SLOT_SIZE;
				boolean isSelected = index == selected;
				boolean hovered = mouseX >= slotX && mouseX < slotX + SLOT_SIZE && mouseY >= slotY && mouseY < slotY + SLOT_SIZE;
				int background = isSelected ? 0xFF3D7A3D : hovered ? 0xFF6A6A6A : 0xFF8B8B8B;
				graphics.fill(slotX, slotY, slotX + SLOT_SIZE, slotY + SLOT_SIZE, background);
				if (isSelected) {
					graphics.fill(slotX, slotY, slotX + SLOT_SIZE, slotY + 1, 0xFF9FFF9F);
					graphics.fill(slotX, slotY + SLOT_SIZE - 1, slotX + SLOT_SIZE, slotY + SLOT_SIZE, 0xFF2A5A2A);
					graphics.fill(slotX, slotY, slotX + 1, slotY + SLOT_SIZE, 0xFF9FFF9F);
					graphics.fill(slotX + SLOT_SIZE - 1, slotY, slotX + SLOT_SIZE, slotY + SLOT_SIZE, 0xFF2A5A2A);
				}
				ItemStack stack = index < stacks.size() ? stacks.get(index) : ItemStack.EMPTY;
				if (!stack.isEmpty()) {
					UiScaleText.item(graphics, stack, slotX + 1, slotY + 1, SLOT_SIZE - 2);
				}
			}
		}
	}

	@Override
	public void onClick(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() != 0) {
			return;
		}
		List<HeadPresetEntry> entries = visibleEntries.get();
		int localX = (int) event.x() - getX();
		int localY = (int) event.y() - getY();
		if (localX < 0 || localY < 0 || localX >= width || localY >= height) {
			return;
		}
		int col = localX / SLOT_SIZE;
		int row = localY / SLOT_SIZE;
		if (col < 0 || col >= COLS) {
			return;
		}
		int index = (scrollRow.getAsInt() + row) * COLS + col;
		if (index >= 0 && index < entries.size()) {
			onSelectIndex.accept(index);
		}
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		defaultButtonNarrationText(output);
	}
}
