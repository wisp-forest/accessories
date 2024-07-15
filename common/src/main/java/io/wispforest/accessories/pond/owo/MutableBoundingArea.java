package io.wispforest.accessories.pond.owo;

import io.wispforest.owo.ui.core.Component;

public interface MutableBoundingArea<T extends Component> extends InclusiveBoundingArea<T>/*, ExclusiveBoundingArea<T>, RefinedBoundingArea<T>*/{
    T deepRecursiveChecking(boolean value);

    boolean deepRecursiveChecking();

}
