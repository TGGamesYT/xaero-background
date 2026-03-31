package me.tg.xaerobackground.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.util.logging.Logger;

public class XaerobackgroundClient implements ClientModInitializer {
    public static volatile NativeImage WALLPAPER_NATIVE = null;
    public static final Identifier WALLPAPER_ID = Identifier.of("xaerobackground", "textures/wallpaper.png");
    private static boolean loaded = false;
    public final static Logger LOGGER =
            Logger.getLogger("Xaerobackground");

    @Override
    public void onInitializeClient() {
        // Try once per tick to load the wallpaper resource into a NativeImage
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (loaded) return;
            if (client == null) return;
            try {
                // ensure resource manager + texture manager exist
                if (client.getResourceManager() == null) return;

                // load wallpaper resource into a NativeImage (blocking small file read)
                try (InputStream is = client.getResourceManager().getResource(WALLPAPER_ID).get().getInputStream()) {
                    // may throw
                    WALLPAPER_NATIVE = NativeImage.read(is);
                    loaded = true;
                } catch (Exception e) {
                    // resource may not exist yet — keep trying next tick
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        });
    }
}
