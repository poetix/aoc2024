package com.codepoetics.aoc2024;

public record Point(int x, int y) {
    public Point minus(Point other) {
        return new Point(x - other.x, y - other. y);
    }

    public Point plus(Point other) {
        return new Point(x + other.x, y + other.y);
    }
}
