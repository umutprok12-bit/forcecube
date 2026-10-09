package com.forcecube;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ClientProxy extends CommonProxy {
    @Override
    public void preInit() {
        RenderingRegistry.registerEntityRenderingHandler(ForceCubeEntity.class, RenderForceCube::new);
        // Registering the KeyBinding is what makes it rebindable in Options > Controls
        ClientRegistry.registerKeyBinding(ClientHandler.SPAWN_KEY);
        MinecraftForge.EVENT_BUS.register(new ClientHandler());
    }
}
