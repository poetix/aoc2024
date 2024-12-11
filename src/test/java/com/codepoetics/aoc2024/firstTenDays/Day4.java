package com.codepoetics.aoc2024.firstTenDays;

import com.codepoetics.aoc2024.*;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.stream.Stream;

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
            return Stream.of(Direction.NORTHEAST, Direction.NORTHWEST)
                    .allMatch(diagonal ->
                            seek("MAS", diagonal.addTo(position), diagonal.inverse())
                            || seek("MAS", diagonal.inverse().addTo(position), diagonal));
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
                ResourceReader.of("/day4.txt").readLines(),
                (ignored, c) -> c
        ));

        System.out.println(grid.countXmases());
        System.out.println(grid.countCrosses());
    }
}
