package io.wispforest.testccessories.fabric.client;

import io.wispforest.testccessories.fabric.accessories.AppleAccessory;
import io.wispforest.testccessories.fabric.accessories.PotatoAccessory;
import net.fabricmc.api.ClientModInitializer;

public class TestccessoriesClientFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        AppleAccessory.clientInit();
        PotatoAccessory.clientInit();
    }
}