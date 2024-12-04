package com.codepoetics.aoc2024;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.function.Function;

public class Day4 {

    public record WordsearchGrid(Grid<Character> grid) {

        private boolean seek(String s, Point position, Direction direction) {
            Point cursor = position;
            for (char c : s.toCharArray()) {
                Character charAt = grid.get(cursor);
                if (charAt == null || c != charAt) return false;
                cursor = direction.addTo(cursor);
            }
            return true;
        }

        private long countXmasesAt(Point position) {
            return Arrays.stream(Direction.values())
                    .filter(direction -> seek("XMAS", position, direction))
                    .count();
        }

        public long countXmases() {
            return grid.populatedPositions().mapToLong(this::countXmasesAt).sum();
        }

        public boolean hasCrossAt(Point position) {
            return Arrays.stream(Direction.DIAGONALS)
                    .filter(diagonal ->
                            seek("MAS", diagonal.addTo(position), diagonal.inverse()))
                    .count() == 2;
        }

        public long countCrosses() {
            return grid.populatedSquares()
                    .filter(entry -> entry.contents() == 'A'
                            && hasCrossAt(entry.position()))
                    .count();
        }
    }

    @Test
    public void countXmasesAndCrosses() {
        WordsearchGrid grid = new WordsearchGrid(DenseGrid.of(
                ResourceReader.of("/day4.txt").readLines().toList(),
                Function.identity()
        ));

        System.out.println(grid.countXmases());
        System.out.println(grid.countCrosses());
    }
}
