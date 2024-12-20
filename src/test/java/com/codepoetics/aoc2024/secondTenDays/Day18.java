package com.codepoetics.aoc2024.secondTenDays;

import com.codepoetics.aoc2024.data.DisjointSet;
import com.codepoetics.aoc2024.graph.WeightedGraph;
import com.codepoetics.aoc2024.grid.Direction;
import com.codepoetics.aoc2024.grid.Point;
import com.codepoetics.aoc2024.parsing.ResourceReader;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Day18 {

    static final class ConnectedObstacleGroup {

        private final DisjointSet<Point> points;
        private final Point leftAnchor;
        private final Point rightAnchor;
        private final Point topAnchor;
        private final Point bottomAnchor;

        ConnectedObstacleGroup() {
            points = new DisjointSet<>();

            leftAnchor = new Point(-1, 0);
            rightAnchor = new Point(71, 0);
            topAnchor = new Point(0, -1);
            bottomAnchor = new Point(0, 71);

            points.addAll(leftAnchor, rightAnchor, topAnchor, bottomAnchor);
        }

        public boolean isBlockadeAfterAdding(Point point) {
            points.add(point);

            if (point.x() == 0) points.connect(leftAnchor, point);
            if (point.x() == 70) points.connect(rightAnchor, point);
            if (point.y() == 0) points.connect(topAnchor, point);
            if (point.y() == 70) points.connect(bottomAnchor, point);

            Arrays.stream(Direction.values()).map(d -> d.addTo(point)).forEach(adjacent -> {
                if (points.contains(adjacent)) {
                    points.connect(point, adjacent);
                }
            });

            return points.isConnected(leftAnchor, topAnchor)
                    || points.isConnected(leftAnchor, rightAnchor)
                    || points.isConnected(topAnchor, bottomAnchor)
                    || points.isConnected(rightAnchor, bottomAnchor);
        }
    }

    @Test
    public void bothParts() {
        var obstacles = ResourceReader.of("/day18.txt").readLines()
                .map(line -> {
                    var xy = line.split(",");
                    return new Point(Integer.parseInt(xy[0]), Integer.parseInt(xy[1]));
                }).toList();

        assertEquals(318, calculateShortestPathWith(obstacles.stream().limit(1024).collect(Collectors.toSet())));
        assertEquals(new Point(56, 29), findFirstBlockadingObstacle(obstacles));
    }

    private static long calculateShortestPathWith(Set<Point> obstacles) {
        var distances = getDistances(obstacles);
        return distances.get(new Point(70, 70));
    }

    private static Map<Point, Long> getDistances(Set<Point> obstacles) {
        WeightedGraph<Point> graph = new WeightedGraph<>();
        Predicate<Point> isUnobstructed = p -> !obstacles.contains(p);

        IntStream.range(0, 71).boxed().flatMap(x ->
                IntStream.range(0, 71)
                        .mapToObj(y -> new Point(x, y))
                        .filter(isUnobstructed)
        ).forEach(unobstructed ->
                unobstructed.adjacents()
                        .filter(isUnobstructed)
                        .forEach(adjacent -> graph.add(unobstructed, adjacent, 1))
        );

        var distances = graph.distanceMap(new Point(0, 0)).distances();
        return distances;
    }

    @Test
    public void part2Slow() {
        Iterator<Point> obstaclesIter = ResourceReader.of("/day18.txt").readLines()
                .map(line -> {
                    var xy = line.split(",");
                    return new Point(Integer.parseInt(xy[0]), Integer.parseInt(xy[1]));
                })
                .iterator();

        assertEquals(new Point(56, 29), findFirstBlockadingObstacleSlow(obstaclesIter));
    }

    private static Point findFirstBlockadingObstacleSlow(Iterator<Point> obstaclesIter) {
        Set<Point> obstaclesSoFar = new HashSet<>();

        while (obstaclesIter.hasNext()) {
            var obstacle = obstaclesIter.next();
            obstaclesSoFar.add(obstacle);
            if (calculateShortestPathWith(obstaclesSoFar) == Long.MAX_VALUE) return obstacle;
        }

        throw new IllegalStateException("No blockading obstacle found");
    }

    private static Point findFirstBlockadingObstacle(Collection<Point> obstacles) {
        ConnectedObstacleGroup connectedObstacles = new ConnectedObstacleGroup();
        return obstacles.stream()
                .filter(connectedObstacles::isBlockadeAfterAdding)
                .findFirst()
                .orElseThrow();
    }

}
