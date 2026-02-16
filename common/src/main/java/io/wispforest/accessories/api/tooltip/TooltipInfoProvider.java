package io.wispforest.accessories.api.tooltip;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;

public interface TooltipInfoProvider<A extends TooltipAdder> {

    static <E extends A, A extends TooltipAdder> E gatherInfo(TooltipInfoProvider<A> provider, E adder, Item.TooltipContext ctx, TooltipFlag type) {
        provider.addInfo(adder, ctx, type);

        return adder;
    }

    void addInfo(A adder, Item.TooltipContext ctx, TooltipFlag type);
}
