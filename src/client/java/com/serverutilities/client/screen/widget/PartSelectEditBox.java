package com.serverutilities.client.screen.widget;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public class PartSelectEditBox extends EditBox {
	private final Runnable onSelected;

	public PartSelectEditBox(Font font, int x, int y, int width, int height, Component hint, Runnable onSelected) {
		super(font, x, y, width, height, hint);
		this.onSelected = onSelected;
	}

	@Override
	public void setFocused(boolean focused) {
		super.setFocused(focused);
		if (focused) {
			onSelected.run();
		}
	}
}
