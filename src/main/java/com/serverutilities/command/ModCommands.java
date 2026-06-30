package com.serverutilities.command;

import com.serverutilities.hologram.HologramHelper;
import com.serverutilities.mixin.accessor.TextDisplayAccessor;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.phys.Vec3;

public final class ModCommands {
	private ModCommands() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			registerHologramCommands(dispatcher);
		});
	}

	private static void registerHologramCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("hologram")
			.requires(source -> Commands.LEVEL_GAMEMASTERS.check(source.permissions()))
			.then(Commands.literal("create")
				.then(Commands.argument("text", StringArgumentType.greedyString())
					.executes(context -> {
						CommandSourceStack source = context.getSource();
						ServerLevel level = source.getLevel();
						Vec3 position = source.getEntity() instanceof ServerPlayer player
							? HologramHelper.placementPosition(player)
							: source.getPosition();
						String text = StringArgumentType.getString(context, "text");
						HologramHelper.create(level, position, Component.literal(text));
						source.sendSuccess(() -> Component.translatable("command.serverutilities.hologram.created", text), true);
						return 1;
					})
					.then(Commands.argument("position", Vec3Argument.vec3())
						.executes(context -> {
							CommandSourceStack source = context.getSource();
							ServerLevel level = source.getLevel();
							Vec3 position = Vec3Argument.getVec3(context, "position");
							String text = StringArgumentType.getString(context, "text");
							HologramHelper.create(level, position, Component.literal(text));
							source.sendSuccess(() -> Component.translatable("command.serverutilities.hologram.created", text), true);
							return 1;
						}))))
			.then(Commands.literal("remove")
				.executes(context -> {
					CommandSourceStack source = context.getSource();
					Vec3 origin = source.getPosition();
					boolean removed = HologramHelper.removeNearest(source.getLevel(), origin);
					if (removed) {
						source.sendSuccess(() -> Component.translatable("command.serverutilities.hologram.removed"), true);
					} else {
						source.sendFailure(Component.translatable("command.serverutilities.hologram.not_found"));
					}
					return removed ? 1 : 0;
				}))
			.then(Commands.literal("list")
				.executes(context -> {
					CommandSourceStack source = context.getSource();
					var holograms = HologramHelper.listNearby(source.getLevel(), source.getPosition(), 32.0D);
					if (holograms.isEmpty()) {
						source.sendSuccess(() -> Component.translatable("command.serverutilities.hologram.none_nearby"), false);
						return 1;
					}
					source.sendSuccess(() -> Component.translatable("command.serverutilities.hologram.list_header", holograms.size()), false);
					for (Display.TextDisplay hologram : holograms) {
						Component text = ((TextDisplayAccessor) hologram).serverutilities$getText();
						Vec3 pos = hologram.position();
						source.sendSuccess(() -> Component.literal(String.format("- %s @ %.1f %.1f %.1f", text.getString(), pos.x, pos.y, pos.z)), false);
					}
					return holograms.size();
				})));
	}
}
