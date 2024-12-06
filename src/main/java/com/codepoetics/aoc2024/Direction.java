package com.codepoetics.aoc2024;

import java.util.Arrays;

public enum Direction {
    NORTH(0, 0, -1),
    NORTHEAST(1,1, -1),
    EAST(2, 1, 0),
    SOUTHEAST(3, 1, 1),
    SOUTH(4, 0, 1),
    SOUTHWEST(5, -1, 1),
    WEST(6, -1, 0),
    NORTHWEST(7, -1, -1);

    private final int index;
    private final int xd;
    private final int yd;

    Direction(int index, int xd, int yd) {
        this.index = index;
        this.xd = xd;
        this.yd = yd;
    }

    public Point addTo(Point p) {
        return new Point(p.x() + xd, p.y() + yd);
    }

    public Direction inverse() {
        return values()[(index + 4) % 8];
    }

    public Direction rotate90Right() {
        return values()[(index + 2) % 8];
    }
}
