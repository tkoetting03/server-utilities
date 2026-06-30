package com.serverutilities.client.mixin;

import com.serverutilities.client.screen.ModUiRenderContext;
import com.serverutilities.client.screen.ModUiSelectionState;
import com.serverutilities.client.screen.widget.UiScale;
import com.serverutilities.client.screen.widget.UiSelectionOutline;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractWidget.class)
public abstract class AbstractWidgetSelectionOutlineMixin {
	@Inject(method = "extractRenderState", at = @At("RETURN"))
	private void serverutilities$drawSelectionOutline(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		AbstractWidget self = (AbstractWidget) (Object) this;
		if (!self.visible) {
			return;
		}
		if (ModUiSelectionState.isEffectButton(self)) {
			if (ModUiSelectionState.isSelected(self)) {
				drawSelectionOutline(graphics, self);
			}
			return;
		}
		boolean selected = ModUiSelectionState.isSelected(self);
		boolean focused = self.isFocused() && ModUiRenderContext.isInteractive(self) && !selected;
		if (selected || focused) {
			drawSelectionOutline(graphics, self);
		}
	}

	private static void drawSelectionOutline(GuiGraphicsExtractor graphics, AbstractWidget widget) {
		if (ModUiRenderContext.shouldScaleWidgetRender(widget)) {
			UiSelectionOutline.drawForWidget(graphics, widget, UiScale.TEXT_SCALE);
		} else {
			UiSelectionOutline.draw(graphics, widget);
		}
	}
}
