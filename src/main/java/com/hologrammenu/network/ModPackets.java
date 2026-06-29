package com.hologrammenu.network;

import com.hologrammenu.HologramMenuMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public final class ModPackets {
	public record HologramEditPayload(int entityId, String action, String lines) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<HologramEditPayload> TYPE =
			new CustomPacketPayload.Type<>(HologramMenuMod.id("hologram_edit"));
		public static final StreamCodec<RegistryFriendlyByteBuf, HologramEditPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.INT,
			HologramEditPayload::entityId,
			ByteBufCodecs.STRING_UTF8,
			HologramEditPayload::action,
			ByteBufCodecs.STRING_UTF8,
			HologramEditPayload::lines,
			HologramEditPayload::new
		);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record HologramPlacePayload(String text) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<HologramPlacePayload> TYPE =
			new CustomPacketPayload.Type<>(HologramMenuMod.id("hologram_place"));
		public static final StreamCodec<RegistryFriendlyByteBuf, HologramPlacePayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8,
			HologramPlacePayload::text,
			HologramPlacePayload::new
		);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record SetPlacementModePayload(boolean enabled) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<SetPlacementModePayload> TYPE =
			new CustomPacketPayload.Type<>(HologramMenuMod.id("set_placement_mode"));
		public static final StreamCodec<RegistryFriendlyByteBuf, SetPlacementModePayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL,
			SetPlacementModePayload::enabled,
			SetPlacementModePayload::new
		);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record SetHologramEditModePayload(boolean enabled) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<SetHologramEditModePayload> TYPE =
			new CustomPacketPayload.Type<>(HologramMenuMod.id("set_hologram_edit_mode"));
		public static final StreamCodec<RegistryFriendlyByteBuf, SetHologramEditModePayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL,
			SetHologramEditModePayload::enabled,
			SetHologramEditModePayload::new
		);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record HologramTrackPayload(int entityId) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<HologramTrackPayload> TYPE =
			new CustomPacketPayload.Type<>(HologramMenuMod.id("hologram_track"));
		public static final StreamCodec<RegistryFriendlyByteBuf, HologramTrackPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT,
			HologramTrackPayload::entityId,
			HologramTrackPayload::new
		);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record HologramUntrackPayload(int entityId) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<HologramUntrackPayload> TYPE =
			new CustomPacketPayload.Type<>(HologramMenuMod.id("hologram_untrack"));
		public static final StreamCodec<RegistryFriendlyByteBuf, HologramUntrackPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT,
			HologramUntrackPayload::entityId,
			HologramUntrackPayload::new
		);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record HologramSyncPayload(java.util.List<Integer> entityIds) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<HologramSyncPayload> TYPE =
			new CustomPacketPayload.Type<>(HologramMenuMod.id("hologram_sync"));
		public static final StreamCodec<RegistryFriendlyByteBuf, HologramSyncPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.collection(java.util.ArrayList::new, ByteBufCodecs.VAR_INT),
			HologramSyncPayload::entityIds,
			HologramSyncPayload::new
		);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record NpcTrackPayload(int entityId) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<NpcTrackPayload> TYPE =
			new CustomPacketPayload.Type<>(HologramMenuMod.id("npc_track"));
		public static final StreamCodec<RegistryFriendlyByteBuf, NpcTrackPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT,
			NpcTrackPayload::entityId,
			NpcTrackPayload::new
		);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record NpcUntrackPayload(int entityId) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<NpcUntrackPayload> TYPE =
			new CustomPacketPayload.Type<>(HologramMenuMod.id("npc_untrack"));
		public static final StreamCodec<RegistryFriendlyByteBuf, NpcUntrackPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT,
			NpcUntrackPayload::entityId,
			NpcUntrackPayload::new
		);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record NpcSyncPayload(java.util.List<Integer> entityIds) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<NpcSyncPayload> TYPE =
			new CustomPacketPayload.Type<>(HologramMenuMod.id("npc_sync"));
		public static final StreamCodec<RegistryFriendlyByteBuf, NpcSyncPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.collection(java.util.ArrayList::new, ByteBufCodecs.VAR_INT),
			NpcSyncPayload::entityIds,
			NpcSyncPayload::new
		);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record NpcConfigPayload(
		int entityId,
		boolean headFollowEnabled,
		float headFollowRadius,
		String dialogue,
		boolean containerEnabled,
		int containerSize,
		boolean particleEffectEnabled,
		String particleEffectId
	) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<NpcConfigPayload> TYPE =
			new CustomPacketPayload.Type<>(HologramMenuMod.id("npc_config"));
		public static final StreamCodec<RegistryFriendlyByteBuf, NpcConfigPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT,
			NpcConfigPayload::entityId,
			ByteBufCodecs.BOOL,
			NpcConfigPayload::headFollowEnabled,
			ByteBufCodecs.FLOAT,
			NpcConfigPayload::headFollowRadius,
			ByteBufCodecs.STRING_UTF8,
			NpcConfigPayload::dialogue,
			ByteBufCodecs.BOOL,
			NpcConfigPayload::containerEnabled,
			ByteBufCodecs.VAR_INT,
			NpcConfigPayload::containerSize,
			ByteBufCodecs.BOOL,
			NpcConfigPayload::particleEffectEnabled,
			ByteBufCodecs.STRING_UTF8,
			NpcConfigPayload::particleEffectId,
			NpcConfigPayload::new
		);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record StorageMenuRequestPayload(net.minecraft.core.BlockPos pos, String subMenuId, int npcEntityId) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<StorageMenuRequestPayload> TYPE =
			new CustomPacketPayload.Type<>(HologramMenuMod.id("storage_menu_request"));
		public static final StreamCodec<RegistryFriendlyByteBuf, StorageMenuRequestPayload> CODEC = StreamCodec.composite(
			net.minecraft.core.BlockPos.STREAM_CODEC,
			StorageMenuRequestPayload::pos,
			ByteBufCodecs.STRING_UTF8,
			StorageMenuRequestPayload::subMenuId,
			ByteBufCodecs.VAR_INT,
			StorageMenuRequestPayload::npcEntityId,
			StorageMenuRequestPayload::new
		);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record StorageMenuSyncPayload(com.hologrammenu.storage.StorageMenuNetwork.MenuData menu) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<StorageMenuSyncPayload> TYPE =
			new CustomPacketPayload.Type<>(HologramMenuMod.id("storage_menu_sync"));
		public static final StreamCodec<RegistryFriendlyByteBuf, StorageMenuSyncPayload> CODEC = StreamCodec.composite(
			com.hologrammenu.storage.StorageMenuNetwork.MenuData.STREAM_CODEC,
			StorageMenuSyncPayload::menu,
			StorageMenuSyncPayload::new
		);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record StorageMenuSavePayload(com.hologrammenu.storage.StorageMenuNetwork.MenuData menu) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<StorageMenuSavePayload> TYPE =
			new CustomPacketPayload.Type<>(HologramMenuMod.id("storage_menu_save"));
		public static final StreamCodec<RegistryFriendlyByteBuf, StorageMenuSavePayload> CODEC = StreamCodec.composite(
			com.hologrammenu.storage.StorageMenuNetwork.MenuData.STREAM_CODEC,
			StorageMenuSavePayload::menu,
			StorageMenuSavePayload::new
		);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record StorageMenuClearPayload(net.minecraft.core.BlockPos pos, String subMenuId, int npcEntityId) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<StorageMenuClearPayload> TYPE =
			new CustomPacketPayload.Type<>(HologramMenuMod.id("storage_menu_clear"));
		public static final StreamCodec<RegistryFriendlyByteBuf, StorageMenuClearPayload> CODEC = StreamCodec.composite(
			net.minecraft.core.BlockPos.STREAM_CODEC,
			StorageMenuClearPayload::pos,
			ByteBufCodecs.STRING_UTF8,
			StorageMenuClearPayload::subMenuId,
			ByteBufCodecs.VAR_INT,
			StorageMenuClearPayload::npcEntityId,
			StorageMenuClearPayload::new
		);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record StorageMenuContextPayload(net.minecraft.core.BlockPos pos, String subMenuId, int npcEntityId) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<StorageMenuContextPayload> TYPE =
			new CustomPacketPayload.Type<>(HologramMenuMod.id("storage_menu_context"));
		public static final StreamCodec<RegistryFriendlyByteBuf, StorageMenuContextPayload> CODEC = StreamCodec.composite(
			net.minecraft.core.BlockPos.STREAM_CODEC,
			StorageMenuContextPayload::pos,
			ByteBufCodecs.STRING_UTF8,
			StorageMenuContextPayload::subMenuId,
			ByteBufCodecs.VAR_INT,
			StorageMenuContextPayload::npcEntityId,
			StorageMenuContextPayload::new
		);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record StorageMenuNavigationStatePayload(boolean showBack) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<StorageMenuNavigationStatePayload> TYPE =
			new CustomPacketPayload.Type<>(HologramMenuMod.id("storage_menu_navigation_state"));
		public static final StreamCodec<RegistryFriendlyByteBuf, StorageMenuNavigationStatePayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL,
			StorageMenuNavigationStatePayload::showBack,
			StorageMenuNavigationStatePayload::new
		);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record ShopStatePayload(net.minecraft.core.BlockPos pos, boolean shopEnabled, java.util.List<com.hologrammenu.storage.StorageMenuNetwork.ShopListingData> listings) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<ShopStatePayload> TYPE =
			new CustomPacketPayload.Type<>(HologramMenuMod.id("shop_state"));
		public static final StreamCodec<RegistryFriendlyByteBuf, ShopStatePayload> CODEC = StreamCodec.composite(
			net.minecraft.core.BlockPos.STREAM_CODEC,
			ShopStatePayload::pos,
			ByteBufCodecs.BOOL,
			ShopStatePayload::shopEnabled,
			com.hologrammenu.storage.StorageMenuNetwork.ShopListingData.STREAM_CODEC.apply(ByteBufCodecs.list()),
			ShopStatePayload::listings,
			ShopStatePayload::new
		);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record SetStoragePlacementModePayload(boolean enabled) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<SetStoragePlacementModePayload> TYPE =
			new CustomPacketPayload.Type<>(HologramMenuMod.id("set_storage_placement_mode"));
		public static final StreamCodec<RegistryFriendlyByteBuf, SetStoragePlacementModePayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL,
			SetStoragePlacementModePayload::enabled,
			SetStoragePlacementModePayload::new
		);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record StorageMenuAssignPayload(net.minecraft.core.BlockPos pos) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<StorageMenuAssignPayload> TYPE =
			new CustomPacketPayload.Type<>(HologramMenuMod.id("storage_menu_assign"));
		public static final StreamCodec<RegistryFriendlyByteBuf, StorageMenuAssignPayload> CODEC = StreamCodec.composite(
			net.minecraft.core.BlockPos.STREAM_CODEC,
			StorageMenuAssignPayload::pos,
			StorageMenuAssignPayload::new
		);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record SetNpcPlacementModePayload(boolean enabled) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<SetNpcPlacementModePayload> TYPE =
			new CustomPacketPayload.Type<>(HologramMenuMod.id("set_npc_placement_mode"));
		public static final StreamCodec<RegistryFriendlyByteBuf, SetNpcPlacementModePayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL,
			SetNpcPlacementModePayload::enabled,
			SetNpcPlacementModePayload::new
		);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record NpcPlacePayload(double x, double y, double z, String npcType, String skinName, String professionId, String displayName) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<NpcPlacePayload> TYPE =
			new CustomPacketPayload.Type<>(HologramMenuMod.id("npc_place"));
		public static final StreamCodec<RegistryFriendlyByteBuf, NpcPlacePayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.DOUBLE,
			NpcPlacePayload::x,
			ByteBufCodecs.DOUBLE,
			NpcPlacePayload::y,
			ByteBufCodecs.DOUBLE,
			NpcPlacePayload::z,
			ByteBufCodecs.STRING_UTF8,
			NpcPlacePayload::npcType,
			ByteBufCodecs.STRING_UTF8,
			NpcPlacePayload::skinName,
			ByteBufCodecs.STRING_UTF8,
			NpcPlacePayload::professionId,
			ByteBufCodecs.STRING_UTF8,
			NpcPlacePayload::displayName,
			NpcPlacePayload::new
		);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record SetNpcEditModePayload(boolean enabled) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<SetNpcEditModePayload> TYPE =
			new CustomPacketPayload.Type<>(HologramMenuMod.id("set_npc_edit_mode"));
		public static final StreamCodec<RegistryFriendlyByteBuf, SetNpcEditModePayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL,
			SetNpcEditModePayload::enabled,
			SetNpcEditModePayload::new
		);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record NpcEditPayload(
		int entityId,
		String action,
		String displayName,
		String skinName,
		String professionId,
		String dialogue,
		boolean headFollowEnabled,
		float headFollowRadius,
		boolean containerEnabled,
		int containerSize,
		String hologramStack,
		boolean particleEffectEnabled,
		String particleEffectId
	) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<NpcEditPayload> TYPE =
			new CustomPacketPayload.Type<>(HologramMenuMod.id("npc_edit"));
		public static final StreamCodec<RegistryFriendlyByteBuf, NpcEditPayload> CODEC = new StreamCodec<>() {
			@Override
			public NpcEditPayload decode(RegistryFriendlyByteBuf buffer) {
				return new NpcEditPayload(
					buffer.readVarInt(),
					buffer.readUtf(),
					buffer.readUtf(),
					buffer.readUtf(),
					buffer.readUtf(),
					buffer.readUtf(),
					buffer.readBoolean(),
					buffer.readFloat(),
					buffer.readBoolean(),
					buffer.readVarInt(),
					buffer.readUtf(),
					buffer.readBoolean(),
					buffer.readUtf()
				);
			}

			@Override
			public void encode(RegistryFriendlyByteBuf buffer, NpcEditPayload payload) {
				buffer.writeVarInt(payload.entityId());
				buffer.writeUtf(payload.action());
				buffer.writeUtf(payload.displayName());
				buffer.writeUtf(payload.skinName());
				buffer.writeUtf(payload.professionId());
				buffer.writeUtf(payload.dialogue());
				buffer.writeBoolean(payload.headFollowEnabled());
				buffer.writeFloat(payload.headFollowRadius());
				buffer.writeBoolean(payload.containerEnabled());
				buffer.writeVarInt(payload.containerSize());
				buffer.writeUtf(payload.hologramStack() == null ? "" : payload.hologramStack());
				buffer.writeBoolean(payload.particleEffectEnabled());
				buffer.writeUtf(payload.particleEffectId() == null ? "" : payload.particleEffectId());
			}
		};

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record NpcOpenMenuPayload(int entityId) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<NpcOpenMenuPayload> TYPE =
			new CustomPacketPayload.Type<>(HologramMenuMod.id("npc_open_menu"));
		public static final StreamCodec<RegistryFriendlyByteBuf, NpcOpenMenuPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT,
			NpcOpenMenuPayload::entityId,
			NpcOpenMenuPayload::new
		);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record ItemStylerOpenPayload() implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<ItemStylerOpenPayload> TYPE =
			new CustomPacketPayload.Type<>(HologramMenuMod.id("item_styler_open"));
		public static final StreamCodec<RegistryFriendlyByteBuf, ItemStylerOpenPayload> CODEC = StreamCodec.unit(new ItemStylerOpenPayload());

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record ItemStylerApplyNamePayload(String styledName) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<ItemStylerApplyNamePayload> TYPE =
			new CustomPacketPayload.Type<>(HologramMenuMod.id("item_styler_apply_name"));
		public static final StreamCodec<RegistryFriendlyByteBuf, ItemStylerApplyNamePayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8,
			ItemStylerApplyNamePayload::styledName,
			ItemStylerApplyNamePayload::new
		);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record GivePlayerHeadPayload(String profileName, String headDatabaseId, String headDatabaseBase64) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<GivePlayerHeadPayload> TYPE =
			new CustomPacketPayload.Type<>(HologramMenuMod.id("give_player_head"));
		public static final StreamCodec<RegistryFriendlyByteBuf, GivePlayerHeadPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8,
			GivePlayerHeadPayload::profileName,
			ByteBufCodecs.STRING_UTF8,
			GivePlayerHeadPayload::headDatabaseId,
			ByteBufCodecs.STRING_UTF8,
			GivePlayerHeadPayload::headDatabaseBase64,
			GivePlayerHeadPayload::new
		);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record HeadPresetListRequestPayload(String category, String query, int page) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<HeadPresetListRequestPayload> TYPE =
			new CustomPacketPayload.Type<>(HologramMenuMod.id("head_preset_list_request"));
		public static final StreamCodec<RegistryFriendlyByteBuf, HeadPresetListRequestPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8,
			HeadPresetListRequestPayload::category,
			ByteBufCodecs.STRING_UTF8,
			HeadPresetListRequestPayload::query,
			ByteBufCodecs.VAR_INT,
			HeadPresetListRequestPayload::page,
			HeadPresetListRequestPayload::new
		);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record HeadPresetListResponsePayload(
		boolean available,
		String message,
		String category,
		String query,
		int page,
		int totalCount,
		java.util.List<com.hologrammenu.head.HeadPresetEntry> entries
	) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<HeadPresetListResponsePayload> TYPE =
			new CustomPacketPayload.Type<>(HologramMenuMod.id("head_preset_list_response"));
		private static final StreamCodec<RegistryFriendlyByteBuf, com.hologrammenu.head.HeadPresetEntry> ENTRY_CODEC =
			StreamCodec.composite(
				ByteBufCodecs.STRING_UTF8,
				com.hologrammenu.head.HeadPresetEntry::id,
				ByteBufCodecs.STRING_UTF8,
				com.hologrammenu.head.HeadPresetEntry::name,
				ByteBufCodecs.STRING_UTF8,
				com.hologrammenu.head.HeadPresetEntry::category,
				ByteBufCodecs.STRING_UTF8,
				com.hologrammenu.head.HeadPresetEntry::base64,
				com.hologrammenu.head.HeadPresetEntry::new
			);
		public static final StreamCodec<RegistryFriendlyByteBuf, HeadPresetListResponsePayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL,
			HeadPresetListResponsePayload::available,
			ByteBufCodecs.STRING_UTF8,
			HeadPresetListResponsePayload::message,
			ByteBufCodecs.STRING_UTF8,
			HeadPresetListResponsePayload::category,
			ByteBufCodecs.STRING_UTF8,
			HeadPresetListResponsePayload::query,
			ByteBufCodecs.VAR_INT,
			HeadPresetListResponsePayload::page,
			ByteBufCodecs.VAR_INT,
			HeadPresetListResponsePayload::totalCount,
			ENTRY_CODEC.apply(ByteBufCodecs.list()),
			HeadPresetListResponsePayload::entries,
			HeadPresetListResponsePayload::new
		);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record AnvilSetLorePayload(java.util.List<String> lines) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<AnvilSetLorePayload> TYPE =
			new CustomPacketPayload.Type<>(HologramMenuMod.id("anvil_set_lore"));
		public static final StreamCodec<RegistryFriendlyByteBuf, AnvilSetLorePayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
			AnvilSetLorePayload::lines,
			AnvilSetLorePayload::new
		);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record AnvilApplyRpgEffectPayload(String effectId, int level, boolean remove) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<AnvilApplyRpgEffectPayload> TYPE =
			new CustomPacketPayload.Type<>(HologramMenuMod.id("anvil_apply_rpg_effect"));
		public static final StreamCodec<RegistryFriendlyByteBuf, AnvilApplyRpgEffectPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8,
			AnvilApplyRpgEffectPayload::effectId,
			ByteBufCodecs.VAR_INT,
			AnvilApplyRpgEffectPayload::level,
			ByteBufCodecs.BOOL,
			AnvilApplyRpgEffectPayload::remove,
			AnvilApplyRpgEffectPayload::new
		);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record AnvilApplyCustomRpgEffectPayload(String effectName, String loreLine, boolean remove) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<AnvilApplyCustomRpgEffectPayload> TYPE =
			new CustomPacketPayload.Type<>(HologramMenuMod.id("anvil_apply_custom_rpg_effect"));
		public static final StreamCodec<RegistryFriendlyByteBuf, AnvilApplyCustomRpgEffectPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8,
			AnvilApplyCustomRpgEffectPayload::effectName,
			ByteBufCodecs.STRING_UTF8,
			AnvilApplyCustomRpgEffectPayload::loreLine,
			ByteBufCodecs.BOOL,
			AnvilApplyCustomRpgEffectPayload::remove,
			AnvilApplyCustomRpgEffectPayload::new
		);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record AnvilApplyEnchantPayload(String enchantmentId, int level, boolean remove) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<AnvilApplyEnchantPayload> TYPE =
			new CustomPacketPayload.Type<>(HologramMenuMod.id("anvil_apply_enchant"));
		public static final StreamCodec<RegistryFriendlyByteBuf, AnvilApplyEnchantPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8,
			AnvilApplyEnchantPayload::enchantmentId,
			ByteBufCodecs.VAR_INT,
			AnvilApplyEnchantPayload::level,
			ByteBufCodecs.BOOL,
			AnvilApplyEnchantPayload::remove,
			AnvilApplyEnchantPayload::new
		);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	private ModPackets() {
	}
}
