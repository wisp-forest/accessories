package io.wispforest.accessories.api.client.rendering;

import com.google.common.base.CaseFormat;
import com.google.common.base.Supplier;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.client.AccessoriesRendererRegistry;
import io.wispforest.accessories.api.client.rendering.RenderingFunction.*;
import io.wispforest.accessories.utils.EndecUtils;
import io.wispforest.endec.Endec;
import io.wispforest.endec.StructEndec;
import io.wispforest.endec.format.edm.EdmSerializer;
import io.wispforest.endec.format.gson.GsonEndec;
import io.wispforest.endec.impl.StructEndecBuilder;
import io.wispforest.owo.serialization.CodecUtils;
import io.wispforest.owo.serialization.endec.MinecraftEndecs;
import io.wispforest.owo.serialization.format.nbt.NbtEndec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.nio.charset.StandardCharsets;
import java.util.*;

@ApiStatus.Experimental
public sealed interface RenderingFunction permits DeferredRenderer, Block, Compound, Conditional, RawRenderer, Entity, Item, Model, Particle, Transformations {

    static Transformations ofTransformation(List<io.wispforest.accessories.api.client.rendering.Transformation> transformations, List<RenderingFunction> renderingFunctions, ArmTarget armTarget) {
        return new Transformations(transformations, new Compound(renderingFunctions, armTarget));
    }

    static Model ofModel(ResourceLocation id) {
        return new Model(id);
    }

    static Block ofBlock(net.minecraft.world.level.block.Block block) {
        return ofBlock(block.defaultBlockState());
    }

    static Block ofBlock(BlockState state) {
        return new Block(state, null, new CompoundTag());
    }

    static Block ofBlockEntity(net.minecraft.world.level.block.Block block, BlockEntityType<? extends net.minecraft.world.level.block.entity.BlockEntity> type, Level level) {
        return ofBlockEntity(block.defaultBlockState(), type, level);
    }

    static Block ofBlockEntity(BlockState blockState, BlockEntityType<? extends net.minecraft.world.level.block.entity.BlockEntity> type, Level level) {
        var blockEntity = type.create(BlockPos.ZERO, blockState);

        if (blockEntity == null) throw new IllegalStateException("Unable to create render function of the given block entity");

        return ofBlockEntity(blockState, type, blockEntity.saveWithoutMetadata(level.registryAccess()));
    }

    static Block ofBlockEntity(BlockState blockState, BlockEntityType<? extends net.minecraft.world.level.block.entity.BlockEntity> type, CompoundTag data) {
        return new Block(blockState, type, data);
    }

    static Item ofItem(ItemStack stack) {
        return new Item(stack);
    }

    static Entity ofEntity(EntityType<? extends net.minecraft.world.entity.Entity> entityType, Level level) {
        var entity = entityType.create(level, EntitySpawnReason.EVENT);
        if (entity == null) throw new IllegalStateException("Unable to create render function of the given entity");

        return Accessories.handleIoError("rendering_function_entity_data", scopedCollector -> {
            var valueOutput = TagValueOutput.createWithContext(scopedCollector, level.registryAccess());

            var string = entity.getEncodeId();
            if (string == null) throw new IllegalStateException("Unable to create render function of the given entity");

            valueOutput.putString("id", string);
            entity.saveWithoutId(valueOutput);

            var compound = valueOutput.buildResult();

            return new Entity(entityType, compound, true);
        });
    }

    static Entity ofEntity(EntityType<? extends net.minecraft.world.entity.Entity> entityType, CompoundTag data) {
        return new Entity(entityType, data, true);
    }

    static Particle ofParticle(ResourceLocation uniqueId, float delay, ParticleOptions particleData, Vector3f delta, float speed, int count, boolean overrideLimiter, boolean alwaysShow) {
        return new Particle(uniqueId, delay, particleData, delta, speed, count, overrideLimiter, alwaysShow);
    }

    //--

