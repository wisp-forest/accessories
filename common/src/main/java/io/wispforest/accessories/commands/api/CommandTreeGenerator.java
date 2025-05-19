package io.wispforest.accessories.commands.api;

import io.wispforest.accessories.commands.api.base.BaseCommandGenerator;

public interface CommandTreeGenerator<S, B, G extends BaseCommandGenerator<S, G>> {
    void generateTrees(G rootGenerator, B context);
}
