package io.wispforest.accessories.networking.holder;

import io.wispforest.accessories.AccessoriesInternals;
import io.wispforest.accessories.api.AccessoriesHolder;
import io.wispforest.accessories.impl.PlayerEquipControl;
import io.wispforest.endec.Endec;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

public record HolderProperty<T>(String name, Endec<T> endec, BiConsumer<AccessoriesHolder, T> setter, Function<AccessoriesHolder, T> getter) {

    public static final Endec<HolderProperty<?>> ENDEC = Endec.STRING.xmap(HolderProperty::getProperty, HolderProperty::name);

    private static final Map<String, HolderProperty<?>> ALL_PROPERTIES = new HashMap<>();

    public static HolderProperty<PlayerEquipControl> EQUIP_CONTROL;

    public static HolderProperty<Boolean> UNUSED_PROP;

    public static HolderProperty<Boolean> COSMETIC_PROP;

    public static HolderProperty<Integer> COLUMN_AMOUNT_PROP;
    public static HolderProperty<Integer> WIDGET_TYPE_PROP;

    public static HolderProperty<Boolean> GROUP_FILTER_PROP;
    public static HolderProperty<Boolean> GROUP_FILTER_OPEN_PROP;
    public static HolderProperty<Set<String>> FILTERED_GROUPS;

    public static HolderProperty<Boolean> MAIN_WIDGET_POSITION_PROP;
    public static HolderProperty<Boolean> SIDE_WIDGET_POSITION_PROP;

    public static HolderProperty<Boolean> CRAFTING_GRID_PROP;

    static { init(); }

    public static HolderProperty<?> getProperty(String name) {
        if(ALL_PROPERTIES.isEmpty()) init();

        var prop = ALL_PROPERTIES.get(name);

        if(prop == null) {
            throw new IllegalStateException("Unable to locate the given HolderProperty! [Name: " + name + "]");
        }

        return prop;
    }

    public HolderProperty {
        ALL_PROPERTIES.put(name, this);
    }

    public void setData(Player player, Object data) {
        AccessoriesInternals.modifyHolder(player, holder -> {
            setter.accept(holder, (T) data);

            return holder;
        });
    }

    public <V> V consumeData(Player player, BiFunction<HolderProperty<T>, T, V> biFunction) {
        var data = this.getter().apply(player.accessoriesHolder());
        return biFunction.apply(this, data);
    }

    public static void init() {
        if(!ALL_PROPERTIES.isEmpty()) return;

        EQUIP_CONTROL = new HolderProperty<>("equip_control", Endec.forEnum(PlayerEquipControl.class), AccessoriesHolder::equipControl, AccessoriesHolder::equipControl);

        COLUMN_AMOUNT_PROP = new HolderProperty<>("column_amount", Endec.VAR_INT, AccessoriesHolder::columnAmount, AccessoriesHolder::columnAmount);
        WIDGET_TYPE_PROP = new HolderProperty<>("widget_type", Endec.VAR_INT, AccessoriesHolder::widgetType, AccessoriesHolder::widgetType);

        MAIN_WIDGET_POSITION_PROP = new HolderProperty<>("main_widget_position", Endec.BOOLEAN, AccessoriesHolder::mainWidgetPosition, AccessoriesHolder::mainWidgetPosition);
        SIDE_WIDGET_POSITION_PROP = new HolderProperty<>("side_widget_position", Endec.BOOLEAN, AccessoriesHolder::sideWidgetPosition, AccessoriesHolder::sideWidgetPosition);

        UNUSED_PROP = new HolderProperty<>("unused_slots", Endec.BOOLEAN, AccessoriesHolder::showUnusedSlots, AccessoriesHolder::showUnusedSlots);

        COSMETIC_PROP = new HolderProperty<>("cosmetic", Endec.BOOLEAN, AccessoriesHolder::cosmeticsShown, AccessoriesHolder::cosmeticsShown);

        GROUP_FILTER_PROP = new HolderProperty<>("group_filter", Endec.BOOLEAN, AccessoriesHolder::showGroupFilter, AccessoriesHolder::showGroupFilter);
        GROUP_FILTER_OPEN_PROP = new HolderProperty<>("group_filter_open", Endec.BOOLEAN, AccessoriesHolder::isGroupFiltersOpen, AccessoriesHolder::isGroupFiltersOpen);
        FILTERED_GROUPS = new HolderProperty<>("filtered_groups", Endec.STRING.setOf(), AccessoriesHolder::filteredGroups, AccessoriesHolder::filteredGroups);

        CRAFTING_GRID_PROP = new HolderProperty<>("crafting_grid", Endec.BOOLEAN, AccessoriesHolder::showCraftingGrid, AccessoriesHolder::showCraftingGrid);
    }
}
