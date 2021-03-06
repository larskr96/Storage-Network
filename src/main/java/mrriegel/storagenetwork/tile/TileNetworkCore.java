package mrriegel.storagenetwork.tile;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import mrriegel.limelib.helper.NBTHelper;
import mrriegel.limelib.helper.StackHelper;
import mrriegel.limelib.network.PacketHandler;
import mrriegel.limelib.tile.CommonTile;
import mrriegel.limelib.util.CombinedEnergyStorage;
import mrriegel.limelib.util.EnergyStorageExt;
import mrriegel.limelib.util.GlobalBlockPos;
import mrriegel.storagenetwork.ModConfig;
import mrriegel.storagenetwork.Network;
import mrriegel.storagenetwork.Registry;
import mrriegel.storagenetwork.StorageNetwork;
import mrriegel.storagenetwork.block.BlockNetworkCore;
import mrriegel.storagenetwork.message.MessageCoreSync;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import cofh.api.energy.IEnergyReceiver;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @author canitzp
 */
public class TileNetworkCore extends CommonTile implements ITickable, IEnergyReceiver {

	public Network network;
	protected boolean needsUpdate;
	protected EnergyStorageExt energy = new EnergyStorageExt(200000, ModConfig.energyinterfaceTransferRate) {
		@Override
		public int extractEnergy(int maxExtract, boolean simulate) {
			if (worldObj.getTotalWorldTime() % 4 == 0)
				markForSync();
			if ((double) getEnergyStored() / (double) getMaxEnergyStored() > 0.95)
				return super.extractEnergy(Math.min(maxExtract, getMaxEnergyStored() / 10), simulate);
			return 0;
		};
	};

	protected IEnergyStorage receiver = new CombinedEnergyStorage(energy) {
		@Override
		public int extractEnergy(int maxExtract, boolean simulate) {
			return 0;
		}
	};
	protected IEnergyStorage extractor = new CombinedEnergyStorage(energy) {
		@Override
		public int receiveEnergy(int maxReceive, boolean simulate) {
			return 0;
		}
	};

	@Override
	public void readFromNBT(NBTTagCompound compound) {
		energy.setEnergyStored(NBTHelper.getInt(compound, "energy"));
		super.readFromNBT(compound);
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		NBTHelper.setInt(compound, "energy", energy.getEnergyStored());
		return super.writeToNBT(compound);
	}

	public void initializeNetwork() {
		this.network = new Network();
		network.corePosition = new GlobalBlockPos(pos, worldObj);
		try {
			//			runThroughNetworkRecursive(pos);
			runThroughNetworkIterative();
			markDirty();
		} catch (StackOverflowError error) {
			StorageNetwork.logger.error("Couldn't build the network due to a StackOverflowError.");
		} catch (Error error) {
			error.printStackTrace();
		}
		worldObj.getChunkFromBlockCoords(pos).setModified(true);
		//		System.out.println("network size: " + network.networkParts.size() + ", no cables: " + network.noCables.size());
	}

	@SuppressWarnings("unused")
	private void runThroughNetworkRecursive(BlockPos pos) {
		for (EnumFacing facing : EnumFacing.values()) {
			BlockPos searchPos = pos.offset(facing);
			if (worldObj.getTileEntity(pos) instanceof IToggleable && !((IToggleable) worldObj.getTileEntity(pos)).isActive())
				return;
			if (worldObj.getTileEntity(pos) instanceof INetworkPart && !((INetworkPart) worldObj.getTileEntity(pos)).getNeighborFaces().contains(facing))
				continue;
			if (worldObj.getTileEntity(searchPos) instanceof INetworkPart && !((INetworkPart) worldObj.getTileEntity(searchPos)).getNeighborFaces().contains(facing.getOpposite()))
				continue;
			if (worldObj.getBlockState(searchPos).getBlock().hasTileEntity(worldObj.getBlockState(searchPos))) {
				TileEntity tile = getWorld().getTileEntity(searchPos);
				if (tile instanceof TileNetworkCore && !tile.getPos().equals(this.pos)) {
					worldObj.setBlockToAir(searchPos);
					worldObj.playEvent(2001, searchPos, Block.getIdFromBlock(Registry.networkCore));
					StackHelper.spawnItemStack(worldObj, searchPos, new ItemStack(Registry.networkCore));
					markForNetworkInit();
				} else if (tile instanceof INetworkPart && !network.networkParts.contains(tile)) {
					network.addPart((INetworkPart) tile);
				} else
					continue;
				runThroughNetworkRecursive(searchPos);
			}
		}
	}

