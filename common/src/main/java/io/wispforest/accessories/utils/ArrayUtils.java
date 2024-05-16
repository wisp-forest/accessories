package io.wispforest.accessories.utils;

import java.lang.reflect.Array;
import java.util.function.Function;

public class ArrayUtils {

    @SafeVarargs
    public static <T> T[] buildWith(Class<T> clazz, T[] oldArray, Function<Integer, T> ...entryFunctions){
        var newArray = (T[]) Array.newInstance(clazz, oldArray.length + entryFunctions.length);

        System.arraycopy(oldArray, 0, newArray, 0, oldArray.length);

        int offset = oldArray.length;

        for (var func : entryFunctions) {
            newArray[offset] = func.apply(offset);

            offset++;
        }

        return newArray;
    }
}
