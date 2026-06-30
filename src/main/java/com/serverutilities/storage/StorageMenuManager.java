package com.serverutilities.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.Optional;

public final class StorageMenuManager {
	public record ResolvedMenu(BlockPos storagePos, StorageMenuDefinition definition) {
	}

	private StorageMenuManager() {
	}

	public static Optional<StorageMenuDefinition> get(ServerLevel level, BlockPos pos) {
		return StorageMenuBlockStore.get(level, pos).map(StorageMenuBlockData::definition);
	}

	public static Optional<StorageMenuDefinition> getActive(ServerLevel level, BlockPos pos) {
		return get(level, pos).filter(StorageMenuDefinition::enabled);
	}

	public static Optional<ResolvedMenu> resolveAtExact(ServerLevel level, BlockPos pos, int containerSize) {
		return getActive(level, pos)
			.filter(definition -> definition.containerSize() == containerSize)
			.map(definition -> new ResolvedMenu(pos, definition));
	}

	public static Optional<ResolvedMenu> resolveActive(ServerLevel level, BlockPos pos) {
		return getActive(level, pos).map(definition -> new ResolvedMenu(pos, definition));
	}

	public static void save(ServerLevel level, BlockPos pos, StorageMenuDefinition definition, boolean invulnerable, boolean hologramLabel) {
		ShopDefinition shop = StorageMenuBlockStore.get(level, pos).map(StorageMenuBlockData::shop).orElse(ShopDefinition.EMPTY);
		StorageMenuHologramSettings hologramSettings = StorageMenuBlockStore.get(level, pos)
			.map(StorageMenuBlockData::hologramSettings)
			.orElse(StorageMenuHologramSettings.DEFAULT);
		StorageMenuBlockStore.set(level, pos, new StorageMenuBlockData(definition, invulnerable, hologramLabel, hologramSettings, shop));
	}

	public static void save(ServerLevel level, BlockPos pos, StorageMenuDefinition definition, boolean invulnerable, boolean hologramLabel, ShopDefinition shop) {
		StorageMenuHologramSettings hologramSettings = StorageMenuBlockStore.get(level, pos)
			.map(StorageMenuBlockData::hologramSettings)
			.orElse(StorageMenuHologramSettings.DEFAULT);
		save(level, pos, definition, invulnerable, hologramLabel, hologramSettings, shop);
	}

	public static void save(
		ServerLevel level,
		BlockPos pos,
		StorageMenuDefinition definition,
		boolean invulnerable,
		boolean hologramLabel,
		StorageMenuHologramSettings hologramSettings,
		ShopDefinition shop
	) {
		StorageMenuBlockStore.set(level, pos, new StorageMenuBlockData(
			definition,
			invulnerable,
			hologramLabel,
			hologramSettings == null ? StorageMenuHologramSettings.DEFAULT : hologramSettings,
			shop == null ? ShopDefinition.EMPTY : shop
		));
	}

	public static void saveShop(ServerLevel level, BlockPos pos, ShopDefinition shop) {
		StorageMenuBlockData existing = StorageMenuBlockStore.get(level, pos).orElse(
			new StorageMenuBlockData(StorageMenuDefinition.empty(StorageMenuSizes.SINGLE_CHEST), false, false, ShopDefinition.EMPTY)
		);
		StorageMenuBlockStore.set(level, pos, existing.withShop(shop));
	}

	public static Optional<ShopDefinition> getShop(ServerLevel level, BlockPos pos) {
		return StorageMenuBlockStore.get(level, pos).map(StorageMenuBlockData::shop);
	}

	public static void save(ServerLevel level, BlockPos pos, StorageMenuDefinition definition, boolean invulnerable) {
		boolean hologramLabel = StorageMenuBlockStore.get(level, pos).map(StorageMenuBlockData::hologramLabel).orElse(false);
		save(level, pos, definition, invulnerable, hologramLabel);
	}

	public static void save(ServerLevel level, BlockPos pos, StorageMenuDefinition definition) {
		boolean invulnerable = StorageMenuBlockStore.get(level, pos).map(StorageMenuBlockData::invulnerable).orElse(false);
		save(level, pos, definition, invulnerable);
	}

	public static void clear(ServerLevel level, BlockPos pos) {
		StorageMenuHologramLabels.remove(level, pos);
		StorageMenuBlockStore.clear(level, pos);
	}

	public static boolean isInvulnerable(ServerLevel level, BlockPos pos) {
		return StorageMenuBlockStore.isInvulnerable(level, pos);
	}
}
