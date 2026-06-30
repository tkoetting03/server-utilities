package com.serverutilities.client.mixin;

import com.serverutilities.client.storage.StorageMenuClientTracker;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {
	@Inject(method = "useItemOn", at = @At("HEAD"))
	private void serverutilities$trackContainerOpen(LocalPlayer player, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
		BlockPos pos = hit.getBlockPos();
		if (player.level().getBlockEntity(pos) instanceof BaseContainerBlockEntity) {
			StorageMenuClientTracker.rememberOpenedContainer(pos);
		}
	}
}
