package com.codepoetics.aoc2024.secondTenDays;

import com.codepoetics.aoc2024.parsing.ResourceReader;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Day19 {

    static class TowelSet {

        public static TowelSet of(Stream<String> lines) {
            var iter = lines.iterator();
            List<String> availablePatterns = Arrays.stream(iter.next().split(",\\s+"))
                    .sorted(Comparator.comparing(String::length))
                    .toList();

            iter.next();
            List<String> requiredPatterns = new ArrayList<>();
            while (iter.hasNext()) {
                requiredPatterns.add(iter.next());
            }
            return new TowelSet(availablePatterns, requiredPatterns);
        }

        private final List<String> availablePatterns;
        private final List<String> requiredPatterns;
        private final Map<String, Long> knownPossible = new HashMap<>();
        private final long[] counts;

        TowelSet(List<String> availablePatterns, List<String> requiredPatterns) {
            this.availablePatterns = availablePatterns;
            this.requiredPatterns = requiredPatterns;
            counts = new long[requiredPatterns.stream().mapToInt(String::length).max().orElse(0)];
        }

        public long countPossible() {
            return requiredPatterns.stream()
                    .filter(s -> countPossible(s) > 0L)
                    .count();
        }

        public long countPossibleDynamic() {
            return requiredPatterns.stream()
                    .filter(s -> countPossibleDynamic(s) > 0L)
                    .count();
        }

        public long totalPossible() {
            return requiredPatterns.stream()
                    .mapToLong(this::countPossible)
                    .sum();
        }

        public long totalPossibleDynamic() {
            return requiredPatterns.stream()
                    .mapToLong(this::countPossibleDynamic)
                    .sum();
        }

        private long countPossibleDynamic(String checking) {
            for (int i = checking.length() - 1; i >= 0; i--) {
                long count = 0;
                long maxLength = checking.length() - i;
                for (String subPattern: availablePatterns) {
                    if (checking.startsWith(subPattern, i)) {
                        count += subPattern.length() == maxLength
                                ? 1
                                : counts[i + subPattern.length()];
                    }
                }
                counts[i] = count;
            }
            return counts[0];
        }

        private long countPossible(String checking) {
            var knownCount = knownPossible.get(checking);
            if (knownCount != null) return knownCount;

            long possibleCount = 0L;
            for (String subPattern : availablePatterns) {
                if (subPattern.length() > checking.length()) break;

                if (checking.startsWith(subPattern)) {
                    if (subPattern.length() == checking.length()) {
                        possibleCount += 1;
                        break;
                    }

                    var remainder = checking.substring(subPattern.length());
                    possibleCount += countPossible(remainder);
                }
            }

            knownPossible.put(checking, possibleCount);
            return possibleCount;
        }
    }

    @Test
    public void test() {
        var towelPatterns = TowelSet.of(Stream.of(
                "r, wr, b, g, bwu, rb, gb, br",
                "",
                "brwrr",
                "bggr",
                "gbbr",
                "rrbgbr",
                "ubwu",
                "bwurrg",
                "brgr",
                "bbrgwb"
            )
        );
        var possible = towelPatterns.countPossible();
        assertEquals(6, possible);
    }

    @Test
    public void part2() {
        var set = TowelSet.of(ResourceReader.of("/day19.txt").readLines());

        assertEquals(615388132411142L,
                    set.totalPossibleDynamic());
    }

    @Test
    public void part() {
        assertEquals(283,
                TowelSet.of(ResourceReader.of("/day19.txt").readLines()).countPossible());
    }
}
