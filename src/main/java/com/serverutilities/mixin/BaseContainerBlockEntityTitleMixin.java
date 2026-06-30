package com.serverutilities.mixin;

import com.serverutilities.storage.StorageMenuDefinition;
import com.serverutilities.storage.StorageMenuManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BaseContainerBlockEntity.class)
public abstract class BaseContainerBlockEntityTitleMixin {
	@Inject(method = "getDisplayName", at = @At("HEAD"), cancellable = true)
	private void serverutilities$storageMenuTitle(CallbackInfoReturnable<Component> cir) {
		BaseContainerBlockEntity self = (BaseContainerBlockEntity) (Object) this;
		if (!(self.getLevel() instanceof ServerLevel level)) {
			return;
		}

		BlockPos pos = self.getBlockPos();
		StorageMenuManager.resolveActive(level, pos)
			.map(StorageMenuManager.ResolvedMenu::definition)
			.flatMap(StorageMenuDefinition::displayTitle)
			.ifPresent(cir::setReturnValue);
	}
}
