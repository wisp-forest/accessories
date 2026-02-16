package io.wispforest.accessories.api.client.tooltip;

import io.wispforest.accessories.api.tooltip.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import org.jetbrains.annotations.ApiStatus;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

@ApiStatus.Internal
public class TooltipComponentBuilderImpl implements TooltipComponentBuilder {

    private final List<ComponentStreamSupplier> components = new ArrayList<>();

    private final List<ClientTooltipComponent> builtComponents = new ArrayList<>();
    private TextWrapper wrapper = TextWrapper.NONE;
    private boolean hasBeenBuilt = false;

    private final Font font = Minecraft.getInstance().font;
    private final Language lang = Language.getInstance();

    public TooltipComponentBuilderImpl() {}

    private void resetCacheState() {
        if (!hasBeenBuilt) return;

        hasBeenBuilt = false;
        builtComponents.clear();
    }

    public void clear() {
        resetCacheState();
        components.clear();
    }

    @Override
    public TooltipComponentBuilder divider() {
        return divider((int) Math.floor(font.lineHeight / 3f));
    }

    @Override
    public TooltipComponentBuilder divider(int height) {
        components.add((wrapper) -> Stream.of(spacer(height)));

        resetCacheState();

        return this;
    }

    @Override
    public TooltipComponentBuilder add(FormattedText text) {
        components.add((wrapper) -> wrapper.wrap(text).map(this::toComponent));

        resetCacheState();

        return this;
    }

    @Override
    public TooltipComponentBuilder add(TooltipComponentBuilder builder) {
        if (!(builder instanceof TooltipComponentBuilderImpl impl)) {
            throw new IllegalStateException("Unable to handle custom TooltipComponentBuilder type: " + builder.getClass().getName());
        }

        components.add(wrapper -> impl.build(wrapper).stream());

        return this;
    }

    @Override
    public TooltipComponentBuilder addAll(Collection<? extends FormattedText> text) {
        components.add((wrapper) -> text.stream().flatMap(wrapper::wrap).map(this::toComponent));

        resetCacheState();

        return this;
    }

    @Override
    public TooltipComponentBuilder addAll(TextPrefixer prefixer, FormattedTextBuilder builder) {
        components.add((wrapper) -> builder.build(prefixer, wrapper).map(this::toComponent));

        resetCacheState();

        return this;
    }

    public TooltipComponentBuilder add(TooltipComponentHolder holder) {
        if (!(holder instanceof ClientTooltipComponentHolder clientHolder)) {
            throw new IllegalStateException("Any class that extends directly from TooltipComponentHolder must be ClientTooltipComponent on the Client Side!");
        }

        components.add((wrapper) -> clientHolder.components().stream());

        resetCacheState();

        return this;
    }

    @Override
    public boolean isEmpty() {
        return components.isEmpty();
    }

    public List<ClientTooltipComponent> build() {
        return build(TextWrapper.NONE);
    }

    public List<ClientTooltipComponent> build(TextWrapper wrapper) {
        if (!hasBeenBuilt || !Objects.equals(this.wrapper, wrapper)) {
            builtComponents.addAll(components.stream().flatMap(supplier -> supplier.get(wrapper)).toList());
            hasBeenBuilt = true;
            this.wrapper = wrapper;
        }

        return (!builtComponents.isEmpty())
            ? Collections.unmodifiableList(builtComponents)
            : List.of();
    }

    private ClientTooltipComponent toComponent(FormattedText text) {
        return ClientTooltipComponent.create(lang.getVisualOrder(text));
    }

    private static ClientTooltipComponent spacer(int height) {
        return new ClientTooltipComponent() {
            @Override public int getHeight(Font font) { return height;}
            @Override public int getWidth(Font font) { return 1; }
        };
    }

    private interface ComponentStreamSupplier {
        Stream<ClientTooltipComponent> get(TextWrapper wrapper);
    }
}
