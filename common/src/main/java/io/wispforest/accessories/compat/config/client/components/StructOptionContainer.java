package io.wispforest.accessories.compat.config.client.components;

import io.wispforest.endec.Endec;
import io.wispforest.endec.format.edm.EdmDeserializer;
import io.wispforest.endec.format.edm.EdmSerializer;
import io.wispforest.endec.impl.ReflectiveEndecBuilder;
import io.wispforest.owo.config.Option;
import io.wispforest.owo.config.ui.component.OptionValueProvider;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.parsing.UIModel;

import java.util.function.Function;
import java.util.function.Predicate;

public class StructOptionContainer<T> extends ConfigurableStructLayout<T> implements OptionValueProvider {

    private final Option<T> option;

    private final T backingValue;

    private Predicate<T> validation = t -> true;

    protected StructOptionContainer(Sizing horizontalSizing, Sizing verticalSizing, UIModel uiModel, Option<T> option, Function<T, T> cloneFunc, boolean sideBySideFormat) {
        super(horizontalSizing, verticalSizing, uiModel, option, sideBySideFormat);

        this.backingValue = cloneFunc.apply(option.value());

        this.option = option;
    }

    public StructOptionContainer<T> validation(Predicate<T> validation) {
        this.validation = validation;

        return this;
    }

    public StructOptionContainer<T> composeAndBuild() {
        return (StructOptionContainer<T>) this.composeComponents((Class<T>) backingValue.getClass(), backingValue)
                .build(backingValue);
    }

    public static <T> StructOptionContainer of(UIModel uiModel, Option<T> option, ReflectiveEndecBuilder builder, boolean sideBySideFormat) {
        return new StructOptionContainer<>(Sizing.expand(),
                Sizing.content(),
                uiModel,
                option,
                createCopyFunc(builder.get(option.clazz())),
                sideBySideFormat)
                .composeAndBuild();
    }

    private static <T> Function<T, T> createCopyFunc(Endec<T> endec) {
        return t -> endec.decodeFully(EdmDeserializer::of, endec.encodeFully(EdmSerializer::of, t));
    }

    @Override
    public boolean isValid() {
        return validation.test(backingValue);
    }

    @Override
    public Object parsedValue() {
        return backingValue;
    }
}
