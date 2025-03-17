package io.wispforest.accessories.api.caching;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;

public class DataComponentsPredicate extends ItemStackBasedPredicate {

    public final DataComponentType<?>[] dataComponentTypes;

    public DataComponentsPredicate(String name, DataComponentType<?>... dataComponentTypes){
        super(name);

        this.dataComponentTypes = dataComponentTypes;
    }

    @Override
    public boolean test(ItemStack stack) {
        for (var dataComponentType : this.dataComponentTypes) {
            if (!stack.has(dataComponentType)) return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(dataComponentTypes);
    }

    @Override
    protected boolean isEqual(Object other) {
        var itemComponentPredicate = (DataComponentsPredicate) other;

        return Arrays.equals(this.dataComponentTypes, itemComponentPredicate.dataComponentTypes);
    }

    @Override
    public String extraStringData() {
        return "DataComponents: " + Arrays.toString(this.dataComponentTypes);
    }
}
