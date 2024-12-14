package com.codepoetics.aoc2024.secondTenDays;

import com.codepoetics.aoc2024.Point;
import com.codepoetics.aoc2024.ResourceReader;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Day14 {

    public record Robot (Point position, Point velocity) {
        Point positionAfter(int width, int height, int moves) {
            return new Point(
                    (position.x() + (velocity().x() * moves) + ((long) width * moves)) % width,
                    (position.y() + (velocity().y() * moves) + ((long) height * moves)) % height);
        }
    }

    record BoundingBox(Point topLeft, Point dimensions) {

        public Stream<BoundingBox> quadrisect() {
            var newDimensions = new Point(dimensions.x() / 2, dimensions().y() / 2);
            return Stream.of(
                    new BoundingBox(topLeft, newDimensions),
                    new BoundingBox(topLeft().plus(new Point(newDimensions.x() + 1, 0)), newDimensions),
                    new BoundingBox(topLeft().plus(new Point(0, newDimensions.y() + 1)), newDimensions),
                    new BoundingBox(topLeft().plus(new Point(newDimensions.x() + 1, newDimensions.y() + 1)), newDimensions)
            );
        }

        public double density(SortedMap<Long, SortedMap<Long, Long>> map) {
            return ((double) countIn(map)) / (dimensions().x() * dimensions().y());
        }

        public long countIn(SortedMap<Long, SortedMap<Long, Long>> map) {
            return map.subMap(topLeft.x(), topLeft.x() + dimensions.x())
                    .values().stream()
                    .mapToLong(byY ->
                            byY.subMap(topLeft.y(), topLeft().y() + dimensions.y())
                                    .values().stream().mapToLong(l -> l).sum()
                    ).sum();
        }
    }

    public static class Robots {

        private static final Pattern pattern = Pattern.compile("p=(-?\\d+),(-?\\d+)\\s+v=(-?\\d+),(-?\\d+)");

        public static Robots of(int width, int height, Stream<String> lines) {
            var robots = lines.map(line -> {
                var matcher = pattern.matcher(line);
                if (!matcher.find()) {
                    throw new IllegalArgumentException(line);
                };
                return new Robot(
                        new Point(Integer.parseInt(matcher.group(1)),
                                Integer.parseInt(matcher.group(2))),
                        new Point(Integer.parseInt(matcher.group(3)),
                                Integer.parseInt(matcher.group(4)))
                );
            }).toList();
            return new Robots(width, height, robots);
        }

        private final BoundingBox frame;
        private final int width;
        private final int height;
        private final List<Robot> robots;

        public Robots(int width, int height, List<Robot> robots) {
            this.width = width;
            this.height = height;
            this.frame = new BoundingBox(new Point(0, 0), new Point(width, height));
            this.robots = robots;
        }

        private SortedMap<Long, SortedMap<Long, Long>> countsOfPositionsAfter(int moves) {
            return positionsAfter(moves).collect(
                    Collectors.groupingBy(
                            Point::x,
                            TreeMap::new,
                            Collectors.groupingBy(
                                    Point::y,
                                    TreeMap::new,
                                    Collectors.counting()
                            )
                    ));
        }

        private Stream<Point> positionsAfter(int moves) {
            return robots.stream().map(robot ->
                    robot.positionAfter(width, height, moves)
            );
        }

        public boolean containsClusterAfter(int moves) {
            var counts = countsOfPositionsAfter(moves);
            double avgDensity = frame.density(counts);

            return frame.quadrisect()
                    .flatMap(BoundingBox::quadrisect)
                    .flatMap(BoundingBox::quadrisect)
                    .anyMatch(q -> q.density(counts) > (avgDensity * 10.0));
        }

        public boolean containsLineAfter(int moves) {
            Map<Long, SortedSet<Long>> pointsByRow = positionsAfter(moves).collect(Collectors.groupingBy(Point::x,
                    Collectors.mapping(Point::y,
                            Collectors.toCollection(TreeSet::new))));

            return pointsByRow.values().stream().anyMatch(Robots::containsLine);
        }

        private static boolean containsLine(SortedSet<Long> row) {
            var iter = row.iterator();
            if (!iter.hasNext()) return false;

            var prev = iter.next();
            var count = 0;

            while (iter.hasNext()) {
                var next = iter.next();
                if (next - prev == 1) {
                    count++;
                    if (count > 5) return true;
                } else {
                    count = 0;
                }
                prev = next;
            }

            return false;
        }

        public long scoreByQuadrant(int moves) {
            var positionMap = countsOfPositionsAfter(moves);

            return frame.quadrisect()
                    .mapToLong(q -> q.countIn(positionMap))
                    .reduce(1, (l, r) -> l * r);
        }

        private void printPositions(SortedMap<Long, SortedMap<Long, Long>> positionMap) {
            IntStream.range(0, height).forEach(y -> {
                var s = IntStream.range(0, width).mapToObj(x -> {
                    var byY = positionMap.get((long) x);
                    var c = byY == null ? 0 : byY.getOrDefault((long) y, 0L);
                    return c > 0 ? Long.toString(c) : ".";
                }).collect(Collectors.joining());
                System.out.println(s);
            });
        }

    }

    @Test
    public void part1Example() {
        var robots = Robots.of(11, 7, ResourceReader.of("/day14_example.txt").readLines());

        assertEquals(12, robots.scoreByQuadrant(100));
    }

    @Test
    public void part1() {
        var robots = Robots.of(101, 103, ResourceReader.of("/day14.txt").readLines());

        assertEquals(230436441, robots.scoreByQuadrant(100));
    }

    @Test
    public void part2() {
        var robots = Robots.of(101, 103, ResourceReader.of("/day14.txt").readLines());

        var egg = IntStream.range(0, 10000).filter(robots::containsClusterAfter).findFirst().orElseThrow();

        assertEquals(8270, egg);
        robots.printPositions(robots.countsOfPositionsAfter(egg));
    }


    @Test
    public void movement() {
        Robot robot = new Robot(new Point(2, 4), new Point(2, -3));
        for (var i=0; i<6; i++) {
            System.out.println(robot.positionAfter(11, 7, i));
        }
    }
}
