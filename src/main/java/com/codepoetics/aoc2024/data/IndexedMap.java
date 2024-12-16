package com.codepoetics.aoc2024.data;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IndexedMap<K, V> {

    public static <K, V> IndexedMap<K, V> of(Stream<V> values, Function<V, K> index) {
        return new IndexedMap<>(values.collect(
                Collectors.groupingBy(index, Collectors.toSet())), index);
    }

    private final Map<K, Set<V>> inner;
    private final Function<V, K> index;

    public IndexedMap(Map<K, Set<V>> inner, Function<V, K> index) {
        this.inner = inner;
        this.index = index;
    }

    public void add(V value) {
        inner.computeIfAbsent(index.apply(value), ignored -> new HashSet<>()).add(value);
    }

    public boolean remove(V value) {
        var indexOfValue = index.apply(value);
        var container = inner.get(indexOfValue);
        if (container == null) return false;

        boolean result = container.remove(value);
        if (container.isEmpty()) {
            inner.remove(indexOfValue);
        }
        return result;
    }

    public Stream<K> indices() {
        return inner.keySet().stream();
    }

    public Stream<V> get(K index) {
        var container = inner.get(index);
        if (container == null) return Stream.empty();
        return container.stream();
    }
}