	private void runThroughNetworkIterative() {
		List<BlockPos> research = Lists.newLinkedList(Collections.singleton(pos));
		Set<BlockPos> done = Sets.newHashSet();
		while (!research.isEmpty()) {
			BlockPos current = research.get(0);
			research.remove(0);
			for (EnumFacing facing : EnumFacing.values()) {
				BlockPos searchPos = current.offset(facing);
				if (worldObj.getTileEntity(current) instanceof IToggleable && !((IToggleable) worldObj.getTileEntity(current)).isActive())
					continue;
				if (worldObj.getTileEntity(current) instanceof INetworkPart && !((INetworkPart) worldObj.getTileEntity(current)).getNeighborFaces().contains(facing))
					continue;
				if (worldObj.getTileEntity(searchPos) instanceof INetworkPart && !((INetworkPart) worldObj.getTileEntity(searchPos)).getNeighborFaces().contains(facing.getOpposite()))
					continue;
				if (worldObj.getBlockState(searchPos).getBlock().hasTileEntity(worldObj.getBlockState(searchPos))) {
					TileEntity tile = getWorld().getTileEntity(searchPos);
					if (tile instanceof TileNetworkCore && !tile.getPos().equals(pos)) {
						worldObj.setBlockToAir(searchPos);
						worldObj.playEvent(2001, searchPos, Block.getIdFromBlock(Registry.networkCore));
						StackHelper.spawnItemStack(worldObj, searchPos, new ItemStack(Registry.networkCore));
						markForNetworkInit();
					} else if (tile instanceof INetworkPart && !done.contains(searchPos)) {
						done.add(searchPos);
						research.add(searchPos);
					}
				}
			}
		}

		for (BlockPos p : done) {
			network.addPart((INetworkPart) worldObj.getTileEntity(p));
		}
	}

	public void markForNetworkInit() {
		needsUpdate = true;
	}

	@Override
	public void update() {
		if (ModConfig.STOPTICK)
			return;
		if ((worldObj.getTotalWorldTime() + (pos.hashCode() % 300)) % (network == null ? 80 : 300) == 0) {
			needsUpdate = true;
			//Lag
			needsUpdate = false;
		}
		if ((network == null || needsUpdate) && onServer() && worldObj.getTotalWorldTime() % 7 == 0) {
			needsUpdate = false;
			initializeNetwork();
			network.onChange();
		}
		if (onServer() && network != null) {
			if (worldObj.getTotalWorldTime() % 15 == 0) {
				if (ModConfig.needsEnergy && getEnergyStorage().getEnergyStored() == 0 && worldObj.getBlockState(pos).getValue(BlockNetworkCore.ACTIVE))
					worldObj.setBlockState(pos, worldObj.getBlockState(pos).withProperty(BlockNetworkCore.ACTIVE, false), 2);
				else if ((getEnergyStorage().getEnergyStored() > 0 || !ModConfig.needsEnergy) && !worldObj.getBlockState(pos).getValue(BlockNetworkCore.ACTIVE))
					worldObj.setBlockState(pos, worldObj.getBlockState(pos).withProperty(BlockNetworkCore.ACTIVE, true), 2);
			}
			distributeEnergy();
			network.importItems();
			network.exportItems();
			network.stockItems();
			if (worldObj.getTotalWorldTime() % 20 == 0)
				for (EntityPlayerMP p : worldObj.getEntitiesWithinAABB(EntityPlayerMP.class, new AxisAlignedBB(pos.add(10, 10, 10), pos.add(-10, -10, -10)))) {
					PacketHandler.sendTo(new MessageCoreSync(this), p);
				}
			if (worldObj.getTotalWorldTime() % 300 == 0) {
				for (INetworkPart part : network.noCables)
					if (part.getNetworkCore() == null || !part.getNetworkCore().getPos().equals(pos)) {
						markForNetworkInit();
						break;
					}
			}
			//			if (worldObj.getTotalWorldTime() % 40 == 0)
			//				System.out.println(network.networkParts.size()+" parts");
		}
	}

