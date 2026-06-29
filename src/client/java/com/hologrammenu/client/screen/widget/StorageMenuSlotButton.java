package com.hologrammenu.client.screen.widget;

import com.hologrammenu.storage.StorageMenuSlotConfig;
import com.hologrammenu.storage.StorageMenuSlotType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

public class StorageMenuSlotButton extends AbstractWidget {
	private final int slotIndex;
	private final IntSupplier selectedIndex;
	private final IntConsumer onSelect;
	private final java.util.function.IntFunction<StorageMenuSlotConfig> slotLookup;

	public StorageMenuSlotButton(
		int x,
		int y,
		int slotIndex,
		IntSupplier selectedIndex,
		IntConsumer onSelect,
		java.util.function.IntFunction<StorageMenuSlotConfig> slotLookup
	) {
		super(x, y, StorageMenuEditorMetrics.SLOT_SIZE, StorageMenuEditorMetrics.SLOT_SIZE, Component.literal("Slot " + slotIndex));
		this.slotIndex = slotIndex;
		this.selectedIndex = selectedIndex;
		this.onSelect = onSelect;
		this.slotLookup = slotLookup;
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		int x = getX();
		int y = getY();
		boolean selected = selectedIndex.getAsInt() == slotIndex;
		int border = selected ? 0xFFFFFFFF : 0xFF8B8B8B;
		graphics.fill(x, y, x + width, y + height, 0xFF3A3A3A);
		graphics.fill(x, y, x + width, y + 1, border);
		graphics.fill(x, y + height - 1, x + width, y + height, border);
		graphics.fill(x, y, x + 1, y + height, border);
		graphics.fill(x + width - 1, y, x + width, y + height, border);

		StorageMenuSlotConfig config = slotLookup.apply(slotIndex);
		ItemStack stack = config.displayStack();
		if (!stack.isEmpty()) {
			UiScaleText.item(graphics, stack, x, y, width);
		}

		var font = Minecraft.getInstance().font;
		int letterX = x + width - UiScaleText.width(font, "F") - 1;
		int letterY = y + 1;
		if (config.type() == StorageMenuSlotType.FILLER) {
			UiScaleText.draw(graphics, font, Component.literal("F"), letterX, letterY, 0xFFAAAAAA);
		} else if (config.type() == StorageMenuSlotType.COMMAND) {
			UiScaleText.draw(graphics, font, Component.literal("C"), letterX, letterY, 0xFF55FF55);
		} else if (config.type() == StorageMenuSlotType.LINK) {
			UiScaleText.draw(graphics, font, Component.literal("L"), letterX, letterY, 0xFF55AAFF);
		} else if (config.type() == StorageMenuSlotType.BACK) {
			UiScaleText.draw(graphics, font, Component.literal("B"), letterX, letterY, 0xFFFFAA55);
		} else if (config.type() == StorageMenuSlotType.SHOP_ITEM) {
			UiScaleText.draw(graphics, font, Component.literal("S"), letterX, letterY, 0xFFFFD755);
		} else if (config.type() == StorageMenuSlotType.CLOSE) {
			UiScaleText.draw(graphics, font, Component.literal("X"), letterX, letterY, 0xFFFF5555);
		}
	}

	@Override
	public void onClick(MouseButtonEvent event, boolean doubleClick) {
		onSelect.accept(slotIndex);
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		defaultButtonNarrationText(output);
	}
}
