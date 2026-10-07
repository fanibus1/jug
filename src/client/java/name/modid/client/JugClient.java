package name.modid.client;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

public class JugClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            dispatcher.register(
                    ClientCommandManager.literal("setp")
                            .then(ClientCommandManager.argument("pitch", FloatArgumentType.floatArg(-90.0F, 90.0F))
                                    .executes(this::executeSetPitch)));
        });
    }

    private int executeSetPitch(CommandContext<FabricClientCommandSource> context) {
        float pitch = FloatArgumentType.getFloat(context, "pitch");
        CameraUtil.setPlayerPitch(pitch);
        return Command.SINGLE_SUCCESS;
    }
}