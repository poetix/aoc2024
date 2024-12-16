package com.codepoetics.aoc2024.data;

import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class SortedSequence<T> {

    public record Count<T>(T item, int count) { }

    private final Map<T, Integer> counts;

    public SortedSequence(Comparator<T> comparator) {
        counts = new TreeMap<>(comparator);
    }

    public void add(T item) {
        counts.compute(item, (ignored, count) -> count == null ? 1 : count + 1);
    }

    public Stream<T> stream() {
        return counts.entrySet().stream().flatMap(entry ->
                IntStream.range(0, entry.getValue())
                        .mapToObj(ignored -> entry.getKey())
        );
    }

    public int getCount(T index) {
        return counts.getOrDefault(index, 0);
    }

    public Stream<Count<T>> streamCounts() {
        return counts.entrySet().stream().map(e -> new Count<>(e.getKey(), e.getValue()));
    }

}
