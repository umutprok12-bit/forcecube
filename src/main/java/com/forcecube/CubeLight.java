package com.forcecube;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Server-side "torch" light. While a player holds the aim key, an invisible light block follows
 * the spot they're aiming at (updates at most every 2 ticks, and only when the block position changes).
 */
public class CubeLight {
    private static class Entry {
        final int dim;
        final BlockPos pos;

        Entry(int dim, BlockPos pos) {
            this.dim = dim;
            this.pos = pos;
        }
    }

    private static final Map<UUID, Entry> LIGHTS = new HashMap<UUID, Entry>();
    private static final Set<UUID> AIMING = new HashSet<UUID>();

    public static void setAiming(UUID id, boolean aiming) {
        if (aiming) {
            AIMING.add(id);
        } else {
            AIMING.remove(id);
            removeLight(id);
        }
    }

    public static boolean isActive(World world, BlockPos pos) {
        int dim = world.provider.getDimension();
        for (Entry e : LIGHTS.values()) {
            if (e.dim == dim && e.pos.equals(pos)) return true;
        }
        return false;
    }

    private static void removeLight(UUID id) {
        Entry e = LIGHTS.remove(id);
        if (e == null) return;
        World w = DimensionManager.getWorld(e.dim);
        if (w != null && w.getBlockState(e.pos).getBlock() == ModBlocks.LIGHT && !isActive(w, e.pos)) {
            w.setBlockToAir(e.pos);
        }
    }

    public static void clearAll() {
        for (UUID id : new ArrayList<UUID>(LIGHTS.keySet())) removeLight(id);
        AIMING.clear();
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side != Side.SERVER) return;
        EntityPlayer p = event.player;
        UUID id = p.getUniqueID();
        if (!AIMING.contains(id)) return;

        if (!ForceCubeConfig.dynamicLight || p.isSpectator() || p.isDead) {
            removeLight(id);
            return;
        }
        if (p.ticksExisted % 2 != 0) return;

        World w = p.world;
        int dim = w.provider.getDimension();
        BlockPos np = new BlockPos(CubePlacement.find(w, p, 1.0F).center);

        Entry cur = LIGHTS.get(id);
        if (cur != null && cur.dim == dim && cur.pos.equals(np) && w.getBlockState(np).getBlock() == ModBlocks.LIGHT) {
            return; // still in the right place
        }
        removeLight(id);

        IBlockState st = w.getBlockState(np);
        if (w.isAirBlock(np) || st.getBlock() == ModBlocks.LIGHT) {
            w.setBlockState(np, ModBlocks.LIGHT.getDefaultState(), 2);
            LIGHTS.put(id, new Entry(dim, np));
        }
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        setAiming(event.player.getUniqueID(), false);
    }
}
