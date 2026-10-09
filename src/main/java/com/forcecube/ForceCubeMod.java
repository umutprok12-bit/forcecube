package com.forcecube;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.relauncher.Side;

@Mod(modid = ForceCubeMod.MODID, name = ForceCubeMod.NAME, version = ForceCubeMod.VERSION, acceptedMinecraftVersions = "[1.12.2]")
public class ForceCubeMod {
    public static final String MODID = "forcecube";
    public static final String NAME = "Force Cube";
    public static final String VERSION = "1.0.0";

    @Mod.Instance(MODID)
    public static ForceCubeMod instance;

    @SidedProxy(clientSide = "com.forcecube.ClientProxy", serverSide = "com.forcecube.CommonProxy")
    public static CommonProxy proxy;

    public static SimpleNetworkWrapper NETWORK;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        ForceCubeConfig.load(event.getSuggestedConfigurationFile());

        NETWORK = NetworkRegistry.INSTANCE.newSimpleChannel(MODID);
        NETWORK.registerMessage(PacketSpawnCube.Handler.class, PacketSpawnCube.class, 0, Side.SERVER);
        NETWORK.registerMessage(PacketAimState.Handler.class, PacketAimState.class, 1, Side.SERVER);
        MinecraftForge.EVENT_BUS.register(new CubeLight());

        // range 64, update every tick, send velocity updates
        EntityRegistry.registerModEntity(new ResourceLocation(MODID, "force_cube"),
                ForceCubeEntity.class, "force_cube", 0, this, 64, 1, true);

        proxy.preInit();
    }

    @Mod.EventHandler
    public void serverStopping(FMLServerStoppingEvent event) {
        CubeLight.clearAll(); // remove any torch-light blocks before the world saves
    }
}
