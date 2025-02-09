package io.wispforest.accessories.compat.config;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public final class MenuButtonInjection {
    public ResourceLocation menuType;
    public int xOffset;
    public int yOffset;

    public MenuButtonInjection() {
        this.menuType = ResourceLocation.fromNamespaceAndPath("minecraft", "");
        this.xOffset = 0;
        this.yOffset = 0;
    }

    @Deprecated
    public MenuButtonInjection(ResourceLocation menuType, int xOffset, int yOffset, boolean mini) {
        this(menuType, xOffset, yOffset);
    }

    public MenuButtonInjection(ResourceLocation menuType, int xOffset, int yOffset) {
        this.menuType = menuType;
        this.xOffset = xOffset;
        this.yOffset = yOffset;
    }


    public ResourceLocation menuType() {
        return menuType;
    }

    public int xOffset() {
        return xOffset;
    }

    public int yOffset() {
        return yOffset;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (MenuButtonInjection) obj;
        return Objects.equals(this.menuType, that.menuType) &&
                this.xOffset == that.xOffset &&
                this.yOffset == that.yOffset;
    }

    @Override
    public int hashCode() {
        return Objects.hash(menuType, xOffset, yOffset);
    }

    @Override
    public String toString() {
        return "MenuButtonInjection[" +
                "menuType=" + menuType + ", " +
                "xOffset=" + xOffset + ", " +
                "yOffset=" + yOffset + ']';
    }
}
