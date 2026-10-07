package name.modid.client.mixin;

import name.modid.client.JugClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
    private Screen prefillChatBox(Screen screen) {

        if (screen instanceof ChatScreen) {
            // Cast ChatScreen to Accessor interface and get initial
            ChatScreenAccessor accessor = (ChatScreenAccessor) screen;
            String initialText = accessor.getInitial();

            // If screen was opened with "/", not empty text, do nothing:
            if (!initialText.equals("")) {
                return screen;
            }

            // Substitute with a new ChatScreen containing prefix.
            String prefix = JugClient.getFullPrefix();

            // If screen was opened with standard empty text:
            return new ChatScreen(prefix);
        }
        return screen;
    }
}