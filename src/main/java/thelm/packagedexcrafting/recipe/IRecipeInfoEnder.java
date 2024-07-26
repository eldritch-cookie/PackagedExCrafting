package thelm.packagedexcrafting.recipe;

import java.util.Collections;
import java.util.List;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import thelm.packagedauto.api.IRecipeInfo;

public interface IRecipeInfoEnder extends IRecipeInfo {

	ItemStack getOutput();

	IRecipe getRecipe();

	InventoryCrafting getMatrix();

	@Override
	default List<ItemStack> getOutputs() {
		ItemStack output = getOutput();
		return output.isEmpty() ? Collections.emptyList() : Collections.singletonList(output);
	}
}
