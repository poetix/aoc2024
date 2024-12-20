package com.codepoetics.aoc2024.secondTenDays;

import com.codepoetics.aoc2024.graph.WeightedGraph;
import com.codepoetics.aoc2024.grid.*;
import com.codepoetics.aoc2024.parsing.ResourceReader;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Day20 {

    static class RaceConditionMap {

        static RaceConditionMap of(Stream<String> lines) {
            AtomicReference<Point> start = new AtomicReference<>();
            AtomicReference<Point> end = new AtomicReference<>();

            Grid<Boolean> grid = SparseGrid.of(lines, (p, c) -> switch(c) {
                case 'S' -> {
                    start.set(p);
                    yield true;
                }
                case 'E' -> {
                    end.set(p);
                    yield true;
                }
                case '.' -> true;
                default -> null;
            });

            WeightedGraph<Point> graph = new WeightedGraph<>();
            grid.populatedPositions().forEach(p ->
                p.adjacents().forEach(a ->
                        graph.add(p, a, 1))
            );

            var distanceMap = graph.distanceMap(end.get());
            return new RaceConditionMap(distanceMap.distances());
        }

        private final Map<Point, Long> distanceMap;

        RaceConditionMap(Map<Point, Long> distanceMap) {
            this.distanceMap = distanceMap;
        }

        public long cheats(int diamondSize) {
            var offsets = diamondOffsets(diamondSize);

            return distanceMap.keySet().stream()
                    .mapToLong(before ->
                        offsets.stream()
                                .map(before::plus)
                                .filter(this::isOnPath)
                                .filter(after -> cheatValue(before, after) >= 100)
                                .count()
                    ).sum();
        }

        private boolean isOnPath(Point p) {
            return distanceMap.getOrDefault(p, Long.MAX_VALUE) != Long.MAX_VALUE;
        }

        private long cheatValue(Point start, Point end) {
            var beforeDistance = distanceMap.get(start);
            var afterDistance = distanceMap.get(end);

            var cost = end.manhattanDistanceFrom(start) - 1;

            return beforeDistance - (afterDistance + cost);
        }

        private List<Point> diamondOffsets(int size) {
            Point origin = new Point(0, 0);
            return IntStream.range(-size, size + 1).boxed().flatMap(x ->
                    IntStream.range(-size, size + 1).boxed().map(y -> new Point(x, y)))
                    .filter(p -> {
                        var distance = p.manhattanDistanceFrom(origin);
                        return 2 <= distance && distance <= size;
                    }).toList();
        }
    }

    @Test
    public void bothParts() {
        var map = RaceConditionMap.of(ResourceReader.of("/day20.txt").readLines());

        assertEquals(1402L, map.cheats(2));
        assertEquals(1020244L, map.cheats(20));
    }
}
