package com.hologrammenu.client.mixin;

import com.hologrammenu.client.mixin.accessor.ScreenRenderablesAccessor;
import com.hologrammenu.client.screen.widget.ContainerTabLayer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ContainerTabRenderMixin {
	@Inject(method = "extractBackground", at = @At("RETURN"))
	private void hologrammenu$renderContainerTabsBeneathUi(
		final GuiGraphicsExtractor graphics,
		final int mouseX,
		final int mouseY,
		final float partialTick,
		final CallbackInfo ci
	) {
		if (!((Object) this instanceof AbstractContainerScreen<?>)) {
			return;
		}

		for (Renderable renderable : ((ScreenRenderablesAccessor) this).hologrammenu$getRenderables()) {
			if (renderable instanceof ContainerTabLayer tab && !tab.isTabSelected()) {
				tab.extractUnselectedTabIcon(graphics, mouseX, mouseY, partialTick);
			}
		}
	}
}
