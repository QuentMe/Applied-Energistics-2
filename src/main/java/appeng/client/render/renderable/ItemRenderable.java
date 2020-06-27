/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved.
 *
 * Applied Energistics 2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Applied Energistics 2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Applied Energistics 2.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.client.render.renderable;

import java.util.function.Function;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.json.ModelTransformation;
import org.apache.commons.lang3.tuple.Pair;

import net.minecraft.client.renderer.TransformationMatrix;
import net.minecraft.item.ItemStack;

public class ItemRenderable<T extends BlockEntity> implements Renderable<T> {

    private final Function<T, Pair<ItemStack, TransformationMatrix>> f;

    public ItemRenderable(Function<T, Pair<ItemStack, TransformationMatrix>> f) {
        this.f = f;
    }

    @Override
    public void renderTileEntityAt(T te, float partialTicks, net.minecraft.client.util.math.MatrixStack matrixStack,
                                   VertexConsumerProvider buffers, int combinedLight, int combinedOverlay) {
        Pair<ItemStack, TransformationMatrix> pair = this.f.apply(te);
        if (pair != null && pair.getLeft() != null) {
            if (pair.getRight() != null) {
                pair.getRight().push(matrixStack);
            } else {
                matrixStack.push();
            }
            MinecraftClient.getInstance().getItemRenderer().renderItem(pair.getLeft(),
                    ModelTransformation.TransformType.GROUND, combinedLight, combinedOverlay, matrixStack, buffers);
            matrixStack.pop();
        }
    }

}
