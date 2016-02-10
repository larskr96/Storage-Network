package mrriegel.storagenetwork;

import java.util.Map.Entry;

import mrriegel.storagenetwork.gui.request.ContainerRequest;
import mrriegel.storagenetwork.proxy.CommonProxy;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLInterModComms;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.registry.GameData;

@Mod(modid = StorageNetwork.MODID, name = StorageNetwork.MODNAME, version = StorageNetwork.VERSION, guiFactory = "mrriegel.storagenetwork.config.GuiFactory")
public class StorageNetwork {
	public static final String MODID = "storagenetwork";
	public static final String VERSION = "1.4";
	public static final String MODNAME = "Storage Network";

	@Instance(StorageNetwork.MODID)
	public static StorageNetwork instance;

	@SidedProxy(clientSide = "mrriegel.storagenetwork.proxy.ClientProxy", serverSide = "mrriegel.storagenetwork.proxy.CommonProxy")
	public static CommonProxy proxy;

	@EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		proxy.preInit(event);
	}

	@EventHandler
	public void init(FMLInitializationEvent event) {
		proxy.init(event);
		NBTTagCompound tagCompound = new NBTTagCompound();
		tagCompound.setString("ContainerClass", ContainerRequest.class.getName());

		// tagCompound.setInteger("GridSlotNumber", 1);
		// tagCompound.setInteger("GridSize", 9);
		// tagCompound.setBoolean("HideButtons", false);
		tagCompound.setBoolean("PhantomItems", false);

		// tagCompound.setInteger("ButtonOffsetX", -16); // ignored if
		// AlignToGrid is set
		// tagCompound.setInteger("ButtonOffsetY", 16); // ignored if
		// AlignToGrid is set
		// ***** OR *****
		tagCompound.setString("AlignToGrid", "left");

		// NBTTagCompound tweakRotate = new NBTTagCompound();
		// tweakRotate.setBoolean("Enabled", true);
		// tweakRotate.setBoolean("ShowButton", true);
		// tweakRotate.setInteger("ButtonX", 0); // ignored if AlignToGrid is
		// set
		// tweakRotate.setInteger("ButtonY", 18); // ignored if AlignToGrid is
		// set
		// tagCompound.setTag("TweakRotate", tweakRotate);
		// [...] (same structure for "TweakBalance" and "TweakClear")

		FMLInterModComms.sendMessage("craftingtweaks", "RegisterProvider", tagCompound);
	}

	@EventHandler
	public void postInit(FMLPostInitializationEvent event) {
		proxy.postInit(event);
		for(Entry<String, ModContainer> e:Loader.instance().getIndexedModList().entrySet())
		System.out.println("qulf: "+e.getKey()+", "+e.getValue().getName());

	}

}