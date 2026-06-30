package com.serverutilities.storage;

import com.serverutilities.npc.NpcMenuOpener;
import com.serverutilities.network.StorageMenuNetworkHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class StorageMenuNavigation {
	private static final Map<UUID, Deque<StorageMenuViewContext>> STACKS = new HashMap<>();
	private static final Map<UUID, Boolean> PROGRAMMATIC_OPEN = new HashMap<>();
	private static final Map<UUID, StorageMenuViewContext> CURRENT_VIEW = new HashMap<>();

	private StorageMenuNavigation() {
	}

	public static void clear(ServerPlayer player) {
		STACKS.remove(player.getUUID());
		PROGRAMMATIC_OPEN.remove(player.getUUID());
		CURRENT_VIEW.remove(player.getUUID());
	}

	public static StorageMenuViewContext currentView(ServerPlayer player) {
		return CURRENT_VIEW.getOrDefault(player.getUUID(), StorageMenuViewContext.root(BlockPos.ZERO));
	}

	public static void setCurrentView(ServerPlayer player, StorageMenuViewContext context) {
		CURRENT_VIEW.put(player.getUUID(), context.immutable());
	}

	public static boolean hasParent(ServerPlayer player) {
		Deque<StorageMenuViewContext> stack = STACKS.get(player.getUUID());
		return stack != null && !stack.isEmpty();
	}

	public static boolean hasParent(Player player) {
		return player instanceof ServerPlayer serverPlayer && hasParent(serverPlayer);
	}

	public static void onDirectOpen(ServerPlayer player) {
		if (Boolean.TRUE.equals(PROGRAMMATIC_OPEN.remove(player.getUUID()))) {
			return;
		}
		clear(player);
		StorageMenuNetworkHandler.sendNavigationState(player);
	}

	public static void openSubMenu(ServerPlayer player, StorageMenuViewContext currentView, String subMenuId) {
		if (subMenuId == null || subMenuId.isBlank()) {
			return;
		}

		ServerLevel level = (ServerLevel) player.level();
		int containerSize = resolveContainerSize(level, currentView);
		if (containerSize <= 0) {
			return;
		}

		StorageSubMenuManager.ensureExists(level, subMenuId, containerSize);
		push(player, currentView);
		player.closeContainer();
		markProgrammatic(player);
		StorageMenuViewContext nextView = currentView.withSubMenu(subMenuId);
		openView(player, nextView, containerSize);
	}

	public static void openBack(ServerPlayer player) {
		Deque<StorageMenuViewContext> stack = STACKS.get(player.getUUID());
		if (stack == null || stack.isEmpty()) {
			return;
		}

		StorageMenuViewContext parentView = stack.pop();
		if (stack.isEmpty()) {
			STACKS.remove(player.getUUID());
		}

		ServerLevel level = (ServerLevel) player.level();
		int containerSize = resolveContainerSize(level, parentView);
		if (containerSize <= 0) {
			return;
		}

		player.closeContainer();
		markProgrammatic(player);
		openView(player, parentView, containerSize);
	}

	private static void openView(ServerPlayer player, StorageMenuViewContext viewContext, int containerSize) {
		if (viewContext.isNpcAnchored()) {
			var entity = player.level().getEntity(viewContext.npcEntityId());
			if (entity instanceof LivingEntity living) {
				NpcMenuOpener.openView(player, living, viewContext, containerSize);
			}
			return;
		}
		StorageMenuOpener.openView(player, viewContext, containerSize);
	}

	private static int resolveContainerSize(ServerLevel level, StorageMenuViewContext viewContext) {
		if (viewContext.isNpcAnchored()) {
			var entity = level.getEntity(viewContext.npcEntityId());
			if (entity instanceof LivingEntity living) {
				return NpcMenuOpener.resolveContainerSize(level, living);
			}
			return 0;
		}
		return StorageMenuOpener.resolveContainerSize(level, viewContext.anchorPos());
	}

	private static void push(ServerPlayer player, StorageMenuViewContext context) {
		STACKS.computeIfAbsent(player.getUUID(), ignored -> new ArrayDeque<>()).push(context.immutable());
	}

	private static void markProgrammatic(ServerPlayer player) {
		PROGRAMMATIC_OPEN.put(player.getUUID(), true);
	}
}
