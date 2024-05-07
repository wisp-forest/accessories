package io.wispforest.accessories.networking.holder;

import io.wispforest.accessories.AccessoriesInternals;
import io.wispforest.accessories.api.AccessoriesHolder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

public record HolderProperty<T>(String name, BiConsumer<FriendlyByteBuf, T> writer, Function<FriendlyByteBuf, T> reader, BiConsumer<AccessoriesHolder, T> setter, Function<AccessoriesHolder, T> getter) {

    private static final Map<String, HolderProperty<?>> ALL_PROPERTIES = new HashMap<>();

    public static HolderProperty<Boolean> LINES_PROP;
    public static HolderProperty<Boolean> COSMETIC_PROP;
    public static HolderProperty<Boolean> UNUSED_PROP;

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

    public void write(FriendlyByteBuf buf, Object data) {
        writer.accept(buf, (T) data);
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

        LINES_PROP = new HolderProperty<>("lines", FriendlyByteBuf::writeBoolean, FriendlyByteBuf::readBoolean, AccessoriesHolder::linesShown, AccessoriesHolder::linesShown);
        COSMETIC_PROP = new HolderProperty<>("cosmetic", FriendlyByteBuf::writeBoolean, FriendlyByteBuf::readBoolean, AccessoriesHolder::cosmeticsShown, AccessoriesHolder::cosmeticsShown);
        UNUSED_PROP = new HolderProperty<>("unused_slots", FriendlyByteBuf::writeBoolean, FriendlyByteBuf::readBoolean, AccessoriesHolder::showUnusedSlots, AccessoriesHolder::showUnusedSlots);
    }
}
