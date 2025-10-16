package io.wispforest.accessories.api.client.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.AccessoriesStorageLookup;
import io.wispforest.accessories.api.client.AccessoriesRenderStateKeys;
import io.wispforest.accessories.api.slot.SlotPath;
import io.wispforest.accessories.client.ClientDelayedCache;
import io.wispforest.accessories.data.CustomRendererLoader;
import io.wispforest.accessories.pond.AccessoriesRenderStateAPI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.slf4j.Logger;

import java.lang.ref.SoftReference;
import java.util.*;

//@Environment(EnvType.CLIENT)
@ApiStatus.Experimental
public class RenderingFunctionOps {

    private static final ClientDelayedCache<ParticleTimeKey> PARTICLE_UPDATE_CACHE = new ClientDelayedCache<>();

    public static void handleFunctions(
            ItemStack stack, SlotPath path, PoseStack matrices, EntityModel<? extends LivingEntityRenderState> model, LivingEntityRenderState renderState, SubmitNodeCollector collector, int light, float partialTicks, @Nullable HumanoidArm arm, int packedLight, int packedOverlay, int color, List<RenderingFunction> functions) {
        handleFunctions(ItemStack.hashItemAndComponents(stack), stack, path, matrices, model, renderState, collector, light, partialTicks, arm, packedLight, packedOverlay, color, functions);
    }

    private static final Map<EntityType, EntityData> ENTITY_CACHE = new HashMap<>();

    public static void handleFunctions(int uniqueKey, ItemStack stack, SlotPath path, PoseStack matrices, EntityModel<? extends LivingEntityRenderState> model, LivingEntityRenderState renderState, SubmitNodeCollector collector, int light, float partialTicks, @Nullable HumanoidArm arm, int packedLight, int packedOverlay, int color, List<RenderingFunction> functions) {
        for (var function : functions) {
            handleFunction(uniqueKey, stack, path, matrices, model, renderState, collector, light, partialTicks, arm, packedLight, packedOverlay, color, function);
        }
    }

