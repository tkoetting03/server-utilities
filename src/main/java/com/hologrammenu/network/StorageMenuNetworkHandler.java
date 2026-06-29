package com.hologrammenu.network;

import com.hologrammenu.npc.NpcConfig;
import com.hologrammenu.npc.NpcHelper;
import com.hologrammenu.npc.NpcMenuManager;
import com.hologrammenu.npc.NpcMenuStore;
import com.hologrammenu.storage.ShopDefinition;
import com.hologrammenu.storage.StorageMenuBlockData;
import com.hologrammenu.storage.StorageMenuBlockStore;
import com.hologrammenu.storage.StorageMenuChrome;
import com.hologrammenu.storage.StorageMenuDefinition;
import com.hologrammenu.storage.StorageMenuInteractions;
import com.hologrammenu.storage.StorageMenuManager;
import com.hologrammenu.storage.StorageMenuNetwork;
import com.hologrammenu.storage.StorageMenuNavigation;
import com.hologrammenu.storage.StorageMenuOpener;
import com.hologrammenu.storage.StorageMenuPermissions;
import com.hologrammenu.storage.StorageMenuSizes;
import com.hologrammenu.storage.StorageMenuSlotConfig;
import com.hologrammenu.storage.StorageMenuSlotType;
import com.hologrammenu.storage.StorageMenuViewContext;
import com.hologrammenu.storage.StorageSubMenuManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;
import java.util.UUID;

public final class StorageMenuNetworkHandler {
	private static final double MAX_EDIT_DISTANCE = 12.0D;
	private static final double MAX_NPC_DISTANCE = 6.0D;

	private StorageMenuNetworkHandler() {
	}

