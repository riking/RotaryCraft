/*******************************************************************************
 * @author Reika Kalseki
 * 
 * Copyright 2015
 * 
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package Reika.RotaryCraft.Containers;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import Reika.DragonAPI.Libraries.IO.ReikaPacketHelper;
import Reika.RotaryCraft.RotaryCraft;
import Reika.RotaryCraft.Base.ContainerIOMachine;
import Reika.RotaryCraft.Base.TileEntity.TileEntityEngine;

public class ContainerEthanol extends ContainerIOMachine
{
	private TileEntityEngine Engine;

	public ContainerEthanol(EntityPlayer player, TileEntityEngine par2TileEntityEngine)
	{
		super(player, par2TileEntityEngine);
		Engine = par2TileEntityEngine;
		int posX = Engine.xCoord;
		int posY = Engine.yCoord;
		int posZ = Engine.zCoord;
		this.addSlotToContainer(new Slot(par2TileEntityEngine, 0, 61, 36));

		this.addPlayerInventory(player);
	}

	/**
	 * Updates crafting matrix; called from onCraftMatrixChanged. Args: none
	 */
	@Override
	public void detectAndSendChanges()
	{
		super.detectAndSendChanges();

		ReikaPacketHelper.sendTankSyncPacket(RotaryCraft.packetChannel, Engine, "fuel");
	}
}
