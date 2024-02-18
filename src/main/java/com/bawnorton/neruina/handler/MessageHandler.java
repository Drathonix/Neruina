package com.bawnorton.neruina.handler;

import com.bawnorton.neruina.config.Config;
import com.bawnorton.neruina.thread.ConditionalRunnable;
import com.bawnorton.neruina.version.VersionedText;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.UUID;
import java.util.function.Consumer;

public final class MessageHandler {
    public void broadcastToPlayers(MinecraftServer server, Text message) {
        ConditionalRunnable.create(() -> {
            switch (Config.getInstance().logLevel) {
                case DISABLED -> {
                }
                case EVERYONE -> server.getPlayerManager()
                                       .getPlayerList()
                                       .forEach(player -> player.sendMessage(message, false));
                case OPERATORS -> server.getPlayerManager()
                        .getPlayerList()
                        .stream()
                        .filter(player -> server.getPermissionLevel(player.getGameProfile()) >= server.getOpPermissionLevel())
                        .forEach(player -> player.sendMessage(message, false));
            }
        }, () -> server.getPlayerManager().getCurrentPlayerCount() > 0);
    }

    public void broadcastToPlayers(MinecraftServer server, Text... messages) {
        broadcastToPlayers(
                server,
                VersionedText.pad(VersionedText.concatDelimited(VersionedText.LINE_BREAK, messages))
        );
    }

    public Text generateEntityActions(Entity entity) {
        return VersionedText.concatDelimited(VersionedText.SPACE,
                generateHandlingActions("entity", entity.getBlockPos(), entity.getUuid()),
                Texts.bracketed(VersionedText.withStyle(VersionedText.translatable("neruina.kill_entity"),
                        style -> style.withColor(Formatting.DARK_RED)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                        "/neruina kill %s".formatted(entity.getUuid())
                                ))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                        VersionedText.translatable("neruina.kill_entity.tooltip")
                                ))
                ))
        );
    }

    public Text generateResourceActions(Throwable e) {
        StringWriter traceString = new StringWriter();
        PrintWriter writer = new PrintWriter(traceString);
        e.printStackTrace(writer);
        String trace = traceString.toString();
        writer.flush();
        writer.close();
        return VersionedText.concatDelimited(VersionedText.SPACE,
                Texts.bracketed(VersionedText.withStyle(VersionedText.translatable("neruina.info"),
                        style -> style.withColor(Formatting.GREEN)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL,
                                        "https://github.com/Bawnorton/Neruina/wiki/What-Is-This%3F"
                                ))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                        VersionedText.translatable("neruina.info.tooltip")
                                ))
                )),
                Texts.bracketed(VersionedText.withStyle(VersionedText.translatable("neruina.copy_crash"),
                        style -> style.withColor(Formatting.GOLD)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, trace))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                        VersionedText.translatable("neruina.copy_crash.tooltip")
                                ))
                ))
        );
    }

    public Text generateHandlingActions(String typeName, BlockPos pos) {
        return generateHandlingActions(typeName, pos, null);
    }

    public Text generateHandlingActions(String typeName, BlockPos pos, @Nullable UUID uuid) {
        return VersionedText.concatDelimited(VersionedText.SPACE,
                Texts.bracketed(VersionedText.withStyle(VersionedText.translatable("neruina.teleport"),
                        style -> style.withColor(Formatting.DARK_AQUA)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                        "/tp @s %s".formatted(posToNums(pos))
                                ))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                        VersionedText.translatable("neruina.teleport.%s.tooltip".formatted(typeName))
                                ))
                )),
                Texts.bracketed(VersionedText.withStyle(VersionedText.translatable("neruina.try_resume"),
                        style -> style.withColor(Formatting.YELLOW)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                        "/neruina resume %s %s".formatted(typeName, uuid == null ? posToNums(pos) : uuid.toString())
                                ))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                        VersionedText.translatable("neruina.try_resume.%s.tooltip".formatted(typeName))
                                ))
                ))
        );
    }

    public void sendToPlayer(PlayerEntity player, Text message, @Nullable Text... actions) {
        player.sendMessage(VersionedText.pad(VersionedText.concatDelimited(VersionedText.LINE_BREAK,
                VersionedText.format(message),
                actions != null ? VersionedText.concatDelimited(VersionedText.LINE_BREAK, actions) : null
        )), false);
    }

    public void sendFeedback(CommandContext<ServerCommandSource> context, String key, Object... args) {
        sendFormattedMessage((text) -> {
            /*? if >=1.20 { */
            context.getSource().sendFeedback(() -> text, true);
            /*? } else {*//*
            context.getSource().sendFeedback(text, true);
            *//*? }*/
        }, key, args);
    }

    public void sendFormattedMessage(Consumer<Text> sender, String key, Object... args) {
        sender.accept(formatText(key, args));
    }

    public Text formatText(String key, Object... args) {
        return VersionedText.format(VersionedText.translatable(key, args));
    }

    public String posToNums(BlockPos pos) {
        return "%s %s %s".formatted(pos.getX(), pos.getY(), pos.getZ());
    }

    public String formatPos(BlockPos pos) {
        return "x=%s y=%s z=%s".formatted(pos.getX(), pos.getY(), pos.getZ());
    }
}