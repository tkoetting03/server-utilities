package com.hologrammenu.network;

import com.hologrammenu.hologram.HologramEditMode;
import com.hologrammenu.hologram.HologramHelper;
import com.hologrammenu.hologram.HologramLineStack;
import com.hologrammenu.hologram.HologramPlacementMode;
import com.hologrammenu.hologram.HologramSync;
import com.hologrammenu.npc.NpcHologramLabels;
import com.hologrammenu.npc.NpcHologramStack;
import com.hologrammenu.npc.NpcConfig;
import com.hologrammenu.npc.NpcEditMode;
import com.hologrammenu.npc.NpcHelper;
import com.hologrammenu.npc.NpcMenuOpener;
import com.hologrammenu.npc.NpcMenuStore;
import com.hologrammenu.npc.NpcParticleEffects;
import com.hologrammenu.npc.NpcPlacementMode;
import com.hologrammenu.npc.NpcSync;
import com.hologrammenu.itemstyler.ItemStylerSessions;
import com.hologrammenu.anvil.AnvilLoreSession;
import com.hologrammenu.storage.StorageMenuItemLore;
import com.hologrammenu.storage.StorageMenuPermissions;
import com.hologrammenu.text.TextFormats;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class NetworkHandler {
	private static final int MAX_PLACEMENT_TEXT_LENGTH = 256;
	private static final int MAX_NPC_NAME_LENGTH = 64;
	private static final int MAX_PROFILE_NAME_LENGTH = 64;
	private static final int MAX_NPC_DIALOGUE_LENGTH = 1024;

	private NetworkHandler() {
	}

	public static void registerServer() {
		PayloadTypeRegistry.clientboundPlay().register(ModPackets.HologramTrackPayload.TYPE, ModPackets.HologramTrackPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(ModPackets.HologramUntrackPayload.TYPE, ModPackets.HologramUntrackPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(ModPackets.HologramSyncPayload.TYPE, ModPackets.HologramSyncPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(ModPackets.HologramOpenScreenPayload.TYPE, ModPackets.HologramOpenScreenPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(ModPackets.NpcTrackPayload.TYPE, ModPackets.NpcTrackPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(ModPackets.NpcUntrackPayload.TYPE, ModPackets.NpcUntrackPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(ModPackets.NpcSyncPayload.TYPE, ModPackets.NpcSyncPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(ModPackets.NpcConfigPayload.TYPE, ModPackets.NpcConfigPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(ModPackets.HologramEditPayload.TYPE, ModPackets.HologramEditPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(ModPackets.HologramOpenAtBlockPayload.TYPE, ModPackets.HologramOpenAtBlockPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(ModPackets.HologramPlacePayload.TYPE, ModPackets.HologramPlacePayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(ModPackets.SetPlacementModePayload.TYPE, ModPackets.SetPlacementModePayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(ModPackets.SetHologramEditModePayload.TYPE, ModPackets.SetHologramEditModePayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(ModPackets.StorageMenuRequestPayload.TYPE, ModPackets.StorageMenuRequestPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(ModPackets.StorageMenuSavePayload.TYPE, ModPackets.StorageMenuSavePayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(ModPackets.StorageMenuClearPayload.TYPE, ModPackets.StorageMenuClearPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(ModPackets.SetStoragePlacementModePayload.TYPE, ModPackets.SetStoragePlacementModePayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(ModPackets.StorageMenuAssignPayload.TYPE, ModPackets.StorageMenuAssignPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(ModPackets.SetNpcPlacementModePayload.TYPE, ModPackets.SetNpcPlacementModePayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(ModPackets.SetNpcEditModePayload.TYPE, ModPackets.SetNpcEditModePayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(ModPackets.NpcPlacePayload.TYPE, ModPackets.NpcPlacePayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(ModPackets.NpcEditPayload.TYPE, ModPackets.NpcEditPayload.CODEC);
			PayloadTypeRegistry.serverboundPlay().register(ModPackets.NpcOpenMenuPayload.TYPE, ModPackets.NpcOpenMenuPayload.CODEC);
			PayloadTypeRegistry.serverboundPlay().register(ModPackets.ItemStylerOpenPayload.TYPE, ModPackets.ItemStylerOpenPayload.CODEC);
			PayloadTypeRegistry.serverboundPlay().register(ModPackets.ItemStylerApplyNamePayload.TYPE, ModPackets.ItemStylerApplyNamePayload.CODEC);
			PayloadTypeRegistry.serverboundPlay().register(ModPackets.AnvilSetLorePayload.TYPE, ModPackets.AnvilSetLorePayload.CODEC);
			PayloadTypeRegistry.clientboundPlay().register(ModPackets.StorageMenuSyncPayload.TYPE, ModPackets.StorageMenuSyncPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(ModPackets.StorageMenuContextPayload.TYPE, ModPackets.StorageMenuContextPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(ModPackets.StorageMenuNavigationStatePayload.TYPE, ModPackets.StorageMenuNavigationStatePayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(ModPackets.ShopStatePayload.TYPE, ModPackets.ShopStatePayload.CODEC);

		StorageMenuNetworkHandler.register();

		ServerPlayNetworking.registerGlobalReceiver(ModPackets.HologramEditPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> handleHologramEdit(player, payload));
		});

		ServerPlayNetworking.registerGlobalReceiver(ModPackets.HologramOpenAtBlockPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> handleHologramOpenAtBlock(player, payload));
		});

		ServerPlayNetworking.registerGlobalReceiver(ModPackets.HologramPlacePayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> handleHologramPlace(player, payload));
		});

		ServerPlayNetworking.registerGlobalReceiver(ModPackets.SetPlacementModePayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> HologramPlacementMode.setActive(player, payload.enabled()));
		});

		ServerPlayNetworking.registerGlobalReceiver(ModPackets.SetHologramEditModePayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> HologramEditMode.setActive(player, payload.enabled()));
		});

		ServerPlayNetworking.registerGlobalReceiver(ModPackets.SetNpcPlacementModePayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> NpcPlacementMode.setActive(player, payload.enabled()));
		});

		ServerPlayNetworking.registerGlobalReceiver(ModPackets.SetNpcEditModePayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> NpcEditMode.setActive(player, payload.enabled()));
		});

		ServerPlayNetworking.registerGlobalReceiver(ModPackets.NpcPlacePayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> handleNpcPlace(player, payload));
		});

		ServerPlayNetworking.registerGlobalReceiver(ModPackets.NpcEditPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> handleNpcEdit(player, payload));
		});

		ServerPlayNetworking.registerGlobalReceiver(ModPackets.NpcOpenMenuPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> handleNpcOpenMenu(player, payload.entityId()));
		});

		ServerPlayNetworking.registerGlobalReceiver(ModPackets.ItemStylerOpenPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> ItemStylerSessions.open(player));
		});

		ServerPlayNetworking.registerGlobalReceiver(ModPackets.ItemStylerApplyNamePayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> handleItemStylerApplyName(player, payload));
		});

				ServerPlayNetworking.registerGlobalReceiver(ModPackets.AnvilSetLorePayload.TYPE, (payload, context) -> {
				ServerPlayer player = context.player();
				context.server().execute(() -> handleAnvilSetLore(player, payload));
			});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			HologramPlacementMode.clear(handler.getPlayer());
			HologramEditMode.clear(handler.getPlayer());
			NpcPlacementMode.clear(handler.getPlayer());
			NpcEditMode.clear(handler.getPlayer());
			com.hologrammenu.storage.StorageMenuPlacementMode.clear(handler.getPlayer());
			com.hologrammenu.storage.StorageMenuNavigation.clear(handler.getPlayer());
			AnvilLoreSession.clear(handler.getPlayer());
			ItemStylerSessions.clear(handler.getPlayer());
		});
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.getPlayer();
			server.execute(() -> {
				HologramSync.syncPlayer(player);
				NpcSync.syncPlayer(player);
			});
		});
	}

	private static void handleHologramPlace(ServerPlayer player, ModPackets.HologramPlacePayload payload) {
		if (!HologramPlacementMode.isActive(player)) {
			return;
		}

		String text = payload.text();
		if (text == null || text.isBlank()) {
			text = "Hologram";
		}
		if (text.length() > MAX_PLACEMENT_TEXT_LENGTH) {
			text = text.substring(0, MAX_PLACEMENT_TEXT_LENGTH);
		}

		var target = HologramHelper.pickPlacementTarget(player, HologramHelper.WAND_MAX_DISTANCE);
		List<Display.TextDisplay> displays = HologramHelper.create((ServerLevel) player.level(), target.position(), HologramLineStack.defaults(text));
		target.blockPos().ifPresent(pos -> displays.forEach(display -> HologramHelper.tagAssociatedBlock(display, pos)));
		player.sendSystemMessage(Component.translatable("hud.hologrammenu.placement.placed", text));
	}

	private static void handleHologramOpenAtBlock(ServerPlayer player, ModPackets.HologramOpenAtBlockPayload payload) {
		if (!HologramEditMode.isActive(player)) {
			return;
		}
		HologramHelper.findEditableByAssociatedBlock((ServerLevel) player.level(), payload.pos(), player)
			.ifPresent(display -> ServerPlayNetworking.send(player, new ModPackets.HologramOpenScreenPayload(
				display.getId(),
				HologramLineStack.serialize(readEditableHologramLines((ServerLevel) player.level(), display))
			)));
	}

	private static void handleHologramEdit(ServerPlayer player, ModPackets.HologramEditPayload payload) {
		if (!HologramEditMode.isActive(player)) {
			return;
		}

		Entity entity = player.level().getEntity(payload.entityId());
		if (!(entity instanceof Display.TextDisplay display) || !HologramHelper.isEditableHologram(display)) {
			player.sendSystemMessage(Component.translatable("screen.hologrammenu.hologram_options.not_found"));
			return;
		}

		if (!HologramHelper.canEdit(player, display)) {
			player.sendSystemMessage(Component.translatable("screen.hologrammenu.hologram_options.too_far"));
			return;
		}

		switch (payload.action()) {
			case "update" -> {
				HologramHelper.replaceGroup((ServerLevel) player.level(), display, HologramLineStack.deserialize(payload.lines()));
				player.sendSystemMessage(Component.translatable("screen.hologrammenu.hologram_options.updated"));
			}
			case "delete" -> {
				HologramHelper.removeGroup((ServerLevel) player.level(), display);
				player.sendSystemMessage(Component.translatable("screen.hologrammenu.hologram_options.deleted"));
			}
			default -> player.sendSystemMessage(Component.translatable("screen.hologrammenu.hologram_options.invalid_action"));
		}
	}

	private static List<HologramLineStack.Line> readEditableHologramLines(ServerLevel level, Display.TextDisplay display) {
		java.util.UUID groupId = HologramLineStack.groupId(display);
		List<Display.TextDisplay> group = groupId == null ? List.of(display) : HologramHelper.findGroup(level, groupId);
		return group.isEmpty() ? List.of(new HologramLineStack.Line("", com.hologrammenu.hologram.HologramScale.DEFAULT)) : HologramLineStack.readGroup(group);
	}

	private static void handleNpcPlace(ServerPlayer player, ModPackets.NpcPlacePayload payload) {
		if (!NpcPlacementMode.isActive(player) || !StorageMenuPermissions.canEdit(player)) {
			return;
		}

		var position = new net.minecraft.world.phys.Vec3(payload.x(), payload.y(), payload.z());
		double maxDistance = NpcHelper.PLACE_MAX_DISTANCE;
		if (player.position().distanceToSqr(position) > maxDistance * maxDistance) {
			return;
		}

		String npcType = payload.npcType() == null || payload.npcType().isBlank() ? "villager" : payload.npcType().trim();
		if ("player".equalsIgnoreCase(npcType) && (payload.skinName() == null || payload.skinName().isBlank())) {
			player.sendSystemMessage(Component.translatable("hud.hologrammenu.npc.missing_skin"));
			return;
		}

		LivingEntity placed = NpcHelper.place(
			player,
			(ServerLevel) player.level(),
			position,
			npcType,
			clamp(payload.skinName(), MAX_PROFILE_NAME_LENGTH),
			clamp(payload.professionId(), MAX_NPC_NAME_LENGTH),
			clamp(payload.displayName(), MAX_NPC_NAME_LENGTH)
		);
		if (placed != null) {
			String displayName = clamp(payload.displayName(), MAX_NPC_NAME_LENGTH);
			NpcHologramStack.writeStyledName(placed, displayName);
			NpcHologramStack.write(placed, NpcHologramStack.defaults(displayName));
			NpcHologramLabels.sync((ServerLevel) player.level(), placed, NpcHologramStack.read(placed));
			NpcSync.track((ServerLevel) player.level(), placed);
		}
		player.sendSystemMessage(Component.translatable("hud.hologrammenu.npc.placed"));
	}

	private static void handleAnvilSetLore(ServerPlayer player, ModPackets.AnvilSetLorePayload payload) {
		java.util.List<String> lines = new java.util.ArrayList<>();
		for (String line : payload.lines()) {
			String normalized = TextFormats.normalize(line);
			if (normalized.isBlank()) {
				continue;
			}
			lines.add(normalized);
			if (lines.size() >= StorageMenuItemLore.MAX_LINES) {
				break;
			}
		}
		if (!(player.containerMenu instanceof AnvilMenu menu)) {
			if (ItemStylerSessions.update(player, stack -> StorageMenuItemLore.withLore(stack, lines))) {
				player.sendSystemMessage(Component.translatable(lines.isEmpty()
					? "screen.hologrammenu.anvil.lore_cleared"
					: "screen.hologrammenu.anvil.lore_applied"));
			}
			return;
		}
		if (menu.getSlot(0).getItem().isEmpty()) {
			AnvilLoreSession.clear(player);
			return;
		}

		AnvilLoreSession.set(player, lines);
		menu.createResult();
		if (lines.isEmpty()) {
			player.sendSystemMessage(Component.translatable("screen.hologrammenu.anvil.lore_cleared"));
		} else if (menu.getSlot(2).getItem().isEmpty()) {
			player.sendSystemMessage(Component.translatable("screen.hologrammenu.anvil.lore_no_output"));
		} else {
			player.sendSystemMessage(Component.translatable("screen.hologrammenu.anvil.lore_applied"));
		}
	}

	private static void handleItemStylerApplyName(ServerPlayer player, ModPackets.ItemStylerApplyNamePayload payload) {
		ItemStylerSessions.update(player, stack -> {
			ItemStack updated = stack.copy();
			String normalized = TextFormats.normalize(clamp(payload.styledName(), MAX_PLACEMENT_TEXT_LENGTH));
			if (normalized.isBlank()) {
				updated.remove(DataComponents.CUSTOM_NAME);
			} else {
				updated.set(DataComponents.CUSTOM_NAME, TextFormats.toComponent(normalized));
			}
			return updated;
		});
	}

	private static void replaceAnvilInput(AnvilMenu menu, ItemStack stack) {
		menu.getSlot(0).set(stack);
		menu.getSlot(0).setChanged();
		menu.createResult();
		menu.broadcastChanges();
	}

	private static void handleNpcEdit(ServerPlayer player, ModPackets.NpcEditPayload payload) {
		if (!StorageMenuPermissions.canEdit(player)) {
			player.sendSystemMessage(Component.translatable("screen.hologrammenu.npc_options.no_permission"));
			return;
		}
		if (!NpcEditMode.isActive(player)) {
			player.sendSystemMessage(Component.translatable("screen.hologrammenu.npc_options.edit_mode_off"));
			return;
		}

		net.minecraft.world.entity.Entity entity = player.level().getEntity(payload.entityId());
		if (!(entity instanceof LivingEntity living) || !NpcHelper.isNpc(living)) {
			player.sendSystemMessage(Component.translatable("screen.hologrammenu.npc_options.not_found"));
			return;
		}

		if (!NpcHelper.canEdit(player, living)) {
			player.sendSystemMessage(Component.translatable("screen.hologrammenu.npc_options.too_far"));
			return;
		}

		switch (payload.action()) {
			case "update" -> {
				if (living instanceof Mannequin && (payload.skinName() == null || payload.skinName().isBlank())) {
					player.sendSystemMessage(Component.translatable("hud.hologrammenu.npc.missing_skin"));
					return;
					}
					NpcConfig config = new NpcConfig(
						payload.headFollowEnabled(),
						NpcConfig.clampRadius(payload.headFollowRadius()),
						clamp(payload.dialogue(), MAX_NPC_DIALOGUE_LENGTH),
						payload.containerEnabled(),
					NpcConfig.normalizeContainerSize(payload.containerSize()),
					payload.particleEffectEnabled(),
					NpcParticleEffects.sanitizeParticleId(clamp(payload.particleEffectId(), MAX_NPC_NAME_LENGTH))
				);
				NpcHelper.update(
					living,
					clamp(payload.displayName(), MAX_PLACEMENT_TEXT_LENGTH),
					clamp(payload.skinName(), MAX_PROFILE_NAME_LENGTH),
					clamp(payload.professionId(), MAX_NPC_NAME_LENGTH),
					config
				);
				String styledName = clamp(payload.displayName(), MAX_PLACEMENT_TEXT_LENGTH);
				java.util.List<NpcHologramStack.Entry> stack = NpcHologramStack.withNameText(
					NpcHologramStack.deserialize(payload.hologramStack()),
					styledName
				);
				NpcHologramStack.writeStyledName(living, styledName);
				NpcHologramStack.write(living, stack);
				NpcHologramLabels.sync((ServerLevel) player.level(), living, stack);
				if (config.containerEnabled()) {
					NpcMenuStore.ensureEnabled((ServerLevel) player.level(), living.getUUID(), config.containerSize());
				} else {
					NpcMenuStore.clear((ServerLevel) player.level(), living.getUUID());
				}
				NpcSync.syncConfig((ServerLevel) player.level(), living);
				player.sendSystemMessage(Component.translatable("screen.hologrammenu.npc_options.updated"));
			}
			case "delete" -> {
				NpcMenuStore.clear((ServerLevel) player.level(), living.getUUID());
				NpcHologramLabels.remove((ServerLevel) player.level(), living);
				NpcSync.untrack((ServerLevel) player.level(), living);
				living.discard();
				player.sendSystemMessage(Component.translatable("screen.hologrammenu.npc_options.deleted"));
			}
			default -> player.sendSystemMessage(Component.translatable("screen.hologrammenu.npc_options.invalid_action"));
		}
	}

	private static void handleNpcOpenMenu(ServerPlayer player, int entityId) {
		if (!StorageMenuPermissions.canEdit(player)) {
			player.sendSystemMessage(Component.translatable("screen.hologrammenu.npc_options.no_permission"));
			return;
		}
		if (!NpcEditMode.isActive(player)) {
			player.sendSystemMessage(Component.translatable("screen.hologrammenu.npc_options.edit_mode_off"));
			return;
		}
		var entity = player.level().getEntity(entityId);
		if (!(entity instanceof LivingEntity living) || !NpcHelper.isNpc(living)) {
			player.sendSystemMessage(Component.translatable("screen.hologrammenu.npc_options.not_found"));
			return;
		}
		if (!NpcHelper.canEdit(player, living)) {
			player.sendSystemMessage(Component.translatable("screen.hologrammenu.npc_options.too_far"));
			return;
		}
		NpcConfig config = NpcConfig.read(living);
		if (!config.containerEnabled()) {
			player.sendSystemMessage(Component.translatable("screen.hologrammenu.npc_options.container_disabled"));
			return;
		}
		NpcMenuOpener.openNpc(player, living);
	}

	private static String clamp(String value, int maxLength) {
		if (value == null) {
			return "";
		}
		return value.length() > maxLength ? value.substring(0, maxLength) : value;
	}
}
