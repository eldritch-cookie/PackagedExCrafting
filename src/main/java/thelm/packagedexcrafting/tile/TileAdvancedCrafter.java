package thelm.packagedexcrafting.tile;

import java.util.List;

import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionHost;
import appeng.api.util.AECableType;
import appeng.api.util.AEPartLocation;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import thelm.packagedauto.api.IPackageCraftingMachine;
import thelm.packagedauto.api.IRecipeInfo;
import thelm.packagedauto.api.MiscUtil;
import thelm.packagedauto.energy.EnergyStorage;
import thelm.packagedauto.tile.TileBase;
import thelm.packagedauto.tile.TileUnpackager;
import thelm.packagedexcrafting.client.gui.GuiAdvancedCrafter;
import thelm.packagedexcrafting.container.ContainerAdvancedCrafter;
import thelm.packagedexcrafting.integration.appeng.networking.HostHelperTileAdvancedCrafter;
import thelm.packagedexcrafting.inventory.InventoryAdvancedCrafter;
import thelm.packagedexcrafting.recipe.IRecipeInfoTiered;

@Optional.InterfaceList({
	@Optional.Interface(iface="appeng.api.networking.IGridHost", modid="appliedenergistics2"),
	@Optional.Interface(iface="appeng.api.networking.security.IActionHost", modid="appliedenergistics2"),
})
public class TileAdvancedCrafter extends TileBase implements ITickable, IPackageCraftingMachine, IGridHost, IActionHost {

	public static boolean enabled = true;

	public static int energyCapacity = 5000;
	public static int energyReq = 1000;
	public static int energyUsage = 125;
	public static boolean drawMEEnergy = true;

	public boolean isWorking = false;
	public int remainingProgress = 0;
	public IRecipeInfoTiered currentRecipe;

	public TileAdvancedCrafter() {
		setInventory(new InventoryAdvancedCrafter(this));
		setEnergyStorage(new EnergyStorage(this, energyCapacity));
		if(Loader.isModLoaded("appliedenergistics2")) {
			hostHelper = new HostHelperTileAdvancedCrafter(this);
		}
	}

	@Override
	protected String getLocalizedName() {
		return I18n.translateToLocal("tile.packagedexcrafting.advanced_crafter.name");
	}

	@Override
	public void update() {
		if(!world.isRemote) {
			if(isWorking) {
				tickProcess();
				if(remainingProgress <= 0) {
					finishProcess();
					if(hostHelper != null && hostHelper.isActive()) {
						hostHelper.ejectItem();
					}
					else {
						ejectItems();
					}
				}
			}
			chargeEnergy();
			if(world.getTotalWorldTime() % 8 == 0) {
				if(hostHelper != null && hostHelper.isActive()) {
					hostHelper.ejectItem();
					if(drawMEEnergy) {
						hostHelper.chargeEnergy();
					}
				}
				else {
					ejectItems();
				}
			}
		}
	}

