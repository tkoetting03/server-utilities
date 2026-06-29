package com.hologrammenu.client.mixin.accessor;

import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Screen.class)
public interface ScreenInvoker {
	@Invoker("addRenderableWidget")
	<T extends GuiEventListener & Renderable & NarratableEntry> T hologrammenu$addRenderableWidget(T widget);

	@Invoker("addRenderableOnly")
	<T extends Renderable> T hologrammenu$addRenderableOnly(T widget);

	@Invoker("removeWidget")
	void hologrammenu$removeWidget(GuiEventListener widget);

	@Invoker("clearWidgets")
	void hologrammenu$clearWidgets();
}
