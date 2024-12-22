package com.codepoetics.aoc2024;

import com.codepoetics.aoc2024.parsing.ResourceReader;
import com.codepoetics.aoc2024.streams.Streams;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Day22 {

    private LongStream randomsFromSeed(long seed) {
        return LongStream.iterate(seed, this::next);
    }

    private long next(long current) {
        return lshiftMixAndPrune(11, rshiftMixAndPrune(5, lshiftMixAndPrune(6, current)));
    }

    private long lshiftMixAndPrune(int lshift, long current) {
        var multiplied = current << lshift;
        current = (multiplied ^ current) % 16777216;
        return current;
    }

    private long rshiftMixAndPrune(int rshift, long current) {
        var multiplied = current >> rshift;
        current = (multiplied ^ current) % 16777216;
        return current;
    }

    private void observeBestPrices(long seed, Map<String, Long> scores) {
        Set<String> observed = new HashSet<>();

        Streams.windowed(5, randomsFromSeed(seed).limit(2000).map(l -> l % 10))
                .forEach(window -> {
                    var asArray = window.toArray();
                    var priceAfterPattern = asArray[4];
                    var pattern = Streams.deltas(Arrays.stream(asArray))
                            .mapToObj(Long::toString)
                            .collect(Collectors.joining(","));

                    if (observed.add(pattern)) {
                        scores.compute(pattern, (ignored, totalScore) ->
                                totalScore == null ? priceAfterPattern : totalScore + priceAfterPattern);
                    }
                });
    }

    @Test
    public void example() {
        assertEquals(5908254, randomsFromSeed(123).skip(10).findFirst().orElseThrow());
    }

    @Test
    public void part1() {
        assertEquals(20441185092L, ResourceReader.of("/day22.txt").readLines().mapToInt(Integer::parseInt)
                .mapToLong(seed -> randomsFromSeed(seed).skip(2000).findFirst().orElseThrow())
                .sum());
    }

    @Test
    public void part2() {
        Map<String, Long> scores = new HashMap<>();

        ResourceReader.of("/day22.txt").readLines().mapToInt(Integer::parseInt)
                .forEach(seed -> observeBestPrices(seed, scores));

        var highestScoring = scores.values().stream().mapToLong(i -> i).max().orElseThrow();
        assertEquals(2268, highestScoring);
    }
}
