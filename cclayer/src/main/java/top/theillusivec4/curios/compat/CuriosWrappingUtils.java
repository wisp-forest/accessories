package top.theillusivec4.curios.compat;

import io.wispforest.accessories.api.Accessory;
import io.wispforest.accessories.api.DropRule;
import io.wispforest.accessories.api.slot.SlotReference;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import top.theillusivec4.curios.mixin.CuriosImplMixinHooks;

import java.util.Optional;

public class CuriosWrappingUtils {

    public static SlotContext create(SlotReference reference){
        return create(reference, true);
    }

    public static SlotContext create(SlotReference reference, boolean visible){
        return new SlotContext(reference.slotName(), reference.entity(), reference.slot(), false, visible);
    }

    public static SlotReference fromContext(SlotContext context){
        return new SlotReference(context.identifier(), context.entity(), context.index());
    }

    //--

    public static ICurio.DropRule convert(DropRule dropRule){
        return switch (dropRule){
            case KEEP -> ICurio.DropRule.ALWAYS_KEEP;
            case DROP -> ICurio.DropRule.ALWAYS_DROP;
            case DESTROY -> ICurio.DropRule.DESTROY;
            case DEFAULT -> ICurio.DropRule.DEFAULT;
        };
    }

    public static DropRule convert(ICurio.DropRule dropRule){
        return switch (dropRule){
            case DEFAULT -> DropRule.DEFAULT;
            case ALWAYS_DROP -> DropRule.DROP;
            case ALWAYS_KEEP -> DropRule.KEEP;
            case DESTROY -> DropRule.DESTROY;
        };
    }

    //--

    public static Optional<Accessory> of(ItemStack stack){
        return CuriosImplMixinHooks.getCurioFromRegistry(stack.getItem())
                .or(() -> Optional.ofNullable((stack.getItem() instanceof ICurioItem itemCurio) ? itemCurio : null))
                .map(WrappedCurio::new);
    }

    //--

    public static String curiosToAccessories(String curiosType){
        return switch (curiosType){
            case "curio" -> "any"; // CONFIRM THIS IS WORKING?
            case "body" -> "cape";
            case "bracelet" -> "wrist";
            case "head" -> "hat";
            case "hands" -> "hand";
            default -> curiosType;
        };
    }

    public static String accessoriesToCurios(String accessoryType){
        return switch (accessoryType){
            case "any" -> "curio"; // CONFIRM THIS IS WORKING?
            case "cape" -> "body" ;
            case "wrist" -> "bracelet";
            case "hat" -> "head";
            case "hand" -> "hands";
            default -> accessoryType;
        };
    }
}
