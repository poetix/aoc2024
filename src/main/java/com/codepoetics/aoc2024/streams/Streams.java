package com.codepoetics.aoc2024.streams;

import java.util.*;
import java.util.function.*;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
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

    public static <A> Stream<A> generated(A accumulator, Function<A, A> step, Predicate<A> guard) {
        Iterator<A> iter = new Iterator<A>() {

            private A current = accumulator;

            @Override
            public boolean hasNext() {
                return guard.test(current);
            }

            @Override
            public A next() {
                var result = current;
                current = step.apply(current);
                return result;
            }
        };

        return StreamSupport.stream(
                Spliterators.spliterator(iter, Integer.MAX_VALUE,
                        Spliterator.NONNULL | Spliterator.IMMUTABLE), false);
    }

    public static Stream<LongStream> windowed(int size, LongStream source) {
        long[] window = new long[size];
        var sourceSpliterator = source.spliterator();

        var spliterator = new Spliterator<LongStream>() {

            private int ptr = 0;
            private boolean isFull;

            private void accept(long next) {
                window[ptr++] = next;
                if (ptr == size) {
                    ptr = 0;
                    isFull = true;
                }
            }

            @Override
            public boolean tryAdvance(Consumer<? super LongStream> action) {
                if (!isFull) {
                    while (!isFull) {
                        if (!sourceSpliterator.tryAdvance((LongConsumer) this::accept)) return false;
                    }
                    action.accept(Arrays.stream(window));
                    return true;
                }

                if (!sourceSpliterator.tryAdvance((LongConsumer) this::accept)) return false;
                action.accept(IntStream.range(0, size).mapToLong(i -> window[(ptr + i) % size]));
                return true;
            }

            @Override
            public Spliterator<LongStream> trySplit() {
                return null;
            }

            @Override
            public long estimateSize() {
                var sourceSize = sourceSpliterator.estimateSize();
                return sourceSize == Long.MAX_VALUE
                        ? Long.MAX_VALUE
                        : sourceSize - size;
            }

            @Override
            public int characteristics() {
                return sourceSpliterator.characteristics();
            }
        };

        return StreamSupport.stream(spliterator, false);
    }

    public static LongStream deltas(LongStream source) {
        return windowed(2, source).mapToLong(window -> {
            var arr = window.toArray();
            return arr[1] - arr[0];
        });
    }
}
