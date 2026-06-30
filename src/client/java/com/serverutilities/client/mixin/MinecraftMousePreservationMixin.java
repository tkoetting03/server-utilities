package com.serverutilities.client.mixin;

import com.serverutilities.client.screen.EditorMousePreservation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMousePreservationMixin {
	@Inject(method = "setScreen", at = @At("TAIL"))
	private void serverutilities$restoreMouseAfterSetScreen(Screen screen, CallbackInfo ci) {
		EditorMousePreservation.restoreIfPending();
	}
}
