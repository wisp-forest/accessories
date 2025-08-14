package io.wispforest.accessories.impl.option;

import io.wispforest.accessories.AccessoriesInternals;
import io.wispforest.endec.SerializationContext;
import io.wispforest.endec.util.MapCarrierDecodable;
import io.wispforest.endec.util.MapCarrierEncodable;
import io.wispforest.accessories.utils.InstanceEndec;
import net.minecraft.world.entity.player.Player;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class AccessoriesPlayerOptionsHolder implements InstanceEndec {

    private Map<PlayerOption<?>, Object> optionToValue = new LinkedHashMap<>();

    public AccessoriesPlayerOptionsHolder() {}

    public static AccessoriesPlayerOptionsHolder getOptions(Player player) {
        return AccessoriesInternals.getPlayerOptions(player);
    }

    public <T> T getData(PlayerOption<T> option) {
        return (this.optionToValue.containsKey(option))
                ? (T) this.optionToValue.get(option)
                : option.defaultValue();
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
            this.optionToValue.put(option, carrier.get(option.keyEndec()));
        }
    }
}
