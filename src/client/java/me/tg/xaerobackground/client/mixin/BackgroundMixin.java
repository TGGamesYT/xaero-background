package me.tg.xaerobackground.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.map.gui.GuiMap;
import me.tg.xaerobackground.client.XaerobackgroundClient;

import static me.tg.xaerobackground.client.XaerobackgroundClient.WALLPAPER_ID;

/**
 * Draw wallpaper behind everything at the very start of GuiMap.render.
 * This uses DrawContext (scaled GUI coords) so the wallpaper will match UI scale.
 * Make sure this mixin is the *only* overlay mixin you keep (remove the TAIL readback mixin).
 */
@Mixin(targets = "xaero.map.gui.GuiMap")
public class BackgroundMixin {

    @Inject(method = "render", at = @At("HEAD"))
    private void drawWallpaperHead(DrawContext guiGraphics, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        // wallpaper must already be registered with the texture manager
        if (XaerobackgroundClient.WALLPAPER_NATIVE == null) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return;

        int scaledW = mc.getWindow().getScaledWidth();
        int scaledH = mc.getWindow().getScaledHeight();
        if (scaledW <= 0 || scaledH <= 0) return;

        // draw full-screen wallpaper in scaled GUI coordinates
        guiGraphics.drawTexturedQuad(
                WALLPAPER_ID,
                0, 0,            // top-left
                scaledW, scaledH,// bottom-right
                0f, 1f,          // u1,u2
                0f, 1f           // v1,v2
        );
    }
}
