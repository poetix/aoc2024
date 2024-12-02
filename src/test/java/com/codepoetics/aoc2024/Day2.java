package com.codepoetics.aoc2024;

import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.IntStream;

public class Day2 {

    record Report(int[] levels) {

        private static boolean deltasAreSafe(PrimitiveIterator.OfInt deltas) {
            var sgn = 0;

            while (deltas.hasNext()) {
                var delta = deltas.next();
                var absDelta = Math.abs(delta);

                if (absDelta < 1 || absDelta > 3) return false;

                var newSgn = Integer.compare(0, delta);
                if (sgn != 0 && newSgn != sgn) return false;
                sgn = newSgn;
            }

            return true;
        }

        private static PrimitiveIterator.OfInt deltas(PrimitiveIterator.OfInt levels) {
            if (!levels.hasNext()) return IntStream.empty().iterator();

            return new PrimitiveIterator.OfInt() {
                private int previous = levels.next();

                @Override
                public boolean hasNext() {
                    return levels.hasNext();
                }

                @Override
                public int nextInt() {
                    var current = levels.nextInt();
                    var delta = current - previous;
                    previous = current;
                    return delta;
                }
            };
        }

        public boolean isSafe() {
            return deltasAreSafe(deltas(Arrays.stream(levels).iterator()));
        }

        public boolean isSafeWithDampening() {
            return isSafe() ||
                    IntStream.range(0, levels.length)
                            .anyMatch(dropped ->
                                    deltasAreSafe(
                                        deltas(IntStream.range(0, levels.length)
                                            .filter(i -> i != dropped)
                                            .map(i -> levels[i])
                                            .iterator())));
        }
    }

    private final List<Report> reports = ResourceReader.of("/day2.txt")
            .readLines()
            .map(line -> new Report(
                    Arrays.stream(line.split("\\s+"))
                        .mapToInt(Integer::parseInt)
                        .toArray())
            )
            .toList();

    @Test
    public void findSafeReports() {
        System.out.printf("Safe reports without dampening: %d%n",
                reports.stream().filter(Report::isSafe).count());

        System.out.printf("Safe reports with dampening: %d%n",
                reports.stream().filter(Report::isSafeWithDampening).count());
    }
}
