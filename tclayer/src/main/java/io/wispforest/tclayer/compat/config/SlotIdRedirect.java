package io.wispforest.tclayer.compat.config;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import it.unimi.dsi.fastutil.Pair;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SlotIdRedirect {

    public String trinketsId;
    public String accessoriesId;

    public int additionalSlot;

    public SlotIdRedirect() {
        this("", "", 0);
    }

    public SlotIdRedirect(String trinketsId, String accessoriesId, int additionalSlot) {
        this.trinketsId = trinketsId;
        this.accessoriesId = accessoriesId;
        this.additionalSlot = additionalSlot;
    }

    public static BiMap<String, String> getBiMap(List<SlotIdRedirect> list) {
        return HashBiMap.create(list.stream().collect(Collectors.toMap(redirect -> redirect.trinketsId, redirect -> redirect.accessoriesId)));
    }

    public static Map<String, Pair<String, Integer>> getMap(List<SlotIdRedirect> list) {
        return list.stream().collect(Collectors.toMap(redirect -> redirect.trinketsId, redirect -> Pair.of(redirect.accessoriesId, redirect.additionalSlot)));
    }
}
