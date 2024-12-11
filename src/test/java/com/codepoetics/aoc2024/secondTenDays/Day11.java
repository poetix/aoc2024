package com.codepoetics.aoc2024.secondTenDays;

import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Stream;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class Day11 {

    static class MemoisingStoneAutomaton {

        private final Map<Long, long[]> countsByStoneAndTimes = new HashMap<>();

        public long countAfter(Stream<Long> stones, int times) {
            return stones.mapToLong(
                    stone -> countAfter(stone, times)
            ).sum();
        }

        private long countAfter(long stone, int times) {
            if (times == 0) return 1;

            long[] memoised = countsByStoneAndTimes.computeIfAbsent(stone, (ignored) -> new long[75]);
            var nextIndex = times - 1;
            var result = memoised[nextIndex];
            if (result > 0) return result;

            result = countAfterUncached(stone, times);
            memoised[nextIndex] = result;
            return result;
        }

        private long countAfterUncached(long stone, int times) {
            if (stone == 0) {
                return countAfter(1L, times - 1);
            }

            String repr = Long.toString(stone);
            if (repr.length() % 2 == 0) {
                String l = repr.substring(0, repr.length() / 2);
                String r = repr.substring(repr.length() / 2);
                return countAfter(Long.parseLong(l), times - 1) +
                            countAfter(Long.parseLong(r), times - 1);
            }

            return countAfter(stone * 2024, times - 1);
        }
    }

    static class AggregatingStoneAutomaton {

        private Map<Long, Long> nextGeneration;
        private long total;

        public long countAfter(Stream<Long> stones, int times) {
            Map<Long, Long> stonesByDenomination = stones.collect(
                    groupingBy(
                            l -> l,
                            counting()
                    )
            );

            for (int i=0; i<times; i++) {
                nextGeneration = new HashMap<>();
                total = 0;
                stonesByDenomination.forEach(this::applyRule);
                stonesByDenomination = nextGeneration;
            }

            return total;
        }

        private void increment(long denominator, long count) {
            total += count;
            nextGeneration.compute(denominator, (ignored, v) ->
                    v == null ? count : v + count);
        }

        private void applyRule(long stone, long count) {
            if (stone == 0) {
                increment(1, count);
                return;
            }

            String repr = Long.toString(stone);
            if (repr.length() % 2 == 0) {
                String l = repr.substring(0, repr.length() / 2);
                String r = repr.substring(repr.length() / 2);
                increment(Long.parseLong(l), count);
                increment(Long.parseLong(r), count);
                return;
            }

            increment(stone * 2024, count);
        }
    }

    @Test
    public void part1_test() {
        var automaton = new AggregatingStoneAutomaton();

        assertEquals(55312, automaton.countAfter(Stream.of(125L, 17L), 25));
    }

    @Test
    public void part1() {

        var automaton = new AggregatingStoneAutomaton();

        assertEquals(194482, automaton.countAfter(
                Stream.of(0L, 27L, 5409930L, 828979L, 4471L, 3L, 68524L, 170L),
                25));
    }

    @Test
    public void part1Memoising() {

        var automaton = new MemoisingStoneAutomaton();

        assertEquals(194482, automaton.countAfter(
                Stream.of(0L, 27L, 5409930L, 828979L, 4471L, 3L, 68524L, 170L),
                25));
    }

    @Test
    public void part2() {
        var automaton = new AggregatingStoneAutomaton();

        assertEquals(232454623677743L, automaton.countAfter(
                Stream.of(0L, 27L, 5409930L, 828979L, 4471L, 3L, 68524L, 170L),
                75));
    }

    @Test
    public void part2Memoising() {
        var automaton = new MemoisingStoneAutomaton();

        assertEquals(232454623677743L, automaton.countAfter(
                Stream.of(0L, 27L, 5409930L, 828979L, 4471L, 3L, 68524L, 170L),
                75));
    }
}
