package com.codepoetics.aoc2024;

import java.util.stream.Stream;

public enum Direction {
    NORTH(0, -1),
    NORTHEAST(1, -1),
    EAST(1, 0),
    SOUTHEAST(1, 1),
    SOUTH(0, 1),
    SOUTHWEST(-1, 1),
    WEST(-1, 0),
    NORTHWEST( -1, -1);

    public static Direction fromChar(char c) {
        return switch(c) {
            case '^' -> NORTH;
            case '>' -> EAST;
            case 'v' -> SOUTH;
            case '<' -> WEST;
            default -> throw new IllegalArgumentException();
        };
    }
    public static Stream<Direction> nsew() {
        return Stream.of(NORTH, SOUTH, EAST, WEST);
    }

    private final Point asPoint;

    Direction(int xd, int yd) {
        this.asPoint = new Point(xd, yd);
    }

    public Point addTo(Point p) {
        return p.plus(asPoint);
    }

    public Direction inverse() {
        return values()[(ordinal() + 4) % 8];
    }

    public Direction rotate90Right() {
        return values()[(ordinal() + 2) % 8];
    }

    public boolean isVertical() {
        return asPoint.x() == 0;
    }
}
