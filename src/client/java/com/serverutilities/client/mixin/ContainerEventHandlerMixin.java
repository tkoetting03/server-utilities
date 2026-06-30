package com.serverutilities.client.mixin;

import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Optional;

@Mixin(ContainerEventHandler.class)
public interface ContainerEventHandlerMixin {
	@Inject(method = "getChildAt", at = @At("HEAD"), cancellable = true)
	private void serverutilities$topMostChildAt(double mouseX, double mouseY, CallbackInfoReturnable<Optional<GuiEventListener>> cir) {
		List<? extends GuiEventListener> children = ((ContainerEventHandler) (Object) this).children();
		for (int index = children.size() - 1; index >= 0; index--) {
			GuiEventListener child = children.get(index);
			if (child.isMouseOver(mouseX, mouseY)) {
				cir.setReturnValue(Optional.of(child));
				return;
			}
		}
		cir.setReturnValue(Optional.empty());
	}
}
