package io.wispforest.accessories.compat.config;

public enum ScreenType {
    NONE(-1),
    ORIGINAL(1),
    EXPERIMENTAL_V1(2);

    private final int screenIndex;

    ScreenType(int screenIndex) {
        this.screenIndex = screenIndex;
    }

    public boolean isValid() {
        return this.screenIndex >= 1;
    }
}
