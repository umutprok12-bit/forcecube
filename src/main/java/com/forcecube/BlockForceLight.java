package com.forcecube;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Random;

/** Invisible, non-solid light source. Placed/removed by CubeLight while a player aims. Cleans itself up if orphaned. */
public class BlockForceLight extends Block {
    public BlockForceLight() {
        super(Material.CIRCUITS);
        setRegistryName(ForceCubeMod.MODID, "force_light");
        setUnlocalizedName(ForceCubeMod.MODID + ".force_light");
        setLightLevel(14.0F / 15.0F); // torch brightness
        setLightOpacity(0);
    }

    @Override public boolean isOpaqueCube(IBlockState state) { return false; }
    @Override public boolean isFullCube(IBlockState state) { return false; }
    @Override public EnumBlockRenderType getRenderType(IBlockState state) { return EnumBlockRenderType.INVISIBLE; }
    @Override public BlockRenderLayer getBlockLayer() { return BlockRenderLayer.CUTOUT; }

    @Nullable
    @Override
    public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) { return NULL_AABB; }

    @Override public boolean canCollideCheck(IBlockState state, boolean hitIfLiquid) { return false; }
    @Override public boolean isReplaceable(IBlockAccess world, BlockPos pos) { return true; }
    @Override public Item getItemDropped(IBlockState state, Random rand, int fortune) { return Items.AIR; }
    @Override public int quantityDropped(Random random) { return 0; }

    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }

    @Override
    public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
        if (!world.isRemote) world.scheduleUpdate(pos, this, 10);
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
        if (world.isRemote) return;
        if (CubeLight.isActive(world, pos)) {
            world.scheduleUpdate(pos, this, 10);
        } else {
            world.setBlockToAir(pos); // nobody is aiming here any more (crash / restart leftovers)
        }
    }
}
