package com.hologrammenu.head;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hologrammenu.HologramMenuMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class MinecraftHeadsConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG_PATH = FabricLoader.getInstance()
		.getConfigDir()
		.resolve(HologramMenuMod.MOD_ID)
		.resolve("minecraft-heads-api.json");

	public String appUuid = "";
	public String apiKey = "";
	public boolean demo = true;

	public static MinecraftHeadsConfig load() {
		if (!Files.exists(CONFIG_PATH)) {
			MinecraftHeadsConfig config = new MinecraftHeadsConfig();
			save(config);
			return config;
		}
		try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
			MinecraftHeadsConfig config = GSON.fromJson(reader, MinecraftHeadsConfig.class);
			return config == null ? new MinecraftHeadsConfig() : config;
		} catch (IOException exception) {
			HologramMenuMod.LOGGER.warn("Failed to read Minecraft-Heads API config", exception);
			return new MinecraftHeadsConfig();
		}
	}

	public boolean hasAppUuid() {
		return appUuid != null && !appUuid.isBlank();
	}

	private static void save(MinecraftHeadsConfig config) {
		try {
			Files.createDirectories(CONFIG_PATH.getParent());
			try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
				GSON.toJson(config, writer);
			}
		} catch (IOException exception) {
			HologramMenuMod.LOGGER.warn("Failed to create Minecraft-Heads API config", exception);
		}
	}
}
