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

@Mixin(EquipmentSlot.Type.class)
public abstract class EquipmentSlotTypeMixin {

    @Final
    @Shadow
    @Mutable
    private static EquipmentSlot.Type[] $VALUES;

    @Invoker("<init>")
    public static EquipmentSlot.Type invokeNew(String internalName, int ordinal) {
        throw new IllegalStateException("How did this mixin stub get called conc");
    }

    @Inject(method = "<clinit>", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/EquipmentSlot$Type;$VALUES:[Lnet/minecraft/world/entity/EquipmentSlot$Type;", shift = At.Shift.AFTER, opcode = Opcodes.PUTSTATIC))
    private static void addInternalAccessoriesEquipmentSlot(CallbackInfo ci) {
        AccessoriesInternals.ACCESSORIES_TYPE = EquipmentSlotTypeMixin.invokeNew("ACCESSORIES", -1);

//        $VALUES = ArrayUtils.buildWith(EquipmentSlot.Type.class, $VALUES,
//                index -> {
//                    var type = EquipmentSlotTypeMixin.invokeNew("ACCESSORIES", index);
//
//                    Accessories.ACCESSORIES_TYPE = type;
//
//                    return type;
//                }
//        );
    }
}
