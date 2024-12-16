package com.codepoetics.aoc2024.graph;

import com.codepoetics.aoc2024.data.IndexedMap;
import com.codepoetics.aoc2024.data.Lst;
import com.codepoetics.aoc2024.data.PriorityQueueSet;

import java.util.*;
import java.util.stream.Stream;

public class WeightedGraph<T> {

    public record WeightedEdge<T>(T source, T target, long weight) { }
    public record DistanceMap<T>(Map<T, Long> distances, Map<T, Set<T>> precursors) {

        public Stream<Lst<T>> getPathsTo(T end) {
            List<Lst<T>> finishedPaths = new ArrayList<>();

            Deque<Lst<T>> precursorQueue = new ArrayDeque<>();
            precursorQueue.push(Lst.of(end));

            while (!precursorQueue.isEmpty()) {
                var next = precursorQueue.removeFirst();
                var head = next.head();

                var more = precursors.get(head);
                if (more == null) {
                    finishedPaths.add(next);
                    continue;
                }

                more.forEach(p -> precursorQueue.push(next.add(p)));
            }

            return finishedPaths.stream();
        }
    }

    public WeightedGraph() {
        this.data = new IndexedMap<>(new HashMap<>(), WeightedEdge::source);
    }

    public void add(T source, T target, long weight) {
        data.add(new WeightedEdge<>(source, target, weight));
    }

    private final IndexedMap<T, WeightedEdge<T>> data;

    private class DistanceMapCalculationContext {
        private final Map<T, Long> distances = new HashMap<>();
        private final Map<T, Set<T>> precursors = new HashMap<>();

        private final PriorityQueueSet<T> queue = new PriorityQueueSet<>();

        public DistanceMap<T> distanceMap(T start) {
            initialise(start);
            populateDistanceMap();
            return new DistanceMap<T>(distances, precursors);
        }

        private void initialise(T start) {
            data.indices().forEach(vertex -> {
                var score = vertex.equals(start) ? 0 : Long.MAX_VALUE;
                queue.add(score, vertex);
                distances.put(vertex, score);
            });
        }

        private void populateDistanceMap() {
            while (!queue.isEmpty()) {
                T source = queue.removeFirst();

                var distU = distances.get(source);
                if (distU == Long.MAX_VALUE) return;

                data.get(source)
                        .filter(weighted -> queue.contains(weighted.target()))
                        .forEach(weighted ->
                                updateScores(source, weighted.target(), distU + weighted.weight())
                        );
            }
        }

        private void updateScores(T source, T target, long newWeight) {
            var distV = distances.get(target);

            if (newWeight > distV) return;
            distances.put(target, newWeight);

            if (newWeight < distV) {
                queue.reprioritise(distV, newWeight, target);
                Set<T> newPrecursors = new HashSet<>();
                newPrecursors.add(source);
                precursors.put(target, newPrecursors);
                return;
            }

            precursors.computeIfAbsent(target, ignored -> new HashSet<>()).add(source);
        }
    }

    public DistanceMap<T> distanceMap(T start) {
        var context = new DistanceMapCalculationContext();
        return context.distanceMap(start);
    }

}
