package io.wispforest.accessories.mixin.client.owo;

import io.wispforest.accessories.client.gui.components.ExtendedScrollContainer;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.container.WrappingParentUIComponent;
import io.wispforest.owo.ui.core.UIComponent;
import io.wispforest.owo.ui.core.OwoUIGraphics;
import io.wispforest.owo.ui.core.Sizing;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ScrollContainer.class, remap = false)
public abstract class ScrollContainerMixin<C extends UIComponent> extends WrappingParentUIComponent<C> {
    @Shadow @Final protected ScrollContainer.ScrollDirection direction;

    @Shadow protected int scrollbarOffset;

    protected ScrollContainerMixin(Sizing horizontalSizing, Sizing verticalSizing, C child) {
        super(horizontalSizing, verticalSizing, child);
    }

    @Inject(method = "draw", at = @At(value = "JUMP", opcode = Opcodes.IF_ACMPNE, ordinal = 2), remap = false)
    private void adjustOffsetForExtendedScrollContainer(OwoUIGraphics context, int mouseX, int mouseY, float partialTicks, float delta, CallbackInfo ci) {
        if(((ScrollContainer) (Object) this) instanceof ExtendedScrollContainer<?> extendedScrollContainer && extendedScrollContainer.oppositeScrollbar()) {
            this.scrollbarOffset = this.direction == ScrollContainer.ScrollDirection.VERTICAL ? this.x() : this.y();
        }
    }
}
