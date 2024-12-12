package com.codepoetics.aoc2024;

import java.util.stream.Stream;

public record Point(int x, int y) {
    public Point minus(Point other) {
        return new Point(x - other.x, y - other. y);
    }

    public Point plus(Point other) {
        return new Point(x + other.x, y + other.y);
    }

    public Stream<Point> adjacents() {
        return Direction.nsew().map(d -> d.addTo(this));
    }
}
