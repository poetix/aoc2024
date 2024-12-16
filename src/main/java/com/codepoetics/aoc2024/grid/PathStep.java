package com.codepoetics.aoc2024.grid;

public record PathStep(Point position, Direction direction) {

    public PathStep turnLeft() {
        return new PathStep(position, direction.rotate90Left());
    }

    public PathStep turnRight() {
        return new PathStep(position, direction().rotate90Right());
    }

    public PathStep goForward() {
        return new PathStep(direction.addTo(position), direction);
    }

    public Point ahead() {
        return direction.addTo(position);
    }
}
