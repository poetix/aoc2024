package com.codepoetics.aoc2024;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public final class Memoisation {

    private Memoisation() {
    }

    public static <A, B, C> BiFunction<A, B, C> memoise(BiFunction<A, B, C> f) {
        Map<A, Map<B, C>> storage = new HashMap<>();
        return (a, b) -> {
            var bToC = storage.computeIfAbsent(a, (ignored) -> new HashMap<>());
            var c = bToC.get(b);
            if (c != null) return c;
            c = f.apply(a, b);
            bToC.put(b, c);
            return c;
        };
    }
}
