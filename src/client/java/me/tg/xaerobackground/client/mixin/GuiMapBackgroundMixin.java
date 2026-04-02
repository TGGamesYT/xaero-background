package me.tg.xaerobackground.client.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xaero.map.graphics.ImprovedFramebuffer;
import xaero.map.gui.GuiMap;
import me.tg.xaerobackground.client.XaerobackgroundClient;

import java.util.OptionalInt;

/**
 * After Xaero finishes rendering all tile textures into its FBO (primaryScaleFBO)
 * but before it restores the MC main framebuffer, run a post-process pass that
 * replaces every very-dark pixel — pure black (unexplored) or dark hover-highlight
 * blended over black — with the corresponding wallpaper pixel.
 *
 * The pass uses a ping-pong strategy:
 *   1. Render (mapFbo + wallpaper) -> tempFbo  using the "wallpaper_replace_black" shader.
 *   2. Blit tempFbo -> mapFbo               using the standard ENTITY_OUTLINE_BLIT pipeline.
 *
 * Xaero then composites mapFbo onto the screen normally, now with wallpaper
 * visible wherever tiles had unexplored sub-areas.
 */
@Mixin(GuiMap.class)
public class GuiMapBackgroundMixin {

    /**
     * Custom pipeline: two-sampler blit that replaces very-dark pixels with wallpaper.
     *   InSampler        = Xaero's rendered map FBO texture
     *   WallpaperSampler = wallpaper texture
     */
    private static final RenderPipeline WALLPAPER_REPLACE_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder()
                    .withLocation(Identifier.of("xaerobackground", "pipeline/wallpaper_replace_black"))
                    .withVertexShader("core/blit_screen")
                    .withFragmentShader(Identifier.of("xaerobackground", "core/wallpaper_replace_black"))
                    .withSampler("InSampler")
                    .withSampler("WallpaperSampler")
                    .withoutBlend()
                    .withDepthWrite(false)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withColorWrite(true, true)
                    .withVertexFormat(VertexFormats.POSITION, VertexFormat.DrawMode.QUADS)
                    .build()
    );

    /** Ping-pong FBO; lazily created and resized to match Xaero's FBO. */
    private static SimpleFramebuffer tempFbo = null;

    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/map/graphics/ImprovedFramebuffer;bindDefaultFramebuffer(Lnet/minecraft/client/MinecraftClient;)V"
            )
    )
    private void redirectBindDefaultFramebuffer(ImprovedFramebuffer mapFbo, MinecraftClient mc) {
        if (XaerobackgroundClient.WALLPAPER_NATIVE != null && mc.getTextureManager() != null) {
            AbstractTexture wallpaperTex = mc.getTextureManager().getTexture(XaerobackgroundClient.WALLPAPER_ID);
            GpuTextureView wallpaperView = wallpaperTex.getGlTextureView();

            if (wallpaperView != null) {
                // Ensure tempFbo matches mapFbo dimensions
                int w = mapFbo.textureWidth;
                int h = mapFbo.textureHeight;
                if (tempFbo == null || tempFbo.textureWidth != w || tempFbo.textureHeight != h) {
                    if (tempFbo != null) tempFbo.delete();
                    tempFbo = new SimpleFramebuffer("xaerobackground_temp", w, h, false);
                }

                var seqBuf = RenderSystem.getSequentialBuffer(VertexFormat.DrawMode.QUADS);
                var indexBuffer = seqBuf.getIndexBuffer(6);
                var vertexBuffer = RenderSystem.getQuadVertexBuffer();

                // Step 1: post-process mapFbo + wallpaper → tempFbo
                var renderPass = RenderSystem.getDevice().createCommandEncoder()
                        .createRenderPass(
                                () -> "xaerobackground_replace_black",
                                tempFbo.getColorAttachmentView(),
                                OptionalInt.empty()
                        );
                try {
                    renderPass.setPipeline(WALLPAPER_REPLACE_PIPELINE);
                    RenderSystem.bindDefaultUniforms(renderPass);
                    renderPass.setVertexBuffer(0, vertexBuffer);
                    renderPass.setIndexBuffer(indexBuffer, seqBuf.getIndexType());
                    renderPass.bindSampler("InSampler", mapFbo.getColorAttachmentView());
                    renderPass.bindSampler("WallpaperSampler", wallpaperView);
                    renderPass.drawIndexed(0, 0, 6, 1);
                } finally {
                    renderPass.close();
                }

                // Step 2: copy processed result back into Xaero's FBO
                tempFbo.drawBlit(mapFbo.getColorAttachmentView());
            }
        }

        // Restore original framebuffer
        mapFbo.bindDefaultFramebuffer(mc);
    }
}
