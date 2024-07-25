package io.wispforest.accessories.criteria;

import com.google.gson.JsonObject;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.data.SlotGroupLoader;
import io.wispforest.endec.*;
import io.wispforest.endec.SerializationContext;
import io.wispforest.endec.format.gson.GsonDeserializer;
import io.wispforest.endec.format.gson.GsonEndec;
import io.wispforest.endec.impl.StructEndecBuilder;
import net.minecraft.advancements.critereon.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

public class AccessoryChangedCriterion extends SimpleCriterionTrigger<AccessoryChangedCriterion.Conditions> {

    private final ResourceLocation location;

    public AccessoryChangedCriterion(ResourceLocation location) {
        this.location = location;
    }

    public void trigger(ServerPlayer player, ItemStack accessory, SlotReference reference, Boolean cosmetic) {
        this.trigger(player, conditions -> {
            return conditions.itemPredicates().map(predicates -> predicates.stream().allMatch(predicate -> predicate.matches(accessory))).orElse(true)
                    && conditions.groups().flatMap(groups -> SlotGroupLoader.INSTANCE.findGroup(false, reference.slotName()).map(group -> groups.stream().noneMatch(s -> s.equals(group.name())))).orElse(true)
                    && conditions.slots().map(slots -> slots.stream().noneMatch(reference.slotName()::equals)).orElse(true)
                    && conditions.indices().map(indices -> indices.stream().noneMatch(index -> index == reference.slot())).orElse(true)
                    && conditions.cosmetic().map(isCosmetic -> isCosmetic && cosmetic).orElse(true);
        });
    }

    @Override
    protected Conditions createInstance(JsonObject jsonObject, ContextAwarePredicate contextAwarePredicate, DeserializationContext deserializationContext) {
        var ctx = SerializationContext.attributes(new ContextAwarePredicateAttribute(contextAwarePredicate), new CriterionIdAttribute(this.getId()));

        return Conditions.ENDEC.decodeFully(ctx, GsonDeserializer::of, jsonObject);
    }

    public static class Conditions extends AbstractCriterionTriggerInstance {
        private static final Endec<Conditions> ENDEC = StructEndecBuilder.of(
                CRITERION_ID.flatFieldOf(conditions -> conditions.getCriterion()),
                CONTEXT_AWARE_PREDICATE_ENDEC.flatFieldOf(conditions -> conditions.getPlayerPredicate()),
                ITEM_PREDICATE_ENDEC.listOf().optionalOf().optionalFieldOf("items", Conditions::itemPredicates, () -> Optional.empty()),
                Endec.STRING.listOf().optionalOf().optionalFieldOf("groups", Conditions::groups, () -> Optional.empty()),
                Endec.STRING.listOf().optionalOf().optionalFieldOf("slots", Conditions::slots, () -> Optional.empty()),
                Endec.INT.listOf().optionalOf().optionalFieldOf("indices", Conditions::indices, () -> Optional.empty()),
                Endec.BOOLEAN.optionalOf().optionalFieldOf("cosmetic", Conditions::cosmetic, () -> Optional.empty()),
                Conditions::new);

        private final Optional<List<ItemPredicate>> itemPredicates;
        private final Optional<List<String>> groups;
        private final Optional<List<String>> slots;
        private final Optional<List<Integer>> indices;
        private final Optional<Boolean> cosmetic;

        public Conditions(
                ResourceLocation id,
                ContextAwarePredicate player,
                Optional<List<ItemPredicate>> itemPredicates,
                Optional<List<String>> groups,
                Optional<List<String>> slots,
                Optional<List<Integer>> indices,
                Optional<Boolean> cosmetic
        ) {
            super(id, player);

            this.itemPredicates = itemPredicates;
            this.groups = groups;
            this.slots = slots;
            this.indices = indices;
            this.cosmetic = cosmetic;
        }

        public Optional<ContextAwarePredicate> player() {
            return Optional.of(this.getPlayerPredicate());
        }

        public Optional<List<ItemPredicate>> itemPredicates() {
            return itemPredicates;
        }

        public Optional<List<String>> groups() {
            return groups;
        }

        public Optional<List<String>> slots() {
            return slots;
        }

        public Optional<List<Integer>> indices() {
            return indices;
        }

        public Optional<Boolean> cosmetic() {
            return cosmetic;
        }
    }

    @Override
    public ResourceLocation getId() {
        return location;
    }

    private static final Endec<ItemPredicate> ITEM_PREDICATE_ENDEC = GsonEndec.INSTANCE.xmap(ItemPredicate::fromJson, ItemPredicate::serializeToJson);
    private static final StructEndec<ContextAwarePredicate> CONTEXT_AWARE_PREDICATE_ENDEC = new StructEndec<>() {
        @Override
        public void encodeStruct(SerializationContext ctx, Serializer<?> serializer, Serializer.Struct struct, ContextAwarePredicate value) {}

        @Override
        public ContextAwarePredicate decodeStruct(SerializationContext ctx, Deserializer<?> deserializer, Deserializer.Struct struct) {
            return ctx.requireAttributeValue(ContextAwarePredicateAttribute.INSTANCE).predicate();
        }
    };

    private static final StructEndec<ResourceLocation> CRITERION_ID = new StructEndec<>() {
        @Override
        public void encodeStruct(SerializationContext ctx, Serializer<?> serializer, Serializer.Struct struct, ResourceLocation value) {}

        @Override
        public ResourceLocation decodeStruct(SerializationContext ctx, Deserializer<?> deserializer, Deserializer.Struct struct) {
            return ctx.requireAttributeValue(CriterionIdAttribute.INSTANCE).id();
        }
    };

    public record ContextAwarePredicateAttribute(ContextAwarePredicate predicate) implements SerializationAttribute.Instance {

        public static final SerializationAttribute.WithValue<ContextAwarePredicateAttribute> INSTANCE = SerializationAttribute.withValue("accessories:context_aware_predicate");

        @Override
        public SerializationAttribute attribute() {
            return INSTANCE;
        }

        @Override
        public Object value() {
            return this;
        }
    }

    public record CriterionIdAttribute(ResourceLocation id) implements SerializationAttribute.Instance {

        public static final SerializationAttribute.WithValue<CriterionIdAttribute> INSTANCE = SerializationAttribute.withValue("accessories:criterion_id");

        @Override
        public SerializationAttribute attribute() {
            return INSTANCE;
        }

        @Override
        public Object value() {
            return this;
        }
    }
}