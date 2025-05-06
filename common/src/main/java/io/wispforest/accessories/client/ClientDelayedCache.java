package io.wispforest.accessories.client;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.minecraft.client.Minecraft;

import java.time.Duration;

public class ClientDelayedCache<K> {

    private final Cache<K, Float> cache;

    public ClientDelayedCache() {
        cache = CacheBuilder.newBuilder()
                .expireAfterAccess(Duration.ofSeconds(30))
                .build();
    }

    public ClientDelayedCache(int maxAmount) {
        cache = CacheBuilder.newBuilder()
                .expireAfterAccess(Duration.ofSeconds(30))
                .maximumSize(maxAmount)
                .build();
    }

    public boolean hasAllottedTime(K key, float totalAmountSecs) {
        var prevAmount = cache.getIfPresent(key);

        // This makes sure the first time a given key was not present that its then run once
        // Designed to prevent incorrect hash and equals logic from eating any possible events
        if (prevAmount == null) {
            prevAmount = 0f;

            cache.put(key, prevAmount);

            return true;
        }

        var currentAmount = prevAmount + Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaTicks();

        var bl = (currentAmount >= totalAmountSecs * 20);

        if (bl) currentAmount = 0;

        cache.put(key, currentAmount);

        return bl;
    }

    public void runIfTimeHasAllotted(K key, float totalAmountSecs, Runnable runnable) {
        if(!hasAllottedTime(key, totalAmountSecs)) return;

        runnable.run();
    }
}
