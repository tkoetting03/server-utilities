package com.hologrammenu.client.mixin;

import com.hologrammenu.client.mixin.accessor.AbstractContainerScreenAccessor;
import com.hologrammenu.client.screen.ItemStylerOverlay;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class ItemStylerContainerScreenMixin {
	@Unique
	private ItemStylerOverlay hologrammenu$itemStylerOverlay;

	@Inject(method = "init", at = @At("TAIL"))
	private void hologrammenu$addItemStylerOverlay(CallbackInfo ci) {
		AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
		if (!ItemStylerOverlay.isItemStylerScreen((Screen) screen)) {
			return;
		}
		AbstractContainerScreenAccessor layout = (AbstractContainerScreenAccessor) screen;
		if (hologrammenu$itemStylerOverlay == null) {
			hologrammenu$itemStylerOverlay = new ItemStylerOverlay(screen, layout);
		}
		hologrammenu$itemStylerOverlay.attach(layout);
	}
}
