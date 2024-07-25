package io.wispforest.accessories.mixin;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.CriterionTrigger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(CriteriaTriggers.class)
public interface CriteriaTriggersAccessor {

    @Invoker("register")
    static <T extends CriterionTrigger<?>> T accessories$callRegister(T trigger) {
        throw new UnsupportedOperationException();
    }
}