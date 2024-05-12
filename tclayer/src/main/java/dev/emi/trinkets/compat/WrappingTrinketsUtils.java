package dev.emi.trinkets.compat;

import dev.emi.trinkets.api.LivingEntityTrinketComponent;
import dev.emi.trinkets.api.SlotReference;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.data.SlotTypeLoader;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public class WrappingTrinketsUtils {

    public static Optional<SlotReference> createReference(io.wispforest.accessories.api.slot.SlotReference slotReference){
        try {
            var capability = AccessoriesCapability.get(slotReference.entity());

            if(capability == null) return Optional.empty();

            var curiosHandler = capability.getContainers().get(trinketsToAccessories_Slot(slotReference.slotName()));

            var slotType = SlotTypeLoader.getSlotType(slotReference.entity().level(), curiosHandler.getSlotName());

            var trinketInv = new WrappedTrinketInventory(new LivingEntityTrinketComponent(capability), curiosHandler, slotType);

            return Optional.of(new SlotReference(trinketInv, slotReference.slot()));
        } catch (Exception e){
            return Optional.empty();
        }
    }

    public static String trinketsToAccessories_Slot(String trinketType){
        return switch (trinketType){
            case "glove" -> "hand";
            case "aglet" -> "anklet";
            default -> trinketType;
        };
    }

    public static String accessoriesToTrinkets_Slot(String accessoryType){
        return switch (accessoryType){
            case "hand" -> "glove";
            case "anklet" -> "aglet";
            default -> accessoryType;
        };
    }

    public static String trinketsToAccessories_Group(String trinketType){
        return switch (trinketType){
            case "leg" -> "legs";
            case "offhand", "hand" -> "arm";
            case "charm" -> "misc";
            default -> trinketType;
        };
    }

    public static String accessoriesToTrinkets_Group(String accessoryType){
        return switch (accessoryType){
            case "legs" -> "leg";
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
}
