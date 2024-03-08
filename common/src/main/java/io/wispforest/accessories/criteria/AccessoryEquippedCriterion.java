package io.wispforest.accessories.criteria;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.data.SlotGroupLoader;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public class AccessoryEquippedCriterion extends SimpleCriterionTrigger<AccessoryEquippedCriterion.Conditions> {

    public void trigger(
            ServerPlayer player,
            ItemStack accessory,
            SlotReference reference,
            Boolean cosmetic
    ) {
        this.trigger(player, conditions -> {
            if (conditions.itemPredicates().isPresent() && !conditions.itemPredicates().get().stream().allMatch(predicate -> predicate.matches(accessory))) {
                return false;
            }
            var group = SlotGroupLoader.INSTANCE.findGroup(false, reference.slotName());
            if (group.isPresent() && conditions.groups.isPresent() && conditions.groups().get().stream().noneMatch(s -> s.equals(group.get().name()))) {
                return false;
            }
            if (conditions.slots().isPresent() && conditions.slots().get().stream().noneMatch(reference.slotName()::equals)) {
                return false;
            }
            if (conditions.indices().isPresent() && conditions.indices().get().stream().noneMatch(index -> index == reference.slot())) {
                return false;
            }
            return conditions.cosmetic().isEmpty() || conditions.cosmetic().get().equals(cosmetic);
        });
    }

    @Override
    public @NotNull Codec<Conditions> codec() {
        return Conditions.CODEC;
    }

    public record Conditions(
            Optional<ContextAwarePredicate> player,
            Optional<List<ItemPredicate>> itemPredicates,
            Optional<List<String>> groups,
            Optional<List<String>> slots,
            Optional<List<Integer>> indices,
            Optional<Boolean> cosmetic
    ) implements SimpleInstance {
        public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ExtraCodecs.strictOptionalField(EntityPredicate.ADVANCEMENT_CODEC, "player").forGetter(Conditions::player),
                ExtraCodecs.strictOptionalField(ItemPredicate.CODEC.listOf(), "items").forGetter(Conditions::itemPredicates),
                ExtraCodecs.strictOptionalField(Codec.STRING.listOf(), "groups").forGetter(Conditions::groups),
                ExtraCodecs.strictOptionalField(Codec.STRING.listOf(), "slots").forGetter(Conditions::slots),
                ExtraCodecs.strictOptionalField(Codec.INT.listOf(), "indices").forGetter(Conditions::indices),
                ExtraCodecs.strictOptionalField(Codec.BOOL, "cosmetic").forGetter(Conditions::cosmetic)
                ).apply(instance, Conditions::new));
    }
}