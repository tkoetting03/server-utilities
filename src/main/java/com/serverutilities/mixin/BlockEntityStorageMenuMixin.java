package com.serverutilities.mixin;

import com.serverutilities.storage.StorageMenuBlockData;
import com.serverutilities.storage.StorageMenuHolder;
import com.serverutilities.storage.StorageMenuHologramLabels;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntity.class)
public abstract class BlockEntityStorageMenuMixin implements StorageMenuHolder {
	@Unique
	private static final String STORAGE_TAG = "serverutilities:storage_menu";

	@Unique
	@Nullable
	private StorageMenuBlockData serverutilities$storageData;

	@Override
	@Nullable
	public StorageMenuBlockData serverutilities$getStorageData() {
		return serverutilities$storageData;
	}

	@Override
	public void serverutilities$setStorageData(@Nullable StorageMenuBlockData data) {
		this.serverutilities$storageData = data;
		((BlockEntity) (Object) this).setChanged();
	}

	@Inject(method = "loadAdditional", at = @At("RETURN"))
	private void serverutilities$loadStorageMenu(ValueInput input, CallbackInfo ci) {
		serverutilities$storageData = input.read(STORAGE_TAG, StorageMenuBlockData.CODEC).orElse(null);
		serverutilities$resyncHologramLabel();
	}

	@Unique
	private void serverutilities$resyncHologramLabel() {
		BlockEntity blockEntity = (BlockEntity) (Object) this;
		if (!(blockEntity.getLevel() instanceof ServerLevel level) || serverutilities$storageData == null) {
			return;
		}
		StorageMenuBlockData data = serverutilities$storageData;
		level.getServer().execute(() -> StorageMenuHologramLabels.sync(
			level,
			blockEntity.getBlockPos(),
			data.definition().title(),
			data.hologramLabel(),
			data.hologramSettings()
		));
	}

	@Inject(method = "saveAdditional", at = @At("RETURN"))
	private void serverutilities$saveStorageMenu(ValueOutput output, CallbackInfo ci) {
		output.storeNullable(STORAGE_TAG, StorageMenuBlockData.CODEC, serverutilities$storageData);
	}
}
