package io.wispforest.accessories.impl.option;

import com.google.common.reflect.Reflection;
import com.mojang.logging.LogUtils;
import io.wispforest.accessories.AccessoriesInternals;
import io.wispforest.accessories.networking.holder.SyncOptionChange;
import io.wispforest.endec.Endec;
import io.wispforest.endec.impl.KeyedEndec;
import io.wispforest.endec.util.MapCarrierEncodable;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.SequencedCollection;
import java.util.SequencedMap;
import java.util.function.Supplier;

public final class PlayerOption<T> {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final Endec<PlayerOption<?>> ENDEC = Endec.STRING.xmap(PlayerOption::getProperty, PlayerOption::name);

    private static final SequencedMap<String, PlayerOption<?>> ALL_PROPERTIES = new LinkedHashMap<>();

    static {
        Reflection.initialize(PlayerOptions.class);
    }

    private final String name;
    private final Endec<T> endec;
    private final KeyedEndec<T> keyEndec;
    private final Supplier<T> defaultValue;

    PlayerOption(String name, Endec<T> endec, T defaultValue) {
        this(name, endec, () -> defaultValue);
    }

    PlayerOption(String name, Endec<T> endec, Supplier<T> defaultValue) {
        if (ALL_PROPERTIES.containsKey(name)) throw new IllegalStateException("Unable to create the given PlayerOption [" + name + "] as it is already contained within ALL_PROPERTIES! ");

        ALL_PROPERTIES.put(name, this);

        this.name = name;
        this.endec = endec;
        this.keyEndec = endec.keyed(name, defaultValue);
        this.defaultValue = defaultValue;
    }

    public static SequencedCollection<PlayerOption<?>> getAllOptions() {
        return ALL_PROPERTIES.sequencedValues();
    }

    public static PlayerOption<?> getProperty(String name) {
        var prop = ALL_PROPERTIES.get(name);

        if (prop == null) {
            throw new IllegalStateException("Unable to locate the given HolderProperty! [Name: " + name + "]");
        }

        return prop;
    }

    public void setData(Player player, Object data) {
        if (player.level().isClientSide()) {
            var holder = AccessoriesPlayerOptionsHolder.getOptions(player);

            if (holder == null) {
                LOGGER.warn("[PlayerOption] Unable to set the given holder value '{}' for the given owner: {}", this.name, player.getName());

                return;
            }
        }

        AccessoriesInternals.INSTANCE.modifyPlayerOptions(player, holder -> {
            holder.setData(this, (T) data);

            return holder;
        });
    }

    public KeyedEndec<T> keyEndec() {
        return keyEndec;
    }

    public String name() {
        return name;
    }

    public Endec<T> endec() {
        return endec;
    }

    public T defaultValue() {
        return defaultValue.get();
    }

    public Optional<T> getData(Player player) {
        return Optional.ofNullable(AccessoriesPlayerOptionsHolder.getOptions(player))
            .flatMap(holder -> holder.getData(this));
    }

    public T getDataOrDefault(Player player) {
        return getData(player)
            .orElseGet(() -> {
                LOGGER.warn("[PlayerOption] Unable to get the given holder value '{}' for the given owner: {}", this.name, player.getName());

                return defaultValue.get();
            });
    }

    public SyncOptionChange toPacket(T data) {
        return SyncOptionChange.of(this, data);
    }

    public void writeToCarrierCasted(MapCarrierEncodable carrier, Object value) {
        carrier.put(this.keyEndec(), (T) value);
    }

    @Override
    public String toString() {
        return "PlayerOption[" +
                "name=" + name + ", " +
                "endec=" + endec + ", " +
                "defaultValue=" + defaultValue + ']';
    }

}
