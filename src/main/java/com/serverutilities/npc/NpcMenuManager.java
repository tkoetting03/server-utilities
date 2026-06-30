package com.serverutilities.npc;

import com.serverutilities.storage.ShopDefinition;
import com.serverutilities.storage.StorageMenuBlockData;
import com.serverutilities.storage.StorageMenuDefinition;
import com.serverutilities.storage.StorageMenuSizes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;
import java.util.UUID;

public final class NpcMenuManager {
	private NpcMenuManager() {
	}

	public static Optional<StorageMenuDefinition> get(ServerLevel level, UUID entityId) {
		return NpcMenuStore.get(level, entityId).map(StorageMenuBlockData::definition);
	}

	public static Optional<StorageMenuDefinition> getActive(ServerLevel level, UUID entityId) {
		return get(level, entityId).filter(StorageMenuDefinition::enabled);
	}

	public static void save(
		ServerLevel level,
		UUID entityId,
		StorageMenuDefinition definition,
		boolean invulnerable,
		boolean hologramLabel,
		ShopDefinition shop
	) {
		NpcMenuStore.set(level, entityId, new StorageMenuBlockData(definition, invulnerable, hologramLabel, shop));
	}

	public static void save(ServerLevel level, UUID entityId, StorageMenuDefinition definition) {
		boolean invulnerable = NpcMenuStore.get(level, entityId).map(StorageMenuBlockData::invulnerable).orElse(false);
		boolean hologramLabel = NpcMenuStore.get(level, entityId).map(StorageMenuBlockData::hologramLabel).orElse(false);
		ShopDefinition shop = NpcMenuStore.get(level, entityId).map(StorageMenuBlockData::shop).orElse(ShopDefinition.EMPTY);
		save(level, entityId, definition, invulnerable, hologramLabel, shop);
	}

	public static Optional<ShopDefinition> getShop(ServerLevel level, UUID entityId) {
		return NpcMenuStore.get(level, entityId).map(StorageMenuBlockData::shop);
	}

	public static void saveShop(ServerLevel level, UUID entityId, ShopDefinition shop) {
		StorageMenuBlockData existing = NpcMenuStore.get(level, entityId).orElse(
			new StorageMenuBlockData(StorageMenuDefinition.empty(StorageMenuSizes.SINGLE_CHEST), false, false, ShopDefinition.EMPTY)
		);
		NpcMenuStore.set(level, entityId, existing.withShop(shop));
	}

	public static void clear(ServerLevel level, UUID entityId) {
		NpcMenuStore.clear(level, entityId);
	}

	public static int resolveContainerSize(ServerLevel level, LivingEntity npc) {
		NpcConfig config = NpcConfig.read(npc);
		return NpcMenuStore.get(level, npc.getUUID())
			.filter(data -> data.definition().enabled())
			.map(data -> data.definition().containerSize())
			.orElse(NpcConfig.normalizeContainerSize(config.containerSize()));
	}

	public static boolean isSupportedContainerSize(int size) {
		return size == StorageMenuSizes.SINGLE_CHEST || size == StorageMenuSizes.DOUBLE_CHEST;
	}
}
