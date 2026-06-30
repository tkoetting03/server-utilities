package com.serverutilities.npc;

import com.serverutilities.particle.ParticlePresetCatalog;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;

public final class NpcParticleEffects {
	private static final int SPAWN_INTERVAL_TICKS = 8;

	private NpcParticleEffects() {
	}

	public static int spawnIntervalTicks() {
		return SPAWN_INTERVAL_TICKS;
	}

	public static Optional<ParticleOptions> resolve(String particleId) {
		if (particleId == null || particleId.isBlank()) {
			return Optional.empty();
		}
		Identifier identifier = Identifier.tryParse(particleId);
		if (identifier == null) {
			return Optional.empty();
		}
		ParticleType<?> type = BuiltInRegistries.PARTICLE_TYPE.getValue(identifier);
		if (type instanceof SimpleParticleType simple) {
			return Optional.of(simple);
		}
		return Optional.empty();
	}

	public static void tickNpc(ServerLevel level, LivingEntity entity, NpcConfig config, long gameTime) {
		if (!config.particleEffectEnabled() || config.particleEffectId() == null || config.particleEffectId().isBlank()) {
			return;
		}
		if (gameTime % SPAWN_INTERVAL_TICKS != 0L) {
			return;
		}
		if (!level.hasNearbyAlivePlayer(entity.getX(), entity.getY(), entity.getZ(), 48.0D)) {
			return;
		}
		Optional<ParticleOptions> particle = resolve(config.particleEffectId());
		if (particle.isEmpty()) {
			return;
		}
		double x = entity.getX();
		double y = entity.getY() + entity.getBbHeight() * 0.65D;
		double z = entity.getZ();
		double spread = Math.max(0.15D, entity.getBbWidth() * 0.35D);
		level.sendParticles(particle.get(), x, y, z, 2, spread, 0.15D, spread, 0.01D);
	}

	public static String sanitizeParticleId(String particleId) {
		if (particleId == null || particleId.isBlank()) {
			return "";
		}
		String trimmed = particleId.trim();
		if (ParticlePresetCatalog.get(trimmed).isPresent()) {
			return trimmed;
		}
		return resolve(trimmed).isPresent() ? trimmed : "";
	}
}
