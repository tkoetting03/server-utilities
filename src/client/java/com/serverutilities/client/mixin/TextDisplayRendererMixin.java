package com.serverutilities.client.mixin;

import com.serverutilities.client.hologram.LateHologramRenderer;
import com.serverutilities.client.render.TextDisplayRenderStateAccess;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.DisplayRenderer;
import net.minecraft.client.renderer.entity.state.TextDisplayEntityRenderState;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DisplayRenderer.TextDisplayRenderer.class)
public abstract class TextDisplayRendererMixin {
	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Display$TextDisplay;Lnet/minecraft/client/renderer/entity/state/TextDisplayEntityRenderState;F)V", at = @At("RETURN"))
	private void serverutilities$markLateHologram(Display.TextDisplay entity, TextDisplayEntityRenderState state, float partialTicks, CallbackInfo ci) {
		((TextDisplayRenderStateAccess) state).serverutilities$setLateHologram(LateHologramRenderer.isManagedHologram(entity));
	}

	@Inject(method = "submitInner", at = @At("HEAD"), cancellable = true)
	private void serverutilities$skipManagedHologram(
		TextDisplayEntityRenderState state,
		PoseStack poseStack,
		SubmitNodeCollector submitNodeCollector,
		int lightCoords,
		float interpolationProgress,
		CallbackInfo ci
	) {
		if (((TextDisplayRenderStateAccess) state).serverutilities$isLateHologram()) {
			ci.cancel();
		}
	}
}
