package io.wispforest.accessories.endec;

import io.wispforest.endec.format.edm.EdmMap;

import java.util.HashMap;

public class EdmUtils {
    public static EdmMap newMap() {
        return EdmMap.wrapMap(new HashMap<>()).asMap();
    }
}
