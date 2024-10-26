package thelm.packagedexcrafting.recipe;

import java.util.List;

import com.blakebr0.extendedcrafting.api.crafting.IEnderCrafterRecipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import thelm.packagedauto.api.IPackageRecipeInfo;

public interface IEnderPackageRecipeInfo extends IPackageRecipeInfo {

	ItemStack getOutput();

	IEnderCrafterRecipe getRecipe();

	CraftingInput getMatrix();

	int getTimeRequired();

	List<ItemStack> getRemainingItems();

	@Override
	default List<ItemStack> getOutputs() {
		ItemStack output = getOutput();
		return output.isEmpty() ? List.of() : List.of(output);
	}
}
