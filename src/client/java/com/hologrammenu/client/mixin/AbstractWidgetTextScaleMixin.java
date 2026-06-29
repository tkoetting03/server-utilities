package com.hologrammenu.client.mixin;

import com.hologrammenu.client.screen.ModUiRenderContext;
import com.hologrammenu.client.screen.widget.UiScale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractWidget.class)
public abstract class AbstractWidgetTextScaleMixin {
	@Unique
	private boolean hologrammenu$scalingRender;

	@Inject(
		method = "extractRenderState",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/gui/components/AbstractWidget;extractWidgetRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V",
			shift = At.Shift.BEFORE
		)
	)
	private void hologrammenu$pushWidgetScale(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		AbstractWidget self = (AbstractWidget) (Object) this;
		if (!ModUiRenderContext.shouldScaleWidgetRender(self)) {
			return;
		}
		float scale = UiScale.TEXT_SCALE;
		int centerX = self.getX() + self.getWidth() / 2;
		int centerY = self.getY() + self.getHeight() / 2;
		graphics.pose().pushMatrix();
		graphics.pose().translate(centerX, centerY);
		graphics.pose().scale(scale, scale);
		graphics.pose().translate(-centerX, -centerY);
		hologrammenu$scalingRender = true;
	}

	@Inject(
		method = "extractRenderState",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/gui/components/AbstractWidget;extractWidgetRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V",
			shift = At.Shift.AFTER
		)
	)
	private void hologrammenu$popWidgetScale(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		if (!hologrammenu$scalingRender) {
			return;
		}
		graphics.pose().popMatrix();
		hologrammenu$scalingRender = false;
	}
}
