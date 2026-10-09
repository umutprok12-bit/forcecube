package com.forcecube;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Client -> server: started / stopped holding the aim key (drives the torch light). */
public class PacketAimState implements IMessage {
    public boolean aiming;

    public PacketAimState() {
    }

    public PacketAimState(boolean aiming) {
        this.aiming = aiming;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        aiming = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(aiming);
    }

    public static class Handler implements IMessageHandler<PacketAimState, IMessage> {
        @Override
        public IMessage onMessage(final PacketAimState msg, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().player;
            ((WorldServer) player.world).addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    CubeLight.setAiming(player.getUniqueID(), msg.aiming);
                }
            });
            return null;
        }
    }
}