    Endec<RenderingFunction> ENDEC = Endec.dispatchedStruct(
            key -> switch (key) {
                case "transformation" -> Transformations.ENDEC;
                case "model" -> Model.ENDEC;
                case "block" -> Block.ENDEC;
                case "item" -> Item.ENDEC;
                case "entity" -> Entity.ENDEC;
                case "particle" -> Particle.ENDEC;
                case "compound" -> Compound.ENDEC;
                case "renderer" -> DeferredRenderer.ENDEC;
                case "conditional" -> Conditional.ENDEC;
                case "data" -> RawRenderer.ENDEC;
                default -> throw new IllegalStateException("A invalid rendering function was created meaning such is unable to be decoded!");
            },
            RenderingFunction::key,
            Endec.STRING,
            "type"
    );

    default String key() {
        return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, this.getClass().getSimpleName());
    }

    record Transformations(List<io.wispforest.accessories.api.client.rendering.Transformation> transformations, Compound renderingFunction) implements RenderingFunction {
        public static final StructEndec<Transformations> ENDEC = StructEndecBuilder.of(
                io.wispforest.accessories.api.client.rendering.Transformation.ENDEC.listOf().fieldOf("transformations", Transformations::transformations),
                Compound.ENDEC.flatFieldOf(Transformations::renderingFunction),
                Transformations::new
        );
    }

    record Model(ResourceLocation id) implements RenderingFunction {
        public static final StructEndec<Model> ENDEC = StructEndecBuilder.of(
                MinecraftEndecs.IDENTIFIER.fieldOf("id", Model::id),
                Model::new
        );
    }

    record Block(BlockState state, @Nullable BlockEntityType<?> type, CompoundTag data) implements RenderingFunction {
        public static final StructEndec<Block> ENDEC = StructEndecBuilder.of(
                EndecUtils.blockStateEndec("id").flatFieldOf(Block::state),
                CodecUtils.toEndec(BuiltInRegistries.BLOCK_ENTITY_TYPE.byNameCodec()).optionalFieldOf("entity_id", Block::type, (BlockEntityType<?>) null),
                NbtEndec.COMPOUND.optionalFieldOf("data", Block::data, CompoundTag::new),
                Block::new
        );
    }

    record Item(ItemStack stack) implements RenderingFunction {
        public static final StructEndec<Item> ENDEC = StructEndecBuilder.of(
                new EndecUtils.LazyStructEndec<>(() -> {
                    var baseCodec = ItemStack.CODEC;

                    try {
                        var field = baseCodec.getClass().getDeclaredField("wrapped");

                        field.setAccessible(true);

                        var supplier = (Supplier<Codec<ItemStack>>) field.get(baseCodec);

                        return CodecUtils.toStructEndec(((MapCodec.MapCodecCodec<ItemStack>) supplier.get()).codec());
                    } catch (NoSuchFieldException | IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }).flatFieldOf(Item::stack),
                Item::new
        );
    }

    record Entity(EntityType<?> entityType, CompoundTag data, boolean allowTicking) implements RenderingFunction {
        public static final StructEndec<Entity> ENDEC = StructEndecBuilder.of(
                CodecUtils.toEndec(BuiltInRegistries.ENTITY_TYPE.byNameCodec()).fieldOf("entity_id", Entity::entityType),
                NbtEndec.COMPOUND.optionalFieldOf("stack", Entity::data, CompoundTag::new),
                Endec.BOOLEAN.optionalFieldOf("allow_ticking", Entity::allowTicking, false),
                Entity::new
        );
    }

    record Particle(ResourceLocation uniqueId, float delay, ParticleOptions particleData, Vector3f delta, float speed, int count, boolean overrideLimiter, boolean alwaysShow) implements RenderingFunction {
        private static final Endec<ParticleOptions> PARTICLE_OPTIONS_ENDEC = CodecUtils.toEndec(ParticleTypes.CODEC);

        public static final StructEndec<Particle> ENDEC = StructEndecBuilder.of(
                MinecraftEndecs.IDENTIFIER.optionalFieldOf("unique_id", Particle::uniqueId, () -> Accessories.of("shared")),
                Endec.FLOAT.optionalFieldOf("delay", Particle::delay, () -> 20f),
                PARTICLE_OPTIONS_ENDEC.fieldOf("particle_data", Particle::particleData),
                EndecUtils.VECTOR_3_F_ENDEC.flatFieldOf(Particle::delta),
                Endec.FLOAT.optionalFieldOf("speed", Particle::speed, 1f),
                Endec.INT.optionalFieldOf("count", Particle::count, 1),
                Endec.BOOLEAN.optionalFieldOf("override_limiter", Particle::overrideLimiter, false),
                Endec.BOOLEAN.optionalFieldOf("always_show", Particle::alwaysShow, false),
                Particle::new
        );

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (Particle) obj;

            var rawParticleData = PARTICLE_OPTIONS_ENDEC.encodeFully(EdmSerializer::of, this.particleData);
            var thatRawParticleData = PARTICLE_OPTIONS_ENDEC.encodeFully(EdmSerializer::of, that.particleData);

            return Objects.equals(rawParticleData, thatRawParticleData) &&
                    Objects.equals(this.delta, that.delta) &&
                    Float.floatToIntBits(this.speed) == Float.floatToIntBits(that.speed) &&
                    this.count == that.count &&
                    this.overrideLimiter == that.overrideLimiter &&
                    this.alwaysShow == that.alwaysShow;
        }

        @Override
        public int hashCode() {
            return Objects.hash(PARTICLE_OPTIONS_ENDEC.encodeFully(EdmSerializer::of, this.particleData), delta, speed, count, overrideLimiter, alwaysShow);
        }

        @Override
        public String toString() {
            return "Particle[" +
                    "particleData=" + particleData + ", " +
                    "delta=" + delta + ", " +
                    "speed=" + speed + ", " +
                    "count=" + count + ", " +
                    "overrideLimiter=" + overrideLimiter +
                    "alwaysShow=" + alwaysShow +']';
        }
    }

    interface ArmedTargeted {
        ArmTarget firstPersonArmTarget();
    }

    record Compound(List<RenderingFunction> renderingFunctions, ArmTarget firstPersonArmTarget) implements RenderingFunction, ArmedTargeted {
        private static final StructEndec<Compound> OLD_FORMAT_ENDEC = StructEndecBuilder.of(
                RenderingFunction.ENDEC.fieldOf("rendering_function", s -> s.renderingFunctions().getFirst()),
                Endec.forEnum(ArmTarget.class).optionalFieldOf("first_person_arm_target", Compound::firstPersonArmTarget, () -> ArmTarget.NONE),
                (function, armTarget) -> new Compound(List.of(function), armTarget)
        );

        public static final StructEndec<Compound> ENDEC = StructEndecBuilder.of(
                RenderingFunction.ENDEC.listOf().fieldOf("rendering_functions", Compound::renderingFunctions),
                Endec.forEnum(ArmTarget.class).optionalFieldOf("first_person_arm_target", Compound::firstPersonArmTarget, () -> ArmTarget.NONE),
                Compound::new
        ).structuredCatchErrors((ctx, serializer, struct, mainException) -> {
            try {
                return OLD_FORMAT_ENDEC.decodeStruct(ctx, serializer, struct);
            } catch (Exception ignored) {}

            throw new RuntimeException(mainException);
        });
    }

    final class RawRenderer implements RenderingFunction, ArmedTargeted {
        public static final StructEndec<RawRenderer> ENDEC = StructEndecBuilder.of(
                GsonEndec.INSTANCE.mapOf().optionalFieldOf("references", RawRenderer::references, HashMap::new),
                GsonEndec.INSTANCE.listOf().fieldOf("rendering_functions", RawRenderer::renderingFunctions),
                Endec.forEnum(ArmTarget.class).optionalFieldOf("first_person_arm_target", RawRenderer::firstPersonArmTarget, () -> ArmTarget.NONE),
                RawRenderer::new
        );

        private final Map<String, JsonElement> references;
        private final List<JsonElement> renderingFunctions;
        private final ArmTarget firstPersonArmTarget;

        private final UUID uuid;

        public RawRenderer(Map<String, JsonElement> references, List<JsonElement> renderingFunctions, ArmTarget firstPersonArmTarget) {
            this.references = references;
            this.renderingFunctions = renderingFunctions;
            this.firstPersonArmTarget = firstPersonArmTarget;

            this.uuid = UUID.nameUUIDFromBytes(this.toString().getBytes(StandardCharsets.UTF_8));
        }

        public Map<String, JsonElement> references() { return Collections.unmodifiableMap(references); }
        public List<JsonElement> renderingFunctions() { return Collections.unmodifiableList(renderingFunctions); }
        public ArmTarget firstPersonArmTarget() { return firstPersonArmTarget; }
        public UUID getUUID() { return uuid; }

        @Override public String key() { return "data"; }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (RawRenderer) obj;
            return Objects.equals(this.references, that.references) &&
                    Objects.equals(this.renderingFunctions, that.renderingFunctions) &&
                    Objects.equals(this.firstPersonArmTarget, that.firstPersonArmTarget);
        }

        @Override
        public int hashCode() {
            return Objects.hash(references, renderingFunctions, firstPersonArmTarget);
        }

        @Override
        public String toString() {
            return "RawRenderer[" + "references=" + references + ", " + "renderingFunctions=" + renderingFunctions + ", " + "firstPersonArmTarget=" + firstPersonArmTarget + ']';
        }
    }

    // TODO: FIRST CHANGE FROM JSON TO EDM WHEN 1.21.4 and CACHE RESULTS OF CUSTOM renderingFunctions SOME HOW?
