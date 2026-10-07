package name.modid.client;

import name.modid.client.mixin.LocalPlayerAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

public class CameraUtil {

    /**
     * Sets the local player's pitch.
     *
     * @param targetPitch The desired pitch in degrees (-90.0F to 90.0F).
     */
    public static void setPlayerPitch(float targetPitch) {
        Minecraft client = Minecraft.getInstance();

        if (client.player == null) {
            return;
        }

        // Clamp between -90 and 90 degrees
        float clampedPitch = Mth.clamp(targetPitch, -90.0F, 90.0F);

        // Update current and previous pitch to prevent camera interpolation jitter
        client.player.setXRot(clampedPitch);
        LocalPlayerAccessor a = (LocalPlayerAccessor) client.player;
        a.setXRotLast(clampedPitch);
    }
}