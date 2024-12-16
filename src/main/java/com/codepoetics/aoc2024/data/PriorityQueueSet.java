package com.codepoetics.aoc2024.data;

import java.util.HashSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

public class PriorityQueueSet<T> {
    private final SortedMap<Long, Set<T>> byPriority = new TreeMap<>();
    private final Set<T> unprioritised = new HashSet<>();

    public void add(long priority, T value) {
        byPriority.computeIfAbsent(priority, ignored -> new HashSet<>()).add(value);
        unprioritised.add(value);
    }

    public boolean contains(T value) {
        return unprioritised.contains(value);
    }

    public T removeFirst() {
        var entryAtPriority = byPriority.firstEntry();
        var priority = entryAtPriority.getKey();
        var atPriority = entryAtPriority.getValue();
        T next = atPriority.iterator().next();
        atPriority.remove(next);
        unprioritised.remove(next);
        if (atPriority.isEmpty()) byPriority.remove(priority);
        return next;
    }

    public void reprioritise(long oldPriority, long newPriority, T value) {
        var atOld = byPriority.get(oldPriority);
        atOld.remove(value);
        if (atOld.isEmpty()) {
            byPriority.remove(oldPriority);
        }
        byPriority.computeIfAbsent(newPriority, ignored -> new HashSet<>()).add(value);
    }

    public boolean isEmpty() {
        return unprioritised.isEmpty();
    }
}
