package redgear.core.tile;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;
import redgear.core.api.tile.IBucketableTank;
import redgear.core.fluids.AdvFluidTank;
import redgear.core.fluids.FluidUtil;
import redgear.core.util.ItemStackUtil;

public abstract class TileEntityTank extends TileEntityInventory implements IFluidHandler, IBucketableTank {
	public TileEntityTank(int idleRate) {
		super(idleRate);
		currMode = ejectMode.MACHINE;
	}

	private final List<AdvFluidTank> tanks = new ArrayList<AdvFluidTank>();

	private ejectMode currMode;

	private enum ejectMode {
		OFF, MACHINE, ALL;

		public static ejectMode increment(ejectMode lastMode) {
			return lastMode == OFF ? MACHINE : lastMode == MACHINE ? ALL : OFF;
		}

		public static ejectMode valueOf(int ordinal) {
			return ordinal == 0 ? OFF : ordinal == 2 ? ALL : MACHINE;
		}
	}

	/**
	 * Adds the given LiquidTank to this tile
	 *
	 * @param newTank New Tank to add
	 * @return index of the new tank used for adding side mappings
	 */
	public int addTank(AdvFluidTank newTank) {
		tanks.add(newTank);
		return tanks.size() - 1;
	}

	public int tanks() {
		return tanks.size();
	}

	private boolean validTank(int index) {
		return index >= 0 && index < tanks.size() && tanks.get(index) != null;
	}

	public AdvFluidTank getTank(int index) {
		if (validTank(index))
			return tanks.get(index);
		else
			return null;
	}

	@Override
	public int fill(ForgeDirection from, FluidStack resource, boolean doFill) {
		int filled = 0;

		for (AdvFluidTank tank : tanks) {
			filled = tank.fillWithMap(resource, doFill);

			if (filled > 0) {
				if (doFill)
					forceSync();
				return filled;
			}
		}

		return 0;
	}