//    @Environment(EnvType.CLIENT)
    @ApiStatus.Experimental
    final class DeferredRenderer implements RenderingFunction, ArmedTargeted {
        public static final StructEndec<DeferredRenderer> ENDEC = StructEndecBuilder.of(
                MinecraftEndecs.IDENTIFIER.optionalFieldOf("renderer_id", DeferredRenderer::rendererId, () -> AccessoriesRendererRegistry.NO_RENDERER_ID),
                GsonEndec.INSTANCE.mapOf().optionalFieldOf("references", DeferredRenderer::references, HashMap::new),
                Endec.forEnum(ArmTarget.class).optionalFieldOf("first_person_arm_target", DeferredRenderer::firstPersonArmTarget, () -> ArmTarget.BOTH),
                DeferredRenderer::new
        );

        private final ResourceLocation rendererId;
        private final Map<String, JsonElement> references;
        private final ArmTarget firstPersonArmTarget;

        private final UUID uuid;

        public DeferredRenderer(ResourceLocation rendererId,
                                Map<String, JsonElement> references,
                                ArmTarget firstPersonArmTarget) {
            this.rendererId = rendererId;
            this.references = references;
            this.firstPersonArmTarget = firstPersonArmTarget;

            this.uuid = UUID.nameUUIDFromBytes(this.toString().getBytes(StandardCharsets.UTF_8));
        }

        public Map<String, JsonElement> references() { return Collections.unmodifiableMap(references); }
        public ResourceLocation rendererId() { return rendererId; }
        public ArmTarget firstPersonArmTarget() { return firstPersonArmTarget; }
        public UUID getUUID() { return uuid; }

        @Override public String key() { return "renderer"; }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (DeferredRenderer) obj;
            return Objects.equals(this.rendererId, that.rendererId) &&
                    Objects.equals(this.references, that.references) &&
                    Objects.equals(this.firstPersonArmTarget, that.firstPersonArmTarget);
        }

        @Override
        public int hashCode() {
            return Objects.hash(rendererId, references, firstPersonArmTarget);
        }

        @Override
        public String toString() {
            return "DeferredRenderer[" + "rendererId=" + rendererId + ", " + "references=" + references + ", " + "firstPersonArmTarget=" + firstPersonArmTarget + ']';
        }

    }

    record Conditional(List<RenderingPredicate> predicates, Compound renderingFunction) implements RenderingFunction {
        public static final StructEndec<Conditional> ENDEC = StructEndecBuilder.of(
                RenderingPredicate.ENDEC.listOf().fieldOf("predicates", Conditional::predicates),
                Compound.ENDEC.flatFieldOf(Conditional::renderingFunction),
                Conditional::new
        );
    }

    enum ArmTarget {
        LEFT(HumanoidArm.LEFT),
        RIGHT(HumanoidArm.RIGHT),
        BOTH(HumanoidArm.LEFT, HumanoidArm.RIGHT),
        NONE;

        private final Set<HumanoidArm> arms;

        ArmTarget(HumanoidArm ...arms){
            var result = EnumSet.noneOf(HumanoidArm.class);

            result.addAll(Set.of(arms));

            this.arms = result;
        }

        public final boolean hasArm(HumanoidArm arm){
            return this.arms.contains(arm);
        }
    }
}
