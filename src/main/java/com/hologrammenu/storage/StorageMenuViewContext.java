package com.hologrammenu.storage;

import net.minecraft.core.BlockPos;

public record StorageMenuViewContext(BlockPos anchorPos, String subMenuId, int npcEntityId) {
	public static final String NO_SUB_MENU = "";
	public static final int NO_NPC_ENTITY = -1;

	public static StorageMenuViewContext root(BlockPos anchorPos) {
		return new StorageMenuViewContext(anchorPos.immutable(), NO_SUB_MENU, NO_NPC_ENTITY);
	}

	public static StorageMenuViewContext forNpc(int entityId) {
		return forNpc(entityId, NO_SUB_MENU);
	}

	public static StorageMenuViewContext forNpc(int entityId, String subMenuId) {
		return new StorageMenuViewContext(BlockPos.ZERO, subMenuId == null ? NO_SUB_MENU : subMenuId, entityId);
	}

	public boolean isRoot() {
		return subMenuId == null || subMenuId.isBlank();
	}

	public boolean isNpcAnchored() {
		return npcEntityId >= 0;
	}

	public StorageMenuViewContext withSubMenu(String id) {
		return new StorageMenuViewContext(anchorPos, id == null ? NO_SUB_MENU : id, npcEntityId);
	}

	public StorageMenuViewContext immutable() {
		return new StorageMenuViewContext(anchorPos.immutable(), subMenuId == null ? NO_SUB_MENU : subMenuId, npcEntityId);
	}
}