	@Override
	public FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain) {
		FluidStack removed = null;

		for (AdvFluidTank tank : tanks) {
			removed = tank.drainWithMap(resource, doDrain);

			if (removed != null && removed.amount > 0) {
				if (doDrain)
					forceSync();
				return removed;
			}
		}

		return null;
	}

	@Override
	public FluidStack drain(ForgeDirection from, int maxDrain, boolean doDrain) {
		FluidStack removed = null;

		for (AdvFluidTank tank : tanks) {
			removed = tank.drainWithMap(maxDrain, doDrain);

			if (removed != null && removed.amount > 0) {
				if (doDrain)
					forceSync();
				return removed;
			}
		}

		return null;
	}

	@Override
	public boolean bucket(EntityPlayer player, int index, ItemStack container) {
		if (FluidContainerRegistry.isFilledContainer(container))
			return fill(player, index, container);
		else if (FluidContainerRegistry.isEmptyContainer(container))
			return empty(player, index, container);
		else
			return false;
	}

	@Override
	public boolean fill(EntityPlayer player, int index, ItemStack container) {
		if (container == null)
			return false;

		FluidStack contents = FluidContainerRegistry.getFluidForFilledItem(container);

		if (contents != null && fill(ForgeDirection.UNKNOWN, contents, false) == contents.amount) {
			fill(ForgeDirection.UNKNOWN, contents, true);
			
			if(player.capabilities.isCreativeMode)
				return true;
			
			ItemStack ans = container.getItem().getContainerItem(container);

			player.inventory.decrStackSize(index, 1);
			
			if (ans != null)
				if (!player.inventory.addItemStackToInventory(ans))
					ItemStackUtil.dropItemStack(player.worldObj, (int) player.posX, (int) player.posY,
							(int) player.posZ, ans);
			player.inventory.markDirty();
			return true;
		} else
			return false;
	}

	@Override
	public boolean empty(EntityPlayer player, int index, ItemStack container) {
		for (AdvFluidTank tank : tanks) {
			FluidStack contents = tank.getFluid();

			if (contents != null) {
				ItemStack filled = FluidContainerRegistry.fillFluidContainer(contents.copy(), container.copy());
				if (filled != null) {
					int capacity = FluidUtil.getContainerCapacity(contents, filled);

					if (tank.canDrainWithMap(capacity)) {
						tank.drainWithMap(capacity, true);
						
						if(player.capabilities.isCreativeMode)
							return true;
						
						player.inventory.decrStackSize(index, 1);

						if (!player.inventory.addItemStackToInventory(filled))
							ItemStackUtil.dropItemStack(player.worldObj, (int) player.posX, (int) player.posY,
									(int) player.posZ, filled);
						player.inventory.markDirty();
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * Don't forget to override this function in all children if you want more
	 * vars!
	 */
	@Override
	public void writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
		NBTTagList tankList = new NBTTagList();

		for (int i = 0; i < tanks.size(); i++) {
			AdvFluidTank tank = getTank(i);

			if (tank != null) {
				NBTTagCompound invTag = new NBTTagCompound();
				invTag.setByte("tank", (byte) i);
				tankList.appendTag(tank.writeToNBT(invTag));
			}
		}

		tag.setTag("Tanks", tankList);
		tag.setInteger("ejectMode", currMode.ordinal());
	}

	/**
	 * Don't forget to override this function in all children if you want more
	 * vars!
	 */
	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		NBTTagList tagList = tag.getTagList("Tanks", 10);

		for (int i = 0; i < tagList.tagCount(); i++) {
			NBTTagCompound invTag = tagList.getCompoundTagAt(i);
			byte slot = invTag.getByte("tank");
			AdvFluidTank tank = getTank(slot);
			if (tank != null)
				tank.readFromNBT(invTag);
		}
		currMode = ejectMode.valueOf(tag.getInteger("ejectMode"));
	}

	protected void incrementEjectMode() {
		currMode = ejectMode.increment(currMode);
	}

	protected String getEjectMode() {
		return currMode.name();
	}

	protected boolean ejectAllFluids() {
		boolean check = false;
		int max = tanks();

		for (int i = 0; i < max; i++)
			check |= ejectFluidAllSides(i);
		return check;
	}

	protected boolean ejectFluidAllSides(int tankIndex) {
		AdvFluidTank temp = getTank(tankIndex);

		if (temp != null)
			return ejectFluidAllSides(temp);
		else
			return false;
	}

	protected boolean ejectFluidAllSides(AdvFluidTank tank) {
		boolean check = false;

		for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS)
			check |= ejectFluid(side, tank);
		return check;
	}

	protected boolean ejectFluid(ForgeDirection side, AdvFluidTank tank, int maxDrain) {
		if (tank == null || tank.getFluid() == null || currMode == ejectMode.OFF)
			return false; //can't drain from a null or empty tank, duh

		TileEntity otherTile = worldObj.getTileEntity(xCoord + side.offsetX, yCoord + side.offsetY, zCoord
				+ side.offsetZ);

		if (otherTile != null && IFluidHandler.class.isAssignableFrom(otherTile.getClass())
				&& (currMode == ejectMode.ALL || TileEntityMachine.class.isAssignableFrom(otherTile.getClass()))) {//IFluidHandler
			FluidStack drain = tank.drainWithMap(maxDrain, false);
			if (drain == null)
				return false;
			int fill = ((IFluidHandler) otherTile).fill(side.getOpposite(), drain, true);
			tank.drain(fill, true);//find out how much the tank can drain. Try to fill all that into the other tile. Actually drain all that the other tile took.
			return true;
		}

		return false;
	}

	protected boolean ejectFluid(ForgeDirection side, int tankIndex, int maxDrain) {
		return ejectFluid(side, getTank(tankIndex), maxDrain);
	}

	protected boolean ejectFluid(ForgeDirection side, AdvFluidTank tank) {
		return ejectFluid(side, tank, tank.getCapacity());
	}

	protected boolean ejectFluid(ForgeDirection side, int tankIndex) {
		AdvFluidTank temp = getTank(tankIndex);

		if (temp != null)
			return ejectFluid(side, temp, temp.getCapacity());
		else
			return false;
	}

	protected void writeFluidStack(NBTTagCompound tag, String name, FluidStack stack) {
		if (stack == null)
			return;

		tag.setTag(name, stack.writeToNBT(new NBTTagCompound()));
	}

	protected FluidStack readFluidStack(NBTTagCompound tag, String name) {
		NBTTagCompound subTag = tag.getCompoundTag(name);

		if (subTag == null)
			return null;

		return FluidStack.loadFluidStackFromNBT(subTag);
	}

	protected boolean fillTank(int slotFullIndex, int slotEmptyIndex, AdvFluidTank tank) {
		ItemStack fullSlot = getStackInSlot(slotFullIndex);

		if (fullSlot == null || tank == null || !validSlot(slotEmptyIndex))
			return false;

		FluidStack contents = FluidContainerRegistry.getFluidForFilledItem(fullSlot);
		ItemStack emptyContainer = FluidUtil.getEmptyContainer(fullSlot);

		if (tank.canFillWithMap(contents, true) && canAddStack(slotEmptyIndex, emptyContainer)) {
			tank.fillWithMap(contents, true);

			if (emptyContainer != null)
				addStack(slotEmptyIndex, emptyContainer);

			decrStackSize(slotFullIndex, 1);
			return true;
		}

		return false;
	}

	protected boolean fillTank(int slotFullIndex, int slotEmptyIndex, int tankIndex) {
		return fillTank(slotFullIndex, slotEmptyIndex, getTank(tankIndex));
	}

	protected boolean emptyTank(int slotEmptyIndex, int slotFullIndex, AdvFluidTank tank) {
		ItemStack emptySlot = getStackInSlot(slotEmptyIndex);

		if (emptySlot == null || tank == null || !validSlot(slotFullIndex))
			return false;

		FluidStack contents = tank.getFluid();

		if (emptySlot != null && contents != null) {
			ItemStack filled = FluidContainerRegistry.fillFluidContainer(contents.copy(), emptySlot.copy());
			if (filled != null) {
				int capacity = FluidUtil.getContainerCapacity(contents, filled);

				if (tank.canDrainWithMap(capacity) && canAddStack(slotFullIndex, filled)) {
					addStack(slotFullIndex, filled);
					tank.drainWithMap(capacity, true);
					decrStackSize(slotEmptyIndex, 1);
					return true;
				}
			}
		}
		return false;
	}

	protected boolean emptyTank(int slotEmptyIndex, int slotFullIndex, int tankIndex) {
		return emptyTank(slotEmptyIndex, slotFullIndex, getTank(tankIndex));
	}

	@Override
	public boolean canFill(ForgeDirection from, Fluid fluid) {
		for (AdvFluidTank tank : tanks)
			if (tank.canAccept(fluid.getID()))
				return true;
		return false;
	}

	@Override
	public boolean canDrain(ForgeDirection from, Fluid fluid) {
		for (AdvFluidTank tank : tanks)
			if (tank.canEject(fluid.getID()))
				return true;
		return false;
	}

	@Override
	public FluidTankInfo[] getTankInfo(ForgeDirection from) {
		FluidTankInfo[] info = new FluidTankInfo[tanks.size()];

		for (int x = 0; x < tanks.size(); x++)
			info[x] = tanks.get(x).getInfo();

		return info;
	}
}
