package com.serverutilities.client.npc;

import com.serverutilities.client.config.ClientSettings;
import com.serverutilities.client.screen.NpcOptionsScreen;
import com.serverutilities.client.storage.StorageMenuClientPermissions;
import com.serverutilities.npc.NpcHelper;
import net.fabricmc.fabric.api.event.client.player.ClientPreAttackCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;

import java.util.Optional;

public final class NpcClientEditInteractions {
	private NpcClientEditInteractions() {
	}

	public static void register() {
		UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
			if (!level.isClientSide() || !ClientSettings.npcEditModeEnabled || !NpcClientRegistry.isNpc(entity)) {
				return InteractionResult.PASS;
			}
			return openOptionsScreen(entity);
		});

		AttackEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
			if (!level.isClientSide() || !ClientSettings.npcEditModeEnabled || !NpcClientRegistry.isNpc(entity)) {
				return InteractionResult.PASS;
			}
			return openOptionsScreen(entity);
		});

		UseItemCallback.EVENT.register((player, level, hand) -> {
			if (!level.isClientSide() || !ClientSettings.npcEditModeEnabled) {
				return InteractionResult.PASS;
			}
			return findTargetNpc().map(NpcClientEditInteractions::openOptionsScreen).orElse(InteractionResult.PASS);
		});

		ClientPreAttackCallback.EVENT.register((client, player, clickCount) -> {
			if (!ClientSettings.npcEditModeEnabled) {
				return false;
			}
			return findTargetNpc()
				.map(entity -> {
					openOptionsScreen(entity);
					return true;
				})
				.orElse(false);
		});
	}

	private static Optional<Entity> findTargetNpc() {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null || client.level == null) {
			return Optional.empty();
		}
		if (client.hitResult instanceof EntityHitResult entityHit && NpcClientRegistry.isNpc(entityHit.getEntity())) {
			Entity entity = entityHit.getEntity();
			if (NpcHelper.canEdit(client.player, entity)) {
				return Optional.of(entity);
			}
		}
		return NpcHelper.findLookAtNpc(client.player, NpcClientRegistry::isNpc).map(entity -> entity);
	}

	private static InteractionResult openOptionsScreen(Entity entity) {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null) {
			return InteractionResult.PASS;
		}
		if (!StorageMenuClientPermissions.canEdit()) {
			client.player.sendOverlayMessage(Component.translatable("screen.serverutilities.npc_options.no_permission"));
			return InteractionResult.FAIL;
		}
		if (!NpcHelper.canEdit(client.player, entity)) {
			client.player.sendOverlayMessage(Component.translatable("screen.serverutilities.npc_options.too_far"));
			return InteractionResult.FAIL;
		}
		if (!(entity instanceof LivingEntity living)) {
			return InteractionResult.PASS;
		}
		client.setScreen(new NpcOptionsScreen(living.getId()));
		return InteractionResult.SUCCESS;
	}
}
