package me.tg.xaerobackground.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.gui.DrawContext;

@Mixin(targets = "xaero.map.gui.GuiMap")
public class OverLayMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void drawWallpaperInstead(DrawContext guiGraphics, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        // Texture path
        Identifier wallpaper = Identifier.of("xaerobackground", "textures/border.png");

        // Get window dimensions
        int width = MinecraftClient.getInstance().getWindow().getScaledWidth();
        int height = MinecraftClient.getInstance().getWindow().getScaledHeight();

        // Draw full-screen wallpaper
        guiGraphics.drawTexturedQuad(
                wallpaper,
                0, 0,            // top-left corner
                width, height,   // bottom-right corner
                0f, 1f,          // u coordinates (full texture width)
                0f, 1f           // v coordinates (full texture height)
        );
    }
}
