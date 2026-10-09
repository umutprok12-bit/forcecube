package com.forcecube;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CubeSpawner {
    public static void spawn(EntityPlayerMP player) {
        World world = player.world;
        if (player.isSpectator()) return;

        CubePlacement.Result r = CubePlacement.find(world, player, 1.0F);
        if (!r.valid) return;

        UUID id = player.getUniqueID();
        List<ForceCubeEntity> owned = new ArrayList<ForceCubeEntity>();
        for (Entity e : world.loadedEntityList) {
            if (e instanceof ForceCubeEntity && id.equals(((ForceCubeEntity) e).getOwnerId())) {
                owned.add((ForceCubeEntity) e);
            }
        }
        for (ForceCubeEntity c : owned) {
            if (c.ticksExisted < 5) return; // anti-spam
        }
        owned.sort((a, b) -> b.ticksExisted - a.ticksExisted); // oldest first
        int max = Math.max(1, ForceCubeConfig.maxCubes);
        while (owned.size() >= max) {
            owned.remove(0).setDead();
        }

        ForceCubeEntity cube = new ForceCubeEntity(world);
        cube.setOwnerId(id);
        cube.setPosition(r.center.x, r.center.y - CubePlacement.HALF, r.center.z);
        world.spawnEntity(cube);

        world.playSound(null, r.center.x, r.center.y, r.center.z,
                SoundEvents.ENTITY_ENDERMEN_TELEPORT, SoundCategory.PLAYERS, 0.5F, 1.7F);
        if (world instanceof WorldServer) {
            for (int i = 0; i < 14; i++) {
                double ox = (world.rand.nextDouble() - 0.5D);
                double oy = (world.rand.nextDouble() - 0.5D);
                double oz = (world.rand.nextDouble() - 0.5D);
                // count 0 => offsets are used as the RGB colour (pink)
                ((WorldServer) world).spawnParticle(EnumParticleTypes.SPELL_MOB,
                        r.center.x + ox, r.center.y + oy, r.center.z + oz, 0, 1.0D, 0.25D, 0.75D, 1.0D);
            }
        }
    }
}
