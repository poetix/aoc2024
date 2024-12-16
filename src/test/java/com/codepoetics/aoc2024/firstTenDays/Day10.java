package com.codepoetics.aoc2024.firstTenDays;

import com.codepoetics.aoc2024.grid.DenseGrid;
import com.codepoetics.aoc2024.grid.Direction;
import com.codepoetics.aoc2024.grid.Grid;
import com.codepoetics.aoc2024.grid.Point;
import com.codepoetics.aoc2024.parsing.ResourceReader;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Day10 {

    static class TrailFinder {

        record Trail(Point head, Point last, Trail tail) {
            static Trail of(Point head) {
                return new Trail(head, head, null);
            }

            public Trail cons(Point head) {
                return new Trail(head, last, this);
            }
        }

        public static TrailFinder forLines(Stream<String> lines) {
            List<Point> starts = new ArrayList<>();

            Grid<Integer> map = DenseGrid.of(lines,
                    (p, c) -> {
                        var value = Integer.parseInt(Character.toString(c));
                        if (value == 0) starts.add(p);
                        return value;
                    });

            return new TrailFinder(map, starts);
        }

        private final Grid<Integer> grid;
        private final List<Point> starts;

        TrailFinder(Grid<Integer> grid, List<Point> starts) {
            this.grid = grid;
            this.starts = starts;
        }

        private int valueAt(Point point) {
            return grid.getOrDefault(point, -1);
        }

        public Stream<Trail> trailsFrom(Point start) {
            int value = valueAt(start);
            if (value == 9) return Stream.of(Trail.of(start));

            var nextValue = value + 1;
            return start.adjacents()
                    .filter(adjacentPoint -> valueAt(adjacentPoint) == nextValue)
                    .flatMap(this::trailsFrom)
                    .map(trail -> trail.cons(start));
        }

        public long sumScores() {
            return starts.stream().mapToLong(start ->
                trailsFrom(start)
                        .map(Trail::last)
                        .distinct()
                        .count())
                .sum();
        }

        public long sumRatings() {
            return starts.stream().mapToLong(start ->
                trailsFrom(start)
                        .distinct()
                        .count())
            .sum();
        }

        private static Stream<Point> getAdjacent(Point p) {
            return Stream.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST)
                    .map(d -> d.addTo(p));
        }
    }

    @Test
    public void withTestData() {
        var finder = TrailFinder.forLines(ResourceReader.of("/day10_test.txt").readLines());

        assertEquals(36, finder.sumScores());
        assertEquals(81, finder.sumRatings());
    }

    @Test
    public void withRealData() {
        var finder = TrailFinder.forLines(ResourceReader.of("/day10.txt").readLines());

        assertEquals(593, finder.sumScores());
        assertEquals(1192, finder.sumRatings());
    }

}
