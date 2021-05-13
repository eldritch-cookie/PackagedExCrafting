package thelm.packagedexcrafting.slot;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;
import thelm.packagedexcrafting.tile.CombinationCrafterTile;

//Code from CoFHCore
public class CombinationCrafterRemoveOnlySlot extends SlotItemHandler {

	public final CombinationCrafterTile tile;

	public CombinationCrafterRemoveOnlySlot(CombinationCrafterTile tile, int index, int x, int y) {
		super(tile.getItemHandler(), index, x, y);
		this.tile = tile;
	}

	@Override
	public boolean canTakeStack(PlayerEntity playerIn) {
		return !tile.isWorking;
	}

	@Override
	public boolean isItemValid(ItemStack stack) {
		return false;
	}
}