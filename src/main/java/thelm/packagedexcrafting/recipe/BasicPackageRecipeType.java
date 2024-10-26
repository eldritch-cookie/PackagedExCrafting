package thelm.packagedexcrafting.recipe;

import java.util.List;

import com.blakebr0.extendedcrafting.init.ModBlocks;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntRBTreeSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.core.Vec3i;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thelm.packagedauto.api.IPackageRecipeInfo;
import thelm.packagedauto.api.IPackageRecipeType;
import thelm.packagedauto.api.IRecipeSlotViewWrapper;
import thelm.packagedauto.api.IRecipeSlotsViewWrapper;

public class BasicPackageRecipeType implements IPackageRecipeType {

	public static final BasicPackageRecipeType INSTANCE = new BasicPackageRecipeType();
	public static final ResourceLocation NAME = ResourceLocation.parse("packagedexcrafting:basic");
	public static final IntSet SLOTS;
	public static final List<ResourceLocation> CATEGORIES = List.of(ResourceLocation.parse("extendedcrafting:basic_crafting"));
	public static final Vec3i COLOR = new Vec3i(139, 139, 139);
	public static final Vec3i COLOR_DISABLED = new Vec3i(64, 64, 64);

	static {
		SLOTS = new IntRBTreeSet();
		for(int i = 3; i < 6; ++i) {
			for(int j = 3; j < 6; ++j) {
				SLOTS.add(9*i+j);
			}
		}
	}

	protected BasicPackageRecipeType() {}

	@Override
	public ResourceLocation getName() {
		return NAME;
	}

	@Override
	public MutableComponent getDisplayName() {
		return Component.translatable("recipe.packagedexcrafting.basic");
	}

	@Override
	public MutableComponent getShortDisplayName() {
		return Component.translatable("recipe.packagedexcrafting.basic.short");
	}

	@Override
	public MapCodec<? extends IPackageRecipeInfo> getRecipeInfoMapCodec() {
		return BasicPackageRecipeInfo.MAP_CODEC;
	}

	@Override
	public Codec<? extends IPackageRecipeInfo> getRecipeInfoCodec() {
		return BasicPackageRecipeInfo.CODEC;
	}

	@Override
	public StreamCodec<RegistryFriendlyByteBuf, ? extends IPackageRecipeInfo> getRecipeInfoStreamCodec() {
		return BasicPackageRecipeInfo.STREAM_CODEC;
	}

	@Override
	public IPackageRecipeInfo generateRecipeInfoFromStacks(List<ItemStack> inputs, List<ItemStack> outputs, Level level) {
		return new BasicPackageRecipeInfo(inputs, level);
	}

	@Override
	public IntSet getEnabledSlots() {
		return SLOTS;
	}

	@Override
	public List<ResourceLocation> getJEICategories() {
		return CATEGORIES;
	}

	@Override
	public Int2ObjectMap<ItemStack> getRecipeTransferMap(IRecipeSlotsViewWrapper recipeLayoutWrapper) {
		Int2ObjectMap<ItemStack> map = new Int2ObjectOpenHashMap<>();
		List<IRecipeSlotViewWrapper> slotViews = recipeLayoutWrapper.getRecipeSlotViews();
		int index = 0;
		int[] slotArray = SLOTS.toIntArray();
		for(IRecipeSlotViewWrapper slotView : slotViews) {
			if(slotView.isInput()) {
				Object displayed = slotView.getDisplayedIngredient().orElse(null);
				if(displayed instanceof ItemStack stack && !stack.isEmpty()) {
					map.put(slotArray[index], stack);
				}
				++index;
			}
			if(index >= 9) {
				break;
			}
		}
		return map;
	}

	@Override
	public Object getRepresentation() {
		return new ItemStack(ModBlocks.BASIC_TABLE.get());
	}

	@Override
	public Vec3i getSlotColor(int slot) {
		if(!SLOTS.contains(slot) && slot != 81) {
			return COLOR_DISABLED;
		}
		return COLOR;
	}
}
