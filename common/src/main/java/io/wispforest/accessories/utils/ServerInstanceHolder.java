package io.wispforest.accessories.utils;

import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Supplier;

public class ServerInstanceHolder {

    private static Supplier<@Nullable MinecraftServer> instance = () -> null;

    public static MinecraftServer getInstance() {
        var server = instance.get();

        Objects.requireNonNull(server, "Unable to get current MinecraftServer instance as it has not been set yet!");

        return server;
    }

    public static void setInstance(@Nullable MinecraftServer server) {
        setInstance(() -> server);
    }

    public static void setInstance(Supplier<MinecraftServer> server) {
        instance = server;
    }
}
