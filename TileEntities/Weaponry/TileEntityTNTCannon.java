/*******************************************************************************
 * @author Reika Kalseki
 * 
 * Copyright 2013
 * 
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package Reika.RotaryCraft.TileEntities.Weaponry;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import Reika.DragonAPI.Libraries.ReikaInventoryHelper;
import Reika.DragonAPI.Libraries.MathSci.ReikaMathLibrary;
import Reika.DragonAPI.Libraries.MathSci.ReikaPhysicsHelper;
import Reika.RotaryCraft.Base.TileEntity.TileEntityLaunchCannon;
import Reika.RotaryCraft.Registry.MachineRegistry;

public class TileEntityTNTCannon extends TileEntityLaunchCannon {

	public static final double gTNT = 7.5;	//Calculated from EntityTNTPrimed; vy -= 0.04, *0.98, 20x a sec

	public int delay = 20;

	public static final double torquecap = 32768D;

	public boolean isCreative = false;

	//Make torque affect max incline angle, speed max distance

	@Override
	public int getMaxLaunchVelocity() {
		return (int)Math.sqrt(power/67.5D);
	}

	@Override
	public int getMaxTheta() {
		if (torque > torquecap)
			return 90;
		int ang = 2*(int)Math.ceil(Math.toDegrees(Math.asin(torque/torquecap)));
		if (ang > 90)
			return 90;
		return ang;
	}

	@Override
	public boolean canExtractItem(int i, ItemStack itemstack, int j) {
		return false;
	}

	@Override
	public double getMaxLaunchDistance() {
		double v = this.getMaxLaunchVelocity();
		double vy = v*Math.sin(Math.toRadians(45));
		double t = vy/9.81D;
		return t*vy; //vx = vy @ 45
	}

	@Override
	public void updateEntity(World world, int x, int y, int z, int meta) {
		super.updateTileEntity();
		this.getSummativeSidedPower();
		if (power < MINPOWER)
			return;
		tickcount++;
		if (tickcount < delay)
			return;
		tickcount = 0;
		if (targetMode)
			this.calcTarget(world, x, y, z);
		if (this.canFire())
			this.fire(world, x, y, z);
		if (targetMode) {
			AxisAlignedBB box = AxisAlignedBB.getBoundingBox(x, y, z, x+1, y+1, z+1).expand(256, 256, 256);
			List in = world.getEntitiesWithinAABB(EntityTNTPrimed.class, box);
			for (int i = 0; i < in.size(); i++) {
				EntityTNTPrimed tnt = (EntityTNTPrimed)in.get(i);
				if (!tnt.onGround) {
					//Nullify air resistance
					tnt.motionX /= 0.869800000190734863D;
					tnt.motionZ /= 0.869800000190734863D;
					if (!world.isRemote)
						tnt.velocityChanged = true;
				}
				else {
					tnt.motionX = 0;
					tnt.motionZ = 0;
					if (!world.isRemote)
						tnt.velocityChanged = true;
				}
			}
		}
	}

	private void calcTarget(World world, int x, int y, int z) {
		double dx = target[0]-x-0.5;
		double dy = target[1]-y-1;
		double dz = target[2]-z-0.5;
		double dl = ReikaMathLibrary.py3d(dx, 0, dz); //Horiz distance
		double g = 8.4695*ReikaMathLibrary.doubpow(dl, 0.2701);
		if (dy > 0)
			g *= (0.8951*ReikaMathLibrary.doubpow(dy, 0.0601));
		velocity = 10;
		theta = 0;
		while (theta <= 0) {
			velocity++;
			double s = ReikaMathLibrary.intpow(velocity, 4)-g*(g*dl*dl+2*dy*velocity*velocity);
			double a = velocity*velocity+Math.sqrt(s);
			theta = (int)Math.toDegrees(Math.atan(a/(g*dl)));
			phi = (int)Math.toDegrees(Math.atan2(dz, dx));
		}
	}

	private boolean canFire() {
		boolean hasTNT = ReikaInventoryHelper.checkForItem(Block.tnt.blockID, inventory);
		return (hasTNT || isCreative);
	}

	@Override
	protected boolean fire(World world, int x, int y, int z) {
		for (int i = 0; i < 1; i++) {
			ReikaInventoryHelper.findAndDecrStack(Block.tnt.blockID, -1, inventory);
			world.playSoundEffect(x+0.5, y+0.5, z+0.5, "random.explode", 0.7F+0.3F*rand.nextFloat()*12, 0.1F*rand.nextFloat());
			world.spawnParticle("hugeexplosion", x+0.5, y+0.5, z+0.5, 1.0D, 0.0D, 0.0D);
			EntityTNTPrimed tnt = new EntityTNTPrimed(world, x+0.5, y+1.5-0.0625, z+0.5, null);
			double[] xyz = ReikaPhysicsHelper.polarToCartesian(velocity/20D, theta, phi);
			tnt.motionX = xyz[0];
			tnt.motionY = xyz[1];
			tnt.motionZ = xyz[2];
			tnt.fuse = 80;
			if (!world.isRemote) {
				tnt.velocityChanged = true;
				world.spawnEntityInWorld(tnt);
			}
		}
		return true;
	}

	/**
	 * Reads a tile entity from NBT.
	 */
	@Override
	public void readFromNBT(NBTTagCompound NBT)
	{
		super.readFromNBT(NBT);
		isCreative = NBT.getBoolean("creative");
	}

	/**
	 * Writes a tile entity to NBT.
	 */
	@Override
	public void writeToNBT(NBTTagCompound NBT)
	{
		super.writeToNBT(NBT);
		NBT.setBoolean("creative", isCreative);
	}

	@Override
	public boolean hasModelTransparency() {
		return false;
	}

	@Override
	public void animateWithTick(World world, int x, int y, int z) {

	}

	@Override
	public AxisAlignedBB getRenderBoundingBox() {
		return INFINITE_EXTENT_AABB;
	}

	@Override
	public int getMachineIndex() {
		return MachineRegistry.TNTCANNON.ordinal();
	}

	@Override
	public boolean isItemValidForSlot(int slot, ItemStack is) {
		return is.itemID == Block.tnt.blockID;
	}

	@Override
	public int getRedstoneOverride() {
		if (!this.canFire())
			return 15;
		return 0;
	}
}
