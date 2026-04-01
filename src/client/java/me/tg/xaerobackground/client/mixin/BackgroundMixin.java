package me.tg.xaerobackground.client.mixin;

import net.minecraft.client.gui.DrawContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.map.gui.GuiMap;

/**
 * Draw wallpaper behind everything at the very start of GuiMap.render.
 * This uses DrawContext (scaled GUI coords) so the wallpaper will match UI scale.
 * Make sure this mixin is the *only* overlay mixin you keep (remove the TAIL readback mixin).
 */
@Mixin(targets = "xaero.map.gui.GuiMap")
public class BackgroundMixin {

    @Inject(method = "render", at = @At("HEAD"))
    private void drawWallpaperHead(DrawContext guiGraphics, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        // Wallpaper is now drawn directly into Xaero's FBO by GuiMapBackgroundMixin
        // before tiles are rendered, so it only appears where chunks are unloaded.
        // Drawing it here in screen-space (via batched DrawContext) would flush after
        // Xaero's FBO composite and incorrectly cover loaded chunks as well.
    }
}
