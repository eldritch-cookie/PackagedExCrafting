package thelm.packagedexcrafting.recipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.blakebr0.extendedcrafting.api.TableCraftingInput;
import com.blakebr0.extendedcrafting.api.crafting.ITableRecipe;
import com.blakebr0.extendedcrafting.init.ModRecipeTypes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import thelm.packagedauto.api.IPackagePattern;
import thelm.packagedauto.api.IPackageRecipeType;
import thelm.packagedauto.util.MiscHelper;
import thelm.packagedauto.util.PackagePattern;

public class UltimatePackageRecipeInfo implements ITablePackageRecipeInfo {

	public static final MapCodec<UltimatePackageRecipeInfo> MAP_CODEC = RecordCodecBuilder.mapCodec(instance->instance.group(
			ResourceLocation.CODEC.fieldOf("id").forGetter(UltimatePackageRecipeInfo::getRecipeId),
			Codec.INT.fieldOf("width").forGetter(UltimatePackageRecipeInfo::getMatrixWidth),
			Codec.INT.fieldOf("height").forGetter(UltimatePackageRecipeInfo::getMatrixHeight),
			ItemStack.OPTIONAL_CODEC.orElse(ItemStack.EMPTY).sizeLimitedListOf(81).fieldOf("input").forGetter(UltimatePackageRecipeInfo::getMatrixAsList)).
			apply(instance, UltimatePackageRecipeInfo::new));
	public static final Codec<UltimatePackageRecipeInfo> CODEC = MAP_CODEC.codec();
	public static final StreamCodec<RegistryFriendlyByteBuf, UltimatePackageRecipeInfo> STREAM_CODEC = StreamCodec.composite(
			ResourceLocation.STREAM_CODEC, UltimatePackageRecipeInfo::getRecipeId,
			ByteBufCodecs.INT, UltimatePackageRecipeInfo::getMatrixWidth,
			ByteBufCodecs.INT, UltimatePackageRecipeInfo::getMatrixHeight,
			ItemStack.OPTIONAL_LIST_STREAM_CODEC, UltimatePackageRecipeInfo::getMatrixAsList,
			UltimatePackageRecipeInfo::new);

	private final ResourceLocation id;
	private final ITableRecipe recipe;
	private final List<ItemStack> input;
	private final TableCraftingInput matrix;
	private final ItemStack output;
	private final List<IPackagePattern> patterns = new ArrayList<>();

	public UltimatePackageRecipeInfo(ResourceLocation id, int width, int height, List<ItemStack> matrixSer) {
		this.id = id;
		matrix = TableCraftingInput.of(width, height, matrixSer, 2);
		input = MiscHelper.INSTANCE.condenseStacks(matrix.items());
		for(int i = 0; i*9 < input.size(); ++i) {
			patterns.add(new PackagePattern(this, i));
		}
		Recipe<?> recipeSer = MiscHelper.INSTANCE.getRecipeManager().byKey(id).map(RecipeHolder::value).orElse(null);
		if(recipeSer instanceof ITableRecipe tableRecipe) {
			recipe = tableRecipe;
			output = recipe.assemble(matrix, MiscHelper.INSTANCE.getRegistryAccess()).copy();
		}
		else {
			recipe = null;
			output = ItemStack.EMPTY;
		}
	}

	public UltimatePackageRecipeInfo(List<ItemStack> inputs, Level level) {
		NonNullList<ItemStack> matrixList = NonNullList.withSize(81, ItemStack.EMPTY);
		int[] slotArray = UltimatePackageRecipeType.SLOTS.toIntArray();
		for(int i = 0; i < 81; ++i) {
			ItemStack toSet = inputs.get(slotArray[i]);
			toSet.setCount(1);
			matrixList.set(i, toSet.copy());
		}
		matrix = TableCraftingInput.of(9, 9, matrixList, 4);
		RecipeHolder<ITableRecipe> recipeHolder = MiscHelper.INSTANCE.getRecipeManager().getRecipeFor(ModRecipeTypes.TABLE.get(), matrix, level).orElse(null);
		if(recipeHolder != null) {
			id = recipeHolder.id();
			recipe = recipeHolder.value();
			output = recipe.assemble(matrix, level.registryAccess()).copy();
		}
		else {
			id = null;
			recipe = null;
			output = null;
		}
		input = MiscHelper.INSTANCE.condenseStacks(matrix.items());
		for(int i = 0; i*9 < input.size(); ++i) {
			this.patterns.add(new PackagePattern(this, i));
		}
	}

	@Override
	public IPackageRecipeType getRecipeType() {
		return UltimatePackageRecipeType.INSTANCE;
	}

	@Override
	public int getTier() {
		return 4;
	}

	@Override
	public boolean isValid() {
		return id != null && recipe != null;
	}

	@Override
	public List<IPackagePattern> getPatterns() {
		return Collections.unmodifiableList(patterns);
	}

	@Override
	public List<ItemStack> getInputs() {
		return Collections.unmodifiableList(input);
	}

	@Override
	public ItemStack getOutput() {
		return output.copy();
	}

	@Override
	public ITableRecipe getRecipe() {
		return recipe;
	}

	public ResourceLocation getRecipeId() {
		return id;
	}

	@Override
	public TableCraftingInput getMatrix() {
		return matrix;
	}

	public List<ItemStack> getMatrixAsList() {
		return Collections.unmodifiableList(matrix.items());
	}

	public int getMatrixWidth() {
		return matrix.width();
	}

	public int getMatrixHeight() {
		return matrix.height();
	}

	@Override
	public List<ItemStack> getRemainingItems() {
		return recipe.getRemainingItems(matrix);
	}

	@Override
	public Int2ObjectMap<ItemStack> getEncoderStacks() {
		Int2ObjectMap<ItemStack> map = new Int2ObjectOpenHashMap<>();
		for(int i = 0; i < getMatrixHeight() * getMatrixWidth(); ++i) {
			map.put(i, matrix.getItem(i));
		}
		return map;
	}

	@Override
	public boolean equals(Object obj) {
		if(obj instanceof UltimatePackageRecipeInfo other) {
			return MiscHelper.INSTANCE.recipeEquals(this, recipe, other, other.recipe);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return MiscHelper.INSTANCE.recipeHashCode(this, recipe);
	}
}
