package com.codepoetics.aoc2024;

import java.util.stream.Stream;

public class TwinListParser {

    private final SortedSequence<Integer> left = new SortedSequence<>(Integer::compare);
    private final SortedSequence<Integer> right = new SortedSequence<>(Integer::compare);

    public void parse(Stream<String> lines) {
        lines.forEach(line -> {
            var columns = line.split("\\s+");
            left.add(Integer.parseInt(columns[0]));
            right.add(Integer.parseInt(columns[1]));
        });
    }

    public int sumDifferences() {
        return Streams.zipToInt(left.stream(), right.stream(), (l, r) -> Math.abs(l - r)).sum();
    }

    public int calculateSimilarity() {
        return left.streamCounts().mapToInt(count ->
                count.item() * count.count() * right.getCount(count.item())
        ).sum();
    }
}
