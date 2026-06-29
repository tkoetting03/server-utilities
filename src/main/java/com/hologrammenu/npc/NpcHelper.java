package com.hologrammenu.npc;

import com.hologrammenu.HologramMenuMod;
import com.hologrammenu.head.HeadProfileHelper;
import com.hologrammenu.mixin.accessor.MannequinAccessor;
import com.hologrammenu.text.TextFormats;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.npc.villager.VillagerType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

public final class NpcHelper {
	public static final double PLACE_MAX_DISTANCE = 6.0D;
	public static final double EDIT_MAX_DISTANCE = 5.0D;
	private static final String SKIN_TAG_PREFIX = "hologrammenu:npc_skin:";
	private static final double EDIT_LOOK_DOT_THRESHOLD = 0.85D;
	private static final double EDIT_RAY_DISTANCE = 3.5D;

	private NpcHelper() {
	}

	public static boolean isNpc(Entity entity) {
		return entity instanceof LivingEntity && entity.entityTags().contains(HologramMenuMod.NPC_TAG);
	}

	public static boolean isPlayerNpc(LivingEntity entity) {
		return entity instanceof Mannequin;
	}

	public static boolean canEdit(Player player, Entity entity) {
		return player.distanceToSqr(entity) <= EDIT_MAX_DISTANCE * EDIT_MAX_DISTANCE;
	}

	public static Optional<LivingEntity> findLookAtNpc(Player player) {
		return findLookAtNpc(player, NpcHelper::isNpc);
	}

	public static Optional<LivingEntity> findLookAtNpc(Player player, Predicate<Entity> isNpc) {
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getLookAngle();
		double maxDistanceSq = EDIT_MAX_DISTANCE * EDIT_MAX_DISTANCE;
		double maxRayDistanceSq = EDIT_RAY_DISTANCE * EDIT_RAY_DISTANCE;

		LivingEntity best = null;
		double bestScore = maxRayDistanceSq;
		AABB searchBox = player.getBoundingBox().inflate(EDIT_MAX_DISTANCE);

		for (LivingEntity entity : player.level().getEntitiesOfClass(LivingEntity.class, searchBox, candidate -> isNpc.test(candidate))) {
			if (!canEdit(player, entity)) {
				continue;
			}

			Vec3 target = entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
			Vec3 toTarget = target.subtract(eye);
			double distanceSq = toTarget.lengthSqr();
			if (distanceSq > maxDistanceSq || distanceSq < 1.0E-4D) {
				continue;
			}

			double dot = look.dot(toTarget.normalize());
			if (dot < EDIT_LOOK_DOT_THRESHOLD) {
				continue;
			}

			double rayDistanceSq = distanceToRaySquared(eye, look, target);
			if (rayDistanceSq > bestScore) {
				continue;
			}

			bestScore = rayDistanceSq;
			best = entity;
		}

		return Optional.ofNullable(best);
	}

