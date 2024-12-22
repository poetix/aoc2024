package com.codepoetics.aoc2024;

import com.codepoetics.aoc2024.data.Lst;
import com.codepoetics.aoc2024.graph.WeightedGraph;
import com.codepoetics.aoc2024.grid.*;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Day21 {

    private static final Grid<Character> doorKeypad = SparseGrid.of(Stream.of(
            "789",
            "456",
            "123",
            ".0A"
    ), (ignored, c) -> c != '.' ? c : null);

    private static final Grid<Character> robotKeypad = SparseGrid.of(Stream.of(
            ".^A",
            "<v>"
    ), (ignored, c) -> c != '.' ? c : null);

    record Keypad(Map<Character, Map<Character, Set<String>>> keyPaths, Map<Character, Map<Character, Long>> pathCostCache, Keypad upstream) {

        static Keypad atDepth(int n) {
            Keypad nextRobot = robot(null);

            for (int i=0; i < n - 1; i++) {
                nextRobot = robot(nextRobot);
            }

            return door(nextRobot);
        }

        static Keypad door(Keypad upstreamRobot) {
            return new Keypad(getKeypadPaths(doorKeypad), new HashMap<>(), upstreamRobot);
        }

        static Keypad robot(Keypad upstreamRobot) {
            return new Keypad(getKeypadPaths(robotKeypad), new HashMap<>(), upstreamRobot);
        }

        private static Map<Character, Map<Character, Set<String>>> getKeypadPaths(Grid<Character> keypad) {
            var graph = getKeypadGraph(keypad);
            Map<Character, Map<Character, Set<String>>> result = new HashMap<>();

            keypad.populatedSquares().forEach(startSquare -> {
                var start = startSquare.position();
                var distanceMap = graph.distanceMap(start);
                var pathResults = result.computeIfAbsent(startSquare.contents(), ignored -> new HashMap<>());

                keypad.populatedSquares().forEach(endSquare -> {
                    var end = endSquare.position();
                    pathResults.put(
                            endSquare.contents(),
                            distanceMap.getPathsTo(end).map(Keypad::pointsToPath).collect(Collectors.toSet()));
                });
            });

            return result;
        }

        private static WeightedGraph<Point> getKeypadGraph(Grid<Character> keypad) {
            WeightedGraph<Point> result = new WeightedGraph<>();
            keypad.populatedPositions().forEach(p -> p.adjacents()
                    .filter(keypad::contains)
                    .forEach(a -> result.add(p, a, 1)));
            return result;
        }

        private static String pointsToPath(Lst<Point> points) {
            if (points.tail().isEmpty()) return "A";
            StringBuilder sb = new StringBuilder();
            while (!points.tail().isEmpty()) {
                var from = points.head();
                var to = points.tail().head();

                if (to.equals(Direction.EAST.addTo(from))) sb.append(">");
                if (to.equals(Direction.WEST.addTo(from))) sb.append("<");
                if (to.equals(Direction.NORTH.addTo(from))) sb.append("^");
                if (to.equals(Direction.SOUTH.addTo(from))) sb.append("v");

                points = points.tail();
            }

            sb.append("A");
            return sb.toString();
        }

        public long complexity(String path) {
            return pathCost(path) * Integer.parseInt(path.replace("A", ""));
        }

        public long pathCost(String path) {
            char prev = 'A';
            long total = 0L;
            for (char next : path.toCharArray()) {
                total += minCost(prev, next);
                prev = next;
            }
            return total;
        }

        private long minCost(char start, char end) {
            return pathCostCache.computeIfAbsent(start, ignored -> new HashMap<>())
                    .computeIfAbsent(end, ignored ->
                keyPaths.get(start).get(end).stream()
                        .mapToLong(this::upstreamPathCost)
                        .min().orElseThrow()
            );
        }

        private long upstreamPathCost(String path) {
            return upstream == null ? path.length() : upstream.pathCost(path);
        }
    }

    @Test
    public void codePaths() {
        var testCodes = List.of("029A",
                "980A",
                "179A",
                "456A",
                "379A");

        var puzzleCodes = List.of(
                "140A",
                "169A",
                "170A",
                "528A",
                "340A"
        );

        var part1Keypad = Keypad.atDepth(2);
        var part2Keypad = Keypad.atDepth(25);

        assertEquals(94284L, puzzleCodes.stream().mapToLong(part1Keypad::complexity).sum());
        assertEquals(116821732384052L, puzzleCodes.stream().mapToLong(part2Keypad::complexity).sum());
    }
}
