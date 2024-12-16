package com.codepoetics.aoc2024.grid;

import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public record DenseGrid<T>(T[][] grid) implements Grid<T> {

    public static <T> DenseGrid<T> of(Stream<String> input, BiFunction<Point, Character, T> interpreter) {
        List<String> lines = input.toList();
        int height = lines.size();
        int width = lines.getFirst().length();
        T[][] grid = (T[][]) new Object[height][width];
        for (int y = 0; y < height; y++) {
            char[] lineChars = lines.get(y).toCharArray();
            for (int x = 0; x < width; x++) {
                grid[y][x] = interpreter.apply(new Point(x, y), lineChars[x]);
            }
        }
        return new DenseGrid<>(grid);
    }

    @Override
    public Stream<Point> populatedPositions() {
        return IntStream.range(0, width())
                .boxed()
                .flatMap(x -> IntStream.range(0, height())
                    .mapToObj(y -> new Point(x, y)));
    }

    @Override
    public Stream<Square<T>> populatedSquares() {
        return IntStream.range(0, width())
                .boxed()
                .flatMap(x -> IntStream.range(0, height())
                        .mapToObj(y -> new Square<>(
                                new Point(x, y),
                                grid[y][x])));
    }

    @Override
    public T get(Point position) {
        if (position.x() < 0 || position.x() >= width() ||
        position.y() < 0 || position.y() >= height()) return null;
        return grid[(int) position.y()][(int) position.x()];
    }

    @Override
    public int width() {
        return grid[0].length;
    }

    @Override
    public int height() {
        return grid.length;
    }
}
