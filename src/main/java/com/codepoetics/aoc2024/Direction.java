package com.codepoetics.aoc2024;

public enum Direction {
    NORTH(0, -1),
    SOUTH(0, 1),
    EAST(1, 0),
    WEST(-1, 0),
    NORTHEAST(1, -1),
    SOUTHEAST(1, 1),
    NORTHWEST(-1, -1),
    SOUTHWEST(-1, 1);

    public static final Direction[] DIAGONALS = new Direction[] {
            NORTHEAST, SOUTHEAST, NORTHWEST, SOUTHWEST
    };

    private final int xd;
    private final int yd;

    Direction(int xd, int yd) {
        this.xd = xd;
        this.yd = yd;
    }

    public Point addTo(Point p) {
        return new Point(p.x() + xd, p.y() + yd);
    }

    public Direction inverse() {
        return switch (this) {
            case NORTH -> SOUTH;
            case SOUTH -> NORTH;
            case EAST -> WEST;
            case WEST -> EAST;
            case NORTHEAST -> SOUTHWEST;
            case SOUTHEAST -> NORTHWEST;
            case NORTHWEST -> SOUTHEAST;
            case SOUTHWEST -> NORTHEAST;
        };
    }
}
