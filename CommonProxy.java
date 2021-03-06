/*******************************************************************************
 * @author Reika Kalseki
 * 
 * Copyright 2015
 * 
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package Reika.RotaryCraft;

import net.minecraft.world.World;
import Reika.DragonAPI.Libraries.Java.ReikaJavaLibrary;
import Reika.RotaryCraft.ModInterface.Lua.LuaMethods;
import Reika.RotaryCraft.Registry.DifficultyEffects;
import Reika.RotaryCraft.Registry.EngineType;
import Reika.RotaryCraft.Registry.ExtractorBonus;
import Reika.RotaryCraft.Registry.MobBait;
import Reika.RotaryCraft.Registry.PacketRegistry;
import Reika.RotaryCraft.Registry.PlantMaterials;
import Reika.RotaryCraft.Registry.PowerReceivers;

public class CommonProxy
{
	public static int IOGoggles;
	public static int NVGoggles;
	public static int NVHelmet;

	public static int armor;
	public static int SteelArmor;

	public static int pipeRender;
	public static int cubeRender;
	public static int connectedRender;

	/**
	 * Client side only register stuff...
	 */
	public void registerRenderers()
	{
		//unused server side. -- see ClientProxy for implementation
	}

	public void addArmorRenders() {}

	public World getClientWorld() {
		return null;
	}

	public void registerRenderInformation() {

	}

	public void registerSounds() {

	}

	public void initClasses() {
		ReikaJavaLibrary.initClass(DifficultyEffects.class);
		ReikaJavaLibrary.initClass(ExtractorBonus.class);
		ReikaJavaLibrary.initClass(MobBait.class);
		ReikaJavaLibrary.initClass(PlantMaterials.class);
		ReikaJavaLibrary.initClass(EngineType.class);
		ReikaJavaLibrary.initClass(PacketRegistry.class);
		ReikaJavaLibrary.initClass(PowerReceivers.class);
		ReikaJavaLibrary.initClass(LuaMethods.class);
	}

}// End class CommonProxy
