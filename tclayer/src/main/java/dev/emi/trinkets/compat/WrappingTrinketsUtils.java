package dev.emi.trinkets.compat;

import dev.emi.trinkets.api.LivingEntityTrinketComponent;
import dev.emi.trinkets.api.SlotReference;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.accessories.data.EntitySlotLoader;
import io.wispforest.accessories.data.SlotGroupLoader;
import io.wispforest.accessories.data.SlotTypeLoader;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class WrappingTrinketsUtils {

    public static Optional<SlotReference> createTrinketsReference(io.wispforest.accessories.api.slot.SlotReference slotReference){
        try {
            var capability = AccessoriesCapability.get(slotReference.entity());

            if(capability == null) return Optional.empty();

            var container = capability.getContainers().get(slotReference.slotName());

            var slotType = SlotTypeLoader.getSlotType(slotReference.entity().level(), container.getSlotName());

            var trinketInv = new WrappedTrinketInventory(new LivingEntityTrinketComponent(capability), container, slotType);

            return Optional.of(new SlotReference(trinketInv, slotReference.slot()));
        } catch (Exception e){
            return Optional.empty();
        }
    }

    public static Optional<io.wispforest.accessories.api.slot.SlotReference> createAccessoriesReference(SlotReference slotReference){
        if(!(slotReference.inventory() instanceof WrappedTrinketInventory wrappedTrinketInventory)) return Optional.empty();

        return Optional.of(
                io.wispforest.accessories.api.slot.SlotReference.of(
                        wrappedTrinketInventory.container.capability().entity(),
                        wrappedTrinketInventory.container.getSlotName(),
                        slotReference.index()
                )
        );
    }

    public static Map<String, Map<String, SlotType>> getGroupedSlots(boolean isClient, EntityType<?> type) {
        var groups = new HashMap<String, Map<String, SlotType>>();
        var entitySlots = EntitySlotLoader.INSTANCE.getSlotTypes(isClient, type);

        if(entitySlots == null) return Map.of();

        for (var group : SlotGroupLoader.INSTANCE.getGroups(false, false)) {
            for (var slot : group.slots()) {
                if(!entitySlots.containsKey(slot)) continue;

                groups.computeIfAbsent(group.name(), string -> new HashMap<>())
                        .put(slot, entitySlots.get(slot));
            }
        }

        return groups;
    }

    public static final Set<String> defaultSlots = Set.of("anklet", "back", "belt", "cape", "charm", "face", "hand", "hat", "necklace", "ring", "shoes", "wrist");

    // Unsafe Operation
    public static String trinketsToAccessories_Slot(Optional<String> group, String trinketType){
        var accessoriesType = switch (trinketType){
            case "glove" -> "hand";
            case "aglet" -> "anklet";
            default -> trinketType;
        };

        if(defaultSlots.contains(accessoriesType)) return accessoriesType;

        if(group.isPresent()) accessoriesType = "trinket_group_" + group.get() + "-" + accessoriesType;

        return accessoriesType;
    }

    // Safe Operation
    public static String accessoriesToTrinkets_Slot(String accessoryType){
        var trinketType = switch (accessoryType){
            case "hand" -> "glove";
            case "anklet" -> "aglet";
            default -> accessoryType;
        };

        return filterGroupInfo(trinketType);
    }

    public static String trinketsToAccessories_Group(String trinketType){
        return switch (trinketType){
            case "legs" -> "leg";
            case "offhand", "hand" -> "arm";
            case "charm" -> "misc";
            default -> trinketType;
        };
    }

    public static String accessoriesToTrinkets_Group(String accessoryType){
        return switch (accessoryType){
            case "leg" -> "legs";
            case "arm" -> "hand";
            case "misc" -> "charm";
            default -> accessoryType;
        };
    }

    public static ResourceLocation trinketsToAccessories_Validators(ResourceLocation location) {
        return switch (location.toString()){
            case "trinkets:all" -> Accessories.of("all");
            case "trinkets:none" -> Accessories.of("none");
            case "trinkets:tag" -> Accessories.of("tag");
            case "trinkets:relevant" -> Accessories.of("relevant");
            default -> location;
        };
    }

    public static String filterGroupInfo(String trinketType) {
        return trinketType.replaceAll("(trinket_group_).*-", "");
    }

    public static Pair<Optional<String>, String> splitGroupInfo(String path){
        if(!path.contains("/")) return Pair.of(Optional.empty(), path);

        var parts = path.split("/");

        if(parts.length <= 1)  return Pair.of(Optional.empty(), path);

        StringBuilder builder = new StringBuilder();

        for (int i = 1; i < parts.length; i++) builder.append(parts[i]);

        return Pair.of(Optional.of(parts[0]), builder.toString());
    }
}
