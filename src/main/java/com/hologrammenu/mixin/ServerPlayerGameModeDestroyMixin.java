package com.hologrammenu.mixin;

import com.hologrammenu.storage.StorageMenuBlockProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeDestroyMixin {
	@Shadow
	protected ServerLevel level;

	@Shadow
	protected ServerPlayer player;

	@Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
	private void hologrammenu$denyInvulnerableDestroy(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
		if (StorageMenuBlockProtection.tryDenyBreak(this.level, pos, this.player)) {
			cir.setReturnValue(false);
		}
	}
}
