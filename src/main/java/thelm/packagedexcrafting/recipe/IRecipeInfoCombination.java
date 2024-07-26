package thelm.packagedexcrafting.recipe;

import java.util.Collections;
import java.util.List;

import com.blakebr0.extendedcrafting.crafting.CombinationRecipe;

import net.minecraft.item.ItemStack;
import thelm.packagedauto.api.IRecipeInfo;

public interface IRecipeInfoCombination extends IRecipeInfo {

	ItemStack getCoreInput();

	List<ItemStack> getPedestalInputs();

	ItemStack getOutput();

	long getEnergyRequired();

	int getEnergyUsage();

	CombinationRecipe getRecipe();

	@Override
	default List<ItemStack> getOutputs() {
		ItemStack output = getOutput();
		return output.isEmpty() ? Collections.emptyList() : Collections.singletonList(output);
	}
}
