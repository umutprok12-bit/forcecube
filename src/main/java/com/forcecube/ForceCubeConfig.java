package com.forcecube;

import net.minecraftforge.common.config.Configuration;

import java.io.File;

public class ForceCubeConfig {
    public static double range = 5.0;
    public static boolean holdToAim = true;
    public static int maxCubes = 1;
    public static boolean previewParticles = true;
    public static boolean dynamicLight = true;
    public static boolean visualRolling = true;
    public static boolean edgeTipping = true;
    public static double tipSpeed = 0.05;

    public static double gravity = 0.08;
    public static double mobPush = 0.05;
    public static double maxPushSpeed = 0.30;
    public static double toolHitStrength = 0.8;
    public static double crushMinSpeed = 0.35;
    public static double crushDamage = 20.0;

    public static void load(File file) {
        Configuration c = new Configuration(file);
        c.load();
        String g = "general";
        range = c.get(g, "range", 5.0, "How far (in blocks) you can aim the cube", 1.0, 64.0).getDouble();
        holdToAim = c.get(g, "holdToAim", true, "true = hold the key to aim, release to spawn. false = tap to spawn instantly").getBoolean();
        maxCubes = c.get(g, "maxCubesPerPlayer", 1, "Spawning past this limit removes your oldest cube", 1, 16).getInt();
        previewParticles = c.get(g, "previewParticles", true, "Pink particles inside the aim preview (disable for a little extra FPS)").getBoolean();
        dynamicLight = c.get(g, "dynamicLight", true, "Holding the key lights up the spot you aim at, like a torch (server side)").getBoolean();

        visualRolling = c.get(g, "visualRolling", true, "Cube visibly rolls/tumbles as it moves, then settles flat (client side, cosmetic)").getBoolean();

        String p = "physics";
        edgeTipping = c.get(p, "edgeTipping", true, "Cube tips off ledges when its centre is no longer supported").getBoolean();
        tipSpeed = c.get(p, "tipSpeed", 0.05, "How hard an overhanging cube slides off a ledge (blocks/tick^2)", 0.0, 0.5).getDouble();
        gravity = c.get(p, "gravity", 0.08, "Downward acceleration per tick (heavier = faster fall)", 0.0, 0.5).getDouble();
        mobPush = c.get(p, "mobPush", 0.05, "Push per tick from a mob walking into the cube (scaled by mob size). Players cannot push it by hand", 0.0, 1.0).getDouble();
        maxPushSpeed = c.get(p, "maxPushSpeed", 0.30, "Top speed the cube reaches from mob pushing (blocks/tick)", 0.05, 2.0).getDouble();
        toolHitStrength = c.get(p, "toolHitStrength", 0.8, "Launch speed when you hit the cube with a tool or sword", 0.0, 3.0).getDouble();
        crushMinSpeed = c.get(p, "crushMinSpeed", 0.35, "Fall speed (blocks/tick) needed before the cube hurts things", 0.05, 3.0).getDouble();
        crushDamage = c.get(p, "crushDamage", 20.0, "Damage per 1.0 blocks/tick of fall speed above the minimum", 0.0, 200.0).getDouble();

        if (c.hasChanged()) c.save();
    }
}
