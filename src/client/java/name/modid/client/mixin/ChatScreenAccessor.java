package name.modid.client.mixin;

import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChatScreen.class)
public interface ChatScreenAccessor {

    // In 1.20.1 Mojang mappings, chatScreen.initial sets initial text.
    // When pressing 'T', initial string is empty "".
    // When pressing '/', initial string is "/".
    @Accessor("initial")
    String getInitial();
}
