package io.wispforest.accessories.pond.owo;

import io.wispforest.owo.ui.core.Component;

public interface ComponentExtension<T extends Component> {
    T allowIndividualOverdraw(boolean value);

    boolean allowIndividualOverdraw();

    static void bypassCheck(Component component, Runnable runnable) {
        if(!(component instanceof ComponentExtension<?> extension)) {
            runnable.run();

            return;
        }

        var value = extension.allowIndividualOverdraw();

        extension.allowIndividualOverdraw(false);

        runnable.run();

        extension.allowIndividualOverdraw(value);
    }
}
