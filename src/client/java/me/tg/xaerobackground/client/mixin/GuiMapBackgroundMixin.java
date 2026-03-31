package me.tg.xaerobackground.client.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.Framebuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xaero.map.gui.GuiMap;
import xaero.map.graphics.TextureUtils;

/**
 * Redirect the specific TextureUtils.clearRenderTarget(FBO, color, depth) call in GuiMap.render
 * and convert the black clear used by Xaero for that FBO into a transparent clear so wallpaper shows through.
 *
 * NOTE: this redirect targets the exact invocation in GuiMap.render that calls TextureUtils.clearRenderTarget(primaryScaleFBO, -16777216, 1.0F).
 * If Xaero changes that call signature or the numeric literal, you'll need to update the target.
 */
@Mixin(GuiMap.class)
public class GuiMapBackgroundMixin {

    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    // This target string should match the invocation in the decompiled code you pasted earlier.
                    target = "Lxaero/map/graphics/TextureUtils;clearRenderTarget(Lnet/minecraft/client/gl/Framebuffer;IF)V"
            )
    )
    private void redirectClearRenderTarget(Framebuffer renderTarget, int color, float depth) {
        // Only modify the exact call that clears to full opaque black with depth 1.0,
        // otherwise keep normal behaviour to avoid changing unrelated clears.
        if (color == -16777216 && depth == 1.0F) {
            // clear to fully transparent black instead of opaque black
            int transparentBlack = 0x00000000;
            // Use the same API call as TextureUtils would (device command encoder)
            RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(
                    renderTarget.getColorAttachment(),
                    transparentBlack,
                    renderTarget.getDepthAttachment(),
                    (double)depth
            );
        } else {
            // fallback: perform the original clear (same as TextureUtils)
            RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(
                    renderTarget.getColorAttachment(),
                    color,
                    renderTarget.getDepthAttachment(),
                    (double)depth
            );
        }
    }
}
