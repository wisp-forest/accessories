package io.wispforest.accessories.api.client.rendering;

import com.google.common.base.CaseFormat;
import com.google.common.base.Supplier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.utils.EndecUtils;
import io.wispforest.endec.Endec;
import io.wispforest.endec.StructEndec;
import io.wispforest.endec.format.edm.EdmSerializer;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.*;

@ApiStatus.Experimental
public sealed interface RenderingFunction permits CustomDataRenderer, RenderingFunction.Block, RenderingFunction.Compound, RenderingFunction.Conditional, RenderingFunction.Entity, RenderingFunction.Item, RenderingFunction.Model, RenderingFunction.Particle, RenderingFunction.Transformation {

    static Transformation ofTransformation(List<io.wispforest.accessories.api.client.Transformation> transformations, RenderingFunction innerRendering) {
        return new Transformation(transformations, innerRendering);
    }

    static Model ofModel(ResourceLocation id, String variant) {
        return new Model(id, variant);
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
        var entity = entityType.create(level);
        if (entity == null) throw new IllegalStateException("Unable to create render function of the given entity");

        var compound = new CompoundTag();

        String string = entity.getEncodeId();
        if (string == null) throw new IllegalStateException("Unable to create render function of the given entity");

        compound.putString("id", string);
        entity.saveWithoutId(compound);

        return new Entity(entityType, compound, true);
    }

    static Entity ofEntity(EntityType<? extends net.minecraft.world.entity.Entity> entityType, CompoundTag data) {
        return new Entity(entityType, data, true);
    }

    static Particle ofParticle(ResourceLocation uniqueId, float delay, ParticleOptions particleData, Vector3f delta, float speed, int count, boolean force) {
        return new Particle(uniqueId, delay, particleData, delta, speed, count, force);
    }

    //--

    Endec<RenderingFunction> ENDEC = Endec.dispatchedStruct(
            key -> switch (key) {
                case "transformation" -> RenderingFunction.Transformation.ENDEC;
                case "model" -> RenderingFunction.Model.ENDEC;
                case "block" -> RenderingFunction.Block.ENDEC;
                case "item" -> RenderingFunction.Item.ENDEC;
                case "entity" -> RenderingFunction.Entity.ENDEC;
                case "particle" -> RenderingFunction.Particle.ENDEC;
                case "compound" -> RenderingFunction.Compound.ENDEC;
                case "renderer" -> CustomDataRenderer.ENDEC;
                case "conditional" -> RenderingFunction.Conditional.ENDEC;
                default -> throw new IllegalStateException("A invalid rendering function was created meaning such is unable to be decoded!");
            },
            RenderingFunction::key,
            Endec.STRING,
            "type"
    );

    default String key() {
        return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, this.getClass().getSimpleName());
    }

    record Transformation(List<io.wispforest.accessories.api.client.Transformation> transformations, RenderingFunction renderingFunction) implements RenderingFunction {
        public static final StructEndec<Transformation> ENDEC = StructEndecBuilder.of(
                io.wispforest.accessories.api.client.Transformation.ENDEC.listOf().fieldOf("transformations", Transformation::transformations),
                RenderingFunction.ENDEC.fieldOf("rendering_function", Transformation::renderingFunction),
                Transformation::new
        );
    }

    record Model(ResourceLocation id, String variant) implements RenderingFunction {
        public static final StructEndec<Model> ENDEC = StructEndecBuilder.of(
                MinecraftEndecs.IDENTIFIER.fieldOf("id", Model::id),
                Endec.STRING.optionalFieldOf("variant", Model::variant, () -> ""),
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

    record Particle(ResourceLocation uniqueId, float delay, ParticleOptions particleData, Vector3f delta, float speed, int count, boolean force) implements RenderingFunction {
        private static final Endec<ParticleOptions> PARTICLE_OPTIONS_ENDEC = CodecUtils.toEndec(ParticleTypes.CODEC);

        public static final StructEndec<Particle> ENDEC = StructEndecBuilder.of(
                MinecraftEndecs.IDENTIFIER.optionalFieldOf("unique_id", Particle::uniqueId, () -> Accessories.of("shared")),
                Endec.FLOAT.optionalFieldOf("delay", Particle::delay, () -> 20f),
                PARTICLE_OPTIONS_ENDEC.fieldOf("particle_data", Particle::particleData),
                EndecUtils.VECTOR_3_F_ENDEC.flatFieldOf(Particle::delta),
                Endec.FLOAT.optionalFieldOf("speed", Particle::speed, 1f),
                Endec.INT.optionalFieldOf("count", Particle::count, 1),
                Endec.BOOLEAN.optionalFieldOf("force", Particle::force, false),
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
                    this.force == that.force;
        }

        @Override
        public int hashCode() {
            return Objects.hash(PARTICLE_OPTIONS_ENDEC.encodeFully(EdmSerializer::of, this.particleData), delta, speed, count, force);
        }

        @Override
        public String toString() {
            return "Particle[" +
                    "particleData=" + particleData + ", " +
                    "delta=" + delta + ", " +
                    "speed=" + speed + ", " +
                    "count=" + count + ", " +
                    "force=" + force + ']';
        }
    }

    record Compound(List<RenderingFunction> renderingFunctions, ArmTarget firstPersonArmTarget) implements RenderingFunction {
        public static final StructEndec<Compound> ENDEC = StructEndecBuilder.of(
                RenderingFunction.ENDEC.listOf().fieldOf("rendering_functions", Compound::renderingFunctions),
                Endec.forEnum(ArmTarget.class).optionalFieldOf("first_person_arm_target", Compound::firstPersonArmTarget, () -> ArmTarget.NONE),
                Compound::new
        );
    }

    record Conditional(List<RenderingFunctionPredicate> predicates, RenderingFunction renderingFunction) implements RenderingFunction {
        public static final StructEndec<Conditional> ENDEC = StructEndecBuilder.of(
                RenderingFunctionPredicate.ENDEC.listOf().fieldOf("predicates", Conditional::predicates),
                RenderingFunction.ENDEC.fieldOf("rendering_function", Conditional::renderingFunction),
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
