package com.codepoetics.aoc2024;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public record SparseGrid<T>(
        Map<Point, T> contents,
        @Override int width,
        @Override int height) implements Grid<T> {

    public static <T> SparseGrid<T> of(Stream<String> input, Function<Character, T> interpreter) {
        AtomicInteger atomicY = new AtomicInteger(0);
        AtomicInteger width = new AtomicInteger();

        Map<Point, T> map = new HashMap<>();
        input.forEach(line -> {
            int y = atomicY.getAndIncrement();
            width.set(line.length());

            IntStream.range(0, line.length()).forEach(x -> {
                var position = new Point(x, y);
                var interpreted = interpreter.apply(line.charAt(x));
                if (interpreted != null) {
                    map.put(position, interpreted);
                }
            });
        });

        return new SparseGrid<>(map, atomicY.get(), width.get());
    }

    @Override
    public Stream<Point> populatedPositions() {
        return contents.keySet().stream();
    }

    @Override
    public Stream<Square<T>> populatedSquares() {
        return contents.entrySet().stream().map(e ->
                new Grid.Square<T>(e.getKey(), e.getValue())
        );
    }

    @Override
    public T get(Point position) {
        return contents.get(position);
    }
}
