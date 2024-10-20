package io.wispforest.accessories.api.caching;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.Objects;

public class DataComponentMapPredicate extends ItemStackBasedPredicate {

    public final DataComponentMap dataComponentMap;

    public DataComponentMapPredicate(String name, DataComponentMap dataComponentMap){
        super(name);

        this.dataComponentMap = dataComponentMap;
    }

    @Override
    public boolean test(ItemStack stack) {
        for (var typedDataComponent : dataComponentMap) {
            if (!typedDataComponent.equals(stack.get(typedDataComponent.type()))) return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(dataComponentMap.stream().toArray());
    }

    @Override
    protected boolean isEqual(Object other) {
        var dataComponentMapPredicate = (DataComponentMapPredicate) other;

        return Objects.equals(this.dataComponentMap, dataComponentMapPredicate.dataComponentMap);
    }

    @Override
    public String extraStringData() {
        return "DataComponents: " + this.dataComponentMap.toString();
    }

    public interface ComponentAddCallback {
        <T> void add(DataComponentType<T> type, T t);
    }
}