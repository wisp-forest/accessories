package io.wispforest.accessories.mixin.client.owo;

import io.wispforest.accessories.pond.owo.ComponentExtension;
import io.wispforest.owo.ui.core.Component;
import io.wispforest.owo.ui.inject.ComponentStub;
import net.minecraft.client.gui.components.AbstractWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = AbstractWidget.class, remap = false)
public abstract class ComponentStubMixin implements ComponentStub, ComponentExtension<Component> {

    @Unique
    private boolean accessories$allowIndividualOverdraw = false;

    @Override
    public Component allowIndividualOverdraw(boolean value) {
        this.accessories$allowIndividualOverdraw = value;

        return (Component) (Object) this;
    }

    @Override
    public boolean allowIndividualOverdraw() {
        return accessories$allowIndividualOverdraw;
    }
}
