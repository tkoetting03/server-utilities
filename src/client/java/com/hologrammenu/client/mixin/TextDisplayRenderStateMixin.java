package com.hologrammenu.client.mixin;

import com.hologrammenu.client.render.TextDisplayRenderStateAccess;
import net.minecraft.client.renderer.entity.state.TextDisplayEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(TextDisplayEntityRenderState.class)
public abstract class TextDisplayRenderStateMixin implements TextDisplayRenderStateAccess {
	@Unique
	private boolean hologrammenu$lateHologram;

	@Override
	public void hologrammenu$setLateHologram(boolean value) {
		hologrammenu$lateHologram = value;
	}

	@Override
	public boolean hologrammenu$isLateHologram() {
		return hologrammenu$lateHologram;
	}
}
