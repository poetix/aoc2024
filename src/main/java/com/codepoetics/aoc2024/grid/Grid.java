package com.codepoetics.aoc2024.grid;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface Grid<T> {

    boolean contains(Point point);

    record Square<T>(Point position, T contents) {}

    Stream<Point> populatedPositions();
    Stream<Square<T>> populatedSquares();

    default Map<T, List<Point>> populatedPositionsByContents() {
        return populatedSquares()
                .collect(Collectors.groupingBy(
                        Grid.Square::contents,
                        Collectors.mapping(Grid.Square::position, Collectors.toList())));
    }

    T get(Point position);
    default T getOrDefault(Point position, T defaultValue) {
        T result = get(position);
        return result == null ? defaultValue : result;
    }

    int width();
    int height();

    default boolean isInBounds(Point p) {
        return p.x() > -1 && p.x() < width()
                && p.y() > -1 && p.y() < height();
    }
}
