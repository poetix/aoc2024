package com.codepoetics.aoc2024.data;

import com.codepoetics.aoc2024.streams.Streams;

import java.util.Arrays;
import java.util.stream.Stream;

public sealed interface Lst<T> permits Lst.Empty, Lst.Cons {

    Lst<Object> EMPTY = new Empty<>();

    static <T> Lst<T> empty() {
        return (Lst<T>) EMPTY;
    }

    @SafeVarargs
    static <T> Lst<T> of(T...items) {
        return of(Arrays.stream(items));
    }

    static <T> Lst<T> of(Stream<T> items) {
        return items.reduce(Lst.empty(), Lst::add, (l, ignored) -> l);
    }

    default Lst<T> add(T item) {
        return new Cons<T>(item, this);
    }

    T head();
    Lst<T> tail();
    boolean isEmpty();
    Stream<T> stream();

    record Empty<T>() implements Lst<T> {

        @Override
        public T head() {
            throw new UnsupportedOperationException("Cannot get head from an empty list");
        }

        @Override
        public Lst<T> tail() {
            throw new UnsupportedOperationException("Cannot get tail from an empty list");
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public Stream<T> stream() {
            return Stream.empty();
        }
    }

    record Cons<T>(@Override T head, @Override Lst<T> tail) implements Lst<T> {

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public Stream<T> stream() {
            return Streams.generated((Lst<T>) this, Lst::tail, l -> !l.isEmpty()).map(Lst::head);
        }
    }
}
