package com.codepoetics.aoc2024.secondTenDays;

import com.codepoetics.aoc2024.data.Lst;
import com.codepoetics.aoc2024.graph.WeightedGraph;
import com.codepoetics.aoc2024.grid.*;
import com.codepoetics.aoc2024.parsing.ResourceReader;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Day16 {

    record Result(long shortestPathLength, long pointsOnShortestPaths) { }

    static class ReindeerMap {

        static ReindeerMap of(Stream<String> lines) {
            AtomicReference<Point> start = new AtomicReference<>();
            AtomicReference<Point> end = new AtomicReference<>();

            Grid<Boolean> paths = SparseGrid.of(lines, (p, c) -> switch(c){
                case '#' -> null;
                case 'S' -> {
                    start.set(p);
                    yield true;
                }
                case 'E' -> {
                    end.set(p);
                    yield true;
                }
                default -> true;
            });

            WeightedGraph<PathStep> graph = new WeightedGraph<>();

            paths.populatedPositions()
                .flatMap(p -> Direction.nsew().map(d -> new PathStep(p, d)))
                .forEach(step -> {
                    if (paths.getOrDefault(step.ahead(), false)) {
                        graph.add(step, step.goForward(), 1);
                    }
                    if (paths.getOrDefault(step.turnLeft().ahead(), false)) {
                        graph.add(step, step.turnLeft(), 1000);
                    }
                    if (paths.getOrDefault(step.turnRight().ahead(), false)) {
                        graph.add(step, step.turnRight(), 1000);
                    }
                });

            return new ReindeerMap(start.get(), end.get(), graph);
        }

        private final Point start;
        private final Point end;
        private final WeightedGraph<PathStep> graph;

        ReindeerMap(Point start, Point end, WeightedGraph<PathStep> graph) {
            this.start = start;
            this.end = end;
            this.graph = graph;
        }

        public Result shortestPaths() {
            var distanceMap = graph.distanceMap(new PathStep(start, Direction.EAST));
            var distances = distanceMap.distances();

            var endpoints = Direction.nsew()
                    .map(d -> new PathStep(end, d))
                    .filter(distances::containsKey)
                    .toList();

            var shortestPathLength = endpoints.stream()
                    .mapToLong(distances::get)
                    .min().orElseThrow();

            var pointsOnShortestPaths = endpoints.stream()
                    .filter(d -> distances.get(d) == shortestPathLength)
                    .flatMap(distanceMap::getPathsTo)
                    .flatMap(Lst::stream)
                    .map(PathStep::position)
                    .distinct()
                    .count();

            return new Result(shortestPathLength, pointsOnShortestPaths);
        }
    }

    @Test
    public void bothParts() {
        var graph = ReindeerMap.of(ResourceReader.of("/day16.txt").readLines());
        var result = graph.shortestPaths();
        assertEquals(85432, result.shortestPathLength());
        assertEquals(465, result.pointsOnShortestPaths());
    }

    @Test
    public void examples() {
        var graph = ReindeerMap.of(ResourceReader.of("/day16_test.txt").readLines());

        var result = graph.shortestPaths();
        assertEquals(7036, result.shortestPathLength());
        assertEquals(45, result.pointsOnShortestPaths());
    }
}
