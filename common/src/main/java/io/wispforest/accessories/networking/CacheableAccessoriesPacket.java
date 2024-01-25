package io.wispforest.accessories.networking;

import net.minecraft.network.FriendlyByteBuf;

import java.util.Optional;

public abstract class CacheableAccessoriesPacket extends AccessoriesPacket {

    private Optional<FriendlyByteBuf> cachedBuf = Optional.empty();

    public CacheableAccessoriesPacket(){ super(); }

    public CacheableAccessoriesPacket(FriendlyByteBuf buf){
        this.cachedBuf = Optional.of(new FriendlyByteBuf(buf.slice()));
    }

    @Override
    public final void write(FriendlyByteBuf buf) {
        if (cachedBuf.isPresent()) {
            var cachedBufCopy = cachedBuf.get();

            buf.writeBytes(cachedBufCopy);

            cachedBuf = Optional.empty();

            return;
        }

        writeUncached(buf);
    }

    protected abstract void writeUncached(FriendlyByteBuf buf);
}