	private void distributeEnergy() {
		if (worldObj.getTotalWorldTime() % 20 == 0) {
			energy.setMaxExtract(getTotalTransfer());
			energy.setMaxReceive(getTotalTransfer());
		}
		int maxTransfer = getTotalTransfer();
		List<TileNetworkEnergyInterface> inters = network.getTileParts(TileNetworkEnergyInterface.class);
		inters.sort(new Comparator<TileNetworkEnergyInterface>() {
			@Override
			public int compare(TileNetworkEnergyInterface o1, TileNetworkEnergyInterface o2) {
				return Integer.compare(o2.getPriority(), o1.getPriority());
			}
		});
		for (TileNetworkEnergyInterface tile : inters) {
			boolean simulate = false;
			if (tile.getTile() instanceof IEnergyReceiver && tile.iomode.canExtract()) {
				int maxReceive = ((IEnergyReceiver) tile.getTile()).receiveEnergy(tile.tileFace.getOpposite(), maxTransfer, true);
				((IEnergyReceiver) tile.getTile()).receiveEnergy(tile.tileFace.getOpposite(), extractor.extractEnergy(maxReceive, simulate), simulate);
			} else if (tile.getTile() != null && tile.getTile().hasCapability(CapabilityEnergy.ENERGY, tile.tileFace.getOpposite()) && tile.iomode.canExtract()) {
				int maxReceive = tile.getTile().getCapability(CapabilityEnergy.ENERGY, tile.tileFace.getOpposite()).receiveEnergy(maxTransfer, true);
				tile.getTile().getCapability(CapabilityEnergy.ENERGY, tile.tileFace.getOpposite()).receiveEnergy(extractor.extractEnergy(maxReceive, simulate), simulate);
			}
		}
		for (INetworkPart part : network.networkParts)
			if (part instanceof TileNetworkEnergyCell) {
				if ((double) getEnergyStored(null) / (double) getMaxEnergyStored(null) < .9) {
					int maxReceive = receiveEnergy(null, maxTransfer, true);
					receiveEnergy(null, ((TileNetworkEnergyCell) part).getEnergy().extractEnergy(maxReceive, false), false);
				} else if ((double) getEnergyStored(null) / (double) getMaxEnergyStored(null) > .6) {
					int maxReceive = ((TileNetworkEnergyCell) part).getEnergy().receiveEnergy(maxTransfer, true);
					((TileNetworkEnergyCell) part).getEnergy().receiveEnergy(getEnergyStorage().extractEnergy(maxReceive, false), false);
				}
			}
	}

	public int getTotalTransfer() {
		return ModConfig.energyinterfaceTransferRate + network.getTileParts(TileNetworkEnergyCell.class).size() * ModConfig.energyinterfaceTransferRate;
	}

	public int getTotalEnergy() {
		int max = getEnergyStored(null);
		for (INetworkPart part : network.networkParts)
			if (part instanceof TileNetworkEnergyCell)
				max += ((TileNetworkEnergyCell) part).getEnergy().getEnergyStored();
		return max;
	}

	public int getTotalMaxEnergy() {
		int max = getMaxEnergyStored(null);
		for (INetworkPart part : network.networkParts)
			if (part instanceof TileNetworkEnergyCell)
				max += ((TileNetworkEnergyCell) part).getEnergy().getMaxEnergyStored();
		return max;
	}

	public EnergyStorageExt getEnergyStorage() {
		return energy;
	}

	public boolean consumeRF(int num, boolean simulate) {
		if (!ModConfig.needsEnergy)
			return true;
		int value = (int) (num * ModConfig.energyMultiplier);
		if (getEnergyStorage().getEnergyStored() < value)
			return false;
		if (!simulate) {
			getEnergyStorage().modifyEnergyStored(-value);
		}
		return true;
	}

	@Override
	public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
		return (ModConfig.needsEnergy && capability == CapabilityEnergy.ENERGY) || super.hasCapability(capability, facing);
	}

	@Override
	public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
		if (ModConfig.needsEnergy && capability == CapabilityEnergy.ENERGY)
			return (T) getEnergyStorage();
		return super.getCapability(capability, facing);
	}

	@Override
	public int receiveEnergy(EnumFacing from, int maxReceive, boolean simulate) {
		if (worldObj.getTotalWorldTime() % 4 == 0)
			markForSync();
		return getEnergyStorage().receiveEnergy(maxReceive, simulate);
	}

	@Override
	public int getEnergyStored(EnumFacing from) {
		return getEnergyStorage().getEnergyStored();
	}

	@Override
	public int getMaxEnergyStored(EnumFacing from) {
		return getEnergyStorage().getMaxEnergyStored();
	}

	@Override
	public boolean canConnectEnergy(EnumFacing from) {
		return ModConfig.needsEnergy;
	}

}