	public static void tickHeadFollow(ServerLevel level) {
		Set<Integer> processed = new HashSet<>();
		for (ServerPlayer player : level.players()) {
			AABB box = player.getBoundingBox().inflate(32.0D);
			for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, NpcHelper::isNpc)) {
				if (!processed.add(entity.getId())) {
					continue;
				}
				if (entity instanceof Mannequin mannequin) {
					((MannequinAccessor) mannequin).hologrammenu$setHideDescription(true);
				}
				NpcConfig config = NpcConfig.read(entity);
				if (!config.headFollowEnabled()) {
					continue;
				}
				Player nearest = level.getNearestPlayer(entity, config.headFollowRadius());
				if (nearest == null) {
					continue;
				}
				lookAt(entity, nearest);
			}
		}
	}

	public static LivingEntity place(
		ServerPlayer player,
		ServerLevel level,
		Vec3 position,
		String type,
		String skinName,
		String professionId,
		String displayName
	) {
		if ("player".equalsIgnoreCase(type)) {
			return placePlayerNpc(level, position, player.getYRot(), skinName, displayName);
		}
		return placeVillagerNpc(level, position, player.getYRot(), professionId, displayName);
	}

	public static String readSkinName(LivingEntity entity) {
		for (String tag : entity.entityTags()) {
			if (tag.startsWith(SKIN_TAG_PREFIX)) {
				return tag.substring(SKIN_TAG_PREFIX.length());
			}
		}
		return "";
	}

	public static void update(
		LivingEntity entity,
		String displayName,
		String skinName,
		String professionId,
		NpcConfig config
	) {
		applyDisplayName(entity, displayName);
		if (entity instanceof Mannequin mannequin) {
			entity.entityTags().removeIf(tag -> tag.startsWith(SKIN_TAG_PREFIX));
			if (skinName != null && !skinName.isBlank()) {
				String trimmed = skinName.trim();
				entity.addTag(SKIN_TAG_PREFIX + trimmed);
				HeadProfileHelper.resolveSkin(trimmed).ifPresent(profile ->
					((MannequinAccessor) mannequin).hologrammenu$setProfile(profile)
				);
			}
		}
		if (entity instanceof Villager villager) {
			VillagerData current = villager.getVillagerData();
			villager.setVillagerData(new VillagerData(
				current.type(),
				resolveProfession(professionId),
				current.level()
			));
		}
		config.write(entity);
		if (entity instanceof Mannequin mannequin) {
			((MannequinAccessor) mannequin).hologrammenu$setHideDescription(true);
		}
	}

	public static String readDisplayName(LivingEntity entity) {
		return NpcHologramStack.readStyledName(entity);
	}

	public static String readProfessionId(Villager villager) {
		return villager.getVillagerData().profession().unwrapKey()
			.map(key -> key.identifier().toString())
			.orElse(VillagerProfession.NONE.identifier().toString());
	}

	private static LivingEntity placePlayerNpc(ServerLevel level, Vec3 position, float yaw, String skinName, String displayName) {
		Mannequin mannequin = EntityType.MANNEQUIN.create(level, EntitySpawnReason.COMMAND);
		if (mannequin == null) {
			return null;
		}

		mannequin.setPos(position.x, position.y, position.z);
		mannequin.setYRot(yaw);
		mannequin.setInvulnerable(true);
		mannequin.setNoGravity(false);
		mannequin.addTag(HologramMenuMod.NPC_TAG);

		if (skinName != null && !skinName.isBlank()) {
			String trimmed = skinName.trim();
			mannequin.addTag(SKIN_TAG_PREFIX + trimmed);
			HeadProfileHelper.resolveSkin(trimmed).ifPresent(profile ->
				((MannequinAccessor) mannequin).hologrammenu$setProfile(profile)
			);
		}
		((MannequinAccessor) mannequin).hologrammenu$setImmovable(true);
		((MannequinAccessor) mannequin).hologrammenu$setHideDescription(true);
		applyDisplayName(mannequin, displayName);
		level.addFreshEntity(mannequin);
		return mannequin;
	}

	private static LivingEntity placeVillagerNpc(ServerLevel level, Vec3 position, float yaw, String professionId, String displayName) {
		Villager villager = EntityType.VILLAGER.create(level, EntitySpawnReason.COMMAND);
		if (villager == null) {
			return null;
		}

		villager.setPos(position.x, position.y, position.z);
		villager.setYRot(yaw);
		villager.setVillagerData(new VillagerData(
			BuiltInRegistries.VILLAGER_TYPE.getOrThrow(VillagerType.PLAINS),
			resolveProfession(professionId),
			1
		));
		villager.setNoAi(true);
		villager.setInvulnerable(true);
		villager.setPersistenceRequired();
		villager.addTag(HologramMenuMod.NPC_TAG);
		applyDisplayName(villager, displayName);
		level.addFreshEntity(villager);
		return villager;
	}

	private static Holder<VillagerProfession> resolveProfession(String professionId) {
		if (professionId == null || professionId.isBlank()) {
			return BuiltInRegistries.VILLAGER_PROFESSION.getOrThrow(VillagerProfession.NONE);
		}

		Identifier location = Identifier.tryParse(professionId.trim());
		if (location == null) {
			return BuiltInRegistries.VILLAGER_PROFESSION.getOrThrow(VillagerProfession.NONE);
		}

		ResourceKey<VillagerProfession> key = ResourceKey.create(Registries.VILLAGER_PROFESSION, location);
		return BuiltInRegistries.VILLAGER_PROFESSION.getOptional(key)
			.map(BuiltInRegistries.VILLAGER_PROFESSION::wrapAsHolder)
			.orElseGet(() -> BuiltInRegistries.VILLAGER_PROFESSION.getOrThrow(VillagerProfession.NONE));
	}

	private static void applyDisplayName(Entity entity, String displayName) {
		if (!(entity instanceof LivingEntity living)) {
			return;
		}
		NpcHologramStack.writeStyledName(living, displayName == null ? "" : displayName);
		if (displayName == null || displayName.isBlank()) {
			entity.setCustomName(null);
			entity.setCustomNameVisible(false);
			return;
		}
		entity.setCustomName(TextFormats.toComponent(displayName.trim()));
		entity.setCustomNameVisible(true);
	}

	private static void lookAt(LivingEntity entity, Player player) {
		Vec3 target = player.getEyePosition().subtract(entity.getEyePosition());
		double horizontal = Math.sqrt(target.x * target.x + target.z * target.z);
		float yaw = (float) (Math.atan2(target.z, target.x) * (180.0D / Math.PI)) - 90.0F;
		float pitch = (float) -(Math.atan2(target.y, horizontal) * (180.0D / Math.PI));
		entity.setYRot(yaw);
		entity.setYHeadRot(yaw);
		entity.setXRot(pitch);
	}

	private static double distanceToRaySquared(Vec3 origin, Vec3 direction, Vec3 point) {
		Vec3 offset = point.subtract(origin);
		double alongRay = offset.dot(direction);
		if (alongRay <= 0.0D) {
			return Double.MAX_VALUE;
		}
		return offset.subtract(direction.scale(alongRay)).lengthSqr();
	}
}
