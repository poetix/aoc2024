package com.codepoetics.aoc2024;

import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiFunction;
import java.util.function.Function;
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

    public static <T> Stream<T> fromIterator(Iterator<T> iterator) {
        return StreamSupport.stream(
                Spliterators.spliterator(
                        iterator,
                        Long.MAX_VALUE,
                        Spliterator.NONNULL & Spliterator.IMMUTABLE), false);
    }

    public static <I, O> Stream<O> interpret(Stream<I> input, Function<Iterator<I>, O> interpreter) {
        var iter = input.iterator();
        return fromIterator(new Iterator<O>() {

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public O next() {
                return interpreter.apply(iter);
            }
        });
    }

    public static <I, T> Stream<T> pairsIn(List<I> items, BiFunction<I, I, T> toPair) {
        return IntStream.range(0, items.size()).boxed().flatMap(i -> {
            var first = items.get(i);
            return IntStream.range(i + 1, items.size()).mapToObj(j ->
                    toPair.apply(first, items.get(j)));
        });
    }
}
