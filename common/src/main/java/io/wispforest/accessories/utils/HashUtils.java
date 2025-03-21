package io.wispforest.accessories.utils;

import java.util.Objects;

public class HashUtils {

    public static int getHash(Throwable throwable) {
        var hash = (throwable.getCause() != null) ? getHash(throwable.getCause()) : 0;

        for (var innerThrowable : throwable.getSuppressed()) {
            hash = Objects.hash(hash, getHash(innerThrowable));
        }

        return Objects.hash(hash, throwable.getMessage());
    }
}
