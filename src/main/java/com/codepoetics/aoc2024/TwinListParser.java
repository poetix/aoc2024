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
        var leftIter = left.stream().iterator();
        var rightIter = right.stream().iterator();
        var total = 0;

        while (leftIter.hasNext()) {
            total += Math.abs((leftIter.next() - rightIter.next()));
        }

        return total;
    }

    public int calculateSimilarity() {
        return left.streamCounts().mapToInt(entry ->
                entry.getValue() * entry.getKey() * right.getCount(entry.getKey())
        ).sum();
    }
}
