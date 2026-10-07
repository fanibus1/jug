package name.modid.client;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public class JugClient implements ClientModInitializer {

    // Prefix state
    public static String activePrefix = null;
    public static boolean prependLeadingSlash = true;

    public static boolean hasPrefix() {
        return !(activePrefix == null || activePrefix.isEmpty());
    }

    private static String getLeadingSlash() {
        return (prependLeadingSlash ? "/" : "");
    }

    public static String getFullPrefix() {
        if (!hasPrefix()) {
            return "";
        }
        return getLeadingSlash() + activePrefix + " ";
    }

    @Override
    public void onInitializeClient() {
        // Register /repc command hierarchy
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            dispatcher.register(
                    ClientCommandManager.literal("repc")
                            .executes(this::showHelp)
                            .then(ClientCommandManager.literal("reset")
                                    .executes(this::resetPrefix))
                            .then(ClientCommandManager.literal("-m")
                                    .then(ClientCommandManager.argument("command", StringArgumentType.greedyString())
                                            .executes(ctx -> setPrefix(ctx,
                                                    StringArgumentType.getString(ctx, "command"), true))))
                            .then(ClientCommandManager.literal("-r")
                                    .then(ClientCommandManager.argument("text", StringArgumentType.greedyString())
                                            .executes(ctx -> setPrefix(ctx, StringArgumentType.getString(ctx, "text"),
                                                    false))))
                            .then(ClientCommandManager.argument("command", StringArgumentType.greedyString())
                                    .executes(ctx -> setPrefix(ctx, StringArgumentType.getString(ctx, "command"),
                                            true))));
        });
    }

    private int setPrefix(CommandContext<FabricClientCommandSource> context, String command, boolean withSlash) {
        activePrefix = command.trim();
        prependLeadingSlash = withSlash;

        String formattedPreview = getLeadingSlash() + activePrefix + " <message>";
        context.getSource().sendFeedback(
                Component.literal("[repc] ").withStyle(ChatFormatting.AQUA)
                        .append(Component.literal("Prefix set to: ").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(formattedPreview).withStyle(ChatFormatting.BOLD,
                                ChatFormatting.GREEN)));
        return Command.SINGLE_SUCCESS;
    }

    private int resetPrefix(CommandContext<FabricClientCommandSource> context) {
        activePrefix = null;
        prependLeadingSlash = true;
        context.getSource().sendFeedback(
                Component.literal("[repc] ").withStyle(ChatFormatting.AQUA)
                        .append(Component.literal("Prefix has been reset. Messages will send normally.")
                                .withStyle(ChatFormatting.GREEN)));
        return Command.SINGLE_SUCCESS;
    }

    private int showHelp(CommandContext<FabricClientCommandSource> context) {
        FabricClientCommandSource src = context.getSource();

        src.sendFeedback(
                Component.literal("\n--- [repc Help & Status] ---").withStyle(ChatFormatting.BOLD,
                        ChatFormatting.GOLD));

        if (activePrefix == null) {
            src.sendFeedback(
                    Component.literal("Current Prefix: ").withStyle(ChatFormatting.BOLD, ChatFormatting.GRAY)
                            .append(Component.literal("None (normal chat)").withStyle(ChatFormatting.BOLD,
                                    ChatFormatting.RED)));
        } else {
            String preview = getLeadingSlash() + activePrefix + " <message>";
            src.sendFeedback(
                    Component.literal("Current Prefix: ").withStyle(ChatFormatting.BOLD, ChatFormatting.GRAY)
                            .append(Component.literal(preview).withStyle(ChatFormatting.BOLD, ChatFormatting.GREEN)));
        }

        src.sendFeedback(
                Component.literal("Available Commands:").withStyle(ChatFormatting.BOLD, ChatFormatting.YELLOW));
        src.sendFeedback(Component.literal("  /repc <command>").withStyle(ChatFormatting.AQUA)
                .append(Component
                        .literal(" - Prepends /<command> to all following chat messages until changed or reset.")
                        .withStyle(ChatFormatting.WHITE)));
        src.sendFeedback(Component.literal("  /repc reset").withStyle(ChatFormatting.AQUA)
                .append(Component.literal(" - Clear prefix.").withStyle(ChatFormatting.WHITE)));
        src.sendFeedback(Component.literal("  /repc -m <command>").withStyle(ChatFormatting.AQUA)
                .append(Component.literal(" - Explicit mode (same as /repc <command>).")
                        .withStyle(ChatFormatting.WHITE)));
        src.sendFeedback(Component.literal("  /repc -r <text>").withStyle(ChatFormatting.AQUA)
                .append(Component.literal(" - Raw mode, prepend <text> WITHOUT a leading slash.")
                        .withStyle(ChatFormatting.WHITE)));

        return Command.SINGLE_SUCCESS;
    }
}