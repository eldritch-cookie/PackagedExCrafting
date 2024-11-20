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

public class AdvancedPackageRecipeInfo implements ITablePackageRecipeInfo {

	public static final MapCodec<AdvancedPackageRecipeInfo> MAP_CODEC = RecordCodecBuilder.mapCodec(instance->instance.group(
			ResourceLocation.CODEC.fieldOf("id").forGetter(AdvancedPackageRecipeInfo::getRecipeId),
			Codec.INT.fieldOf("width").forGetter(AdvancedPackageRecipeInfo::getMatrixWidth),
			Codec.INT.fieldOf("height").forGetter(AdvancedPackageRecipeInfo::getMatrixHeight),
			ItemStack.OPTIONAL_CODEC.orElse(ItemStack.EMPTY).sizeLimitedListOf(25).fieldOf("input").forGetter(AdvancedPackageRecipeInfo::getMatrixAsList)).
			apply(instance, AdvancedPackageRecipeInfo::new));
	public static final Codec<AdvancedPackageRecipeInfo> CODEC = MAP_CODEC.codec();
	public static final StreamCodec<RegistryFriendlyByteBuf, AdvancedPackageRecipeInfo> STREAM_CODEC = StreamCodec.composite(
			ResourceLocation.STREAM_CODEC, AdvancedPackageRecipeInfo::getRecipeId,
			ByteBufCodecs.INT, AdvancedPackageRecipeInfo::getMatrixWidth,
			ByteBufCodecs.INT, AdvancedPackageRecipeInfo::getMatrixHeight,
			ItemStack.OPTIONAL_LIST_STREAM_CODEC, AdvancedPackageRecipeInfo::getMatrixAsList,
			AdvancedPackageRecipeInfo::new);

	private final ResourceLocation id;
	private final ITableRecipe recipe;
	private final List<ItemStack> input;
	private final TableCraftingInput matrix;
	private final ItemStack output;
	private final List<IPackagePattern> patterns = new ArrayList<>();

	public AdvancedPackageRecipeInfo(ResourceLocation id, int width, int height, List<ItemStack> matrixSer) {
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

	public AdvancedPackageRecipeInfo(List<ItemStack> inputs, Level level) {
		NonNullList<ItemStack> matrixList = NonNullList.withSize(25, ItemStack.EMPTY);
		int[] slotArray = AdvancedPackageRecipeType.SLOTS.toIntArray();
		for(int i = 0; i < 25; ++i) {
			ItemStack toSet = inputs.get(slotArray[i]);
			toSet.setCount(1);
			matrixList.set(i, toSet.copy());
		}
		matrix = TableCraftingInput.of(5, 5, matrixList, 2);
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
		return AdvancedPackageRecipeType.INSTANCE;
	}

	@Override
	public int getTier() {
		return 2;
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
		int[] slotArray = AdvancedPackageRecipeType.SLOTS.toIntArray();
		for(int i = 0; i < getMatrixHeight() * getMatrixWidth(); ++i) {
			map.put(slotArray[i], matrix.getItem(i));
		}
		return map;
	}

	@Override
	public boolean equals(Object obj) {
		if(obj instanceof AdvancedPackageRecipeInfo other) {
			return MiscHelper.INSTANCE.recipeEquals(this, recipe, other, other.recipe);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return MiscHelper.INSTANCE.recipeHashCode(this, recipe);
	}
}
