package com.hologrammenu.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hologrammenu.HologramMenuMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ClientConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG_PATH = FabricLoader.getInstance()
		.getConfigDir()
		.resolve(HologramMenuMod.MOD_ID + "-client.json");

	private static ClientConfig instance = new ClientConfig();

	public String defaultPlacementText = "Hologram";

	public static ClientConfig get() {
		return instance;
	}

	public static void load() {
		if (!Files.exists(CONFIG_PATH)) {
			instance = new ClientConfig();
			save();
			return;
		}

		try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
			ClientConfig loaded = GSON.fromJson(reader, ClientConfig.class);
			instance = loaded != null ? loaded : new ClientConfig();
		} catch (IOException exception) {
			HologramMenuMod.LOGGER.error("Failed to load client config, using defaults", exception);
			instance = new ClientConfig();
		}

		ClientSettings.apply(instance);
	}

	public static void save() {
		try {
			Files.createDirectories(CONFIG_PATH.getParent());
			try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
				GSON.toJson(instance, writer);
			}
		} catch (IOException exception) {
			HologramMenuMod.LOGGER.error("Failed to save client config", exception);
		}
	}

	public static void setPlacementModeEnabled(boolean enabled) {
		ClientSettings.placementModeEnabled = enabled;
	}
}
