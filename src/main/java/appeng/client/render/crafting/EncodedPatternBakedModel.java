package appeng.client.render.crafting;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableMap;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelOverrideList;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.util.math.MatrixStack;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.renderer.TransformationMatrix;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.client.model.PerspectiveMapWrapper;

import appeng.client.render.DelegateBakedModel;
import appeng.items.misc.EncodedPatternItem;

/**
 * This model is used to substitute the crafting result's item model for our
 * encoded pattern model if these two conditions are met: - The player is
 * holding shift - The itemstack is being rendered in the UI (not on the ground,
 * etc.)
 *
 * We do this by abusing a custom {@link ModelOverrideList} since it will be
 * called each frame with the itemstack that is about to be rendered. We return
 * a custom IBakedModel ({@link ShiftHoldingModelWrapper} if the player is
 * holding down shift from the override list. This custom baked model implements
 * {@link #doesHandlePerspectives()} and returns the crafting result model if
 * the model for {@link ModelTransformation.TransformType#GUI} is requested.
 */
public class EncodedPatternBakedModel extends DelegateBakedModel {

    private final CustomOverrideList overrides;

    EncodedPatternBakedModel(BakedModel baseModel) {
        super(baseModel);
        this.overrides = new CustomOverrideList();
    }

    @Override
    public ModelOverrideList getOverrides() {
        return this.overrides;
    }

    /**
     * Since the ItemOverrideList handling comes before handling the perspective
     * awareness (which is the first place where we know how we are being rendered)
     * we need to remember the model of the crafting output, and make the decision
     * on which to render later on. Sadly, Forge is pretty inconsistent when it will
     * call the handlePerspective method, so some methods are called even on this
     * interim-model. Usually those methods only matter for rendering on the ground
     * and other cases, where we wouldn't render the crafting output model anyway,
     * so in those cases we delegate to the model of the encoded pattern.
     */
    private static class ShiftHoldingModelWrapper extends DelegateBakedModel {

        private final BakedModel outputModel;

        private ShiftHoldingModelWrapper(BakedModel patternModel, BakedModel outputModel) {
            super(patternModel);
            this.outputModel = outputModel;
        }

        @Override
        public boolean doesHandlePerspectives() {
            return true;
        }

        @Override
        public BakedModel handlePerspective(ModelTransformation.TransformType cameraTransformType, MatrixStack mat) {
            // No need to re-check for shift being held since this model is only handed out
            // in that case
            if (cameraTransformType == ModelTransformation.TransformType.GUI) {
                ImmutableMap<ModelTransformation.TransformType, TransformationMatrix> transforms = PerspectiveMapWrapper
                        .getTransforms(outputModel.getTransformation());
                return PerspectiveMapWrapper.handlePerspective(this.outputModel, transforms, cameraTransformType, mat);
            } else {
                return getBaseModel().handlePerspective(cameraTransformType, mat);
            }
        }

        // This determines diffuse lighting in the UI, and since we want to render
        // the outputModel in the UI, we need to use it's setting here
        @Override
        public boolean isSideLit() {
            return outputModel.isSideLit();
        }

    }

    @Override
    public boolean doesHandlePerspectives() {
        return true;
    }

    @Override
    public BakedModel handlePerspective(ModelTransformation.TransformType cameraTransformType, MatrixStack mat) {
        return getBaseModel().handlePerspective(cameraTransformType, mat);
    }

    /**
     * Item Override Lists are the only point during item rendering where we can
     * access the item stack that is being rendered. So this is the point where we
     * actually check if shift is being held, and if so, determine the crafting
     * output model.
     */
    private class CustomOverrideList extends ModelOverrideList {

        @Nullable
        @Override
        public BakedModel getModelWithOverrides(BakedModel originalModel, ItemStack stack, @Nullable World world,
                                                @Nullable LivingEntity entity) {
            boolean shiftHeld = Screen.hasShiftDown();
            if (shiftHeld) {
                EncodedPatternItem iep = (EncodedPatternItem) stack.getItem();
                ItemStack output = iep.getOutput(stack);
                if (!output.isEmpty()) {
                    BakedModel realModel = MinecraftClient.getInstance().getItemRenderer().getItemModelMesher()
                            .getItemModel(output);
                    // Give the item model a chance to handle the overrides as well
                    realModel = realModel.getOverrides().getModelWithOverrides(realModel, output, world, entity);
                    return new ShiftHoldingModelWrapper(getBaseModel(), realModel);
                }
            }

            return getBaseModel().getOverrides().getModelWithOverrides(originalModel, stack, world, entity);
        }
    }

}
