/*******************************************************************************
 * @author Reika Kalseki
 * 
 * Copyright 2013
 * 
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package Reika.RotaryCraft.TileEntities.Transmission;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import Reika.DragonAPI.Libraries.ReikaInventoryHelper;
import Reika.DragonAPI.Libraries.IO.ReikaSoundHelper;
import Reika.DragonAPI.Libraries.MathSci.ReikaMathLibrary;
import Reika.DragonAPI.Libraries.Registry.ReikaParticleHelper;
import Reika.DragonAPI.Libraries.World.ReikaWorldHelper;
import Reika.RotaryCraft.RotaryConfig;
import Reika.RotaryCraft.API.CVTController;
import Reika.RotaryCraft.API.PowerGenerator;
import Reika.RotaryCraft.API.ShaftMerger;
import Reika.RotaryCraft.API.ShaftPowerEmitter;
import Reika.RotaryCraft.Auxiliary.ItemStacks;
import Reika.RotaryCraft.Auxiliary.PowerSourceList;
import Reika.RotaryCraft.Auxiliary.Interfaces.InertIInv;
import Reika.RotaryCraft.Auxiliary.Interfaces.SimpleProvider;
import Reika.RotaryCraft.Base.TileEntity.RotaryCraftTileEntity;
import Reika.RotaryCraft.Base.TileEntity.TileEntity1DTransmitter;
import Reika.RotaryCraft.Base.TileEntity.TileEntityIOMachine;
import Reika.RotaryCraft.Registry.MachineRegistry;

public class TileEntityAdvancedGear extends TileEntity1DTransmitter implements ISidedInventory, PowerGenerator {

	private boolean isReleasing = false;
	public int releaseTorque = 0;
	public int releaseOmega = 0;
	/** Stored energy, in joules */
	private long energy;

	private boolean lastPower;

	public static final int WORMRATIO = 16;

	private CVTController controller;

	private ItemStack[] belts = new ItemStack[31];

	private boolean hasLubricant = false;

	private CVTState[] cvtState = new CVTState[2];

	public boolean isRedstoneControlled;

	public void setController(CVTController c) {
		controller = c;
	}

	public enum GearType {
		WORM(),
		CVT(),
		COIL();

		public static final GearType[] list = values();

		public boolean isLubricated() {
			return this == CVT;
		}

		public boolean hasLosses() {
			return this == WORM;
		}
	}

	public GearType getType() {
		return GearType.list[this.getBlockMetadata()/4];
	}

	public void addLubricant() {
		hasLubricant = true;
	}

	public boolean hasLubricant() {
		return hasLubricant;
	}

	//-ve ratio is torque mode for cvt
	@Override
	public void readFromSplitter(TileEntitySplitter spl) { //Complex enough to deserve its own function
		int sratio = spl.getRatioFromMode();
		if (sratio == 0)
			return;
		boolean favorbent = false;
		if (sratio < 0) {
			favorbent = true;
			sratio = -sratio;
		}
		if (this.getType() == GearType.WORM || this.getType() == GearType.CVT && this.getEffectiveRatio() < 0) {
			if (xCoord == spl.writeinline[0] && zCoord == spl.writeinline[1]) { //We are the inline
				omega = -(int)(spl.omega/this.getEffectiveRatio()*this.getPowerLossFraction(spl.omega)); //omega always constant
				if (sratio == 1) { //Even split, favorbent irrelevant
					torque = -(int)(spl.torque/2*this.getEffectiveRatio());
					return;
				}
				if (favorbent) {
					torque = -(int)(spl.torque/sratio*this.getEffectiveRatio());
				}
				else {
					torque = -(int)(this.getEffectiveRatio()*(int)(spl.torque*((sratio-1D)/(sratio))));
				}
			}
			else if (xCoord == spl.writebend[0] && zCoord == spl.writebend[1]) { //We are the bend
				omega = -(int)(spl.omega/this.getEffectiveRatio()*this.getPowerLossFraction(spl.omega)); //omega always constant
				if (sratio == 1) { //Even split, favorbent irrelevant
					torque = -(int)(spl.torque/2*this.getEffectiveRatio());
					return;
				}
				if (favorbent) {
					torque = -(int)(this.getEffectiveRatio()*(int)(spl.torque*((sratio-1D)/(sratio))));
				}
				else {
					torque = -(int)(spl.torque/sratio*this.getEffectiveRatio());
				}
			}
			else { //We are not one of its write-to blocks
				torque = 0;
				omega = 0;
				power = 0;
				return;
			}
		}
		else {
			if (xCoord == spl.writeinline[0] && zCoord == spl.writeinline[1]) { //We are the inline
				omega = (int)(spl.omega*this.getEffectiveRatio()*this.getPowerLossFraction(spl.omega)); //omega always constant
				if (sratio == 1) { //Even split, favorbent irrelevant
					torque = (int)(spl.torque/2/this.getEffectiveRatio());
					return;
				}
				if (favorbent) {
					torque = (int)(spl.torque/sratio/this.getEffectiveRatio());
				}
				else {
					torque = (int)((spl.torque*((sratio-1D))/sratio)/(this.getEffectiveRatio()));
				}
			}
			else if (xCoord == spl.writebend[0] && zCoord == spl.writebend[1]) { //We are the bend
				omega = (int)(spl.omega*this.getEffectiveRatio()*this.getPowerLossFraction(spl.omega)); //omega always constant
				if (sratio == 1) { //Even split, favorbent irrelevant
					torque = (int)(spl.torque/2/this.getEffectiveRatio());
					return;
				}
				if (favorbent) {
					torque = (int)(spl.torque*((sratio-1D)/(sratio))/this.getEffectiveRatio());
				}
				else {
					torque = (int)(spl.torque/sratio/this.getEffectiveRatio());
				}
			}
			else { //We are not one of its write-to blocks
				torque = 0;
				omega = 0;
				power = 0;
				return;
			}
		}
	}

	private double getEffectiveRatio() {
		GearType type = this.getType();
		if (type == GearType.COIL)
			return 1;
		if (type == GearType.WORM)
			return WORMRATIO;
		return this.getCVTRatio();
	}

	private int getCVTRatio() {
		if (isRedstoneControlled) {
			boolean red = worldObj.isBlockIndirectlyGettingPowered(xCoord, yCoord, zCoord);
			return this.getCVTState(red).gearRatio;
		}
		else {
			return ratio;
		}
	}

	private double getPowerLossFraction(int speed) {
		if (this.getType() == GearType.WORM)
			return (128-4*ReikaMathLibrary.logbase(speed, 2))/100;
		return 1;
	}

	@Override
	public boolean isUseableByPlayer(EntityPlayer var1) {
		if (this.getType() == GearType.WORM)
			return false;
		return this.isPlayerAccessible(var1);
	}

	@Override
	public void updateEntity(World world, int x, int y, int z, int meta)
	{
		super.updateTileEntity();
		this.getIOSides(world, x, y, z, meta);
		if (this.getType() == GearType.CVT) {
			if (controller != null && controller.isActive() && controller.getCVT().equals(this)) {
				boolean torque = controller.isTorque();
				int r = controller.getControlledRatio();
				ratio = torque ? r : -r;
			}
		}
		if (this.getType() != GearType.COIL)
			this.transferPower(world, x, y, z, meta);
		else
			this.store(world, x, y, z, meta);
		power = (long)omega*(long)torque;
		//ReikaJavaLibrary.pConsole(torque+" @ "+omega);

		this.basicPowerReceiver();
		lastPower = world.isBlockIndirectlyGettingPowered(x, y, z);
	}

	private void store(World world, int x, int y, int z, int meta) {
		this.transferPower(world, x, y, z, meta);
		isReleasing = world.isBlockIndirectlyGettingPowered(x, y, z);
		if (!isReleasing) {
			torque = omega = 0;
			power = 0;
			if (energy + ((long)torquein*(long)omegain) < 0 || energy + ((long)torquein*(long)omegain) > Long.MAX_VALUE) {
				for (int i = 0; i < 16; i++)
					ReikaSoundHelper.playSoundAtBlock(world, x, y, z, "random.explode", 5, 0.2F);
				ReikaParticleHelper.EXPLODE.spawnAroundBlock(world, x, y, z, 2);
				int r = 20;
				for (int i = -r; i <= r; i++) {
					for (int j = -r; j <= r; j++) {
						for (int k = -r; k <= r; k++) {
							double dd = ReikaMathLibrary.py3d(i, j*2, k);
							if (dd <= r+0.5) {
								if (world.getBlockId(x+i, y+j, z+k) != Block.bedrock.blockID) {
									world.setBlock(x+i, y+j, z+k, 0);
									world.markBlockForUpdate(x+i, y+j, z+k);
								}
							}
							if (!world.isRemote && rand.nextInt(8) == 0)
								ReikaWorldHelper.ignite(world, x+i, y+j, z+k);
						}
					}
				}
			}
			else
				energy += ((long)torquein*(long)omegain);
		}
		else if (energy > 0 && releaseTorque > 0 && releaseOmega > 0) {
			torque = releaseTorque;
			omega = releaseOmega;
			power = (long)torque*(long)omega;
			energy -= power;
			if (energy <= 0) {
				energy = 0;
				torque = 0;
				omega = 0;
				power = 0;
			}
		}
	}

	public void getIOSides(World world, int x, int y, int z, int metadata) {
		while (metadata > 3)
			metadata -= 4;
		super.getIOSides(world, x, y, z, metadata, false);
	}

	public void getRatio() {
		int sign = 1;
		if (ratio < 0)
			sign = -1;
		if (Math.abs(ratio) > this.getMaxRatio())
			ratio = this.getMaxRatio()*sign;
		if (ratio == 0)
			ratio = 1;
	}

	public int getMaxRatio() {
		if (belts[0] == null)
			return 1;
		if (belts[0].itemID != ItemStacks.belt.itemID || belts[0].getItemDamage() != ItemStacks.belt.getItemDamage())
			return 1;
		for (int i = 1; i <= 2; i++) {
			if (belts[i] == null)
				return 2;
			if (belts[i].itemID != ItemStacks.belt.itemID || belts[i].getItemDamage() != ItemStacks.belt.getItemDamage())
				return 2;
		}
		for (int i = 3; i <= 6; i++) {
			if (belts[i] == null)
				return 4;
			if (belts[i].itemID != ItemStacks.belt.itemID || belts[i].getItemDamage() != ItemStacks.belt.getItemDamage())
				return 4;
		}
		for (int i = 7; i <= 14; i++) {
			if (belts[i] == null)
				return 8;
			if (belts[i].itemID != ItemStacks.belt.itemID || belts[i].getItemDamage() != ItemStacks.belt.getItemDamage())
				return 8;
		}
		for (int i = 15; i <= 30; i++) {
			if (belts[i] == null)
				return 16;
			if (belts[i].itemID != ItemStacks.belt.itemID || belts[i].getItemDamage() != ItemStacks.belt.getItemDamage())
				return 16;
		}
		return 32;
	}

	public void readFromCross(TileEntityShaft cross) {
		if (xCoord == cross.writex && zCoord == cross.writez) {
			omega = cross.readomega[0];
			if (this.getType() == GearType.WORM)
				omega = (int)((((omega / WORMRATIO)*(100-4*ReikaMathLibrary.logbase(omega, 2)+28)))/100);
			torque = cross.readtorque[0];
			if (this.getType() == GearType.WORM)
				torque = torque * WORMRATIO;
		}
		else if (xCoord == cross.writex2 && zCoord == cross.writez2) {
			omega = cross.readomega[1];
			if (this.getType() == GearType.WORM)
				omega = (int)((((omega / WORMRATIO)*(100-4*ReikaMathLibrary.logbase(omega, 2)+28)))/100);
			torque = cross.readtorque[1];
			if (this.getType() == GearType.WORM)
				torque = torque * WORMRATIO;
		}
		else
			return; //not its output
	}

	@Override
	public void transferPower(World world, int x, int y, int z, int meta) {
		this.getRatio();
		omegain = torquein = 0;
		TileEntity te = worldObj.getBlockTileEntity(readx, ready, readz);
		if (!this.isProvider(te) || !this.isIDTEMatch(world, readx, ready, readz)) {
			omega = 0;
			torque = 0;
			power = 0;
			return;
		}
		MachineRegistry m = MachineRegistry.machineList[((RotaryCraftTileEntity)(te)).getMachineIndex()];
		if (m == MachineRegistry.SHAFT) {
			TileEntityShaft devicein = (TileEntityShaft)te;
			if (devicein.getBlockMetadata() >= 6) {
				this.readFromCross(devicein);
				return;
			}
			if (devicein.writex == x && devicein.writey == y && devicein.writez == z) {
				torquein = devicein.torque;
				omegain = devicein.omega;
			}
		}
		if (te instanceof SimpleProvider) {
			this.copyStandardPower(worldObj, readx, ready, readz);
		}
		if (te instanceof ShaftPowerEmitter) {
			ShaftPowerEmitter sp = (ShaftPowerEmitter)te;
			if (sp.isEmitting() && sp.canWriteToBlock(xCoord, yCoord, zCoord)) {
				torquein = sp.getTorque();
				omegain = sp.getOmega();
			}
		}
		if (m == MachineRegistry.SPLITTER) {
			TileEntitySplitter devicein = (TileEntitySplitter)te;
			if (devicein.getBlockMetadata() >= 8) {
				this.readFromSplitter(devicein);
				//ReikaJavaLibrary.pConsole(torque+" @ "+omega);
				return;
			}
			else if (devicein.writex == x && devicein.writez == z) {
				torquein = devicein.torque;
				omegain = devicein.omega;
			}
		}

		switch(this.getType()) {
		case WORM:
			omega = (int)((omegain / WORMRATIO)*this.getPowerLossFraction(omegain));
			if (torquein <= RotaryConfig.torquelimit/WORMRATIO)
				torque = torquein * WORMRATIO;
			else {
				torque = RotaryConfig.torquelimit;
				world.spawnParticle("crit", x+rand.nextFloat(), y+rand.nextFloat(), z+rand.nextFloat(), -0.5+rand.nextFloat(), rand.nextFloat(), -0.5+rand.nextFloat());
				world.playSoundEffect(x+0.5, y+0.5, z+0.5, "mob.blaze.hit", 0.1F, 1F);
			}
			break;
		case CVT:
			int ratio = this.getCVTRatio();
			if (hasLubricant) {
				boolean speed = true;
				if (ratio > 0) {
					if (omegain <= RotaryConfig.omegalimit/ratio)
						omega = omegain * ratio;
					else {
						omega = RotaryConfig.omegalimit;
						world.spawnParticle("crit", x+rand.nextFloat(), y+rand.nextFloat(), z+rand.nextFloat(), -0.5+rand.nextFloat(), rand.nextFloat(), -0.5+rand.nextFloat());
						world.playSoundEffect(x+0.5, y+0.5, z+0.5, "mob.blaze.hit", 0.1F, 1F);
					}
					torque = torquein / ratio;
				}
				else {
					if (torquein <= RotaryConfig.torquelimit/-ratio)
						torque = torquein * -ratio;
					else {
						torque = RotaryConfig.torquelimit;
						world.spawnParticle("crit", x+rand.nextFloat(), y+rand.nextFloat(), z+rand.nextFloat(), -0.5+rand.nextFloat(), rand.nextFloat(), -0.5+rand.nextFloat());
						world.playSoundEffect(x+0.5, y+0.5, z+0.5, "mob.blaze.hit", 0.1F, 1F);
					}
					omega = omegain / -ratio;
				}
			}
			else {
				omega = torque = 0;
			}
			break;
		case COIL:

			break;
		}
	}

	/**
	 * Writes a tile entity to NBT.
	 */
	@Override
	public void writeToNBT(NBTTagCompound NBT)
	{
		super.writeToNBT(NBT);
		NBT.setInteger("ratio", ratio);
		NBT.setLong("e", energy);
		NBT.setInteger("relo", releaseOmega);
		NBT.setInteger("relt", releaseTorque);
		NBT.setBoolean("lube", hasLubricant);
		NBT.setBoolean("redstone", isRedstoneControlled);
		NBT.setInteger("cvton", this.getCVTState(true).ordinal());
		NBT.setInteger("cvtoff", this.getCVTState(false).ordinal());

		NBTTagList nbttaglist = new NBTTagList();

		for (int i = 0; i < belts.length; i++)
		{
			if (belts[i] != null)
			{
				NBTTagCompound nbttagcompound = new NBTTagCompound();
				nbttagcompound.setByte("Slot", (byte)i);
				belts[i].writeToNBT(nbttagcompound);
				nbttaglist.appendTag(nbttagcompound);
			}
		}

		NBT.setTag("Items", nbttaglist);
	}

	/**
	 * Reads a tile entity from NBT.
	 */
	@Override
	public void readFromNBT(NBTTagCompound NBT)
	{
		super.readFromNBT(NBT);
		ratio = NBT.getInteger("ratio");
		energy = NBT.getLong("e");
		releaseOmega = NBT.getInteger("relo");
		releaseTorque = NBT.getInteger("relt");
		hasLubricant = NBT.getBoolean("lube");
		isRedstoneControlled = NBT.getBoolean("redstone");
		cvtState[0] = CVTState.list[NBT.getInteger("cvtoff")];
		cvtState[1] = CVTState.list[NBT.getInteger("cvton")];

		NBTTagList nbttaglist = NBT.getTagList("Items");
		belts = new ItemStack[this.getSizeInventory()];

		for (int i = 0; i < nbttaglist.tagCount(); i++)
		{
			NBTTagCompound nbttagcompound = (NBTTagCompound)nbttaglist.tagAt(i);
			byte byte0 = nbttagcompound.getByte("Slot");

			if (byte0 >= 0 && byte0 < belts.length)
			{
				belts[byte0] = ItemStack.loadItemStackFromNBT(nbttagcompound);
			}
		}
	}

	@Override
	public int getSizeInventory() {
		return belts.length;
	}

	@Override
	public ItemStack getStackInSlot(int var1) {
		return belts[var1];
	}

	@Override
	public ItemStack decrStackSize(int var1, int var2) {
		return ReikaInventoryHelper.decrStackSize(this, var1, var2);
	}

	@Override
	public ItemStack getStackInSlotOnClosing(int var1) {
		return ReikaInventoryHelper.getStackInSlotOnClosing(this, var1);
	}

	@Override
	public void setInventorySlotContents(int var1, ItemStack var2) {
		belts[var1] = var2;
	}

	@Override
	public int getInventoryStackLimit() {
		return 1;
	}

	@Override
	public boolean isItemValidForSlot(int i, ItemStack itemstack) {
		return itemstack.itemID == ItemStacks.belt.itemID && itemstack.getItemDamage() == ItemStacks.belt.getItemDamage();
	}

	@Override
	public boolean isInvNameLocalized() {
		return false;
	}

	@Override
	public void openChest() {
	}

	@Override
	public void closeChest() {
	}

	@Override
	public boolean hasModelTransparency() {
		return false;
	}

	@Override
	public void animateWithTick(World world, int x, int y, int z) {
		if (!this.isInWorld()) {
			phi = 0;
			return;
		}
		phi += ReikaMathLibrary.doubpow(ReikaMathLibrary.logbase(omega+1, 2), 1.05);
	}

	@Override
	public int getMachineIndex() {
		return MachineRegistry.ADVANCEDGEARS.ordinal();
	}

	@Override
	public boolean canExtractItem(int i, ItemStack itemstack, int j) {
		return false;
	}

	@Override
	public int getRedstoneOverride() {
		return 0;
	}

	@Override
	public void onEMP() {
		//super.onEMP();
	}

	@Override
	public boolean isFlipped() {
		return isFlipped;
	}

	@Override
	public void setFlipped(boolean set) {
		isFlipped = set;
	}

	public int[] getAccessibleSlotsFromSide(int var1) {
		if (this instanceof InertIInv)
			return new int[0];
		return ReikaInventoryHelper.getWholeInventoryForISided(this);
	}

	public boolean canInsertItem(int i, ItemStack is, int side) {
		if (this instanceof InertIInv)
			return false;
		return ((IInventory)this).isItemValidForSlot(i, is);
	}

	public final String getInvName() {
		return this.getMultiValuedName();
	}

	public long getEnergy() {
		return energy;
	}

	public void setEnergyFromNBT(NBTTagCompound NBT) {
		energy = NBT.getLong("energy");
	}

	@Override
	public long getMaxPower() {
		if (this.getType() != GearType.COIL)
			return 0;
		return releaseOmega*releaseTorque;
	}

	@Override
	public long getCurrentPower() {
		return power;
	}

	@Override
	public PowerSourceList getPowerSources(TileEntityIOMachine io, ShaftMerger caller) {
		if (this.getType() == GearType.COIL)
			return new PowerSourceList().addSource(this);
		else
			return super.getPowerSources(io, caller);
	}

	public void incrementCVTState(boolean on) {
		int i = on ? 1 : 0;
		cvtState[i] = this.getCVTState(on).next();
		while (!this.getCVTState(on).isValid(this)) {
			cvtState[i] = this.getCVTState(on).next();
		}
	}

	private CVTState getCVTState(boolean on) {
		int i = on ? 1 : 0;
		return cvtState[i] != null ? cvtState[i] : CVTState.S1;
	}

	public String getCVTString(boolean on) {
		return this.getCVTState(on).toString();
	}

	private static enum CVTState {
		S1(1),
		S2(2),
		S4(4),
		S8(8),
		S16(16),
		S32(32),
		T1(-1),
		T2(-2),
		T4(-4),
		T8(-8),
		T16(-16),
		T32(-32);

		public final int gearRatio;

		public static final CVTState[] list = values();

		private CVTState(int ratio) {
			gearRatio = ratio;
		}

		public CVTState next() {
			if (this.ordinal() == list.length-1)
				return list[0];
			else
				return list[this.ordinal()+1];
		}

		public boolean isValid(TileEntityAdvancedGear te) {
			int abs = Math.abs(gearRatio);
			int max = Math.abs(te.getMaxRatio());
			return max >= abs;
		}

		@Override
		public String toString() {
			return Math.abs(gearRatio)+"x "+(gearRatio > 0 ? "Speed" : "Torque");
		}
	}
}
