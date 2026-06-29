package com.hologrammenu.client.mixin;

import com.hologrammenu.client.screen.ModUiRenderContext;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Screen.class)
public abstract class ScreenModUiMixin {
	@Inject(method = "addRenderableWidget", at = @At("RETURN"))
	private <T extends GuiEventListener & net.minecraft.client.gui.components.Renderable & net.minecraft.client.gui.narration.NarratableEntry> void hologrammenu$markModWidget(
		T widget,
		CallbackInfoReturnable<T> cir
	) {
		Screen screen = (Screen) (Object) this;
		if (widget instanceof AbstractWidget abstractWidget
			&& screen.getClass().getName().startsWith("com.hologrammenu.client.screen.")) {
			ModUiRenderContext.markIfInteractive(abstractWidget);
		}
	}
}
