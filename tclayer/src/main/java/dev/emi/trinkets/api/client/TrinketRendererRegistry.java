package dev.emi.trinkets.api.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.emi.trinkets.compat.WrappedTrinketInventory;
import dev.emi.trinkets.compat.WrappingTrinketsUtils;
import io.wispforest.accessories.api.client.AccessoriesRendererRegistry;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.api.client.AccessoryRenderer;
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

                    var reference = WrappingTrinketsUtils.createReference(ref);

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
            return Optional.ofNullable(AccessoriesRendererRegistry.getRender(item)).map(accessoryRenderer -> {
                return (stack, ref, contextModel, matrices, vertexConsumers, light, entity, limbAngle, limbDistance, tickDelta, animationProgress, headYaw, headPitch) -> {
                    var slotName = ((WrappedTrinketInventory) ref.inventory()).container.getSlotName();

                    var reference = SlotReference.of(entity, slotName, ref.index());

                    accessoryRenderer.render(stack, reference, matrices, contextModel, vertexConsumers, light, limbAngle, limbDistance, tickDelta, animationProgress, headYaw, headPitch);
                };
            });
        });
    }
}
