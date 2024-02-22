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
        if (this.cachedBuf.isPresent()) { // Should only happen when sending the packet from Integrated Server to Client of such Server
            var cachedBufCopy = this.cachedBuf.get();

            buf.writeBytes(cachedBufCopy);

            this.cachedBuf = Optional.empty();

            cachedBufCopy.release();

            return;
        }

        writeUncached(buf);
    }

    protected abstract void writeUncached(FriendlyByteBuf buf);

    @Override
    public void handle(Player player) {
        if(this.cachedBuf.isPresent()){ // TODO: Fix issue with buf not being read on network thread within single player
            var buf = this.cachedBuf.get();

            this.read(buf);

            buf.release();
        }

        super.handle(player);
    }
}
