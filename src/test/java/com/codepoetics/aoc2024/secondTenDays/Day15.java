package com.codepoetics.aoc2024.secondTenDays;

import com.codepoetics.aoc2024.*;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Day15 {

    interface BoxSet {
        boolean containsBox(Point position);
        boolean canMoveBoxAt(Point position, Direction direction, Predicate<Point> hasWall);
        void moveBoxAt(Point position, Direction direction);
        long score();
        String represent(int width, int height, Point botPosition, Predicate<Point> hasWall);
    }

    static class SingleCellBoxSet implements BoxSet {

        private final Set<Point> boxPositions;

        SingleCellBoxSet(Set<Point> boxPositions) {
            this.boxPositions = boxPositions;
        }

        @Override
        public boolean containsBox(Point position) {
            return boxPositions.contains(position);
        }

        @Override
        public boolean canMoveBoxAt(Point position, Direction direction, Predicate<Point> hasWall) {
            var afterMove = direction.addTo(position);
            if (hasWall.test(afterMove)) return false;

            return !containsBox(afterMove) || canMoveBoxAt(afterMove, direction, hasWall);
        }

        @Override
        public void moveBoxAt(Point position, Direction direction) {
            boxPositions.remove(position);
            var newPosition = direction.addTo(position);
            while (containsBox(newPosition)) {
                newPosition = direction.addTo(newPosition);
            }
            boxPositions.add(newPosition);
        }

        @Override
        public long score() {
            return boxPositions.stream().mapToLong(p -> p.y() * 100 + p.x()).sum();
        }

        @Override
        public String represent(int width, int height, Point botPosition, Predicate<Point> hasWall) {
            return IntStream.range(0, height).mapToObj(y ->
                    IntStream.range(0, width).mapToObj(x -> {
                        var p = new Point(x, y);
                        return hasWall.test(p) ? "#"
                                : p.equals(botPosition) ? "@"
                                : boxPositions.contains(p) ? "O" : ".";
                    }).collect(Collectors.joining())).collect(Collectors.joining("\n"));
        }
    }

    static class DualCellBoxSet implements BoxSet {

        private final Map<Point, Point> boxPositions;

        DualCellBoxSet(Map<Point, Point> boxPositions) {
            this.boxPositions = boxPositions;
        }

        @Override
        public boolean containsBox(Point position) {
            return boxPositions.containsKey(position);
        }

        @Override
        public boolean canMoveBoxAt(Point position, Direction direction, Predicate<Point> hasWall) {
            var boxStart = boxPositions.get(position);
            var newLeft = direction.addTo(boxStart);
            var newRight = Direction.EAST.addTo(newLeft);

            if (hasWall.test(newLeft) || hasWall.test(newRight)) return false;

            var boxAtNewLeft = boxPositions.get(newLeft);
            var boxAtNewRight = boxPositions.get(newRight);

            if (!(boxAtNewLeft == null
                    || boxAtNewLeft.equals(boxStart)
                    || canMoveBoxAt(boxAtNewLeft, direction, hasWall))) return false;

            return boxAtNewRight == null
                    || boxAtNewRight.equals(boxStart)
                    || canMoveBoxAt(boxAtNewRight, direction, hasWall);
        }

        @Override
        public void moveBoxAt(Point position, Direction direction) {
            var movedBoxLeft = boxPositions.get(position);
            var movedBoxRight = Direction.EAST.addTo(movedBoxLeft);

            boxPositions.remove(movedBoxLeft);
            boxPositions.remove(movedBoxRight);

            var newLeft = direction.addTo(movedBoxLeft);
            var newRight = direction.addTo(movedBoxRight);

            if (containsBox(newLeft)) moveBoxAt(newLeft, direction);
            if (containsBox(newRight)) moveBoxAt(newRight, direction);

            boxPositions.put(newLeft, newLeft);
            boxPositions.put(newRight, newLeft);
        }

        @Override
        public long score() {
            return boxPositions.values().stream().distinct().mapToLong(p ->
                    p.y() * 100 + p.x()).sum();
        }

        @Override
        public String represent(int width, int height, Point botPosition, Predicate<Point> hasWall) {
            return IntStream.range(0, height).mapToObj(y ->
                    IntStream.range(0, width).mapToObj(x -> {
                        var p = new Point(x, y);
                        var boxAt = boxPositions.get(p);
                        if (hasWall.test(p)) return "#";
                        if (p.equals(botPosition)) return "@";
                        if (boxAt != null && boxAt.equals(p)) return "[";
                        if (boxAt != null) return "]";
                        return ".";
                    }).collect(Collectors.joining())).collect(Collectors.joining("\n"));
        }
    }

    record BoxPuzzleState(Point botPosition, BoxSet boxPositions) {

        public long score() {
            return boxPositions().score();
        }

        public BoxPuzzleState afterMove(Direction direction, Predicate<Point> hasWall) {
            var newPosition = direction.addTo(botPosition);

            if (hasWall.test(newPosition)) return this;
            if (!boxPositions.containsBox(newPosition)) return new BoxPuzzleState(newPosition, boxPositions);
            if (!boxPositions.canMoveBoxAt(newPosition, direction, hasWall)) return this;

            boxPositions.moveBoxAt(newPosition, direction);
            return new BoxPuzzleState(newPosition, boxPositions);
        }

        public String represent(int width, int height, Predicate<Point> hasWall) {
            return boxPositions.represent(width, height, botPosition, hasWall);
        }
    }

    static class BoxPuzzle {

        public static BoxPuzzle from(Stream<String> lines) {
            List<String> allLines = lines.toList();
            var gridLines = allLines.stream().takeWhile(line -> !line.trim().isBlank());
            var directionLines = allLines.stream().dropWhile(line -> !line.trim().isBlank()).skip(1);

            Set<Point> boxPositions = new HashSet<>();
            AtomicReference<Point> botPosition = new AtomicReference<>();
            Grid<Boolean> walls = SparseGrid.of(gridLines, (p, c) ->
                    switch (c) {
                        case '#' -> true;
                        case '@' -> {
                            botPosition.set(p);
                            yield null;
                        }
                        case 'O' -> {
                            boxPositions.add(p);
                            yield null;
                        }
                        default -> null;
                    });

            List<Direction> instructions = directionLines.flatMap(line ->
                    line.chars().mapToObj(c -> Direction.fromChar((char) c))
            ).toList();

            return new BoxPuzzle(botPosition.get(), boxPositions, walls, instructions);
        }

        private final Point initialBotPosition;
        private final Set<Point> initialBoxPositions;
        private final Grid<Boolean> walls;
        private final List<Direction> instructions;

        BoxPuzzle(Point initialBotPosition, Set<Point> initialBoxPositions, Grid<Boolean> walls, List<Direction> instructions) {
            this.initialBotPosition = initialBotPosition;
            this.initialBoxPositions = initialBoxPositions;
            this.walls = walls;
            this.instructions = instructions;
        }

        public BoxPuzzleState run() {
            return instructions.stream()
                    .reduce(new BoxPuzzleState(initialBotPosition, new SingleCellBoxSet(initialBoxPositions)),
                            (s, d) -> s.afterMove(d, p -> walls.getOrDefault(p, false)),
                            (l, ignored) -> l);
        }

        public BoxPuzzleState runWide() {
            Map<Point, Point> wideBoxMap = new HashMap<>();
            initialBoxPositions.forEach(p -> {
                var widened = new Point(p.x() * 2, p.y());
                wideBoxMap.put(widened, widened);
                wideBoxMap.put(Direction.EAST.addTo(widened), widened);
            });

            BoxPuzzleState wideState = new BoxPuzzleState(
                    new Point(initialBotPosition.x() * 2, initialBotPosition.y()),
                    new DualCellBoxSet(wideBoxMap));

            return instructions.stream()
                    .reduce(wideState,
                            (s, d) -> s.afterMove(d, p -> walls.getOrDefault(new Point(p.x() / 2, p.y()), false)),
                            (l, ignored) -> l);
        }

        public String represent(BoxPuzzleState state) {
            return state.represent(
                    walls.width(),
                    walls.height(),
                    p -> walls.getOrDefault(p, false));
        }

        public String representWide(BoxPuzzleState state) {
            return state.represent(
                    walls.width() * 2,
                    walls.height(),
                    p -> walls.getOrDefault(new Point(p.x() / 2, p.y()), false));
        }
    }

    @Test
    public void part1() {
        BoxPuzzle puzzle = BoxPuzzle.from(ResourceReader.of("/day15.txt").readLines());
        assertEquals(1485257L, puzzle.run().score());
    }

    @Test
    public void part2Test() {
        BoxPuzzle puzzle = BoxPuzzle.from(ResourceReader.of("/day15_test.txt").readLines());
        assertEquals(9021, puzzle.runWide().score());
        //System.out.println(puzzle.representWide(puzzle.runWide()));
    }

    @Test
    public void part2() {
        BoxPuzzle puzzle = BoxPuzzle.from(ResourceReader.of("/day15.txt").readLines());
        assertEquals(1475512L, puzzle.runWide().score());
    }

}
