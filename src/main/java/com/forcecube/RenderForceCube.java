package com.forcecube;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

@SideOnly(Side.CLIENT)
public class RenderForceCube extends Render<ForceCubeEntity> {
    public RenderForceCube(RenderManager manager) {
        super(manager);
        this.shadowSize = 0.0F;
    }

    @Override
    public void doRender(ForceCubeEntity e, double x, double y, double z, float yaw, float partialTicks) {
        float pulse = 0.5F + 0.5F * MathHelper.sin((e.ticksExisted + partialTicks) * 0.12F);

        double[] q = CubeOrientation.nlerp(e.prevQ, e.q, partialTicks);
        // lowest corner of the rotated cube, so a tilted cube sits ON the floor instead of sinking into it
        double m10 = 2.0D * (q[1] * q[2] + q[0] * q[3]);
        double m11 = 1.0D - 2.0D * (q[1] * q[1] + q[3] * q[3]);
        double m12 = 2.0D * (q[2] * q[3] - q[0] * q[1]);
        double lift = 0.5D * (Math.abs(m10) + Math.abs(m11) + Math.abs(m12)) - 0.5D;

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y + 0.5D + lift, z);
        double w = Math.max(-1.0D, Math.min(1.0D, q[0]));
        double sn = Math.sqrt(1.0D - w * w);
        if (sn > 1.0E-4D) {
            GlStateManager.rotate((float) Math.toDegrees(2.0D * Math.acos(w)), (float) (q[1] / sn), (float) (q[2] / sn), (float) (q[3] / sn));
        }
        CubeRenderer.begin();

        // solid pink body
        CubeRenderer.normalBlend(true);
        CubeRenderer.fill(0.5D, 0.75F, 0.12F, 0.52F, 0.85F);

        // glow layers (additive, full-bright, pulsing)
        CubeRenderer.additiveBlend();
        CubeRenderer.fill(0.505D, 1.0F, 0.30F, 0.80F, 0.18F + 0.10F * pulse);
        CubeRenderer.fill(0.60D, 1.0F, 0.25F, 0.75F, 0.07F + 0.05F * pulse);
        CubeRenderer.fill(0.72D, 1.0F, 0.25F, 0.75F, 0.03F + 0.03F * pulse);
        CubeRenderer.edges(0.505D, 1.0F, 0.65F, 0.95F, 1.0F, 2.5F);

        CubeRenderer.end();
        GlStateManager.popMatrix();
    }

    @Nullable
    @Override
    protected ResourceLocation getEntityTexture(ForceCubeEntity entity) {
        return null; // no texture needed
    }
}
