package io.wispforest.accessories.client.gui;

import io.wispforest.owo.ui.core.ParentComponent;
import org.jetbrains.annotations.Nullable;

public abstract class ComponentKey<T extends io.wispforest.owo.ui.core.Component> {

    static <T extends io.wispforest.owo.ui.core.Component> ComponentKey<T> of(Class<T> clazz, String id) {
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

    public final boolean removeFromRoot(ParentComponent parent) {
        var component = getFrom(parent);
        return component != null && component.parent() != null && removeFrom(component.parent());
    }

    public final boolean removeFrom(ParentComponent parent) {
        var component = getFrom(parent);
        if (component != null) parent.removeChild(component);
        return component != null;
    }

    @SafeVarargs
    public final <P extends ParentComponent> boolean removeFrom(ParentComponent parent, ComponentKey<P>... keys) {
        for (var key : keys) {
            if (parent == null) return false;
            parent = key.getFrom(parent);
        }

        return removeFrom(parent);
    }

    public final @Nullable T getFrom(ParentComponent parent) {
        return parent.childById(clazz(), id());
    }

    public final boolean has(ParentComponent parent) {
        return getFrom(parent) != null;
    }

    public final <C extends io.wispforest.owo.ui.core.Component> C withId(C c) {
        return (C) c.id(id());
    }
}
