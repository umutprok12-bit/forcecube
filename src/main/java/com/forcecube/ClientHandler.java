package com.forcecube;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

@SideOnly(Side.CLIENT)
public class ClientHandler {
    /** Shows up in Options > Controls under "Force Cube" - rebind it there. */
    public static final KeyBinding SPAWN_KEY = new KeyBinding("key.forcecube.spawn", Keyboard.KEY_V, "key.categories.forcecube");

    private boolean aiming;
    private boolean sentAiming;
    private CubePlacement.Result preview;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP p = mc.player;
        if (p == null || mc.world == null) {
            aiming = false;
            sentAiming = false;
            preview = null;
            return;
        }
        if (mc.currentScreen != null || p.isSpectator()) {
            aiming = false;
            preview = null;
            SPAWN_KEY.isPressed(); // drain queued presses
            syncAim();
            return;
        }

        handleKey();
        if (aiming) spawnPreviewParticles(mc, p);
        handleRiding(mc, p);
        syncAim();
    }

    /** Tells the server when you start/stop holding the key so it can move the torch light. */
    private void syncAim() {
        if (aiming != sentAiming) {
            sentAiming = aiming;
            ForceCubeMod.NETWORK.sendToServer(new PacketAimState(aiming));
        }
    }

    private void handleKey() {
        boolean tapped = SPAWN_KEY.isPressed();
        boolean down = SPAWN_KEY.isKeyDown();
        if (ForceCubeConfig.holdToAim) {
            if (tapped || down) aiming = true;
            if (aiming && !down) {
                aiming = false;
                preview = null;
                ForceCubeMod.NETWORK.sendToServer(new PacketSpawnCube());
            }
        } else {
            aiming = false;
            preview = null;
            if (tapped) ForceCubeMod.NETWORK.sendToServer(new PacketSpawnCube());
        }
    }

    private void spawnPreviewParticles(Minecraft mc, EntityPlayerSP p) {
        if (!ForceCubeConfig.previewParticles || preview == null || !preview.valid) return;
        if (p.ticksExisted % 2 != 0) return;
        double ox = (mc.world.rand.nextDouble() - 0.5D);
        double oy = (mc.world.rand.nextDouble() - 0.5D);
        double oz = (mc.world.rand.nextDouble() - 0.5D);
        // REDSTONE particle: the "speed" values are the RGB colour (pink)
        mc.world.spawnParticle(EnumParticleTypes.REDSTONE, preview.center.x + ox, preview.center.y + oy, preview.center.z + oz, 1.0D, 0.25D, 0.75D);
    }

    /** Carries the local player along when the cube is moved while they stand on it. */
    private void handleRiding(Minecraft mc, EntityPlayerSP p) {
        if (!p.onGround) return;
        AxisAlignedBB bb = p.getEntityBoundingBox();
        AxisAlignedBB feet = new AxisAlignedBB(bb.minX, bb.minY - 0.05D, bb.minZ, bb.maxX, bb.minY + 0.02D, bb.maxZ);
        for (ForceCubeEntity cube : mc.world.getEntitiesWithinAABB(ForceCubeEntity.class, feet)) {
            if (Math.abs(cube.getEntityBoundingBox().maxY - bb.minY) > 0.06D) continue;
            double dx = cube.posX - cube.prevPosX;
            double dz = cube.posZ - cube.prevPosZ;
            if (dx == 0.0D && dz == 0.0D) continue;
            AxisAlignedBB moved = bb.offset(dx, 0.0D, dz).shrink(0.001D);
            if (mc.world.getCollisionBoxes(p, moved).isEmpty()) {
                p.setPosition(p.posX + dx, p.posY, p.posZ + dz);
                p.prevPosX += dx;
                p.prevPosZ += dz;
                p.lastTickPosX += dx;
                p.lastTickPosZ += dz;
            }
            break;
        }
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (!aiming) return;
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP p = mc.player;
        if (p == null || mc.world == null) return;

        float pt = event.getPartialTicks();
        preview = CubePlacement.find(mc.world, p, pt); // recomputed per frame so it follows the crosshair smoothly

        double px = p.lastTickPosX + (p.posX - p.lastTickPosX) * pt;
        double py = p.lastTickPosY + (p.posY - p.lastTickPosY) * pt;
        double pz = p.lastTickPosZ + (p.posZ - p.lastTickPosZ) * pt;
        float pulse = 0.5F + 0.5F * (float) Math.sin((mc.world.getTotalWorldTime() + pt) * 0.25D);
        float r = 1.0F, g = preview.valid ? 0.30F : 0.10F, b = preview.valid ? 0.80F : 0.10F;

        GlStateManager.pushMatrix();
        GlStateManager.translate(preview.center.x - px, preview.center.y - py, preview.center.z - pz);
        CubeRenderer.begin();
        CubeRenderer.additiveBlend();
        CubeRenderer.fill(0.5D, r, g, b, 0.12F + 0.10F * pulse);
        CubeRenderer.fill(0.58D, r, g, b, 0.04F + 0.04F * pulse);
        CubeRenderer.edges(0.5D, r, preview.valid ? 0.65F : 0.2F, preview.valid ? 0.95F : 0.2F, 1.0F, 2.5F);
        CubeRenderer.end();
        GlStateManager.popMatrix();
    }
}
