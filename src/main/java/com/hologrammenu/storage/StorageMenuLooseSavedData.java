package com.hologrammenu.storage;

import com.hologrammenu.HologramMenuMod;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class StorageMenuLooseSavedData extends SavedData {
	private record StoredEntry(BlockPos pos, String blockId, StorageMenuBlockData data) {
		private static final Codec<StoredEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			BlockPos.CODEC.fieldOf("pos").forGetter(StoredEntry::pos),
			Codec.STRING.fieldOf("block_id").forGetter(StoredEntry::blockId),
			StorageMenuBlockData.CODEC.fieldOf("data").forGetter(StoredEntry::data)
		).apply(instance, StoredEntry::new));
	}

	public static final Codec<StorageMenuLooseSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		StoredEntry.CODEC.listOf().fieldOf("entries").forGetter(StorageMenuLooseSavedData::toStoredEntries)
	).apply(instance, StorageMenuLooseSavedData::fromStoredEntries));

	public static final SavedDataType<StorageMenuLooseSavedData> TYPE = new SavedDataType<>(
		HologramMenuMod.id("storage_menu_loose"),
		StorageMenuLooseSavedData::new,
		CODEC,
		DataFixTypes.SAVED_DATA_COMMAND_STORAGE
	);

	private final Map<BlockPos, StoredEntry> entries;

	public StorageMenuLooseSavedData() {
		this(new HashMap<>());
	}

	public StorageMenuLooseSavedData(Map<BlockPos, StoredEntry> entries) {
		this.entries = new HashMap<>(entries);
	}

	private static StorageMenuLooseSavedData fromStoredEntries(List<StoredEntry> storedEntries) {
		Map<BlockPos, StoredEntry> map = new HashMap<>();
		for (StoredEntry entry : storedEntries) {
			map.put(entry.pos().immutable(), entry);
		}
		return new StorageMenuLooseSavedData(map);
	}

	private List<StoredEntry> toStoredEntries() {
		return entries.values().stream().toList();
	}

	public Optional<StorageMenuBlockData> get(BlockPos pos, Identifier blockId) {
		StoredEntry entry = entries.get(pos);
		if (entry == null) {
			return Optional.empty();
		}
		if (!entry.blockId().equals(blockId.toString())) {
			entries.remove(pos);
			setDirty();
			return Optional.empty();
		}
		return Optional.of(entry.data());
	}

	public void put(BlockPos pos, Identifier blockId, StorageMenuBlockData data) {
		if (!data.definition().enabled() && !data.definition().hasConfiguredSlots()) {
			remove(pos);
			return;
		}
		entries.put(pos.immutable(), new StoredEntry(pos.immutable(), blockId.toString(), data));
		setDirty();
	}

	public void remove(BlockPos pos) {
		if (entries.remove(pos) != null) {
			setDirty();
		}
	}
}
