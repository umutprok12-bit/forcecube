package com.forcecube;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;

/** Shared by the client preview and the server spawn so both agree on where the cube goes. */
public class CubePlacement {
    public static final double HALF = 0.5D;

    public static class Result {
        public final Vec3d center;
        public final boolean valid;

        public Result(Vec3d center, boolean valid) {
            this.center = center;
            this.valid = valid;
        }
    }

    public static Result find(World world, EntityPlayer player, float partialTicks) {
        double range = ForceCubeConfig.range;
        Vec3d eye = player.getPositionEyes(partialTicks);
        Vec3d look = player.getLook(partialTicks);
        Vec3d end = eye.addVector(look.x * range, look.y * range, look.z * range);

        RayTraceResult hit = world.rayTraceBlocks(eye, end, false, true, false);
        Vec3d c;
        if (hit != null && hit.typeOfHit == RayTraceResult.Type.BLOCK) {
            Vec3i n = hit.sideHit.getDirectionVec();
            c = new Vec3d(hit.hitVec.x + n.getX() * HALF, hit.hitVec.y + n.getY() * HALF, hit.hitVec.z + n.getZ() * HALF);
        } else {
            c = end; // nothing in range: float at the end of the ray and fall
        }

        // If it clips into something (e.g. aimed at a wall near the floor), nudge it up
        for (int i = 0; i <= 10; i++) {
            Vec3d t = c.addVector(0, i * 0.1D, 0);
            if (isFree(world, player, t)) return new Result(t, true);
        }
        return new Result(c, false);
    }

    public static AxisAlignedBB boxAt(Vec3d c) {
        return new AxisAlignedBB(c.x - HALF, c.y - HALF, c.z - HALF, c.x + HALF, c.y + HALF, c.z + HALF);
    }

    private static boolean isFree(World world, EntityPlayer player, Vec3d c) {
        if (c.y < HALF || c.y > 255) return false;
        AxisAlignedBB box = boxAt(c).shrink(0.001D);
        if (!world.getCollisionBoxes(null, box).isEmpty()) return false;
        if (!world.getEntitiesWithinAABB(EntityLivingBase.class, box).isEmpty()) return false;

        boolean ignoreOwn = ForceCubeConfig.maxCubes <= 1; // single-cube mode: your old cube gets replaced anyway
        for (ForceCubeEntity other : world.getEntitiesWithinAABB(ForceCubeEntity.class, box)) {
            if (ignoreOwn && player.getUniqueID().equals(other.getOwnerId())) continue;
            return false;
        }
        return true;
    }
}
