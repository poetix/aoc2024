package com.codepoetics.aoc2024.secondTenDays;

import com.codepoetics.aoc2024.graph.WeightedGraph;
import com.codepoetics.aoc2024.grid.ConnectedRegion;
import com.codepoetics.aoc2024.grid.Point;
import com.codepoetics.aoc2024.parsing.ResourceReader;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Day18 {

    record ConnectedObstacleGroup(
            ConnectedRegion points,
            boolean meetsLeftEdge,
            boolean meetsRightEdge,
            boolean meetsTopEdge,
            boolean meetsBottomEdge) {

        static ConnectedObstacleGroup empty() {
            return new ConnectedObstacleGroup(new ConnectedRegion(),
                    false, false, false, false);
        }

        public boolean isConnectedTo(Point point) {
            return points.isConnected(point);
        }

        public ConnectedObstacleGroup fuse(ConnectedObstacleGroup other) {
            points.merge(other.points);
            return new ConnectedObstacleGroup(points,
                    meetsLeftEdge || other.meetsLeftEdge,
                    meetsRightEdge || other.meetsRightEdge,
                    meetsTopEdge || other.meetsTopEdge,
                    meetsBottomEdge || other.meetsBottomEdge);
        }

        public boolean isBlockade() {
            return (meetsLeftEdge && (meetsTopEdge || meetsRightEdge))
                    || (meetsTopEdge && meetsBottomEdge)
                    || (meetsBottomEdge && meetsRightEdge);
        }

        public ConnectedObstacleGroup add(Point p) {
            points.add(p);
            return new ConnectedObstacleGroup(points,
                    meetsLeftEdge || (p.x() == 0 && p.y() > 0),
                    meetsRightEdge || (p.x() == 70),
                    meetsTopEdge || (p.y() == 0 && p.x() > 0),
                    meetsBottomEdge || p.y() == 70 && p.x() < 70);
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
        Set<ConnectedObstacleGroup> connectedGroups = new HashSet<>();

        for (Point point : obstacles) {
            List<ConnectedObstacleGroup> connectedToPoint = connectedGroups.stream()
                    .filter(group -> group.isConnectedTo(point))
                    .toList();

            connectedToPoint.forEach(connectedGroups::remove);
            ConnectedObstacleGroup containingGroup = connectedToPoint.stream()
                    .reduce(ConnectedObstacleGroup::fuse)
                    .orElseGet(ConnectedObstacleGroup::empty)
                    .add(point);
            connectedGroups.add(containingGroup);

            if (containingGroup.isBlockade()) {
                return point;
            }
        }

        throw new IllegalStateException("No obstacle blockades the route");
    }

}
