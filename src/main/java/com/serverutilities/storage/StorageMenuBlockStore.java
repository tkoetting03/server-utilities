package com.serverutilities.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

public final class StorageMenuBlockStore {
	private StorageMenuBlockStore() {
	}

	public static Optional<StorageMenuBlockData> get(ServerLevel level, BlockPos pos) {
		BlockPos immutablePos = pos.immutable();
		BlockState state = level.getBlockState(immutablePos);
		if (state.isAir()) {
			return Optional.empty();
		}

		BlockEntity blockEntity = level.getBlockEntity(immutablePos);
		if (blockEntity instanceof StorageMenuHolder holder) {
			StorageMenuBlockData data = holder.serverutilities$getStorageData();
			if (data != null) {
				return Optional.of(data);
			}
		}

		Identifier blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
		Optional<StorageMenuBlockData> loose = loose(level).get(immutablePos, blockId);
		if (loose.isPresent()) {
			return loose;
		}

		return legacy(level, immutablePos);
	}

	public static void set(ServerLevel level, BlockPos pos, StorageMenuBlockData data) {
		BlockPos immutablePos = pos.immutable();
		BlockState state = level.getBlockState(immutablePos);
		if (state.isAir()) {
			return;
		}

		BlockEntity blockEntity = level.getBlockEntity(immutablePos);
		if (blockEntity instanceof StorageMenuHolder holder) {
			holder.serverutilities$setStorageData(data);
			legacyStorage(level).remove(immutablePos);
			loose(level).remove(immutablePos);
			return;
		}

		Identifier blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
		loose(level).put(immutablePos, blockId, data);
		legacyStorage(level).remove(immutablePos);
	}

	public static void clear(ServerLevel level, BlockPos pos) {
		BlockPos immutablePos = pos.immutable();
		BlockEntity blockEntity = level.getBlockEntity(immutablePos);
		if (blockEntity instanceof StorageMenuHolder holder) {
			holder.serverutilities$setStorageData(null);
		}
		loose(level).remove(immutablePos);
		legacyStorage(level).remove(immutablePos);
	}

	public static void ensureEnabled(ServerLevel level, BlockPos pos, int containerSize) {
		Optional<StorageMenuBlockData> existing = get(level, pos);
		if (existing.isPresent()) {
			StorageMenuBlockData data = existing.get();
			StorageMenuDefinition definition = data.definition();
			if (definition.containerSize() != containerSize) {
				definition = StorageMenuDefinition.empty(containerSize).withEnabled(true).withTitle(definition.title());
			} else if (!definition.enabled()) {
				definition = definition.withEnabled(true);
			} else {
				return;
			}
			set(level, pos, data.withDefinition(definition));
			return;
		}

		set(level, pos, new StorageMenuBlockData(
			StorageMenuDefinition.empty(containerSize).withEnabled(true),
			false,
			false
		));
	}

	public static boolean isInvulnerable(ServerLevel level, BlockPos pos) {
		return get(level, pos).map(StorageMenuBlockData::invulnerable).orElse(false);
	}

	private static Optional<StorageMenuBlockData> legacy(ServerLevel level, BlockPos pos) {
		return legacyStorage(level).get(pos).map(definition -> {
			StorageMenuBlockData data = new StorageMenuBlockData(definition, false, false);
			set(level, pos, data);
			return data;
		});
	}

	private static StorageMenuSavedData legacyStorage(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(StorageMenuSavedData.TYPE);
	}

	private static StorageMenuLooseSavedData loose(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(StorageMenuLooseSavedData.TYPE);
	}
}
