package com.serverutilities.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record StorageMenuBlockData(
	StorageMenuDefinition definition,
	boolean invulnerable,
	boolean hologramLabel,
	StorageMenuHologramSettings hologramSettings,
	ShopDefinition shop
) {
	public StorageMenuBlockData {
		hologramSettings = hologramSettings == null ? StorageMenuHologramSettings.DEFAULT : hologramSettings;
		shop = shop == null ? ShopDefinition.EMPTY : shop;
	}

	public static final Codec<StorageMenuBlockData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		StorageMenuPersistence.MenuEntry.CODEC.fieldOf("menu").forGetter(data -> StorageMenuPersistence.MenuEntry.fromDefinition(data.definition())),
		Codec.BOOL.fieldOf("invulnerable").forGetter(StorageMenuBlockData::invulnerable),
		Codec.BOOL.lenientOptionalFieldOf("hologram_label", false).forGetter(StorageMenuBlockData::hologramLabel),
		StorageMenuHologramSettings.CODEC.lenientOptionalFieldOf("hologram_settings", StorageMenuHologramSettings.DEFAULT).forGetter(StorageMenuBlockData::hologramSettings),
		ShopPersistence.ShopEntry.CODEC.lenientOptionalFieldOf("shop", ShopPersistence.ShopEntry.fromDefinition(ShopDefinition.EMPTY))
			.forGetter(data -> ShopPersistence.ShopEntry.fromDefinition(data.shop()))
	).apply(instance, (menu, invulnerable, hologramLabel, hologramSettings, shop) -> new StorageMenuBlockData(
		menu.toDefinition(),
		invulnerable,
		hologramLabel,
		hologramSettings,
		shop.toDefinition()
	)));

	public StorageMenuBlockData(StorageMenuDefinition definition, boolean invulnerable) {
		this(definition, invulnerable, false, StorageMenuHologramSettings.DEFAULT, ShopDefinition.EMPTY);
	}

	public StorageMenuBlockData(StorageMenuDefinition definition, boolean invulnerable, boolean hologramLabel) {
		this(definition, invulnerable, hologramLabel, StorageMenuHologramSettings.DEFAULT, ShopDefinition.EMPTY);
	}

	public StorageMenuBlockData(StorageMenuDefinition definition, boolean invulnerable, boolean hologramLabel, ShopDefinition shop) {
		this(definition, invulnerable, hologramLabel, StorageMenuHologramSettings.DEFAULT, shop);
	}

	public StorageMenuBlockData withDefinition(StorageMenuDefinition newDefinition) {
		return new StorageMenuBlockData(newDefinition, invulnerable, hologramLabel, hologramSettings, shop);
	}

	public StorageMenuBlockData withInvulnerable(boolean value) {
		return new StorageMenuBlockData(definition, value, hologramLabel, hologramSettings, shop);
	}

	public StorageMenuBlockData withHologramLabel(boolean value) {
		return new StorageMenuBlockData(definition, invulnerable, value, hologramSettings, shop);
	}

	public StorageMenuBlockData withHologramSettings(StorageMenuHologramSettings value) {
		return new StorageMenuBlockData(definition, invulnerable, hologramLabel, value == null ? StorageMenuHologramSettings.DEFAULT : value, shop);
	}

	public StorageMenuBlockData withShop(ShopDefinition value) {
		return new StorageMenuBlockData(definition, invulnerable, hologramLabel, hologramSettings, value == null ? ShopDefinition.EMPTY : value);
	}
}
