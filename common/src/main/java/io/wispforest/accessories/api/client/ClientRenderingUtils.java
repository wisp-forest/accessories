package io.wispforest.accessories.api.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import io.wispforest.accessories.data.CustomRendererLoader;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.slf4j.Logger;

import java.util.List;

@ApiStatus.Experimental
@Environment(EnvType.CLIENT)
public class ClientRenderingUtils {
    public static void handle(List<RenderingFunction> functions, @Nullable HumanoidArm arm, LivingEntity targetEntity, EntityModel<? extends LivingEntity> entityModel, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay, int color) {
        var client = Minecraft.getInstance();
        var level = Minecraft.getInstance().level;

        var tickRateManager = level.tickRateManager();

        var partialTicks = client.getTimer().getGameTimeDeltaPartialTick(!tickRateManager.isEntityFrozen(targetEntity));

        for (var function : functions) {
            switch (function) {
                case RenderingFunction.Transformation transformation -> {
                    poseStack.pushPose();

                    ClientTransformationUtils.transformStack(transformation.transformations(), poseStack, entityModel, () -> handle(List.of(transformation.renderingFunction()), arm, targetEntity, entityModel, poseStack, buffer, packedLight, packedOverlay, color));

                    poseStack.popPose();
                }
                case RenderingFunction.Block blockData -> {
                    var state = blockData.state();
                    var blockEntity = (blockData.type() != null) ? net.minecraft.world.level.block.entity.BlockEntity.loadStatic(BlockPos.ZERO, blockData.state(), blockData.data(), level.registryAccess()) : null;

                    poseStack.translate(-0.5, 0, -0.5);

                    renderBlock(client, state, blockEntity, 0, poseStack, buffer, packedLight, packedOverlay, color);
                }
                case RenderingFunction.Entity entityData -> {
                    Entity entity;

                    try {
                        entity = entityData.entityType().create(level);

                        if (entity == null) return;

                        entity.load(entityData.data());
                    } catch (RuntimeException var3) {
                        return;
                    }

                    client.getEntityRenderDispatcher()
                            .render(entity, 0, 0, 0, 0, partialTicks, poseStack, buffer, packedLight);
                }
                case RenderingFunction.Item itemData -> {
                    ItemStack stack = itemData.stack();

                    client.getItemRenderer().render(
                            stack,
                            ItemDisplayContext.GUI,
                            false,
                            poseStack,
                            buffer,
                            packedLight,
                            packedOverlay,
                            client.getItemRenderer().getModel(stack, level, null, 0)
                    );
                }
                case RenderingFunction.Model modelData -> {
                    var model = Minecraft.getInstance().getModelManager().getModel(new ModelResourceLocation(modelData.id(), modelData.variant()));

                    client.getItemRenderer().render(
                            Items.BEDROCK.getDefaultInstance(),
                            ItemDisplayContext.GROUND,
                            false,
                            poseStack,
                            buffer,
                            packedLight,
                            packedOverlay,
                            model
                    );
                }
                case RenderingFunction.Particle particleData -> {
                    var entityPos = targetEntity.getPosition(partialTicks);

                    var pos = new Vector3f((float) entityPos.x(), (float) entityPos.y(), (float) entityPos.z())
                            .mulDirection(poseStack.last().pose())
                            .mul(poseStack.last().normal());

                    renderParticle(level, particleData, pos.x(), pos.y(), pos.z());
                }
                case RenderingFunction.Compound compoundFunction -> {
                    if (arm == null || compoundFunction.firstPersonArmTarget().hasArm(arm)) {
                        handle(compoundFunction.renderingFunctions(), arm, targetEntity, entityModel, poseStack, buffer, packedLight, packedOverlay, color);
                    }
                }
                case CustomDataRenderer renderer -> {
                    var renderFunction = CustomRendererLoader.getOrResolveRenderer(renderer, !CustomRendererLoader.isConstantResolveTarget());

                    if(renderFunction != null) handle(List.of(renderFunction), arm, targetEntity, entityModel, poseStack, buffer, packedLight, packedOverlay, color);
                }
                default -> throw new IllegalStateException("Unimplemented RendererFunc: " + function.key());
            }
        }
    }

    private static final Logger LOGGER = LogUtils.getLogger();

    private static void renderParticle(Level level, RenderingFunction.Particle particle, double x, double y, double z) {
        var random = level.getRandom();

        try {
            if (particle.count() == 0) {
                double xSpd = particle.speed() * particle.delta().x();
                double ySpd = particle.speed() * particle.delta().y();
                double zSpd = particle.speed() * particle.delta().z();

                level.addParticle(particle.particleData(), particle.force(), x, y, z, xSpd, ySpd, zSpd);
            } else {
                for (int i = 0; i < particle.count(); i++) {
                    double g = random.nextGaussian() * particle.delta().x();
                    double h = random.nextGaussian() * particle.delta().y();
                    double j = random.nextGaussian() * particle.delta().z();

                    double k = random.nextGaussian() * (double)particle.speed();
                    double l = random.nextGaussian() * (double)particle.speed();
                    double m = random.nextGaussian() * (double)particle.speed();

                    level.addParticle(particle.particleData(), particle.force(), x + g, y + h, z + j, k, l, m);
                }
            }
        } catch (Throwable var16) {
            LOGGER.warn("Could not spawn particle effect {}", particle.particleData());
        }
    }

    private static void renderBlock(Minecraft client, BlockState state, @Nullable BlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay, int color) {
        RenderSystem.runAsFancy(() -> {
            if (state.getRenderShape() != RenderShape.ENTITYBLOCK_ANIMATED) {
                client.getBlockRenderer().renderSingleBlock(state, poseStack, buffer, packedLight, packedOverlay);
            }

            if (blockEntity != null) {
                BlockEntityRenderer<BlockEntity> медведь = client.getBlockEntityRenderDispatcher().getRenderer(blockEntity);
                if (медведь != null) {
                    медведь.render(blockEntity, partialTick, poseStack, buffer, 15728880, OverlayTexture.NO_OVERLAY);
                }
            }

//            if (buffer instanceof MultiBufferSource.BufferSource || buffer instanceof OutlineBufferSource) {
//                RenderSystem.setShaderLights(new Vector3f(-1.5F, -0.5F, 0.0F), new Vector3f(0.0F, -1.0F, 0.0F));
//                if (buffer instanceof MultiBufferSource.BufferSource bufferSource) {
//                    bufferSource.endBatch();
//                } else if (buffer instanceof OutlineBufferSource outlineBufferSource) {
//                    outlineBufferSource.endOutlineBatch();
//                }
//                Lighting.setupFor3DItems();
//            }
        });
    }
}
