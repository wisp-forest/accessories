package io.wispforest.accessories.api.client.rendering;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import io.wispforest.accessories.client.ClientDelayedCache;
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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.*;
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

import java.lang.ref.SoftReference;
import java.util.*;

@ApiStatus.Experimental
@Environment(EnvType.CLIENT)
public class ClientRenderingUtils {

    private static final ClientDelayedCache<ParticleTimeKey> PARTICLE_UPDATE_CACHE = new ClientDelayedCache<>();

    public static void handle(ItemStack stack, LivingEntity targetEntity, @Nullable HumanoidArm arm, EntityModel<? extends LivingEntity> entityModel, PoseStack poseStack, MultiBufferSource buffer, float partialTicks, int packedLight, int packedOverlay, int color, List<RenderingFunction> functions) {
        handle(ItemStack.hashItemAndComponents(stack), targetEntity, arm, entityModel, poseStack, buffer, partialTicks, packedLight, packedOverlay, color, functions);
    }
    private static final Map<EntityType, EntityData> ENTITY_CACHE = new HashMap<>();

    private static final class EntityData {
        private final CompoundTag defaultData;

        private @Nullable SoftReference<Entity> reference;
        private boolean wasSpawnable;

        private EntityData(@Nullable SoftReference<Entity> reference, CompoundTag defaultData, boolean wasSpawnable) {
            this.reference = reference;
            this.defaultData = defaultData;
            this.wasSpawnable = wasSpawnable;
        }

        private boolean canBeGotten() {
            return reference != null && reference.get() != null;
        }

        public @Nullable SoftReference<Entity> reference() {
            return reference;
        }

        public void resetEntity() {
            if (this.reference == null) return;

            var entity = this.reference.get();

            if (entity == null) return;

            try {
                entity.load(defaultData);
            } catch (Exception ignored) {}
        }

        public void createNewReference(EntityType type, Level level) {
            var entity = type.create(level);

            if (entity == null) {
                this.wasSpawnable = false;

                return;
            }

            this.reference = new SoftReference<>(entity);
        }

        public CompoundTag defaultData() {
            return defaultData;
        }

        public boolean wasSpawnable() {
            return wasSpawnable;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (EntityData) obj;
            return Objects.equals(this.reference, that.reference) &&
                    Objects.equals(this.defaultData, that.defaultData) &&
                    this.wasSpawnable == that.wasSpawnable;
        }

        @Override
        public int hashCode() {
            return Objects.hash(reference, defaultData, wasSpawnable);
        }

        @Override
        public String toString() {
            return "EntityData[" +
                    "reference=" + reference + ", " +
                    "defaultData=" + defaultData + ", " +
                    "wasSpawnable=" + wasSpawnable + ']';
        }
    }

    public static void handle(int uniqueKey, LivingEntity targetEntity, @Nullable HumanoidArm arm, EntityModel<? extends LivingEntity> entityModel, PoseStack poseStack, MultiBufferSource buffer, float partialTicks, int packedLight, int packedOverlay, int color, List<RenderingFunction> functions) {
        var client = Minecraft.getInstance();
        var level = Minecraft.getInstance().level;

        for (var function : functions) {
            switch (function) {
                case RenderingFunction.Transformation transformation -> {
                    ClientTransformationUtils.transformStack(transformation.transformations(), poseStack, targetEntity, entityModel, () -> handle(uniqueKey, targetEntity, arm, entityModel, poseStack, buffer, partialTicks, packedLight, packedOverlay, color, List.of(transformation.renderingFunction())));
                }
                case RenderingFunction.Block blockData -> {
                    var state = blockData.state();
                    var blockEntity = (blockData.type() != null) ? net.minecraft.world.level.block.entity.BlockEntity.loadStatic(BlockPos.ZERO, blockData.state(), blockData.data(), level.registryAccess()) : null;

                    poseStack.pushPose();

                    poseStack.translate(-0.5, 0, -0.5);

                    renderBlock(client, state, blockEntity, 0, poseStack, buffer, packedLight, packedOverlay, color);

                    poseStack.popPose();
                }
                case RenderingFunction.Entity entityData -> {
                    try {
                        var currentEntityData = ENTITY_CACHE.computeIfAbsent(entityData.entityType(), entityType -> {
                            var entity = entityData.entityType().create(level);

                            if (entity != null) {
                                var defaultData = entity.saveWithoutId(new CompoundTag());

                                return new EntityData(new SoftReference<>(entity), defaultData, true);
                            }

                            return new EntityData(null, new CompoundTag(), false);
                        });

                        if (!currentEntityData.wasSpawnable()) continue;

                        if (!currentEntityData.canBeGotten()) {
                            currentEntityData.createNewReference(entityData.entityType(), level);
                        }

                        if (!currentEntityData.wasSpawnable()) continue;

                        Entity entity = currentEntityData.reference().get();

                        if (entity == null) continue;

                        boolean customData = false;

                        if (!entityData.data().isEmpty()) {
                            customData = true;

                            entity.load(entityData.data());
                        }

                        if (entityData.allowTicking() || entity instanceof Display) entity.tick();

                        client.getEntityRenderDispatcher()
                                .render(entity, 0, 0, 0, 0, partialTicks, poseStack, buffer, packedLight);

                        if (customData) {
                            currentEntityData.resetEntity();
                        }
                    } catch (Exception e) {
                        continue;
                    }
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
                    if (PARTICLE_UPDATE_CACHE.hasAllottedTime(new ParticleTimeKey(targetEntity.getUUID(), uniqueKey, particleData), particleData.delay())) {
                        var pos = new Vector3f(0, 0, 0)
                                .mulPosition(poseStack.last().pose())
                                .add(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().toVector3f());

                        renderParticle(level, particleData, pos.x(), pos.y(), pos.z());
                    }
                }
                case RenderingFunction.Compound compoundFunction -> {
                    if (arm == null || compoundFunction.firstPersonArmTarget().hasArm(arm)) {
                        handle(uniqueKey, targetEntity, arm, entityModel, poseStack, buffer, partialTicks, packedLight, packedOverlay, color, compoundFunction.renderingFunctions());
                    }
                }
                case CustomDataRenderer renderer -> {
                    var renderFunction = CustomRendererLoader.getOrResolveRenderer(renderer, !CustomRendererLoader.isConstantResolveTarget());

                    if(renderFunction != null) handle(uniqueKey, targetEntity, arm, entityModel, poseStack, buffer, partialTicks, packedLight, packedOverlay, color, List.of(renderFunction));
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

    private record ParticleTimeKey(UUID entityUUID, int uniqueKey, RenderingFunction.Particle particleData) {

    }
}
