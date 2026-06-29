package com.hologrammenu.client.npc;

import com.hologrammenu.HologramMenuMod;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.entity.npc.villager.Villager;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class NpcClientRegistry {
	private static final Set<Integer> NPC_IDS = ConcurrentHashMap.newKeySet();

	private NpcClientRegistry() {
	}

	public static void sync(Collection<Integer> entityIds) {
		NPC_IDS.clear();
		NPC_IDS.addAll(entityIds);
	}

	public static void track(int entityId) {
		NPC_IDS.add(entityId);
	}

	public static void untrack(int entityId) {
		NPC_IDS.remove(entityId);
	}

	public static void clear() {
		NPC_IDS.clear();
	}

	public static boolean isNpc(Entity entity) {
		if (entity == null) {
			return false;
		}
		if (NPC_IDS.contains(entity.getId())) {
			return true;
		}
		if (entity instanceof LivingEntity living && entity.entityTags().contains(HologramMenuMod.NPC_TAG)) {
			return true;
		}
		return isModNpcHeuristic(entity);
	}

	private static boolean isModNpcHeuristic(Entity entity) {
		if (!(entity instanceof LivingEntity living) || !living.isInvulnerable()) {
			return false;
		}
		if (living instanceof Mannequin) {
			return true;
		}
		return living instanceof Villager villager && villager.isNoAi();
	}
}
