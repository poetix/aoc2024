package com.codepoetics.aoc2024;

import java.util.PrimitiveIterator;
import java.util.stream.IntStream;

public final class Iterators {

    private Iterators() { }

    public static PrimitiveIterator.OfInt deltas(PrimitiveIterator.OfInt levels) {
        if (!levels.hasNext()) return IntStream.empty().iterator();

        return new PrimitiveIterator.OfInt() {
            private int previous = levels.next();

            @Override
            public boolean hasNext() {
                return levels.hasNext();
            }

            @Override
            public int nextInt() {
                var current = levels.nextInt();
                var delta = current - previous;
                previous = current;
                return delta;
            }
        };
    }

}
