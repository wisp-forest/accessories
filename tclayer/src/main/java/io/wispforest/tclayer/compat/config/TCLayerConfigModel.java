package io.wispforest.tclayer.compat.config;

import io.wispforest.owo.config.Option;
import io.wispforest.owo.config.annotation.Config;
import io.wispforest.owo.config.annotation.Sync;

import java.util.ArrayList;
import java.util.List;

@Config(name = "tclayer", wrapperName = "TCLayerConfig")
public class TCLayerConfigModel {

    @Sync(Option.SyncMode.OVERRIDE_CLIENT)
    public List<SlotIdRedirect> slotIdRedirects = new ArrayList<>(List.of(new SlotIdRedirect("charm/spell_book", "spellbook", 1)));

    public boolean useInjectionMethod = true;
}
