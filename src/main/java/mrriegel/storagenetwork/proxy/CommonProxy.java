package mrriegel.storagenetwork.proxy;

import mrriegel.storagenetwork.StorageNetwork;
import mrriegel.storagenetwork.Util;
import mrriegel.storagenetwork.config.ConfigHandler;
import mrriegel.storagenetwork.handler.GuiHandler;
import mrriegel.storagenetwork.init.CraftingRecipes;
import mrriegel.storagenetwork.init.ModBlocks;
import mrriegel.storagenetwork.init.ModItems;
import mrriegel.storagenetwork.network.PacketHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;

public class CommonProxy {
	public void preInit(FMLPreInitializationEvent event) {
		ConfigHandler.refreshConfig(event.getSuggestedConfigurationFile());
		PacketHandler.init();

	}

	public void init(FMLInitializationEvent event) {
		NetworkRegistry.INSTANCE.registerGuiHandler(StorageNetwork.instance, new GuiHandler());
		ModBlocks.init();
		ModItems.init();
		CraftingRecipes.init();
	}

	public void postInit(FMLPostInitializationEvent event) {
		Util.init();
	}
}