package thelm.packagedexcrafting.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import thelm.packagedexcrafting.block.entity.CombinationCrafterBlockEntity;

public class CombinationCrafterRenderer implements BlockEntityRenderer<CombinationCrafterBlockEntity> {

	public CombinationCrafterRenderer(BlockEntityRendererProvider.Context context) {

	}

	@Override
	public void render(CombinationCrafterBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
		Minecraft minecraft = Minecraft.getInstance();
		ItemStack stack = blockEntity.getItemHandler().getStackInSlot(blockEntity.isWorking ? 0 : 1);
		if(!stack.isEmpty()) {
			poseStack.pushPose();
			poseStack.translate(0.5, 1.2, 0.5);
			float scale = stack.getItem() instanceof BlockItem ? 0.9F : 0.65F;
			poseStack.scale(scale, scale, scale);
			double tick = System.currentTimeMillis() / 800D;
			poseStack.translate(0, Math.sin(tick % (Math.PI*2)) * 0.065, 0);
			poseStack.mulPose(Vector3f.YP.rotationDegrees((float)(tick*40 % 360)));
			minecraft.getItemRenderer().renderStatic(stack, ItemTransforms.TransformType.GROUND, packedLight, packedOverlay, poseStack, bufferSource, 0);
			poseStack.popPose();
		}
	}
}