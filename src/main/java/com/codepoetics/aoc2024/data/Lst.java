package com.codepoetics.aoc2024.data;

import com.codepoetics.aoc2024.streams.Streams;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
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

    T head();
    Lst<T> tail();
    boolean isEmpty();
    int size();
    Stream<T> stream();
    T last();
    Lst<T> add(T value);

    default Lst<T> reverse() {
        AtomicReference<Lst<T>> result = new AtomicReference<>(Lst.empty());
        stream().forEach(value -> result.set(result.get().add(value)));
        return result.get();
    }

    default Lst<T> filter(Predicate<T> filter) {
        if (isEmpty()) return this;
        return filter.test(head())
            ? tail().filter(filter).add(head())
            : tail().filter(filter);
    }

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
        public T last() {
            throw new UnsupportedOperationException("Cannot get last element from an empty list");
        }
        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public Stream<T> stream() {
            return Stream.empty();
        }

        @Override
        public int size() { return 0; }

        @Override
        public Lst<T> add(T element) {
            return new Cons<>(element, this, 1, element);
        }
    }

    record Cons<T>(@Override T head, @Override Lst<T> tail, @Override int size, @Override T last) implements Lst<T> {

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public Stream<T> stream() {
            return Streams.generated((Lst<T>) this, Lst::tail, l -> !l.isEmpty())
                    .limit(size)
                    .map(Lst::head);
        }

        @Override
        public Lst<T> add(T value) {
            return new Cons<>(value, this, size + 1, last);
        }
    }
}
