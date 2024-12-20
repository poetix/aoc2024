package com.codepoetics.aoc2024.data;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class DisjointSet<T> {

    private static final int LESS_THAN = -1;
    private static final int GREATER_THAN = 1;

    private final Map<T, T> parents = new HashMap<>();
    private final Map<T, Integer> ranks = new HashMap<>();

    public void add(T element) {
        parents.put(element, element);
        ranks.put(element, 0);
    }

    @SafeVarargs
    public final void addAll(T... elements) {
        Arrays.stream(elements).forEach(this::add);
    }

    public void connect(T first, T second) {
        T root1 = findRoot(first);
        T root2 = findRoot(second);

        if (root1.equals(root2)) return;

        switch(Integer.compare(ranks.get(root1), ranks.get(root2))) {
            case LESS_THAN -> parents.put(root1, root2);
            case GREATER_THAN-> parents.put(root2, root1);
            default -> {
                parents.put(root2, root1);
                ranks.put(root1, ranks.get(root1) + 1);
            }
        }
    }

    public boolean isConnected(T first, T second) {
        return findRoot(first).equals(findRoot(second));
    }

    public boolean contains(T element) {
        return parents.containsKey(element);
    }

    private T findRoot(T element) {
        if (!parents.get(element).equals(element)) {
            parents.put(element, findRoot(parents.get(element))); // Path compression: flatten the tree
        }
        return parents.get(element);
    }
}
