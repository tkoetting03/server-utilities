package com.hologrammenu.client.hologram;

import com.hologrammenu.client.config.ClientSettings;
import com.hologrammenu.client.screen.HologramOptionsScreen;
import com.hologrammenu.hologram.HologramHelper;
import com.hologrammenu.hologram.HologramLineStack;
import com.hologrammenu.hologram.HologramScale;
import com.hologrammenu.mixin.accessor.TextDisplayAccessor;
import com.hologrammenu.text.TextFormats;
import net.fabricmc.fabric.api.event.client.player.ClientPreAttackCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class HologramClientEditInteractions {
	private HologramClientEditInteractions() {
	}

	public static void register() {
		UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
			if (!level.isClientSide() || !ClientSettings.hologramEditModeEnabled || !HologramClientRegistry.isEditableHologram(entity)) {
				return InteractionResult.PASS;
			}
			return openOptionsScreen(entity);
		});

		AttackEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
			if (!level.isClientSide() || !ClientSettings.hologramEditModeEnabled || !HologramClientRegistry.isEditableHologram(entity)) {
				return InteractionResult.PASS;
			}
			return openOptionsScreen(entity);
		});

		UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
			if (!level.isClientSide() || !ClientSettings.hologramEditModeEnabled) {
				return InteractionResult.PASS;
			}
			return findTargetHologram().map(HologramClientEditInteractions::openOptionsScreen).orElse(InteractionResult.PASS);
		});

		UseItemCallback.EVENT.register((player, level, hand) -> {
			if (!level.isClientSide() || !ClientSettings.hologramEditModeEnabled) {
				return InteractionResult.PASS;
			}
			return findTargetHologram().map(HologramClientEditInteractions::openOptionsScreen).orElse(InteractionResult.PASS);
		});

		ClientPreAttackCallback.EVENT.register((client, player, clickCount) -> {
			if (!ClientSettings.hologramEditModeEnabled) {
				return false;
			}
			return findTargetHologram()
				.map(entity -> {
					openOptionsScreen(entity);
					return true;
				})
				.orElse(false);
		});
	}

	private static Optional<Entity> findTargetHologram() {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null || client.level == null) {
			return Optional.empty();
		}

		if (client.hitResult instanceof EntityHitResult entityHit && HologramClientRegistry.isEditableHologram(entityHit.getEntity())) {
			Entity entity = entityHit.getEntity();
			if (HologramHelper.canEdit(client.player, entity)) {
				return Optional.of(entity);
			}
		}

		return HologramHelper.findLookAtHologram(
			client.player,
			HologramClientRegistry::isEditableHologram,
			HologramHelper.EDIT_MAX_DISTANCE
		).map(entity -> entity);
	}

	private static InteractionResult openOptionsScreen(Entity entity) {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null || !HologramHelper.canEdit(client.player, entity)) {
			if (client.player != null) {
				client.player.sendOverlayMessage(Component.translatable("screen.hologrammenu.hologram_options.too_far"));
			}
			return InteractionResult.FAIL;
		}

		client.setScreen(new HologramOptionsScreen(entity.getId(), readEditableLines(client, entity)));
		return InteractionResult.SUCCESS;
	}

	private static List<HologramLineStack.Line> readEditableLines(Minecraft client, Entity entity) {
		if (!(entity instanceof net.minecraft.world.entity.Display.TextDisplay display)) {
			return HologramLineStack.defaults("");
		}
		String groupId = registryOrTagGroupId(display);
		if (groupId.isBlank() || client.level == null) {
			String text = TextFormats.fromComponent(((TextDisplayAccessor) display).hologrammenu$getText());
			return HologramLineStack.parseLegacyText(text, HologramScale.getScale(display));
		}

		Map<Integer, net.minecraft.world.entity.Display.TextDisplay> group = new LinkedHashMap<>();
		for (int entityId : HologramClientRegistry.knownIds()) {
			Entity candidate = client.level.getEntity(entityId);
			if (candidate instanceof net.minecraft.world.entity.Display.TextDisplay line
				&& groupId.equals(registryOrTagGroupId(line))) {
				group.put(line.getId(), line);
			}
		}
		for (Entity candidate : client.level.entitiesForRendering()) {
			if (candidate instanceof net.minecraft.world.entity.Display.TextDisplay line
				&& HologramClientRegistry.isEditableHologram(line)
				&& groupId.equals(registryOrTagGroupId(line))) {
				group.put(line.getId(), line);
			}
		}
		for (net.minecraft.world.entity.Display.TextDisplay line : client.level.getEntities(
			net.minecraft.world.entity.EntityType.TEXT_DISPLAY,
			display.getBoundingBox().inflate(2.0D, 4.0D, 2.0D),
			candidate -> HologramClientRegistry.isEditableHologram(candidate)
				&& groupId.equals(registryOrTagGroupId(candidate))
		)) {
			group.put(line.getId(), line);
		}
		List<net.minecraft.world.entity.Display.TextDisplay> sortedGroup = new ArrayList<>(group.values());
		sortedGroup.sort(Comparator.comparingInt(HologramClientEditInteractions::registryOrTagLineIndex));
		return sortedGroup.isEmpty()
			? HologramLineStack.parseLegacyText(TextFormats.fromComponent(((TextDisplayAccessor) display).hologrammenu$getText()), HologramScale.getScale(display))
			: HologramLineStack.readGroup(sortedGroup);
	}

	private static String registryOrTagGroupId(Entity entity) {
		String registryGroup = HologramClientRegistry.groupId(entity);
		if (!registryGroup.isBlank()) {
			return registryGroup;
		}
		UUID tagGroup = HologramLineStack.groupId(entity);
		return tagGroup == null ? "" : tagGroup.toString();
	}

	private static int registryOrTagLineIndex(Entity entity) {
		if (!HologramClientRegistry.groupId(entity).isBlank()) {
			return HologramClientRegistry.lineIndex(entity);
		}
		return HologramLineStack.lineIndex(entity);
	}
}
