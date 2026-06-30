package com.serverutilities.client.mixin;

import com.serverutilities.client.screen.EditorMousePreservation;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerPreservationMixin {
	@Inject(method = "grabMouse", at = @At("TAIL"))
	private void serverutilities$restoreMouseAfterGrab(CallbackInfo ci) {
		EditorMousePreservation.restoreIfPending();
	}

	@Inject(method = "releaseMouse", at = @At("TAIL"))
	private void serverutilities$restoreMouseAfterRelease(CallbackInfo ci) {
		EditorMousePreservation.restoreIfPending();
	}
}
