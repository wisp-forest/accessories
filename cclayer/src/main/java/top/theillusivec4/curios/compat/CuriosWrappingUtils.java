package top.theillusivec4.curios.compat;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import io.wispforest.accessories.api.Accessory;
import io.wispforest.accessories.api.DropRule;
import io.wispforest.accessories.api.slot.SlotReference;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.event.CurioAttributeModifierEvent;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import top.theillusivec4.curios.mixin.CuriosImplMixinHooks;

import java.util.Optional;
import java.util.UUID;

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
            case "feet" -> "shoes"; // Special Case for artifacts
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
            case "shoes" -> "feet"; // Special Case for artifacts
            default -> accessoryType;
        };
    }

    //--

    public static Multimap<Attribute, AttributeModifier> getAttributeModifiers(Multimap<Attribute, AttributeModifier> multimap, SlotContext slotContext, UUID uuid, ItemStack stack) {
        if (stack.getTag() != null && stack.getTag().contains("CurioAttributeModifiers", 9)) {
            multimap.clear();

            ListTag listnbt = stack.getTag().getList("CurioAttributeModifiers", 10);
            String identifier = slotContext.identifier();

            for (int i = 0; i < listnbt.size(); ++i) {
                CompoundTag compoundnbt = listnbt.getCompound(i);

                if (compoundnbt.getString("Slot").equals(identifier)) {
                    ResourceLocation rl = ResourceLocation.tryParse(compoundnbt.getString("AttributeName"));
                    UUID id = uuid;

                    if (rl != null) {

                        if (compoundnbt.contains("UUID")) {
                            id = compoundnbt.getUUID("UUID");
                        }

                        if (id.getLeastSignificantBits() != 0L && id.getMostSignificantBits() != 0L) {
                            AttributeModifier.Operation operation =
                                    AttributeModifier.Operation.fromValue(compoundnbt.getInt("Operation"));
                            double amount = compoundnbt.getDouble("Amount");
                            String name = compoundnbt.getString("Name");

                            if (rl.getNamespace().equals("curios")) {
                                String identifier1 = CuriosWrappingUtils.curiosToAccessories(rl.getPath());

                                if (CuriosApi.getSlot(identifier1).isPresent()) {
                                    CuriosApi.addSlotModifier(multimap, identifier1, id, amount, operation);
                                }
                            } else {
                                Attribute attribute = BuiltInRegistries.ATTRIBUTE.getOptional(rl).orElse(null);

                                if (attribute != null) {
                                    multimap.put(attribute, new AttributeModifier(id, name, amount, operation));
                                }
                            }
                        }
                    }
                }
            }
        }

        CurioAttributeModifierEvent evt = new CurioAttributeModifierEvent(stack, slotContext, uuid, multimap);

        MinecraftForge.EVENT_BUS.post(evt);

        return HashMultimap.create(evt.getModifiers());
    }

}
