package io.wispforest.accessories.impl.option;

import java.util.Optional;

public interface PlayerOptionsAccess {

    <T> Optional<T> getData(PlayerOption<T> option);

    default <T> T getDefaultedData(PlayerOption<T> option) {
        return getData(option).orElseGet(option::defaultValue);
    }

    default boolean hasData(PlayerOption<?> option) {
        return getData(option).isPresent();
    }

    <T> void setData(PlayerOption<T> option, T data);
}
