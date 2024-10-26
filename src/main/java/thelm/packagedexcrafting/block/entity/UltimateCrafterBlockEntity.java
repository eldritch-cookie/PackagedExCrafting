package thelm.packagedexcrafting.block.entity;

import java.util.List;

import com.blakebr0.extendedcrafting.api.TableCraftingInput;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import thelm.packagedauto.api.IPackageCraftingMachine;
import thelm.packagedauto.api.IPackageRecipeInfo;
import thelm.packagedauto.block.PackagedAutoBlocks;
import thelm.packagedauto.block.entity.BaseBlockEntity;
import thelm.packagedauto.energy.EnergyStorage;
import thelm.packagedauto.util.MiscHelper;
import thelm.packagedexcrafting.inventory.UltimateCrafterItemHandler;
import thelm.packagedexcrafting.menu.UltimateCrafterMenu;
import thelm.packagedexcrafting.recipe.ITablePackageRecipeInfo;

public class UltimateCrafterBlockEntity extends BaseBlockEntity implements IPackageCraftingMachine {

	public static int energyCapacity = 5000;
	public static int energyReq = 5000;
	public static int energyUsage = 500;
	public static boolean drawMEEnergy = true;

	public boolean isWorking = false;
	public int remainingProgress = 0;
	public ITablePackageRecipeInfo currentRecipe;

	public UltimateCrafterBlockEntity(BlockPos pos, BlockState state) {
		super(PackagedExCraftingBlockEntities.ULTIMATE_CRAFTER.get(), pos, state);
		setItemHandler(new UltimateCrafterItemHandler(this));
		setEnergyStorage(new EnergyStorage(this, energyCapacity));
	}

	@Override
	protected Component getDefaultName() {
		return Component.translatable("block.packagedexcrafting.ultimate_crafter");
	}

	@Override
	public void tick() {
		if(!level.isClientSide) {
			if(isWorking) {
				tickProcess();
				if(remainingProgress <= 0) {
					finishProcess();
					ejectItems();
				}
			}
			chargeEnergy();
			if(level.getGameTime() % 8 == 0) {
				ejectItems();
			}
		}
	}

	@Override
	public boolean acceptPackage(IPackageRecipeInfo recipeInfo, List<ItemStack> stacks, Direction direction) {
		if(!isBusy() && recipeInfo.isValid() && recipeInfo instanceof ITablePackageRecipeInfo recipe) {
			if(recipe.getTier() == 4) {
				ItemStack slotStack = itemHandler.getStackInSlot(81);
				ItemStack outputStack = recipe.getOutput();
				if(slotStack.isEmpty() || ItemStack.isSameItemSameComponents(slotStack, outputStack) && slotStack.getCount()+outputStack.getCount() <= outputStack.getMaxStackSize()) {
					currentRecipe = recipe;
					isWorking = true;
					remainingProgress = energyReq;
					TableCraftingInput matrix = recipe.getMatrix();
					for(int i = 0; i < matrix.height(); ++i) {
						for(int j = 0; j < matrix.width(); ++j) {
							itemHandler.setStackInSlot(i*9+j, matrix.getItem(i*matrix.width()+j).copy());
						}
					}
					setChanged();
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public boolean isBusy() {
		return isWorking || !itemHandler.getStacks().subList(0, 81).stream().allMatch(ItemStack::isEmpty);
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
		if(itemHandler.getStackInSlot(81).isEmpty()) {
			itemHandler.setStackInSlot(81, currentRecipe.getOutput());
		}
		else {
			itemHandler.getStackInSlot(81).grow(currentRecipe.getOutput().getCount());
		}
		List<ItemStack> remainingItems = currentRecipe.getRemainingItems();
		TableCraftingInput matrix = currentRecipe.getMatrix();
		for(int i = 0; i < matrix.height(); ++i) {
			for(int j = 0; j < matrix.width(); ++j) {
				itemHandler.setStackInSlot(i*9+j, remainingItems.get(i*matrix.width()+j));
			}
		}
		endProcess();
	}

	public void endProcess() {
		remainingProgress = 0;
		isWorking = false;
		currentRecipe = null;
		setChanged();
	}

	protected void ejectItems() {
		int endIndex = isWorking ? 81 : 0;
		for(Direction direction : Direction.values()) {
			BlockPos offsetPos = worldPosition.relative(direction);
			Block block = level.getBlockState(offsetPos).getBlock();
			IItemHandler itemHandler = level.getCapability(Capabilities.ItemHandler.BLOCK, offsetPos, direction.getOpposite());
			if(block != PackagedAutoBlocks.UNPACKAGER.get() && itemHandler != null) {
				for(int i = 81; i >= endIndex; --i) {
					ItemStack stack = this.itemHandler.getStackInSlot(i);
					if(stack.isEmpty()) {
						continue;
					}
					ItemStack stackRem = ItemHandlerHelper.insertItem(itemHandler, stack, false);
					this.itemHandler.setStackInSlot(i, stackRem);
				}
			}
		}
	}

	protected void chargeEnergy() {
		ItemStack energyStack = itemHandler.getStackInSlot(82);
		IEnergyStorage itemEnergyStorage = energyStack.getCapability(Capabilities.EnergyStorage.ITEM);
		if(itemEnergyStorage != null) {
			int energyRequest = Math.min(energyStorage.getMaxReceive(), energyStorage.getMaxEnergyStored() - energyStorage.getEnergyStored());
			energyStorage.receiveEnergy(itemEnergyStorage.extractEnergy(energyRequest, false), false);
			if(energyStack.getCount() <= 0) {
				itemHandler.setStackInSlot(82, ItemStack.EMPTY);
			}
		}
	}

	@Override
	public int getComparatorSignal() {
		if(isWorking) {
			return 1;
		}
		if(!itemHandler.getStacks().subList(0, 82).stream().allMatch(ItemStack::isEmpty)) {
			return 15;
		}
		return 0;
	}

	@Override
	public void loadAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
		super.loadAdditional(nbt, registries);
		currentRecipe = null;
		isWorking = nbt.getBoolean("working");
		remainingProgress = nbt.getInt("progress");
		if(nbt.contains("recipe")) {
			CompoundTag tag = nbt.getCompound("recipe");
			IPackageRecipeInfo recipe = MiscHelper.INSTANCE.loadRecipe(tag, registries);
			if(recipe instanceof ITablePackageRecipeInfo tableRecipe && tableRecipe.getTier() == 4) {
				currentRecipe = tableRecipe;
			}
		}
	}

	@Override
	public void saveAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
		super.saveAdditional(nbt, registries);
		nbt.putBoolean("working", isWorking);
		nbt.putInt("progress", remainingProgress);
		if(currentRecipe != null) {
			CompoundTag tag = MiscHelper.INSTANCE.saveRecipe(new CompoundTag(), currentRecipe, registries);
			nbt.put("recipe", tag);
		}
	}

	public int getScaledEnergy(int scale) {
		if(energyStorage.getMaxEnergyStored() <= 0) {
			return 0;
		}
		return Math.min(scale * energyStorage.getEnergyStored() / energyStorage.getMaxEnergyStored(), scale);
	}

	public int getScaledProgress(int scale) {
		if(remainingProgress <= 0 || energyReq <= 0) {
			return 0;
		}
		return scale * (energyReq-remainingProgress) / energyReq;
	}

	@Override
	public AbstractContainerMenu createMenu(int windowId, Inventory inventory, Player player) {
		sync(false);
		return new UltimateCrafterMenu(windowId, inventory, this);
	}
}
