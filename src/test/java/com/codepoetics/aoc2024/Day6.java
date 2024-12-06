package com.codepoetics.aoc2024;

import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;


public class Day6 {

    static class GameGrid {

        record PathStep(Point position, Direction direction) {}

        private final Grid<Boolean> grid;

        private final Point initialGuardPosition;

        GameGrid(Grid<Boolean> grid, Point initialGuardPosition) {
            this.grid = grid;
            this.initialGuardPosition = initialGuardPosition;
        }

        public static GameGrid fromInput(Stream<String> input) {
            AtomicReference<Point> initialGuardPosition = new AtomicReference<>();

            Grid<Boolean> grid = SparseGrid.of(input, (p, c) -> switch(c) {
                case '^' -> {
                    initialGuardPosition.set(p);
                    yield null;
                }
                case '#' -> true;
                default -> null;
            });

            return new GameGrid(grid, initialGuardPosition.get());
        }

        private boolean inGrid(Point p) {
            return p.y() >= 0 && p.y() < grid.height()
                    && p.x() >= 0 && p.x() < grid.width();
        }

        private boolean hasObstacle(Point p) {
            return inGrid(p) && grid.get(p) != null;
        }

        private boolean endsInLoop(Point obstacle) {
            Set<PathStep> visited = new HashSet<>();

            Point position = initialGuardPosition;
            Direction direction = Direction.NORTH;

            while (inGrid(position)) {
                PathStep step = new PathStep(position, direction);
                if (visited.contains(step)) return true;
                visited.add(step);

                var nextPosition = direction.addTo(position);
                while (hasObstacle(nextPosition) || nextPosition.equals(obstacle)) {
                    direction = direction.rotate90Right();
                    nextPosition = direction.addTo(position);
                }

                position = nextPosition;
            }

            return false;
        }

        private boolean endsInLoop(Set<PathStep> knownPath, PathStep currentStep, Point obstacle) {
            Set<PathStep> newSteps = new HashSet<>();

            while (inGrid(currentStep.position()) ) {
                Direction nextDirection = currentStep.direction();
                Point nextPosition = nextDirection.addTo(currentStep.position());

                while (hasObstacle(nextPosition) || obstacle.equals(nextPosition)) {
                    nextDirection = nextDirection.rotate90Right();
                    nextPosition = nextDirection.addTo(currentStep.position());
                }

                newSteps.add(currentStep);
                currentStep = new PathStep(nextPosition, nextDirection);
                if (knownPath.contains(currentStep) || newSteps.contains(currentStep)) return true;
            }

            return false;
        }

        public Set<Point> guardPath() {
            Set<Point> visited = new HashSet<>();

            Point position = initialGuardPosition;
            Direction direction = Direction.NORTH;

            while (inGrid(position)) {
                Point nextPosition = direction.addTo(position);

                while (hasObstacle(nextPosition)) {
                    direction = direction.rotate90Right();
                    nextPosition = direction.addTo(position);
                }

                visited.add(position);
                position = nextPosition;
            }

            return visited;
        }

        public int guardPathSize() {
            return guardPath().size();
        }

        public List<PathStep> guardPath2() {
            List<PathStep> path = new ArrayList<>();

            PathStep currentStep = new PathStep(initialGuardPosition, Direction.NORTH);

            while (inGrid(currentStep.position)) {
                var nextDirection = currentStep.direction();
                Point nextPosition = nextDirection.addTo(currentStep.position());

                while (hasObstacle(nextPosition)) {
                    nextDirection = nextDirection.rotate90Right();
                    nextPosition = nextDirection.addTo(currentStep.position());
                }

                path.add(currentStep);
                currentStep = new PathStep(nextPosition, nextDirection);
            }

            return path;
        }

        public long countObstaclePositions() {
            return guardPath().stream()
                    .filter(p -> !p.equals(initialGuardPosition) &&
                            endsInLoop(p))
                    .distinct()
                    .count();
        }

        public int countObstaclePositions2(List<PathStep> guardPath) {
            Set<PathStep> pathSoFar = new HashSet<>();
            Set<Point> obstacles = new HashSet<>();
            Set<Point> tried = new HashSet<>();

            PathStep currentStep = guardPath.getFirst();
            for (PathStep nextStep : guardPath.subList(1, guardPath.size())) {
                if (!tried.contains(nextStep.position) &&
                        endsInLoop(pathSoFar, currentStep, nextStep.position())) {
                    obstacles.add(nextStep.position());
                }
                tried.add(nextStep.position);
                pathSoFar.add(currentStep);
                currentStep = nextStep;
            }

            return obstacles.size();
        }
    }



    @Test
    public void getPathSize() {
        GameGrid grid = GameGrid.fromInput(ResourceReader.of("/day6.txt").readLines());

        System.out.println(grid.guardPathSize());
    }

    @Test
    public void getObstaclePositions() {
        GameGrid grid = GameGrid.fromInput(ResourceReader.of("/day6.txt").readLines());

        System.out.println(grid.countObstaclePositions());
    }

    @Test
    public void getObstaclePositions2() {
        GameGrid grid = GameGrid.fromInput(ResourceReader.of("/day6.txt").readLines());

        System.out.println(grid.countObstaclePositions2(grid.guardPath2()));
    }
}
