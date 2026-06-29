package com.hologrammenu.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record StorageMenuBlockData(
	StorageMenuDefinition definition,
	boolean invulnerable,
	boolean hologramLabel,
	ShopDefinition shop
) {
	public static final Codec<StorageMenuBlockData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		StorageMenuPersistence.MenuEntry.CODEC.fieldOf("menu").forGetter(data -> StorageMenuPersistence.MenuEntry.fromDefinition(data.definition())),
		Codec.BOOL.fieldOf("invulnerable").forGetter(StorageMenuBlockData::invulnerable),
		Codec.BOOL.lenientOptionalFieldOf("hologram_label", false).forGetter(StorageMenuBlockData::hologramLabel),
		ShopPersistence.ShopEntry.CODEC.lenientOptionalFieldOf("shop", ShopPersistence.ShopEntry.fromDefinition(ShopDefinition.EMPTY))
			.forGetter(data -> ShopPersistence.ShopEntry.fromDefinition(data.shop()))
	).apply(instance, (menu, invulnerable, hologramLabel, shop) -> new StorageMenuBlockData(
		menu.toDefinition(),
		invulnerable,
		hologramLabel,
		shop.toDefinition()
	)));

	public StorageMenuBlockData(StorageMenuDefinition definition, boolean invulnerable) {
		this(definition, invulnerable, false, ShopDefinition.EMPTY);
	}

	public StorageMenuBlockData(StorageMenuDefinition definition, boolean invulnerable, boolean hologramLabel) {
		this(definition, invulnerable, hologramLabel, ShopDefinition.EMPTY);
	}

	public StorageMenuBlockData withDefinition(StorageMenuDefinition newDefinition) {
		return new StorageMenuBlockData(newDefinition, invulnerable, hologramLabel, shop);
	}

	public StorageMenuBlockData withInvulnerable(boolean value) {
		return new StorageMenuBlockData(definition, value, hologramLabel, shop);
	}

	public StorageMenuBlockData withHologramLabel(boolean value) {
		return new StorageMenuBlockData(definition, invulnerable, value, shop);
	}

	public StorageMenuBlockData withShop(ShopDefinition value) {
		return new StorageMenuBlockData(definition, invulnerable, hologramLabel, value == null ? ShopDefinition.EMPTY : value);
	}
}
