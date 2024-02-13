package io.wispforest.accessories.networking;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

public abstract class CacheableAccessoriesPacket extends AccessoriesPacket {

    private Optional<FriendlyByteBuf> cachedBuf = Optional.empty();

    public CacheableAccessoriesPacket(){ super(); }

    public CacheableAccessoriesPacket(boolean emptyPacket){
        super(emptyPacket);
    }

    public CacheableAccessoriesPacket(FriendlyByteBuf buf){
        super(false);

        this.cachedBuf = Optional.of(new FriendlyByteBuf(buf.copy()));
    }

    @Override
    public final void write(FriendlyByteBuf buf) {
        if (cachedBuf.isPresent()) { // Should only happen when sending the packet from Integrated Server to Client of such Server
            var cachedBufCopy = cachedBuf.get();

            buf.writeBytes(cachedBufCopy);

            cachedBuf = Optional.empty();

            cachedBufCopy.release();

            return;
        }

        writeUncached(buf);
    }

    protected abstract void writeUncached(FriendlyByteBuf buf);

    @Override
    public void handle(Player player) {
        if(cachedBuf.isPresent()){
            var buf = cachedBuf.get();

            this.read(buf);

            buf.release();
        }

        super.handle(player);
    }
}
