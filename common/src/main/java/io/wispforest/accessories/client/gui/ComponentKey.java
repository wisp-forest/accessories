package io.wispforest.accessories.client.gui;

import io.wispforest.owo.ui.core.ParentUIComponent;
import io.wispforest.owo.ui.core.UIComponent;
import org.jetbrains.annotations.Nullable;

public abstract class ComponentKey<T extends UIComponent> {

    static <T extends UIComponent> ComponentKey<T> of(Class<T> clazz, String id) {
        return new ComponentKey<T>() {
            @Override
            public Class<T> clazz() {
                return clazz;
            }

            @Override
            public String id() {
                return id;
            }
        };
    }

    public abstract Class<T> clazz();

    public abstract String id();

    public final boolean removeFromRoot(ParentUIComponent parent) {
        var component = getFrom(parent);
        return component != null && component.parent() != null && removeFrom(component.parent());
    }

    public final boolean removeFrom(ParentUIComponent parent) {
        var component = getFrom(parent);
        if (component != null) parent.removeChild(component);
        return component != null;
    }

    @SafeVarargs
    public final <P extends ParentUIComponent> boolean removeFrom(ParentUIComponent parent, ComponentKey<P>... keys) {
        for (var key : keys) {
            if (parent == null) return false;
            parent = key.getFrom(parent);
        }

        return removeFrom(parent);
    }

    public final @Nullable T getFrom(ParentUIComponent parent) {
        return parent.childById(clazz(), id());
    }

    public final boolean has(ParentUIComponent parent) {
        return getFrom(parent) != null;
    }

    public final <C extends UIComponent> C withId(C c) {
        return (C) c.id(id());
    }
}
