package com.codepoetics.aoc2024.secondTenDays;

import com.codepoetics.aoc2024.grid.DenseGrid;
import com.codepoetics.aoc2024.grid.Direction;
import com.codepoetics.aoc2024.grid.Grid;
import com.codepoetics.aoc2024.grid.Point;
import com.codepoetics.aoc2024.parsing.ResourceReader;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.stream.Collectors.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class Day12 {

    static class Garden {

        record Region(Set<Point> points, long perimeterLength, Set<Point> perimeterPoints) {
            int area() {
                return points.size();
            }

            long cost() {
                return area() * perimeterLength;
            }

            long bulkCost() {
                return area() * perimeterSides();
            }

            long perimeterSides() {
                return Stream.of(Direction.WEST, Direction.EAST)
                        .mapToLong(this::perimeterSidesForDirection)
                        .sum() * 2;
            }

            private long perimeterSidesForDirection(Direction d) {
                Map<Long, SortedSet<Long>> byDirection = perimeterPoints.stream()
                        .filter(p -> points.contains(d.addTo(p)))
                        .collect(groupingBy(
                                Point::x,
                                mapping(Point::y, toCollection(TreeSet::new))));

                return byDirection.values().stream().mapToLong(this::countDiscrete).sum();
            }

            private long countDiscrete(SortedSet<Long> positions) {
                 int count = 1;
                 var iter = positions.iterator();
                 var current = iter.next();
                 while (iter.hasNext()) {
                     var next = iter.next();
                     if (next - current > 1) count++;
                     current = next;
                 }
                 return count;
            }

            // For comparison

            long bulkCostByCountingCorners() {
                return area() * perimeterSidesByCountingCorners();
            }

            long perimeterSidesByCountingCorners() {
                return points.stream().mapToLong(this::countCorners)
                        .sum();
            }

            private static boolean[] hasPerimeter = new boolean[8];

            private long countCorners(Point p) {
                for (Direction direction : Direction.values()) {
                    hasPerimeter[direction.ordinal()] = perimeterPoints.contains(direction.addTo(p));
                }
                return IntStream.range(0, 4)
                        .map(i -> i << 1)
                        .filter(i -> {
                            var ahead = hasPerimeter[i];
                            var right = hasPerimeter[(i + 2) % 8];
                            var diag = hasPerimeter[(i + 1) % 8];
                            return (ahead && right) ||
                                    (!ahead && !right && diag);
                        })
                        .count();
            }
        }

        public static Garden of(Stream<String> lines) {
            var grid = DenseGrid.of(lines, (p, c) -> c);

            return new Garden(grid);
        }

        private final Grid<Character> grid;

        Garden(Grid<Character> grid) {
            this.grid = grid;
        }

        public Stream<Region> regions() {
            Set<Point> positions = grid.populatedPositions()
                    .collect(Collectors.toSet());

            List<Region> regions = new ArrayList<>();
            while (!positions.isEmpty()) {
                Point start = positions.iterator().next();
                regions.add(takeRegion(start, positions));
            }
            return regions.stream();
        }

        private Region takeRegion(Point start, Set<Point> positions) {
            Deque<Point> search = new ArrayDeque<>();
            search.add(start);
            Set<Point> region = new HashSet<>();

            while (!search.isEmpty()) {
                var current = search.removeFirst();
                if (positions.remove(current)) {
                    region.add(current);
                    adjacentsOfSameChar(current)
                            .filter(positions::contains)
                            .forEach(search::add);
                }
            }

            return region.stream().flatMap(p ->
                    p.adjacents().filter(a -> !region.contains(a))
            ).collect(
                    Collectors.teeing(
                            Collectors.counting(),
                            Collectors.toSet(),
                            (count, perimeterPoints) -> new Region(region, count, perimeterPoints)
                    ));
        }

        private Stream<Point> adjacentsOfSameChar(Point p) {
            Character atP = grid.get(p);
            return p.adjacents().filter(a -> atP.equals(grid.get(a)));
        }
    }

    @Test
    public void part1Example() {
        var garden = Garden.of(ResourceReader.of("/day12_example.txt").readLines());

        assertEquals(1930, garden.regions().mapToLong(Garden.Region::cost).sum());
    }

    @Test
    public void part2Examples() {
        assertEquals(80, Garden.of(Stream.of(
                "AAAA",
                "BBCD",
                "BBCC",
                "EEEC"
        )).regions().mapToLong(Garden.Region::bulkCost).sum());

        var garden = Garden.of(ResourceReader.of("/day12_example.txt").readLines());

        assertEquals(1206   , garden.regions().mapToLong(Garden.Region::bulkCost).sum());
    }

    @Test
    public void part2() {
        var garden = Garden.of(ResourceReader.of("/day12.txt").readLines());

        assertEquals(841078, garden.regions().mapToLong(Garden.Region::bulkCost).sum());
    }

    @Test
    public void part1() {
        var garden = Garden.of(ResourceReader.of("/day12.txt").readLines());

        assertEquals(1374934, garden.regions().mapToLong(Garden.Region::cost).sum());
    }
}
