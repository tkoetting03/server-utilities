package com.serverutilities.client.mixin;

import com.serverutilities.client.mixin.accessor.AbstractContainerScreenAccessor;
import com.serverutilities.client.screen.ItemStylerOverlay;
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
	private ItemStylerOverlay serverutilities$itemStylerOverlay;

	@Inject(method = "init", at = @At("TAIL"))
	private void serverutilities$addItemStylerOverlay(CallbackInfo ci) {
		AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
		if (!ItemStylerOverlay.isItemStylerScreen((Screen) screen)) {
			return;
		}
		AbstractContainerScreenAccessor layout = (AbstractContainerScreenAccessor) screen;
		if (serverutilities$itemStylerOverlay == null) {
			serverutilities$itemStylerOverlay = new ItemStylerOverlay(screen, layout);
		}
		serverutilities$itemStylerOverlay.attach(layout);
	}
}
