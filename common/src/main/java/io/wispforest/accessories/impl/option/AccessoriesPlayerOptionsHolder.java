package io.wispforest.accessories.impl.option;

import com.google.common.collect.Streams;
import io.wispforest.accessories.AccessoriesInternals;
import io.wispforest.accessories.utils.InstanceEndec;
import io.wispforest.endec.Endec;
import io.wispforest.endec.SerializationContext;
import io.wispforest.endec.format.edm.EdmElement;
import io.wispforest.endec.format.edm.EdmEndec;
import io.wispforest.endec.util.MapCarrierDecodable;
import io.wispforest.endec.util.MapCarrierEncodable;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class AccessoriesPlayerOptionsHolder implements InstanceEndec, PlayerOptionsAccess {

    public static final Endec<@Nullable AccessoriesPlayerOptionsHolder> ENDEC = Endec.of((ctx, serializer, holder) -> {
        if (holder != null && holder.isDefaultedValues()) {
            holder = null;
        }

        serializer.writeOptional(ctx, EdmEndec.MAP, Optional.ofNullable(holder).map(obj -> {
                var map = EdmElement.consumeMap(new LinkedHashMap<>()).asMap();

                obj.encode(map, ctx);

                return map;
        }));
    }, (ctx, deserializer) -> {
        return deserializer.readOptional(ctx, EdmEndec.MAP).map(edmMap -> {
            var holder = new AccessoriesPlayerOptionsHolder();

            holder.decode(edmMap, ctx);

            return holder;
        }).orElse(null);
    });

    private Map<PlayerOption<?>, Object> optionToValue = new LinkedHashMap<>();

    public AccessoriesPlayerOptionsHolder() {}

    public static AccessoriesPlayerOptionsHolder getOptions(Player player) {
        return AccessoriesInternals.INSTANCE.getPlayerOptions(player);
    }

    public boolean hasData(PlayerOption<?> option) {
        return optionToValue.containsKey(option);
    }

    public <T> Optional<T> getData(PlayerOption<T> option) {
        return (this.optionToValue.containsKey(option))
            ? Optional.of((T) this.optionToValue.get(option))
            : Optional.empty();
    }

    public <T> void setData(PlayerOption<T> option, T data) {
        Objects.requireNonNull(option, "Unable to set data as the given PlayerOption instance is null!");

        this.optionToValue.put(option, data);
    }

    @Override
    public void encode(MapCarrierEncodable carrier, SerializationContext ctx) {
        optionToValue.forEach((playerOption, object) -> playerOption.writeToCarrierCasted(carrier, object));
    }

    @Override
    public void decode(MapCarrierDecodable carrier, SerializationContext ctx) {
        this.optionToValue.clear();

        for (PlayerOption<?> option : PlayerOption.getAllOptions()) {
            if (!carrier.has(option.keyEndec())) continue;

            this.optionToValue.put(option, carrier.get(option.keyEndec()));
        }
    }

    public boolean isDefaultedValues() {
        return this.optionToValue.keySet().stream().allMatch(option -> Objects.equals(getDefaultedData(option), option.defaultValue()));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof AccessoriesPlayerOptionsHolder otherHolder)) return false;

        var allOptions = Streams.concat(this.optionToValue.keySet().stream(), otherHolder.optionToValue.keySet().stream())
            .collect(Collectors.toSet());

        for (var playerOption : allOptions) {
            if (!Objects.equals(otherHolder.getDefaultedData(playerOption), getDefaultedData(playerOption))) {
                return false;
            }
        }

        return true;
    }

    public static AccessoriesPlayerOptionsHolder createOrCopy(@Nullable AccessoriesPlayerOptionsHolder holder) {
        if (holder == null) return new AccessoriesPlayerOptionsHolder();

        return holder.copy();
    }

    public AccessoriesPlayerOptionsHolder copy() {
        var holder = new AccessoriesPlayerOptionsHolder();

        holder.optionToValue.putAll(this.optionToValue);

        return holder;
    }
}
