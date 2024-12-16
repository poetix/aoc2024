package com.codepoetics.aoc2024.streams;

import java.util.Spliterator;
import java.util.function.IntConsumer;
import java.util.function.ToIntBiFunction;

class ZippingToIntSpliterator<L, R, O> implements Spliterator.OfInt {

    static <L, R> Spliterator.OfInt zipping(Spliterator<L> lefts, Spliterator<R> rights, ToIntBiFunction<L, R> combiner) {
        return new ZippingToIntSpliterator<>(lefts, rights, combiner);
    }

    private final Spliterator<L> lefts;
    private final Spliterator<R> rights;
    private final ToIntBiFunction<L, R> combiner;
    private boolean rightHadNext = false;

    private ZippingToIntSpliterator(Spliterator<L> lefts, Spliterator<R> rights, ToIntBiFunction<L, R> combiner) {
        this.lefts = lefts;
        this.rights = rights;
        this.combiner = combiner;
    }

    @Override
    public boolean tryAdvance(IntConsumer action) {
        rightHadNext = false;
        boolean leftHadNext = lefts.tryAdvance(l ->
                rights.tryAdvance(r -> {
                    rightHadNext = true;
                    action.accept(combiner.applyAsInt(l, r));
                }));
        return leftHadNext && rightHadNext;
    }

    @Override
    public Spliterator.OfInt trySplit() {
        return null;
    }

    @Override
    public long estimateSize() {
        return Math.min(lefts.estimateSize(), rights.estimateSize());
    }

    @Override
    public int characteristics() {
        return lefts.characteristics() & rights.characteristics()
                & ~(Spliterator.DISTINCT | Spliterator.SORTED);
    }
}