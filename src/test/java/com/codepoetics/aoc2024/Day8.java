package com.codepoetics.aoc2024;

import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Day8 {

    static class AntennaModel {

        public static AntennaModel of(Stream<String> lines) {
            return new AntennaModel(SparseGrid.of(lines, (ignored, c) ->
                    c == '.' ? null : c));
        }

        AntennaModel(Grid<Character> grid) {
            this.grid = grid;
        }

        private final Grid<Character> grid;

        private Stream<AntennaPair> antennaPairs() {
            return grid.populatedPositionsByContents().values().stream()
                    .flatMap(this::antennaPairs);
        }

        public long countAntinodePositionsPart1() {
            return antennaPairs()
                    .flatMap(pair -> pair.antinodesPart1(grid::isInBounds))
                    .distinct()
                    .count();
        }

        public long countAntinodePositionsPart2() {
            return antennaPairs()
                    .flatMap(pair -> pair.antinodesPart2(grid::isInBounds))
                    .distinct()
                    .count();
        }

        private Stream<AntennaPair> antennaPairs(List<Point> positions) {
            return Streams.pairsIn(positions, AntennaPair::new);
        }
    }

    public record AntennaPair(Point p1, Point p2) {
        Stream<Point> antinodesPart1(Predicate<Point> inBounds) {
            return Stream.of(
                    p2.plus(p2).minus(p1),
                    p1.plus(p1).minus(p2))
                    .filter(inBounds);
        }

        Stream<Point> antinodesPart2(Predicate<Point> inBounds) {
            return Stream.concat(
                    nodesFromTo(p1, p2, inBounds),
                    nodesFromTo(p2, p1, inBounds));
        }

        private Stream<Point> nodesFromTo(Point from, Point to, Predicate<Point> inBounds) {
            var delta = to.minus(from);
            var nextAntinode = from;

            Stream.Builder<Point> result = Stream.builder();
            while (inBounds.test(nextAntinode)) {
                result.add(nextAntinode);
                nextAntinode = nextAntinode.plus(delta);
            }
            return result.build();
        }

    }

    private final AntennaModel testModel = AntennaModel.of(ResourceReader.of("/day8_test.txt").readLines());
    private final AntennaModel puzzleModel = AntennaModel.of(ResourceReader.of("/day8.txt").readLines());

    @Test
    public void findAntinodes() {
        assertEquals(14, testModel.countAntinodePositionsPart1());
        assertEquals(34, testModel.countAntinodePositionsPart2());

        System.out.println(puzzleModel.countAntinodePositionsPart1());
        System.out.println(puzzleModel.countAntinodePositionsPart2());
    }
}
