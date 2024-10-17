package io.wispforest.cclayer.compat.config;

import io.wispforest.owo.config.annotation.Config;
import io.wispforest.owo.config.annotation.Modmenu;

@Modmenu(modId = "cclayer")
@Config(name = "cclayer", wrapperName = "CCLayerConfig")
public class CCLayerConfigModel {
    public boolean useInjectionMethod = true;
}
