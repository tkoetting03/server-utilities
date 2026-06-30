package com.serverutilities.client.npc;

import com.serverutilities.npc.NpcConfig;
import com.serverutilities.network.ModPackets;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class NpcClientConfigStore {
	private static final Map<Integer, NpcConfig> BY_ENTITY = new ConcurrentHashMap<>();
	private static final Map<Integer, String> SKIN_BY_ENTITY = new ConcurrentHashMap<>();

	private NpcClientConfigStore() {
	}

	public static void put(int entityId, NpcConfig config) {
		BY_ENTITY.put(entityId, config);
	}

	public static void apply(ModPackets.NpcConfigPayload payload) {
		BY_ENTITY.put(payload.entityId(), NpcConfig.fromPayload(payload));
		SKIN_BY_ENTITY.put(payload.entityId(), payload.skinName() == null ? "" : payload.skinName());
	}

	public static void remove(int entityId) {
		BY_ENTITY.remove(entityId);
		SKIN_BY_ENTITY.remove(entityId);
	}

	public static void clear() {
		BY_ENTITY.clear();
		SKIN_BY_ENTITY.clear();
	}

	public static String skinName(int entityId) {
		return SKIN_BY_ENTITY.getOrDefault(entityId, "");
	}

	public static NpcConfig resolve(int entityId, LivingEntity entity) {
		NpcConfig cached = BY_ENTITY.get(entityId);
		if (cached != null) {
			return cached;
		}
		if (entity != null) {
			return NpcConfig.read(entity);
		}
		return NpcConfig.defaults();
	}
}
