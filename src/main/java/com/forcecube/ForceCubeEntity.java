package com.forcecube;

import com.google.common.base.Optional;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Heavy, solid 1x1x1 physics cube.
 * - Players can NOT push it by hand. Tools/swords launch it, mobs can shove it.
 * - Crushes living things it falls onto.
 * Server is authoritative; the client only interpolates positions it receives.
 */
public class ForceCubeEntity extends Entity {
    public static final DamageSource CRUSH = new DamageSource("forcecube_crush");

    private static final DataParameter<Optional<UUID>> OWNER =
            EntityDataManager.<Optional<UUID>>createKey(ForceCubeEntity.class, DataSerializers.OPTIONAL_UNIQUE_ID);

    private int lerpSteps;
    private double lerpX, lerpY, lerpZ;
    private final Set<Integer> crushed = new HashSet<Integer>();

    // Client-only visual orientation (quaternion w,x,y,z). Purely cosmetic; never synced.
    public double[] q = CubeOrientation.identity();
    public double[] prevQ = CubeOrientation.identity();
    private int restTicks;

    public ForceCubeEntity(World worldIn) {
        super(worldIn);
        this.setSize(1.0F, 1.0F);
        this.preventEntitySpawning = true; // blocks can't be placed inside it
    }

    @Override
    protected void entityInit() {
        this.dataManager.register(OWNER, Optional.<UUID>absent());
    }

    @Nullable
    public UUID getOwnerId() {
        return this.dataManager.get(OWNER).orNull();
    }

    public void setOwnerId(@Nullable UUID id) {
        this.dataManager.set(OWNER, Optional.fromNullable(id));
    }

    // ---------- collision ----------

    @Nullable
    @Override
    public AxisAlignedBB getCollisionBoundingBox() {
        return this.getEntityBoundingBox(); // players/mobs/other cubes collide with it and can stand on it
    }

    @Override
    public boolean canBeCollidedWith() {
        return !this.isDead;
    }

    @Override
    public boolean canBePushed() {
        return false;
    }

    @Override
    protected boolean canTriggerWalking() {
        return false;
    }

    // ---------- physics ----------

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (this.world.isRemote) {
            if (this.lerpSteps > 0) {
                double nx = this.posX + (this.lerpX - this.posX) / this.lerpSteps;
                double ny = this.posY + (this.lerpY - this.posY) / this.lerpSteps;
                double nz = this.posZ + (this.lerpZ - this.posZ) / this.lerpSteps;
                --this.lerpSteps;
                this.setPosition(nx, ny, nz);
            }
            if (ForceCubeConfig.visualRolling) this.updateVisualRotation();
            return;
        }

        if (!this.hasNoGravity()) {
            this.motionY -= ForceCubeConfig.gravity;
        }
        if (this.isInWater()) {
            this.motionX *= 0.85D;
            this.motionZ *= 0.85D;
            this.motionY = this.motionY * 0.85D + ForceCubeConfig.gravity * 0.5D;
        }

        double fallSpeed = -this.motionY;
        if (fallSpeed > ForceCubeConfig.crushMinSpeed) {
            this.crushEntities(fallSpeed);
        }
        this.pushByMobs();
        this.tipOffEdges();

        this.move(MoverType.SELF, this.motionX, this.motionY, this.motionZ);

        if (this.onGround) {
            if (fallSpeed > ForceCubeConfig.crushMinSpeed) {
                this.world.playSound(null, this.posX, this.posY, this.posZ, SoundEvents.BLOCK_ANVIL_LAND,
                        SoundCategory.BLOCKS, (float) Math.min(1.0D, fallSpeed), 0.8F);
            }
            this.crushed.clear();
        }

        // Heavy: lots of ground friction, so it only slides a little (ice still slides further)
        double friction = 0.98D;
        if (this.onGround) {
            BlockPos below = new BlockPos(this.posX, this.getEntityBoundingBox().minY - 1.0D, this.posZ);
            IBlockState st = this.world.getBlockState(below);
            float slip = st.getBlock().getSlipperiness(st, this.world, below, this);
            friction = 0.3D + slip * 0.4D; // normal ~0.54, ice ~0.69
        }
        this.motionX *= friction;
        this.motionZ *= friction;
        this.motionY *= 0.98D;
        if (Math.abs(this.motionX) < 0.003D) this.motionX = 0.0D;
        if (Math.abs(this.motionZ) < 0.003D) this.motionZ = 0.0D;

