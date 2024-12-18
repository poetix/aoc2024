package com.codepoetics.aoc2024.grid;

import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;

public class ConnectedRegion {

    private final SortedMap<Long, SortedSet<Long>> contents = new TreeMap<>();

    public void add(Point point) {
        ysAtX(point.x()).add(point.y());
    }

    private SortedSet<Long> ysAtX(long x) {
        return contents.computeIfAbsent(x, (ignored) -> new TreeSet<>());
    }

    public boolean isConnected(Point point) {
        return contents.subMap(point.x() - 1, point.x() + 2).values().stream().anyMatch(ySet ->
                !ySet.subSet(point.y() - 1, point.y() + 2).isEmpty());
    }

    public boolean isNSEWConnected(Point point) {
        return contents.subMap(point.x() - 1, point.x() + 2).entrySet().stream().anyMatch(e -> {
            var x = e.getKey();
            return e.getValue().stream().anyMatch(y ->
                    (point.x() == x && (point.y() == y + 1 || point.y() == y - 1)
                            || (point.y() == y && (point.x() == x + 1 || point.x() == x - 1))));
        });
    }

    public void merge(ConnectedRegion other) {
        other.contents.forEach((x, ys) -> ys.forEach(ysAtX(x)::add));
    }

    public boolean contains(Point point) {
        var ys = contents.get(point.x());
        return ys != null && ys.contains(point.y());
    }

    public Stream<Point> contents() {
        return contents.entrySet().stream().flatMap(e -> {
            var x = e.getKey();
            return e.getValue().stream().map(y -> new Point(x, y));
        });
    }

}
