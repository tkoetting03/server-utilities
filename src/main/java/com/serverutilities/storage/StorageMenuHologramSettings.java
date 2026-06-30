package com.serverutilities.storage;

import com.serverutilities.hologram.HologramLineStack;
import com.serverutilities.hologram.HologramScale;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record StorageMenuHologramSettings(float heightOffset, float scale, boolean seeThroughWalls) {
	public static final StorageMenuHologramSettings DEFAULT = new StorageMenuHologramSettings(0.0F, HologramScale.DEFAULT, true);

	public static final Codec<StorageMenuHologramSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.FLOAT.lenientOptionalFieldOf("height_offset", DEFAULT.heightOffset()).forGetter(StorageMenuHologramSettings::heightOffset),
		Codec.FLOAT.lenientOptionalFieldOf("scale", DEFAULT.scale()).forGetter(StorageMenuHologramSettings::scale),
		Codec.BOOL.lenientOptionalFieldOf("see_through_walls", DEFAULT.seeThroughWalls()).forGetter(StorageMenuHologramSettings::seeThroughWalls)
	).apply(instance, StorageMenuHologramSettings::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, StorageMenuHologramSettings> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.FLOAT,
		StorageMenuHologramSettings::heightOffset,
		ByteBufCodecs.FLOAT,
		StorageMenuHologramSettings::scale,
		ByteBufCodecs.BOOL,
		StorageMenuHologramSettings::seeThroughWalls,
		StorageMenuHologramSettings::new
	);

	public StorageMenuHologramSettings {
		heightOffset = HologramLineStack.clampHeightOffset(heightOffset);
		scale = HologramScale.clamp(scale);
	}

	public StorageMenuHologramSettings withHeightOffset(float value) {
		return new StorageMenuHologramSettings(value, scale, seeThroughWalls);
	}

	public StorageMenuHologramSettings withScale(float value) {
		return new StorageMenuHologramSettings(heightOffset, value, seeThroughWalls);
	}

	public StorageMenuHologramSettings withSeeThroughWalls(boolean value) {
		return new StorageMenuHologramSettings(heightOffset, scale, value);
	}
}
