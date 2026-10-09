package com.forcecube;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/** Texture-less cube drawing: cheap, full-bright, no texture binds (good for low-end GPUs). */
@SideOnly(Side.CLIENT)
public class CubeRenderer {
    private static final int[][] FACES = {
            {0, 2, 6, 4}, {1, 3, 7, 5}, {0, 1, 5, 4}, {2, 3, 7, 6}, {0, 1, 3, 2}, {4, 5, 7, 6}
    };

    public static void begin() {
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.disableCull();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);
    }

    public static void end() {
        GlStateManager.depthMask(true);
        GlStateManager.enableCull();
        GlStateManager.enableTexture2D();
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static void normalBlend(boolean writeDepth) {
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.depthMask(writeDepth);
    }

    public static void additiveBlend() {
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.depthMask(false);
    }

    private static double cx(int i, double h) { return (i & 1) == 0 ? -h : h; }
    private static double cy(int i, double h) { return (i & 2) == 0 ? -h : h; }
    private static double cz(int i, double h) { return (i & 4) == 0 ? -h : h; }

    public static void fill(double h, float r, float g, float b, float a) {
        Tessellator t = Tessellator.getInstance();
        BufferBuilder bb = t.getBuffer();
        bb.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        for (int[] f : FACES) {
            for (int i : f) {
                bb.pos(cx(i, h), cy(i, h), cz(i, h)).color(r, g, b, a).endVertex();
            }
        }
        t.draw();
    }

    public static void edges(double h, float r, float g, float b, float a, float width) {
        GlStateManager.glLineWidth(width);
        Tessellator t = Tessellator.getInstance();
        BufferBuilder bb = t.getBuffer();
        bb.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < 8; i++) {
            for (int bit = 1; bit <= 4; bit <<= 1) {
                if ((i & bit) == 0) {
                    int j = i | bit;
                    bb.pos(cx(i, h), cy(i, h), cz(i, h)).color(r, g, b, a).endVertex();
                    bb.pos(cx(j, h), cy(j, h), cz(j, h)).color(r, g, b, a).endVertex();
                }
            }
        }
        t.draw();
        GlStateManager.glLineWidth(1.0F);
    }
}