	public static void register() {
		ServerPlayNetworking.registerGlobalReceiver(ModPackets.StorageMenuRequestPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> handleRequest(player, payload.pos(), payload.subMenuId(), payload.npcEntityId()));
		});

		ServerPlayNetworking.registerGlobalReceiver(ModPackets.StorageMenuSavePayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> handleApply(player, payload.menu()));
		});

		ServerPlayNetworking.registerGlobalReceiver(ModPackets.StorageMenuClearPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> handleClear(player, payload.pos(), payload.subMenuId(), payload.npcEntityId()));
		});

		ServerPlayNetworking.registerGlobalReceiver(ModPackets.StorageMenuAssignPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> com.hologrammenu.storage.StorageMenuPlacementHandler.assign(player, payload.pos()));
		});

		ServerPlayNetworking.registerGlobalReceiver(ModPackets.SetStoragePlacementModePayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> com.hologrammenu.storage.StorageMenuPlacementMode.setActive(player, payload.enabled()));
		});
	}

	public static void sendContext(ServerPlayer player, StorageMenuViewContext viewContext) {
		ServerPlayNetworking.send(player, new ModPackets.StorageMenuContextPayload(
			viewContext.anchorPos().immutable(),
			viewContext.subMenuId() == null ? StorageMenuViewContext.NO_SUB_MENU : viewContext.subMenuId(),
			viewContext.npcEntityId()
		));
	}

	public static void sendNavigationState(ServerPlayer player) {
		ServerPlayNetworking.send(player, new ModPackets.StorageMenuNavigationStatePayload(StorageMenuNavigation.hasParent(player)));
	}

	public static void sendShopState(ServerPlayer player, BlockPos pos, ShopDefinition shop) {
		if (shop == null) {
			return;
		}
		ServerPlayNetworking.send(player, new ModPackets.ShopStatePayload(
			pos.immutable(),
			shop.enabled(),
			shop.asList().stream().map(StorageMenuNetwork.ShopListingData::fromListing).toList()
		));
	}

	public static void sendNpcShopState(ServerPlayer player, int npcEntityId, ShopDefinition shop) {
		if (shop == null) {
			return;
		}
		ServerPlayNetworking.send(player, new ModPackets.ShopStatePayload(
			BlockPos.ZERO,
			shop.enabled(),
			shop.asList().stream().map(StorageMenuNetwork.ShopListingData::fromListing).toList()
		));
	}

	private static void handleRequest(ServerPlayer player, BlockPos pos, String subMenuId, int npcEntityId) {
		StorageMenuViewContext viewContext = viewContextFor(pos, subMenuId, npcEntityId);
		if (viewContext.isNpcAnchored()) {
			if (!canAccessNpc(player, npcEntityId)) {
				return;
			}
			LivingEntity npc = getNpc(player, npcEntityId);
			if (npc == null) {
				return;
			}
			ServerLevel level = (ServerLevel) player.level();
			int containerSize = NpcMenuManager.resolveContainerSize(level, npc);
			StorageMenuDefinition definition;
			boolean invulnerable = false;
			boolean hologramLabel = false;
			ShopDefinition shop = ShopDefinition.EMPTY;
			if (viewContext.isRoot()) {
				Optional<StorageMenuBlockData> npcData = NpcMenuStore.get(level, npc.getUUID());
				definition = npcData.map(StorageMenuBlockData::definition).orElse(StorageMenuDefinition.empty(containerSize));
				invulnerable = npcData.map(StorageMenuBlockData::invulnerable).orElse(false);
				hologramLabel = npcData.map(StorageMenuBlockData::hologramLabel).orElse(false);
				shop = npcData.map(StorageMenuBlockData::shop).orElse(ShopDefinition.EMPTY);
			} else {
				definition = StorageSubMenuManager.get(level, viewContext.subMenuId())
					.orElse(StorageMenuDefinition.empty(containerSize));
			}
			StorageMenuNetwork.MenuData data = StorageMenuNetwork.MenuData.fromDefinition(viewContext, definition, invulnerable, hologramLabel, shop);
			ServerPlayNetworking.send(player, new ModPackets.StorageMenuSyncPayload(data));
			return;
		}

		if (!canAccessBlock(player, pos)) {
			return;
		}

		ServerLevel level = (ServerLevel) player.level();
		int containerSize = StorageMenuOpener.resolveContainerSize(level, pos);
		if (containerSize <= 0) {
			containerSize = StorageMenuSizes.SINGLE_CHEST;
		}

		StorageMenuDefinition definition;
		boolean invulnerable = false;
		boolean hologramLabel = false;
		com.hologrammenu.storage.StorageMenuHologramSettings hologramSettings = com.hologrammenu.storage.StorageMenuHologramSettings.DEFAULT;
		ShopDefinition shop = ShopDefinition.EMPTY;
		if (viewContext.isRoot()) {
			Optional<StorageMenuBlockData> blockData = StorageMenuBlockStore.get(level, pos);
			definition = blockData.map(StorageMenuBlockData::definition).orElse(StorageMenuDefinition.empty(containerSize));
			invulnerable = blockData.map(StorageMenuBlockData::invulnerable).orElse(false);
			hologramLabel = blockData.map(StorageMenuBlockData::hologramLabel).orElse(false);
			hologramSettings = blockData.map(StorageMenuBlockData::hologramSettings).orElse(com.hologrammenu.storage.StorageMenuHologramSettings.DEFAULT);
			shop = blockData.map(StorageMenuBlockData::shop).orElse(ShopDefinition.EMPTY);
		} else {
			definition = StorageSubMenuManager.get(level, viewContext.subMenuId())
				.orElse(StorageMenuDefinition.empty(containerSize));
		}

		StorageMenuNetwork.MenuData data = StorageMenuNetwork.MenuData.fromDefinition(viewContext, definition, invulnerable, hologramLabel, hologramSettings, shop);
		ServerPlayNetworking.send(player, new ModPackets.StorageMenuSyncPayload(data));
	}

	private static void handleApply(ServerPlayer player, StorageMenuNetwork.MenuData menuData) {
		StorageMenuViewContext viewContext = menuData.viewContext();
		if (viewContext.isNpcAnchored()) {
			handleNpcApply(player, menuData, viewContext);
			return;
		}

		BlockPos pos = viewContext.anchorPos();
		if (!canAccessBlock(player, pos)) {
			return;
		}

		if (!isSupportedContainerSize(menuData.containerSize())) {
			StorageMenuInteractions.sendFeedback(player, "screen.hologrammenu.storage_menu.invalid_size");
			return;
		}

		ServerLevel level = (ServerLevel) player.level();
		if (level.getBlockState(pos).isAir()) {
			return;
		}

		StorageMenuDefinition definition = StorageMenuChrome.stripRuntimeChrome(menuData.toDefinition()).withEnabled(true);
		if (viewContext.isRoot()) {
			definition = ensureLinkSubMenus(level, definition, menuData.containerSize());
			StorageMenuManager.save(level, pos, definition, menuData.invulnerable(), menuData.hologramLabel(), menuData.hologramSettings(), menuData.toShopDefinition());
			com.hologrammenu.storage.StorageMenuHologramLabels.sync(level, pos, definition.title(), menuData.hologramLabel(), menuData.hologramSettings());
			com.hologrammenu.storage.StorageMenuLiveRefresh.refreshViewers(level, pos, definition);
		} else {
			definition = ensureLinkSubMenus(level, definition, menuData.containerSize());
			StorageSubMenuManager.save(level, viewContext.subMenuId(), definition);
		}

		ShopDefinition savedShop = viewContext.isRoot()
			? StorageMenuManager.getShop(level, pos).orElse(ShopDefinition.EMPTY)
			: ShopDefinition.EMPTY;
		ServerPlayNetworking.send(player, new ModPackets.StorageMenuSyncPayload(
			StorageMenuNetwork.MenuData.fromDefinition(viewContext, definition, menuData.invulnerable(), menuData.hologramLabel(), menuData.hologramSettings(), savedShop)
		));
		sendShopState(player, pos, savedShop);
	}

	private static void handleNpcApply(ServerPlayer player, StorageMenuNetwork.MenuData menuData, StorageMenuViewContext viewContext) {
		if (!canAccessNpc(player, viewContext.npcEntityId())) {
			return;
		}
		LivingEntity npc = getNpc(player, viewContext.npcEntityId());
		if (npc == null) {
			return;
		}
		if (!NpcMenuManager.isSupportedContainerSize(menuData.containerSize())) {
			StorageMenuInteractions.sendFeedback(player, "screen.hologrammenu.storage_menu.invalid_size");
			return;
		}

		ServerLevel level = (ServerLevel) player.level();
		UUID entityId = npc.getUUID();
		StorageMenuDefinition definition = StorageMenuChrome.stripRuntimeChrome(menuData.toDefinition()).withEnabled(true);
		if (viewContext.isRoot()) {
			definition = ensureLinkSubMenus(level, definition, menuData.containerSize());
			NpcMenuManager.save(level, entityId, definition, menuData.invulnerable(), menuData.hologramLabel(), menuData.toShopDefinition());
		} else {
			definition = ensureLinkSubMenus(level, definition, menuData.containerSize());
			StorageSubMenuManager.save(level, viewContext.subMenuId(), definition);
		}

		ShopDefinition savedShop = viewContext.isRoot()
			? NpcMenuManager.getShop(level, entityId).orElse(ShopDefinition.EMPTY)
			: ShopDefinition.EMPTY;
		ServerPlayNetworking.send(player, new ModPackets.StorageMenuSyncPayload(
			StorageMenuNetwork.MenuData.fromDefinition(viewContext, definition, menuData.invulnerable(), menuData.hologramLabel(), savedShop)
		));
		sendNpcShopState(player, viewContext.npcEntityId(), savedShop);
	}

	private static void handleClear(ServerPlayer player, BlockPos pos, String subMenuId, int npcEntityId) {
		StorageMenuViewContext viewContext = viewContextFor(pos, subMenuId, npcEntityId);
		if (viewContext.isNpcAnchored()) {
			if (!canAccessNpc(player, npcEntityId)) {
				return;
			}
			LivingEntity npc = getNpc(player, npcEntityId);
			if (npc == null) {
				return;
			}
			ServerLevel level = (ServerLevel) player.level();
			int containerSize = NpcMenuManager.resolveContainerSize(level, npc);
			if (viewContext.isRoot()) {
				NpcMenuManager.clear(level, npc.getUUID());
			} else {
				StorageSubMenuManager.clear(level, viewContext.subMenuId());
			}
			StorageMenuInteractions.sendFeedback(player, "screen.hologrammenu.storage_menu.cleared");
			var empty = StorageMenuDefinition.empty(Math.max(containerSize, StorageMenuSizes.SINGLE_CHEST));
			ServerPlayNetworking.send(player, new ModPackets.StorageMenuSyncPayload(
				StorageMenuNetwork.MenuData.fromDefinition(viewContext, empty, false, false, ShopDefinition.EMPTY)
			));
			return;
		}

		if (!canAccessBlock(player, pos)) {
			return;
		}

		ServerLevel level = (ServerLevel) player.level();
		if (viewContext.isRoot()) {
			StorageMenuManager.clear(level, pos);
			com.hologrammenu.storage.StorageMenuLiveRefresh.refreshViewers(level, pos, StorageMenuDefinition.empty(Math.max(StorageMenuOpener.resolveContainerSize(level, pos), StorageMenuSizes.SINGLE_CHEST)));
		} else {
			StorageSubMenuManager.clear(level, viewContext.subMenuId());
		}
		StorageMenuInteractions.sendFeedback(player, "screen.hologrammenu.storage_menu.cleared");

		int containerSize = StorageMenuOpener.resolveContainerSize(level, pos);
		if (containerSize <= 0) {
			containerSize = StorageMenuSizes.SINGLE_CHEST;
		}
		var empty = StorageMenuDefinition.empty(containerSize);
		ServerPlayNetworking.send(player, new ModPackets.StorageMenuSyncPayload(
			StorageMenuNetwork.MenuData.fromDefinition(viewContext, empty, false, false, ShopDefinition.EMPTY)
		));
	}

	private static boolean isSupportedContainerSize(int size) {
		return size == 5 || size == 9 || StorageMenuSizes.isChestSize(size) || size == 27;
	}

	private static StorageMenuDefinition ensureLinkSubMenus(ServerLevel level, StorageMenuDefinition definition, int containerSize) {
		StorageMenuDefinition updated = definition;
		for (StorageMenuSlotConfig config : definition.asList()) {
			if (config.type() != StorageMenuSlotType.LINK) {
				continue;
			}
			String subMenuId = config.subMenuId();
			if (subMenuId == null || subMenuId.isBlank()) {
				subMenuId = StorageSubMenuManager.createId();
				updated = updated.withSlot(config.withSubMenuId(subMenuId));
			}
			StorageSubMenuManager.ensureExists(level, subMenuId, containerSize);
		}
		return updated;
	}

	private static StorageMenuViewContext viewContextFor(BlockPos pos, String subMenuId, int npcEntityId) {
		StorageMenuViewContext viewContext = npcEntityId >= 0
			? StorageMenuViewContext.forNpc(npcEntityId)
			: StorageMenuViewContext.root(pos);
		if (subMenuId != null && !subMenuId.isBlank()) {
			viewContext = viewContext.withSubMenu(subMenuId);
		}
		return viewContext;
	}

	private static LivingEntity getNpc(ServerPlayer player, int npcEntityId) {
		var entity = player.level().getEntity(npcEntityId);
		if (entity instanceof LivingEntity living && NpcHelper.isNpc(living)) {
			return living;
		}
		return null;
	}

	private static boolean canAccessBlock(ServerPlayer player, BlockPos pos) {
		if (!StorageMenuPermissions.canEdit(player)) {
			return false;
		}
		return player.blockPosition().distSqr(pos) <= MAX_EDIT_DISTANCE * MAX_EDIT_DISTANCE;
	}

	private static boolean canAccessNpc(ServerPlayer player, int npcEntityId) {
		if (!StorageMenuPermissions.canEdit(player)) {
			return false;
		}
		LivingEntity npc = getNpc(player, npcEntityId);
		return npc != null && player.distanceToSqr(npc) <= MAX_NPC_DISTANCE * MAX_NPC_DISTANCE;
	}
}
