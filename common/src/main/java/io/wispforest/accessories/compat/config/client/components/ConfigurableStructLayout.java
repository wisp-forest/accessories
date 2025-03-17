package io.wispforest.accessories.compat.config.client.components;

import io.wispforest.owo.config.Option;
import io.wispforest.owo.config.annotation.RangeConstraint;
import io.wispforest.owo.config.ui.OptionComponents;
import io.wispforest.owo.config.ui.component.ConfigEnumButton;
import io.wispforest.owo.config.ui.component.ConfigSlider;
import io.wispforest.owo.config.ui.component.ConfigTextBox;
import io.wispforest.owo.config.ui.component.SearchAnchorComponent;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Component;
import io.wispforest.owo.ui.core.ParentComponent;
import io.wispforest.owo.ui.core.Positioning;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.parsing.UIModel;
import io.wispforest.owo.util.NumberReflection;
import io.wispforest.owo.util.Observable;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableObject;
import org.intellij.lang.annotations.Identifier;

import java.lang.reflect.AccessFlag;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class ConfigurableStructLayout<T> extends FlowLayout {

    public final Map<Field, ComponentFactory<T, ?>> handlers = new LinkedHashMap<>();

    public boolean sideBySideFormat;

    public final UIModel model;

    public final String configName;
    public final Option.Key optionKey;

    protected ConfigurableStructLayout(Sizing horizontalSizing, Sizing verticalSizing, UIModel uiModel, Option<?> option, boolean sideBySideFormat) {
        super(horizontalSizing, verticalSizing, sideBySideFormat ? Algorithm.HORIZONTAL : Algorithm.VERTICAL);

        this.sideBySideFormat = sideBySideFormat;

        this.model = uiModel;

        this.configName = option.configName();
        this.optionKey = option.key();
    }

    protected ConfigurableStructLayout<T> build(T value) {
        handlers.forEach((field, handler) -> {
            var component = new MutableObject<Component>();

            var name = field.getName();
            var translationKey = "text.config." + configName + ".option." + optionKey.asString() + "." + name;

            component.setValue(handler.createComponent(value, field, t -> ReflectOps.get(field, t), (t, f) -> ReflectOps.set(field, t, f), translationKey, this));

            this.child(component.getValue());
        });

        return this;
    }

    public ConfigurableStructLayout<T> composeComponents(Class<T> clazz, List<Field> validFields, T value) {
        for (var field : validFields) {
            var fieldClazz = field.getType();

            if (NumberReflection.isNumberType(fieldClazz)) {
                this.numberField(field, ReflectOps.get(field, value));
            } else if (fieldClazz == String.class) {
                this.stringField(field, ReflectOps.get(field, value));
            } else if (fieldClazz == ResourceLocation.class) {
                this.identifierField(field, ReflectOps.get(field, value));
            } else if (fieldClazz.isEnum()) {
                this.createEnumButton(field, ReflectOps.get(field, value));
            } else {
                throw new IllegalArgumentException("Unable to handle the given field type found within the struct class! [ParentClass: " + clazz.getSimpleName() + ", FieldName: " + field.getName() + "]");
            }
        }

        return this;
    }

    public static <T> ConfigurableStructLayout<T> of(Class<T> clazz, T value, UIModel uiModel, Option<?> option) {
        var validFields = Arrays.stream(clazz.getFields()).filter(field -> {
            return !Modifier.isStatic(field.getModifiers()) && !Modifier.isTransient(field.getModifiers());
        }).toList();

        var sideBySideFormat = validFields.size() <= 2;

        return new ConfigurableStructLayout<T>(Sizing.expand(), Sizing.content(), uiModel, option, sideBySideFormat)
                .composeComponents(clazz, validFields, value)
                .build(value);
    }

    public <F extends Number> ConfigurableStructLayout<T> numberField(Field field, F defaultValue) {
        if (field.isAnnotationPresent(RangeConstraint.class)) {
            return rangeControlsHandle(field, defaultValue,
                    NumberReflection.isFloatingPointType(field.getType())
                            ? field.getAnnotation(RangeConstraint.class).decimalPlaces()
                            : 0
            );
        }

        return textBoxHandle(field, defaultValue, configTextBox -> configTextBox.configureForNumber((Class<F>) field.getType()));
    }

    public ConfigurableStructLayout<T> stringField(Field field, String defaultValue) {
        return textBoxHandle(field, defaultValue, configTextBox -> {
        });
    }

    public ConfigurableStructLayout<T> identifierField(Field field, Identifier defaultValue) {
        return textBoxHandle(field, defaultValue, configTextBox -> {
            configTextBox.inputPredicate(s -> s.matches("[a-z0-9_.:\\-]*"));
            configTextBox.applyPredicate(s -> ResourceLocation.tryParse(s) != null);
            configTextBox.valueParser(ResourceLocation::parse);
        });
    }

    public <F> ConfigurableStructLayout<T> textBoxHandle(Field field, F defaultValue, Consumer<ConfigTextBox> processor) {
        return textBoxHandle(field, defaultValue, f -> f != null ? Objects.toString(f) : "", processor);
    }

    public <F> ConfigurableStructLayout<T> textBoxHandle(Field field, F defaultValue, Function<F, String> toStringFunc, Consumer<ConfigTextBox> processor) {
        this.handlers.put(field, textBoxFactory(defaultValue, toStringFunc, processor));

        return this;
    }

    public <F extends Number> ConfigurableStructLayout<T> rangeControlsHandle(Field field, F defaultValue, int decimalPlaces) {
        this.handlers.put(field, (ComponentFactory<T, F>) (t, field1, getter, setter, translationKey, parentComponent) -> {
            var name = field1.getName();

            boolean withDecimals = decimalPlaces > 0;

            // ------------
            // Slider setup
            // ------------

            var clazz = (Class<F>) field.getType();

            var value = getter.apply(t);
            var optionComponent = model.expandTemplate(FlowLayout.class,
                    "range-config-option",
                    OptionComponents.packParameters(translationKey, value.toString())
            );

            if (sideBySideFormat) optionComponent.horizontalSizing(Sizing.expand(50));

            var constraint = field.getAnnotation(RangeConstraint.class);
            double min = constraint.min(), max = constraint.max();

            var sliderInput = optionComponent.childById(ConfigSlider.class, "value-slider");

            var fieldId = sliderInput.id() + "-" + name;

            sliderInput.id(fieldId);

            sliderInput.min(min).max(max).decimalPlaces(decimalPlaces).snap(!withDecimals).setFromDiscreteValue(value.doubleValue());
            sliderInput.valueType(clazz);

            var resetButton = optionComponent.childById(ButtonComponent.class, "reset-button");

            resetButton.active = (withDecimals ? value.doubleValue() : Math.round(value.doubleValue())) != defaultValue.doubleValue();
            resetButton.onPress(button -> {
                sliderInput.setFromDiscreteValue(defaultValue.doubleValue());
                button.active = false;
            });

            sliderInput.onChanged().subscribe(newValue -> {
                resetButton.active = (withDecimals ? newValue : Math.round(newValue)) != defaultValue.doubleValue();
            });

            sliderInput.onChanged().subscribe(newValue -> {
                setter.accept(t, (F) parentComponent.childById(ConfigSlider.class, fieldId).parsedValue());
            });

            // ------------------------------------
            // Component handles and text box setup
            // ------------------------------------

            var sliderControls = optionComponent.childById(FlowLayout.class, "slider-controls");
            var textControls = (ParentComponent) textBoxFactory(defaultValue, Objects::toString, configTextBox -> {
                configTextBox.configureForNumber(clazz);

                var predicate = configTextBox.applyPredicate();
                configTextBox.applyPredicate(predicate.and(s -> {
                    final var parsed = Double.parseDouble(s);
                    return parsed >= min && parsed <= max;
                }));
            }).createComponent(t, field, getter, setter, translationKey, parentComponent);

            textControls.childById(FlowLayout.class, "controls-flow").positioning(Positioning.layout());

            var textInput = textControls.childById(ConfigTextBox.class, "value-box-" + name);

            // ------------
            // Toggle setup
            // ------------

            var controlsLayout = optionComponent.childById(FlowLayout.class, "controls-flow");
            var toggleButton = optionComponent.childById(ButtonComponent.class, "toggle-button");

            var textMode = new MutableBoolean(false);
            toggleButton.onPress(button -> {
                textMode.setValue(textMode.isFalse());

                if (textMode.isTrue()) {
                    sliderControls.remove();
                    textInput.text(sliderInput.decimalPlaces() == 0 ? String.valueOf((int) sliderInput.discreteValue()) : String.valueOf(sliderInput.discreteValue()));

                    controlsLayout.child(textControls);
                } else {
                    textControls.remove();
                    sliderInput.setFromDiscreteValue(((Number) textInput.parsedValue()).doubleValue());

                    controlsLayout.child(sliderControls);
                }

                button.tooltip(textMode.isTrue()
                        ? net.minecraft.network.chat.Component.translatable("text.owo.config.button.range.edit_with_slider")
                        : net.minecraft.network.chat.Component.translatable("text.owo.config.button.range.edit_as_text")
                );
            });

            return optionComponent;
        });

        return this;
    }

    private <F> ComponentFactory<T, F> textBoxFactory(F defaultValue, Function<F, String> toStringFunc, Consumer<ConfigTextBox> processor) {
        return (t, field1, getter, setter, translationKey, parentComponent) -> {
            var optionComponent = model.expandTemplate(FlowLayout.class,
                    "text-box-config-option",
                    OptionComponents.packParameters(translationKey, toStringFunc.apply(getter.apply(t)))
            );

            if (sideBySideFormat) optionComponent.horizontalSizing(Sizing.expand(50));

            var valueBox = optionComponent.childById(ConfigTextBox.class, "value-box");
            var resetButton = optionComponent.childById(ButtonComponent.class, "reset-button");

            var fieldId = valueBox.id() + "-" + field1.getName();

            if (sideBySideFormat) valueBox.horizontalSizing(Sizing.fixed(Math.round(valueBox.horizontalSizing().get().value / 1.5f)));

            valueBox.id(fieldId);

            resetButton.active = !valueBox.getValue().equals(toStringFunc.apply(defaultValue));
            resetButton.onPress(button -> {
                valueBox.setValue(toStringFunc.apply(defaultValue));
                button.active = false;
            });

            var onChanged = valueBox.onChanged();

            onChanged.subscribe(s -> resetButton.active = !s.equals(toStringFunc.apply(defaultValue)));
            onChanged.subscribe(s -> {
                setter.accept(t, (F) parentComponent.childById(ConfigTextBox.class, fieldId).parsedValue());
            });

            processor.accept(valueBox);

            optionComponent.child(new SearchAnchorComponent(
                    optionComponent,
                    optionKey.child(field1.getName()),
                    () -> optionComponent.childById(LabelComponent.class, "option-name").text().getString(),
                    valueBox::getValue
            ));

            return optionComponent;
        };
    }

    public <F extends Enum<?>> ConfigurableStructLayout<T> createEnumButton(Field field, F defaultValue) {
        var factory = new ComponentFactory<T, F>() {
            @Override
            public Component createComponent(T t, Field field, Function<T, F> getter, BiConsumer<T, F> setter, String translationKey, ParentComponent parentComponent) {
                var optionComponent = model.expandTemplate(FlowLayout.class,
                        "enum-config-option",
                        OptionComponents.packParameters(translationKey, getter.apply(t).toString())
                );

                if (sideBySideFormat) optionComponent.horizontalSizing(Sizing.expand(50));

                var enumButton = optionComponent.childById(ConfigEnumButton.class, "enum-button");
                var resetButton = optionComponent.childById(ButtonComponent.class, "reset-button");

                if (sideBySideFormat) enumButton.horizontalSizing(Sizing.fixed(Math.round(enumButton.horizontalSizing().get().value / 1.5f)));

                var tempOption = new Option<>(
                        configName,
                        optionKey.child(field.getName()),
                        getter.apply(t),
                        Observable.of(getter.apply(t)),
                        new Option.BoundField<>(t, field),
                        null,
                        Option.SyncMode.NONE,
                        null
                );

                enumButton.init(tempOption, defaultValue.ordinal());

                resetButton.active = true;
                resetButton.onPress(button -> {
                    enumButton.select(defaultValue.ordinal());
                    button.active = false;
                });

                enumButton.onPress(button -> {
                    resetButton.active = enumButton.parsedValue() != defaultValue;

                    setter.accept(t, (F) enumButton.parsedValue());
                });

                optionComponent.child(new SearchAnchorComponent(
                        optionComponent,
                        optionKey.child(field.getName()),
                        () -> optionComponent.childById(LabelComponent.class, "option-name").text().getString(),
                        () -> enumButton.getMessage().getString()
                ));

                return optionComponent;
            }
        };

        this.handlers.put(field, factory);

        return this;
    }

    public interface ComponentFactory<T, F> {
        Component createComponent(T t, Field field, Function<T, F> getter, BiConsumer<T, F> setter, String translation, ParentComponent parentComponent);
    }

    static class ReflectOps {
        static <F> void set(Field field, Object t, F f) {
            try {
                field.set(t, f);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        static <F> F get(Field field, Object t) {
            try {
                return (F) field.get(t);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        static <T> T defaultConstruct(Class<T> clazz) {
            try {
                return (T) clazz.getConstructors()[0].newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
