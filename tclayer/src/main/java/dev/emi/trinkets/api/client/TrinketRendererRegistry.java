/*
 * MIT License
 *
 * Copyright (c) 2019 Emily Rose Ploszaj
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package dev.emi.trinkets.api.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.emi.trinkets.compat.WrappedTrinketInventory;
import dev.emi.trinkets.compat.WrappingTrinketsUtils;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.client.AccessoriesRendererRegistry;
import io.wispforest.accessories.api.client.AccessoryRenderer;
import io.wispforest.accessories.api.client.DefaultAccessoryRenderer;
import io.wispforest.accessories.api.slot.SlotReference;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class TrinketRendererRegistry {
    private static final Map<Item, TrinketRenderer> RENDERERS = new HashMap<>();

    /**
     * Registers a trinket renderer for the provided item
     */
    public static void registerRenderer(Item item, TrinketRenderer trinketRenderer) {
        AccessoriesRendererRegistry.registerRenderer(item,
            () -> new AccessoryRenderer(){
                @Override
                public <M extends LivingEntity> void render(ItemStack stack, SlotReference ref, PoseStack matrices, EntityModel<M> model, MultiBufferSource multiBufferSource, int light, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
                    matrices.pushPose();

                    var reference = WrappingTrinketsUtils.createTrinketsReference(ref, true);

                    if(reference.isEmpty()) return;

                    trinketRenderer.render(stack, reference.get(), model, matrices, multiBufferSource, light, ref.entity(), limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch);

                    matrices.popPose();
                }
            }
        );

        RENDERERS.put(item, trinketRenderer);
    }

    public static Optional<TrinketRenderer> getRenderer(Item item) {
        return Optional.ofNullable(RENDERERS.get(item)).or(() -> {
            return Optional.ofNullable(AccessoriesRendererRegistry.getRender(item)).flatMap(accessoryRenderer -> {
                if(accessoryRenderer == DefaultAccessoryRenderer.INSTANCE && !Accessories.config().clientOptions.forceNullRenderReplacement()) {
                    return Optional.empty();
                }

                return Optional.of(
                        (stack, ref, contextModel, matrices, vertexConsumers, light, entity, limbAngle, limbDistance, tickDelta, animationProgress, headYaw, headPitch) -> {
                            var slotName = ((WrappedTrinketInventory) ref.inventory()).container.getSlotName();

                            var reference = SlotReference.of(entity, slotName, ref.index());

                            accessoryRenderer.render(stack, reference, matrices, contextModel, vertexConsumers, light, limbAngle, limbDistance, tickDelta, animationProgress, headYaw, headPitch);
                        }
                );
            });
        });
    }
}
