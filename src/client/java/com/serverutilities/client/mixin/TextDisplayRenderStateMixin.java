package com.serverutilities.client.mixin;

import com.serverutilities.client.render.TextDisplayRenderStateAccess;
import net.minecraft.client.renderer.entity.state.TextDisplayEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(TextDisplayEntityRenderState.class)
public abstract class TextDisplayRenderStateMixin implements TextDisplayRenderStateAccess {
	@Unique
	private boolean serverutilities$lateHologram;

	@Override
	public void serverutilities$setLateHologram(boolean value) {
		serverutilities$lateHologram = value;
	}

	@Override
	public boolean serverutilities$isLateHologram() {
		return serverutilities$lateHologram;
	}
}
