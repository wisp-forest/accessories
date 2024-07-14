package io.wispforest.accessories.networking;

import io.wispforest.accessories.networking.base.NetworkBuilderRegister;
import io.wispforest.accessories.networking.client.AccessoryBreak;
import io.wispforest.accessories.networking.client.SyncContainerData;
import io.wispforest.accessories.networking.client.SyncData;
import io.wispforest.accessories.networking.client.SyncEntireContainer;
import io.wispforest.accessories.networking.holder.SyncHolderChange;
import io.wispforest.accessories.networking.server.MenuScroll;
import io.wispforest.accessories.networking.server.NukeAccessories;
import io.wispforest.accessories.networking.server.ScreenOpen;
import io.wispforest.accessories.networking.server.SyncCosmeticToggle;

public class AccessoriesPackets {

    public static void register(NetworkBuilderRegister register) {
        register.registerBuilderC2S(ScreenOpen.class, ScreenOpen.ENDEC);
        register.registerBuilderC2S(NukeAccessories.class, NukeAccessories.ENDEC);
        register.registerBuilderC2S(SyncCosmeticToggle.class, SyncCosmeticToggle.ENDEC);

        register.registerBuilderS2C(SyncEntireContainer.class, SyncEntireContainer.ENDEC);
        register.registerBuilderS2C(SyncContainerData.class, SyncContainerData.ENDEC);
        register.registerBuilderS2C(SyncData.class, SyncData.ENDEC);
        register.registerBuilderS2C(AccessoryBreak.class, AccessoryBreak.ENDEC);

        register.registerBuilderBiDi(MenuScroll.class, MenuScroll.ENDEC);
        register.registerBuilderBiDi(SyncHolderChange.class, SyncHolderChange.ENDEC);
    }
}
