package com.codepoetics.aoc2024;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.IntConsumer;
import java.util.function.ToIntBiFunction;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class Streams {

    private Streams() { }

    public static <L, R> IntStream zipToInt(Stream<L> left, Stream<R> right, ToIntBiFunction<L, R> zip) {
        var spliterator = ZippingToIntSpliterator.zipping(left.spliterator(), right.spliterator(), zip);

        return StreamSupport.intStream(spliterator, false);
    }
}
