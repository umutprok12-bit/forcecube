package com.forcecube;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Client -> server: "I released the spawn key". The server does its own raytrace. */
public class PacketSpawnCube implements IMessage {
    public PacketSpawnCube() {
    }

    @Override
    public void fromBytes(ByteBuf buf) {
    }

    @Override
    public void toBytes(ByteBuf buf) {
    }

    public static class Handler implements IMessageHandler<PacketSpawnCube, IMessage> {
        @Override
        public IMessage onMessage(PacketSpawnCube msg, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().player;
            ((WorldServer) player.world).addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    CubeLight.setAiming(player.getUniqueID(), false);
                    CubeSpawner.spawn(player);
                }
            });
            return null;
        }
    }
}