	@Override
	public boolean acceptPackage(IRecipeInfo recipeInfo, List<ItemStack> stacks, EnumFacing facing) {
		if(!isBusy() && recipeInfo instanceof IRecipeInfoTiered) {
			IRecipeInfoTiered recipe = (IRecipeInfoTiered)recipeInfo;
			if(recipe.getTier() == 2) {
				ItemStack slotStack = inventory.getStackInSlot(25);
				ItemStack outputStack = recipe.getOutput();
				if(slotStack.isEmpty() || slotStack.getItem() == outputStack.getItem() && slotStack.getItemDamage() == outputStack.getItemDamage() && ItemStack.areItemStackShareTagsEqual(slotStack, outputStack) && slotStack.getCount()+outputStack.getCount() <= outputStack.getMaxStackSize()) {
					currentRecipe = recipe;
					isWorking = true;
					remainingProgress = energyReq;
					for(int i = 0; i < 25; ++i) {
						inventory.setInventorySlotContents(i, recipe.getMatrix().getStackInSlot(i).copy());
					}
					markDirty();
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public boolean isBusy() {
		return isWorking || !inventory.stacks.subList(0, 25).stream().allMatch(ItemStack::isEmpty);
	}

	protected void tickProcess() {
		int energy = energyStorage.extractEnergy(Math.min(energyUsage, remainingProgress), false);
		remainingProgress -= energy;
	}

	protected void finishProcess() {
		if(currentRecipe == null) {
			endProcess();
			return;
		}
		if(inventory.getStackInSlot(25).isEmpty()) {
			inventory.setInventorySlotContents(25, currentRecipe.getOutput());
		}
		else {
			inventory.getStackInSlot(25).grow(currentRecipe.getOutput().getCount());
		}
		for(int i = 0; i < 25; ++i) {
			inventory.setInventorySlotContents(i, MiscUtil.getContainerItem(inventory.getStackInSlot(i)));
		}
		endProcess();
	}

	public void endProcess() {
		remainingProgress = 0;
		isWorking = false;
		currentRecipe = null;
		markDirty();
	}

	protected void ejectItems() {
		int endIndex = isWorking ? 25 : 0;
		for(EnumFacing facing : EnumFacing.VALUES) {
			TileEntity tile = world.getTileEntity(pos.offset(facing));
			if(tile != null && !(tile instanceof TileUnpackager) && tile.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing.getOpposite())) {
				IItemHandler itemHandler = tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing.getOpposite());
				for(int i = 25; i >= endIndex; --i) {
					ItemStack stack = inventory.getStackInSlot(i);
					if(stack.isEmpty()) {
						continue;
					}
					ItemStack stackRem = ItemHandlerHelper.insertItem(itemHandler, stack, false);
					inventory.setInventorySlotContents(i, stackRem);
				}
			}
		}
	}

	protected void chargeEnergy() {
		ItemStack energyStack = inventory.getStackInSlot(26);
		if(energyStack.hasCapability(CapabilityEnergy.ENERGY, null)) {
			int energyRequest = Math.min(energyStorage.getMaxReceive(), energyStorage.getMaxEnergyStored() - energyStorage.getEnergyStored());
			energyStorage.receiveEnergy(energyStack.getCapability(CapabilityEnergy.ENERGY, null).extractEnergy(energyRequest, false), false);
			if(energyStack.getCount() <= 0) {
				inventory.setInventorySlotContents(26, ItemStack.EMPTY);
			}
		}
	}

	@Override
	public int getComparatorSignal() {
		if(isWorking) {
			return 1;
		}
		if(!inventory.stacks.subList(0, 26).stream().allMatch(ItemStack::isEmpty)) {
			return 15;
		}
		return 0;
	}

	public HostHelperTileAdvancedCrafter hostHelper;

	@Override
	public void invalidate() {
		super.invalidate();
		if(hostHelper != null) {
			hostHelper.invalidate();
		}
	}

	@Override
	public void onChunkUnload() {
		super.onChunkUnload();
		if(hostHelper != null) {
			hostHelper.invalidate();
		}
	}

	@Optional.Method(modid="appliedenergistics2")
	@Override
	public IGridNode getGridNode(AEPartLocation dir) {
		return getActionableNode();
	}

	@Optional.Method(modid="appliedenergistics2")
	@Override
	public AECableType getCableConnectionType(AEPartLocation dir) {
		return AECableType.SMART;
	}

	@Optional.Method(modid="appliedenergistics2")
	@Override
	public void securityBreak() {
		world.destroyBlock(pos, true);
	}

	@Optional.Method(modid="appliedenergistics2")
	@Override
	public IGridNode getActionableNode() {
		return hostHelper.getNode();
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		isWorking = nbt.getBoolean("Working");
		remainingProgress = nbt.getInteger("Progress");
		currentRecipe = null;
		if(nbt.hasKey("Recipe")) {
			NBTTagCompound tag = nbt.getCompoundTag("Recipe");
			IRecipeInfo recipe = MiscUtil.readRecipeFromNBT(tag);
			if(recipe instanceof IRecipeInfoTiered && ((IRecipeInfoTiered)recipe).getTier() == 2) {
				currentRecipe = (IRecipeInfoTiered)recipe;
			}
		}
		if(hostHelper != null) {
			hostHelper.readFromNBT(nbt);
		}
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setBoolean("Working", isWorking);
		nbt.setInteger("Progress", remainingProgress);
		if(currentRecipe != null) {
			NBTTagCompound tag = MiscUtil.writeRecipeToNBT(new NBTTagCompound(), currentRecipe);
			nbt.setTag("Recipe", tag);
		}
		if(hostHelper != null) {
			hostHelper.writeToNBT(nbt);
		}
		return nbt;
	}

	public int getScaledEnergy(int scale) {
		if(energyStorage.getMaxEnergyStored() <= 0) {
			return 0;
		}
		return scale * energyStorage.getEnergyStored() / energyStorage.getMaxEnergyStored();
	}

	public int getScaledProgress(int scale) {
		if(remainingProgress <= 0) {
			return 0;
		}
		return scale * (energyReq-remainingProgress) / energyReq;
	}

	@SideOnly(Side.CLIENT)
	@Override
	public GuiContainer getClientGuiElement(EntityPlayer player, Object... args) {
		return new GuiAdvancedCrafter(new ContainerAdvancedCrafter(player.inventory, this));
	}

	@Override
	public Container getServerGuiElement(EntityPlayer player, Object... args) {
		return new ContainerAdvancedCrafter(player.inventory, this);
	}
}