    public static void handleFunction(int uniqueKey, ItemStack stack, SlotPath path, PoseStack matrices, EntityModel<? extends LivingEntityRenderState> model, LivingEntityRenderState renderState, SubmitNodeCollector collector, int light, float partialTicks, @Nullable HumanoidArm arm, int packedLight, int packedOverlay, int color, RenderingFunction renderingFunction) {
        var client = Minecraft.getInstance();
        var level = client.level;
//        var targetEntity = reference.entity();

        var cameraState = renderState.getStateData(AccessoriesRenderStateKeys.CAMERA_STATE);

        switch (renderingFunction) {
            case RenderingFunction.Transformations transformation -> {
                TransformOps.transformStack(transformation.transformations(), matrices, model, () -> handleFunction(uniqueKey, stack, path, matrices, model, renderState, collector, light, partialTicks, arm, packedLight, packedOverlay, color, transformation.renderingFunction()));
            }
            case RenderingFunction.Block blockData -> {
                var state = blockData.state();
                var blockEntity = (blockData.type() != null) ? BlockEntity.loadStatic(BlockPos.ZERO, blockData.state(), blockData.data(), level.registryAccess()) : null;

                matrices.pushPose();

                matrices.translate(-0.5, 0, -0.5);

                // TODO: WHY THE HELL IS THE ZERO and NOT PARTIAL TICKS?
                renderBlock(client, state, blockEntity, cameraState, 0, matrices, collector, packedLight, packedOverlay, color);

                matrices.popPose();
            }
            case RenderingFunction.Entity entityData -> {
                try {
                    var currentEntityData = ENTITY_CACHE.computeIfAbsent(entityData.entityType(), entityType -> {
                        var entity = entityData.entityType().create(level, EntitySpawnReason.EVENT);

                        if (entity != null) {
                            var defaultData = Accessories.handleIoError("rendering_function_default_entity_data", scopedCollector -> {
                                var valueOutput = TagValueOutput.createWithContext(scopedCollector, level.registryAccess());

                                entity.saveWithoutId(valueOutput);

                                return valueOutput.buildResult();
                            });

                            return new EntityData(new SoftReference<>(entity), defaultData, true);
                        }

                        return new EntityData(null, new CompoundTag(), false);
                    });

                    if (!currentEntityData.wasSpawnable()) return;

                    if (!currentEntityData.canBeGotten()) {
                        currentEntityData.createNewReference(entityData.entityType(), level);
                    }

                    if (!currentEntityData.wasSpawnable()) return;

                    Entity entity = currentEntityData.reference().get();

                    if (entity == null) return;

                    boolean customData = false;

                    if (!entityData.data().isEmpty()) {
                        customData = true;

                        Accessories.handleIoError("rendering_function_entity_data", scopedCollector -> {
                            entity.load(TagValueInput.create(scopedCollector, level.registryAccess(), entityData.data()));
                        });
                    }

                    if (entityData.allowTicking() || entity instanceof Display) entity.tick();

                    var dispatcher = client.getEntityRenderDispatcher();

                    var state = dispatcher.extractEntity(entity, partialTicks);

                    dispatcher.submit(state, cameraState, 0, 0, 0, matrices, collector);

                    if (customData) {
                        currentEntityData.resetEntity(level);
                    }
                } catch (Exception ignored) {}
            }
            case RenderingFunction.Item itemData -> {
                ItemStack renderStack = itemData.stack();

                var state = new ItemStackRenderState();

                // TODO: FIND SOME WAY TO CONVERT RENDER FUNCTIONS TO A STATE OBJECT.... FUCK MY LIFE
                client.getItemModelResolver().updateForTopItem(
                    state,
                    renderStack,
                    ItemDisplayContext.GUI,
                    null,
                    null,
                    Objects.hash(itemData, uniqueKey) // TODO: CONFIRM THIS IS CORRECT
                );

                state.submit(matrices, collector, packedLight, packedOverlay, 0);
            }
            case RenderingFunction.Model modelData -> {
                var modelStack = Items.BEDROCK.getDefaultInstance();

                modelStack.set(DataComponents.ITEM_MODEL, modelData.id());

                var state = new ItemStackRenderState();

                // TODO: FIND SOME WAY TO CONVERT RENDER FUNCTIONS TO A STATE OBJECT.... FUCK MY LIFE
                client.getItemModelResolver().updateForTopItem(
                    state,
                    modelStack,
                    ItemDisplayContext.GUI,
                    null,
                    null,
                    Objects.hash(modelData, uniqueKey) // TODO: CONFIRM THIS IS CORRECT
                );
            }
            case RenderingFunction.Particle particleData -> {
                if (!PARTICLE_UPDATE_CACHE.hasAllottedTime(new ParticleTimeKey(((AccessoriesRenderStateAPI) renderState).getEntityUUIDForState(), uniqueKey, particleData), particleData.delay())) return;

                var pos = new Vector3f(0, 0, 0)
                        .mulPosition(matrices.last().pose())
                        .add(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().toVector3f());

                renderParticle(level, particleData, pos.x(), pos.y(), pos.z());
            }
            case RenderingFunction.Compound compoundFunction -> {
                if (arm != null && !compoundFunction.firstPersonArmTarget().hasArm(arm)) return;

                handleFunctions(uniqueKey, stack, path, matrices, model, renderState, collector, light, partialTicks, arm, packedLight, packedOverlay, color, compoundFunction.renderingFunctions());
            }
            case RenderingFunction.RawRenderer data -> {
                var renderFunction = CustomRendererLoader.getOrResolveRawRenderer(data, !CustomRendererLoader.isConstantResolveTarget());

                if(renderFunction == null) return;

                handleFunction(uniqueKey, stack, path, matrices, model, renderState, collector, light, partialTicks, arm, packedLight, packedOverlay, color, renderFunction);
            }
            case RenderingFunction.DeferredRenderer renderer -> {
                var renderFunction = CustomRendererLoader.getOrResolveRenderer(renderer, !CustomRendererLoader.isConstantResolveTarget());

                if(renderFunction == null) return;

                var lookup = renderState.getStateData(AccessoriesRenderStateKeys.STORAGE_LOOKUP);

                renderFunction.ifLeft(accessoryRenderer -> {
                    try {
                        // TODO: FIND SOME WAY TO CONVERT RENDER FUNCTIONS TO A STATE OBJECT.... FUCK MY LIFE
//                        if (!accessoryRenderer.shouldRender(stack, path, lookup, null, renderState, true)) return;
//
//                        accessoryRenderer.render(null, renderState, (EntityModel<LivingEntityRenderState>) model, matrices, collector);
                    } catch (Exception ignored) {}
                }).ifRight(function1 -> {
                    handleFunction(uniqueKey, stack, path, matrices, model, renderState, collector, light, partialTicks, arm, packedLight, packedOverlay, color, function1);
                });
            }
            default -> throw new IllegalStateException("Unimplemented RendererFunc: " + renderingFunction.key());
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

                level.addParticle(particle.particleData(), particle.overrideLimiter(), particle.alwaysShow(), x, y, z, xSpd, ySpd, zSpd);
            } else {
                for (int i = 0; i < particle.count(); i++) {
                    double g = random.nextGaussian() * particle.delta().x();
                    double h = random.nextGaussian() * particle.delta().y();
                    double j = random.nextGaussian() * particle.delta().z();

                    double k = random.nextGaussian() * (double)particle.speed();
                    double l = random.nextGaussian() * (double)particle.speed();
                    double m = random.nextGaussian() * (double)particle.speed();

                    level.addParticle(particle.particleData(), particle.overrideLimiter(), particle.alwaysShow(), x + g, y + h, z + j, k, l, m);
                }
            }
        } catch (Throwable var16) {
            LOGGER.warn("Could not spawn particle effect {}", particle.particleData());
        }
    }

    private static void renderBlock(Minecraft client, BlockState state, @Nullable BlockEntity blockEntity, CameraRenderState cameraState, float partialTick, PoseStack matrices, SubmitNodeCollector collector, int packedLight, int packedOverlay, int color) {
        if (state.getRenderShape() != RenderShape.INVISIBLE) {
            collector.submitBlock(matrices, state, packedLight, packedOverlay, 0);
        }

        if (blockEntity != null) {
            var dispatcher = client.getBlockEntityRenderDispatcher();

            var медведь = dispatcher.tryExtractRenderState(blockEntity, partialTick, null);

            if (медведь != null) {
                dispatcher.submit(медведь, matrices, collector, cameraState);
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
    }

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

        public void resetEntity(Level level) {
            if (this.reference == null) return;

            var entity = this.reference.get();

            if (entity == null) return;

            try {
                Accessories.handleIoError("rendering_function_entity_data", scopedCollector -> {
                    entity.load(TagValueInput.create(scopedCollector, level.registryAccess(), defaultData));
                });
            } catch (Exception ignored) {}
        }

        public void createNewReference(EntityType type, Level level) {
            var entity = type.create(level, EntitySpawnReason.EVENT);

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

    private record ParticleTimeKey(UUID entityUUID, int uniqueKey) {
        private ParticleTimeKey(UUID entityUUID, int uniqueKey, RenderingFunction.Particle particleData) {
            this(entityUUID, Objects.hash(uniqueKey, particleData));
        }
    }

    public static boolean shouldRender(ItemStack stack, SlotPath path, AccessoriesStorageLookup storageLookup, LivingEntity entity, LivingEntityRenderState entityState, List<RenderingFunction> renderingFunctions) {
        for (var function : renderingFunctions) {
            var result = shouldRender(stack, path, storageLookup, entity, entityState, function);

            if (result != null && result) return true;
        }

        return false;
    }

    @Nullable
    public static Boolean shouldRender(ItemStack stack, SlotPath path, AccessoriesStorageLookup storageLookup, LivingEntity entity, LivingEntityRenderState entityState, RenderingFunction renderingFunction) {
        if (renderingFunction instanceof RenderingFunction.ArmedTargeted armedTargeted && entityState.hasStateData(AccessoriesRenderStateKeys.ARM)) {
            if (armedTargeted.firstPersonArmTarget().hasArm(entityState.getStateData(AccessoriesRenderStateKeys.ARM))) return true;
        }

        return switch (renderingFunction) {
            case RenderingFunction.Transformations transformation -> {
                yield shouldRender(stack, path, storageLookup, entity, entityState, transformation.renderingFunction());
            }
            case RenderingFunction.Compound compoundFunction -> {
                yield shouldRender(stack, path, storageLookup, entity, entityState, compoundFunction.renderingFunctions());
            }
            case RenderingFunction.RawRenderer data -> {
                var renderFunction = CustomRendererLoader.getOrResolveRawRenderer(data, !CustomRendererLoader.isConstantResolveTarget());

                if(renderFunction == null) yield null;

                yield shouldRender(stack, path, storageLookup, entity, entityState, renderFunction);
            }
            case RenderingFunction.DeferredRenderer renderer -> {
                var possibleRenderer = CustomRendererLoader.getOrResolveRenderer(renderer, !CustomRendererLoader.isConstantResolveTarget());

                if(possibleRenderer == null) yield null;

                yield Either.unwrap(
                        possibleRenderer.mapBoth(
                                accessoryRenderer -> accessoryRenderer.shouldRender(stack, path, storageLookup, entity, entityState, true),
                                renderFunction -> shouldRender(stack, path, storageLookup, entity, entityState, renderFunction))
                );
            }
            default -> null;
        };
    }
}
