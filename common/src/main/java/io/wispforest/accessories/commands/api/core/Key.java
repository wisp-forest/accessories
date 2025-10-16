package io.wispforest.accessories.commands.api.core;

import io.wispforest.accessories.commands.api.base.Argument;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.SequencedCollection;

public final class Key {
    private final List<String> path;

    public Key(String key) {
        this(key.split("/"));
    }

    public Key(String... keyParts) {
        this(List.of(keyParts));
    }

    public Key(SequencedCollection<String> keyParts) {
        this.path = keyParts.stream().filter(string -> !string.isBlank()).toList();
    }

    public List<String> path() {
        return path;
    }

    public Key child(Key key) {
        var parts = new ArrayList<>(path);

        parts.addAll(key.path());

        return new Key(parts);
    }

    public Key child(String keyPart) {
        var parts = new ArrayList<>(path);

        if(!keyPart.isBlank()) parts.add(keyPart);

        return new Key(parts);
    }

    @Nullable
    public Key parent() {
        if (path.size() - 1 <= 0) return null;

        var parts = new ArrayList<>(path);

        parts.removeLast();

        return new Key(parts);
    }

    public boolean isEmpty() {
        return this.path().isEmpty();
    }

    public String topPath() {
        return this.path.getLast();
    }

    public List<Argument<?>> asArgumentList() {
        return (List<Argument<?>>) (Object) this.path().stream()
                .map(Argument::asKeyPath)
                .toList();
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof Key otherKey) && this.path.equals(otherKey.path);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.path);
    }
}
