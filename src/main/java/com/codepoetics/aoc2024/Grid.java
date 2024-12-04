package com.codepoetics.aoc2024;

import java.util.stream.Stream;

public interface Grid<T> {

    record Square<T>(Point position, T contents) {}

    Stream<Point> populatedPositions();
    Stream<Square<T>> populatedSquares();
    T get(Point position);

    int width();
    int height();
}
