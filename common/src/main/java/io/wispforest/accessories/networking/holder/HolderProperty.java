package io.wispforest.accessories.networking.holder;

import io.wispforest.accessories.AccessoriesInternals;
import io.wispforest.accessories.api.AccessoriesHolder;
import io.wispforest.accessories.impl.PlayerEquipControl;
import io.wispforest.endec.Endec;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

public record HolderProperty<T>(String name, Endec<T> endec, BiConsumer<AccessoriesHolder, T> setter, Function<AccessoriesHolder, T> getter) {

    public static final Endec<HolderProperty<?>> ENDEC = Endec.STRING.xmap(HolderProperty::getProperty, HolderProperty::name);

    private static final Map<String, HolderProperty<?>> ALL_PROPERTIES = new HashMap<>();

    public static HolderProperty<Boolean> LINES_PROP;
    public static HolderProperty<Boolean> COSMETIC_PROP;
    public static HolderProperty<Boolean> UNUSED_PROP;
    public static HolderProperty<Boolean> UNIQUE_PROP;
    public static HolderProperty<PlayerEquipControl> EQUIP_CONTROL;

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

        LINES_PROP = new HolderProperty<>("lines", Endec.BOOLEAN, AccessoriesHolder::linesShown, AccessoriesHolder::linesShown);
        COSMETIC_PROP = new HolderProperty<>("cosmetic", Endec.BOOLEAN, AccessoriesHolder::cosmeticsShown, AccessoriesHolder::cosmeticsShown);
        UNUSED_PROP = new HolderProperty<>("unused_slots", Endec.BOOLEAN, AccessoriesHolder::showUnusedSlots, AccessoriesHolder::showUnusedSlots);
        UNIQUE_PROP = new HolderProperty<>("unique_slots", Endec.BOOLEAN, AccessoriesHolder::showUniqueSlots, AccessoriesHolder::showUniqueSlots);
        EQUIP_CONTROL = new HolderProperty<>("equip_control", Endec.forEnum(PlayerEquipControl.class), AccessoriesHolder::equipControl, AccessoriesHolder::equipControl);
    }
}
