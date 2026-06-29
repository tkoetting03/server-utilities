package com.hologrammenu.mixin;

import com.hologrammenu.storage.StorageMenuBlockData;
import com.hologrammenu.storage.StorageMenuHolder;
import com.hologrammenu.storage.StorageMenuHologramLabels;
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
	private static final String STORAGE_TAG = "hologrammenu:storage_menu";

	@Unique
	@Nullable
	private StorageMenuBlockData hologrammenu$storageData;

	@Override
	@Nullable
	public StorageMenuBlockData hologrammenu$getStorageData() {
		return hologrammenu$storageData;
	}

	@Override
	public void hologrammenu$setStorageData(@Nullable StorageMenuBlockData data) {
		this.hologrammenu$storageData = data;
		((BlockEntity) (Object) this).setChanged();
	}

	@Inject(method = "loadAdditional", at = @At("RETURN"))
	private void hologrammenu$loadStorageMenu(ValueInput input, CallbackInfo ci) {
		hologrammenu$storageData = input.read(STORAGE_TAG, StorageMenuBlockData.CODEC).orElse(null);
		hologrammenu$resyncHologramLabel();
	}

	@Unique
	private void hologrammenu$resyncHologramLabel() {
		BlockEntity blockEntity = (BlockEntity) (Object) this;
		if (!(blockEntity.getLevel() instanceof ServerLevel level) || hologrammenu$storageData == null) {
			return;
		}
		StorageMenuBlockData data = hologrammenu$storageData;
		level.getServer().execute(() -> StorageMenuHologramLabels.sync(
			level,
			blockEntity.getBlockPos(),
			data.definition().title(),
			data.hologramLabel(),
			data.hologramSettings()
		));
	}

	@Inject(method = "saveAdditional", at = @At("RETURN"))
	private void hologrammenu$saveStorageMenu(ValueOutput output, CallbackInfo ci) {
		output.storeNullable(STORAGE_TAG, StorageMenuBlockData.CODEC, hologrammenu$storageData);
	}
}
