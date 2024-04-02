package io.wispforest.accessories.networking.client.holder;

import io.wispforest.accessories.client.gui.AccessoriesScreen;
import io.wispforest.accessories.networking.AccessoriesPacket;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

public class SyncHolderChange extends AccessoriesPacket {

    private HolderProperty<?> property;
    private Object data;

    public SyncHolderChange() {}

    private SyncHolderChange(HolderProperty<?> property, Object data){
        super(false);

        this.property = property;
        this.data = data;
    }

    public static <T> SyncHolderChange of(HolderProperty<T> property, T data) {
        return new SyncHolderChange(property, data);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(this.property.name());
        this.property.write(buf, this.data);
    }

    @Override
    protected void read(FriendlyByteBuf buf) {
        this.property = HolderProperty.getProperty(buf.readUtf());
        this.data = this.property.reader().apply(buf);
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void handle(Player player) {
        super.handle(player);

        this.property.setData(player, this.data);

        if(Minecraft.getInstance().screen instanceof AccessoriesScreen accessoriesScreen) {
            accessoriesScreen.updateButtons(this.property.name());
        }
    }

}