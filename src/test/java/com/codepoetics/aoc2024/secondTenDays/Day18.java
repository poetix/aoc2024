package com.codepoetics.aoc2024.secondTenDays;

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

    record ConnectedObstacleGroup(Set<Point> points, boolean meetsLeftEdge, boolean meetsRightEdge, boolean meetsTopEdge) {

        static ConnectedObstacleGroup empty() {
            return new ConnectedObstacleGroup(new HashSet<>(), false, false, false);
        }

        public boolean isConnectedTo(Point point) {
            return Arrays.stream(Direction.values()).anyMatch(d -> points.contains(d.addTo(point)));
        }

        public ConnectedObstacleGroup fuse(ConnectedObstacleGroup other) {
            points.addAll(other.points());
            return new ConnectedObstacleGroup(points,
                    meetsLeftEdge || other.meetsLeftEdge,
                    meetsRightEdge || other.meetsRightEdge,
                    meetsTopEdge || other.meetsTopEdge);
        }

        public boolean isBlockade() {
            return meetsLeftEdge && (meetsTopEdge || meetsRightEdge);
        }

        public ConnectedObstacleGroup add(Point p) {
            points.add(p);
            return new ConnectedObstacleGroup(points,
                    meetsLeftEdge || (p.x() == 0 && p.y() > 0),
                    meetsRightEdge || (p.x() == 70),
                    meetsTopEdge || (p.y() == 0 && p.x() > 0));
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

        return graph.distanceMap(new Point(0, 0)).distances().get(new Point(70, 70));
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
        Set<ConnectedObstacleGroup> connectedGroups = new HashSet<>();

        for (Point point : obstacles) {
            List<ConnectedObstacleGroup> inGroups = new ArrayList<>();

            for (ConnectedObstacleGroup group : connectedGroups) {
                if (group.isConnectedTo(point)) {
                    inGroups.add(group);
                }
            }

            connectedGroups.removeAll(inGroups);
            ConnectedObstacleGroup containingGroup = getContainingGroup(inGroups)
                    .add(point);
            connectedGroups.add(containingGroup);

            if (containingGroup.isBlockade()) {
                return point;
            }
        }
        throw new IllegalStateException("No obstacle blockades the route");
    }

    private static ConnectedObstacleGroup getContainingGroup(List<ConnectedObstacleGroup> inGroups) {
        if (inGroups.isEmpty()) {
            return ConnectedObstacleGroup.empty();
        }

        if (inGroups.size() == 1) {
            return inGroups.getFirst();
        }

        return inGroups.stream().reduce(ConnectedObstacleGroup::fuse).orElseThrow();
    }

}