        this.pushOutEntities();
    }

    /** Cube rolls like it's tumbling over its edges: 90 degrees per block travelled. */
    private void updateVisualRotation() {
        this.prevQ = this.q.clone();
        double dx = this.posX - this.prevPosX;
        double dz = this.posZ - this.prevPosZ;
        double dy = this.posY - this.prevPosY;
        double d = Math.sqrt(dx * dx + dz * dz);
        if (d > 0.0005D) {
            this.restTicks = 0;
            double angle = d * (Math.PI / 2.0D);
            // axis perpendicular to the travel direction, so the top leans forward
            this.q = CubeOrientation.mul(CubeOrientation.axisAngle(dz / d, 0.0D, -dx / d, angle), this.q);
        } else if (Math.abs(dy) < 0.001D) {
            if (++this.restTicks > 3) { // came to rest: settle flat on the nearest face
                this.q = CubeOrientation.nlerp(this.q, CubeOrientation.nearest(this.q), 0.25D);
            }
        } else {
            this.restTicks = 0;
        }
    }

    private boolean supportedAt(double x, double z) {
        double minY = this.getEntityBoundingBox().minY;
        AxisAlignedBB probe = new AxisAlignedBB(x - 0.05D, minY - 0.05D, z - 0.05D, x + 0.05D, minY + 0.001D, z + 0.05D);
        return !this.world.getCollisionBoxes(this, probe).isEmpty();
    }

    /** If the centre of the cube hangs past a ledge it tips off instead of balancing on the edge. */
    private void tipOffEdges() {
        if (!ForceCubeConfig.edgeTipping || !this.onGround) return;
        if (this.supportedAt(this.posX, this.posZ)) return;
        double vx = 0.0D, vz = 0.0D;
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4.0D;
            double cx = Math.cos(a), cz = Math.sin(a);
            if (!this.supportedAt(this.posX + cx * 0.45D, this.posZ + cz * 0.45D)) {
                vx += cx;
                vz += cz;
            }
        }
        double len = Math.sqrt(vx * vx + vz * vz);
        if (len < 0.01D) return;
        this.motionX += vx / len * ForceCubeConfig.tipSpeed;
        this.motionZ += vz / len * ForceCubeConfig.tipSpeed;
    }

    /** Hurts living things in the path of a fast fall. */
    private void crushEntities(double fallSpeed) {
        AxisAlignedBB sweep = this.getEntityBoundingBox().expand(0.0D, this.motionY, 0.0D); // extends downward
        for (EntityLivingBase e : this.world.getEntitiesWithinAABB(EntityLivingBase.class, sweep)) {
            if (e.noClip || this.crushed.contains(e.getEntityId())) continue;
            float dmg = (float) ((fallSpeed - ForceCubeConfig.crushMinSpeed) * ForceCubeConfig.crushDamage);
            if (dmg < 1.0F) continue;
            e.hurtResistantTime = 0;
            e.attackEntityFrom(CRUSH, dmg);
            this.crushed.add(e.getEntityId());
        }
    }

    /** Mobs/NPCs walking into the cube shove it (bigger mobs shove harder). Players cannot. */
    private void pushByMobs() {
        AxisAlignedBB bb = this.getEntityBoundingBox();
        AxisAlignedBB box = new AxisAlignedBB(bb.minX - 0.2D, bb.minY + 0.1D, bb.minZ - 0.2D, bb.maxX + 0.2D, bb.maxY - 0.1D, bb.maxZ + 0.2D);
        boolean pushed = false;
        for (EntityLiving mob : this.world.getEntitiesWithinAABB(EntityLiving.class, box)) {
            if (mob.moveForward == 0.0F && mob.moveStrafing == 0.0F) continue;
            double yaw = Math.toRadians(mob.rotationYaw);
            double sin = Math.sin(yaw), cos = Math.cos(yaw);
            double dx = mob.moveStrafing * cos - mob.moveForward * sin;
            double dz = mob.moveForward * cos + mob.moveStrafing * sin;
            double len = Math.sqrt(dx * dx + dz * dz);
            if (len < 0.01D) continue;
            dx /= len;
            dz /= len;
            double tx = this.posX - mob.posX, tz = this.posZ - mob.posZ;
            double tl = Math.sqrt(tx * tx + tz * tz);
            if (tl < 1.0E-4D || (dx * tx + dz * tz) / tl < 0.5D) continue; // must be walking toward it

            double mass = MathHelper.clamp(mob.width * mob.height / 1.1D, 0.5D, 3.0D);
            double s = ForceCubeConfig.mobPush * mass;
            this.motionX += dx * s;
            this.motionZ += dz * s;
            pushed = true;
        }
        if (pushed) {
            double sp = Math.sqrt(this.motionX * this.motionX + this.motionZ * this.motionZ);
            if (sp > ForceCubeConfig.maxPushSpeed) {
                double f = ForceCubeConfig.maxPushSpeed / sp;
                this.motionX *= f;
                this.motionZ *= f;
            }
        }
    }

    /** Stops the cube from squishing mobs/players it slides into. */
    private void pushOutEntities() {
        AxisAlignedBB box = this.getEntityBoundingBox().shrink(0.02D);
        for (EntityLivingBase e : this.world.getEntitiesWithinAABB(EntityLivingBase.class, box)) {
            if (e.noClip) continue;
            double dx = e.posX - this.posX;
            double dz = e.posZ - this.posZ;
            double len = Math.sqrt(dx * dx + dz * dz);
            if (len < 1.0E-4D) {
                dx = 1.0D;
                dz = 0.0D;
                len = 1.0D;
            }
            e.motionX += dx / len * 0.1D;
            e.motionZ += dz / len * 0.1D;
            e.velocityChanged = true;
        }
    }

    // ---------- interaction ----------

    private static boolean isTool(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.getItem() instanceof ItemSword || !stack.getItem().getToolClasses(stack).isEmpty();
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (source == DamageSource.OUT_OF_WORLD) {
            this.setDead();
            return true;
        }
        Entity attacker = source.getTrueSource();
        if (attacker instanceof EntityPlayer) {
            // Bare hands do nothing - it's too heavy. Tools and swords can launch it.
            if (!isTool(((EntityPlayer) attacker).getHeldItemMainhand())) return false;
            if (!this.world.isRemote) {
                Vec3d look = attacker.getLookVec();
                double h = Math.sqrt(look.x * look.x + look.z * look.z);
                if (h > 1.0E-4D) {
                    this.motionX += look.x / h * ForceCubeConfig.toolHitStrength;
                    this.motionZ += look.z / h * ForceCubeConfig.toolHitStrength;
                    if (this.onGround) this.motionY += 0.1D;
                }
            }
            return true;
        }
        return false; // fire, explosions, etc. don't destroy it
    }

    @Override
    public boolean processInitialInteract(EntityPlayer player, EnumHand hand) {
        if (player.isSneaking()) { // shift + right click removes it
            if (!this.world.isRemote) this.setDead();
            return true;
        }
        return false;
    }

    // ---------- client smoothing ----------

    @SideOnly(Side.CLIENT)
    @Override
    public void setPositionAndRotationDirect(double x, double y, double z, float yaw, float pitch, int posRotationIncrements, boolean teleport) {
        this.lerpX = x;
        this.lerpY = y;
        this.lerpZ = z;
        this.lerpSteps = 3;
    }

    // ---------- save/load ----------

    @Override
    protected void readEntityFromNBT(NBTTagCompound c) {
        if (c.hasUniqueId("Owner")) this.setOwnerId(c.getUniqueId("Owner"));
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound c) {
        UUID id = this.getOwnerId();
        if (id != null) c.setUniqueId("Owner", id);
    }
}
