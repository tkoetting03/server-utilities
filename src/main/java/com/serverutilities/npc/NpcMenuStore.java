package com.serverutilities.npc;

import com.serverutilities.ServerUtilitiesMod;
import com.serverutilities.storage.StorageMenuBlockData;
import com.serverutilities.storage.StorageMenuDefinition;
import com.serverutilities.storage.StorageMenuSizes;
import com.serverutilities.storage.ShopDefinition;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class NpcMenuStore {
	private NpcMenuStore() {
	}

	public static Optional<StorageMenuBlockData> get(ServerLevel level, UUID entityId) {
		return savedData(level).get(entityId);
	}

	public static void set(ServerLevel level, UUID entityId, StorageMenuBlockData data) {
		savedData(level).put(entityId, data);
	}

	public static void clear(ServerLevel level, UUID entityId) {
		savedData(level).remove(entityId);
	}

	public static void ensureEnabled(ServerLevel level, UUID entityId, int containerSize) {
		int size = NpcConfig.normalizeContainerSize(containerSize);
		Optional<StorageMenuBlockData> existing = get(level, entityId);
		if (existing.isPresent()) {
			StorageMenuBlockData data = existing.get();
			StorageMenuDefinition definition = data.definition();
			if (definition.containerSize() != size) {
				definition = StorageMenuDefinition.empty(size).withEnabled(true).withTitle(definition.title());
			} else if (!definition.enabled()) {
				definition = definition.withEnabled(true);
			} else {
				return;
			}
			set(level, entityId, data.withDefinition(definition));
			return;
		}

		set(level, entityId, new StorageMenuBlockData(
			StorageMenuDefinition.empty(size).withEnabled(true),
			false,
			false
		));
	}

	private static NpcMenuSavedData savedData(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(NpcMenuSavedData.TYPE);
	}

	static final class NpcMenuSavedData extends SavedData {
		private record StoredEntry(String entityId, StorageMenuBlockData data) {
			private static final Codec<StoredEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.STRING.fieldOf("entity_id").forGetter(StoredEntry::entityId),
				StorageMenuBlockData.CODEC.fieldOf("data").forGetter(StoredEntry::data)
			).apply(instance, StoredEntry::new));
		}

		public static final Codec<NpcMenuSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			StoredEntry.CODEC.listOf().fieldOf("entries").forGetter(NpcMenuSavedData::toStoredEntries)
		).apply(instance, NpcMenuSavedData::fromStoredEntries));

		public static final SavedDataType<NpcMenuSavedData> TYPE = new SavedDataType<>(
			ServerUtilitiesMod.id("npc_menus"),
			NpcMenuSavedData::new,
			CODEC,
			DataFixTypes.SAVED_DATA_COMMAND_STORAGE
		);

		private final Map<UUID, StorageMenuBlockData> menus = new HashMap<>();

		NpcMenuSavedData() {
		}

		NpcMenuSavedData(Map<UUID, StorageMenuBlockData> menus) {
			this.menus.putAll(menus);
		}

		private List<StoredEntry> toStoredEntries() {
			return menus.entrySet().stream()
				.map(entry -> new StoredEntry(entry.getKey().toString(), entry.getValue()))
				.toList();
		}

		private static NpcMenuSavedData fromStoredEntries(List<StoredEntry> entries) {
			Map<UUID, StorageMenuBlockData> menus = new HashMap<>();
			for (StoredEntry entry : entries) {
				try {
					menus.put(UUID.fromString(entry.entityId()), entry.data());
				} catch (IllegalArgumentException ignored) {
				}
			}
			return new NpcMenuSavedData(menus);
		}

		Optional<StorageMenuBlockData> get(UUID entityId) {
			return Optional.ofNullable(menus.get(entityId));
		}

		void put(UUID entityId, StorageMenuBlockData data) {
			menus.put(entityId, data);
			setDirty();
		}

		void remove(UUID entityId) {
			if (menus.remove(entityId) != null) {
				setDirty();
			}
		}
	}
}
