package io.wispforest.accessories.mixin;

import io.wispforest.accessories.AccessoriesInternals;
import net.minecraft.world.entity.EquipmentSlot;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("unused")
@Mixin(EquipmentSlot.class)
public abstract class EquipmentSlotMixin {

    @Invoker("<init>")
    public static EquipmentSlot invokeNew(String internalName, int ordinal, EquipmentSlot.Type type, int index, int filterFlag, String name) {
        throw new IllegalStateException("How did this mixin stub get called conc");
    }

    @Final
    @Shadow
    @Mutable
    private static EquipmentSlot[] $VALUES;

    // PUTSTATIC net/minecraft/world/entity/EquipmentSlot.$VALUES : [Lnet/minecraft/world/entity/EquipmentSlot;
    @Inject(method = "<clinit>", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/EquipmentSlot;$VALUES:[Lnet/minecraft/world/entity/EquipmentSlot;", shift = At.Shift.AFTER, opcode = Opcodes.PUTSTATIC))
    private static void addInternalAccessoriesEquipmentSlot(CallbackInfo ci) {
        AccessoriesInternals.INTERNAL_SLOT = EquipmentSlotMixin.invokeNew("ACCESSORIES", -1, AccessoriesInternals.ACCESSORIES_TYPE, 0, -1,  "accessories");

//        $VALUES = ArrayUtils.buildWith(
//                EquipmentSlot.class,
//                $VALUES,
//                index -> {
//                    var slot = EquipmentSlotMixin.invokeNew("ACCESSORIES", index, Accessories.ACCESSORIES_TYPE, 0, -1,  "accessories");
//
//                    Accessories.INTERNAL_SLOT = slot;
//
//                    return slot;
//                }
//        );
    }
}
