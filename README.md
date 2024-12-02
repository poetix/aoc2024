# Advent of Code 2024

This repository contains my solutions for the challenges in the [2024 Advent of Code](https://adventofcode.com/2024).

This year I have decided to practice "modern" Java, using newer language features where appropriate.

## Day 1

We are given numbers in two columns. In part 1, we need to sort the columns independently, then sum the absolute differences between the numbers in the left and right columns.

I decided I wanted to sort list entries as they came in - but Java doesn't have a `SortedList` (because it would break the `List` contract, which says that the *n*th item by insertion order can be retrieved with `get(n)`).

So as a first step I implemented a `SortedSequence` that uses a `TreeMap` to keep unique elements in sorted order in the key-set, and keep a count of repeated elements:

```java
public class SortedSequence<T> {

    private final Map<T, Integer> counts;

    public SortedSequence(Comparator<T> comparator) {
        counts = new TreeMap<>(comparator);
    }

    public void add(T item) {
        counts.compute(item, (ignored, count) -> count == null ? 1 : count + 1);
    }

    public Stream<T> stream() {
        return counts.entrySet().stream().flatMap(entry ->
            IntStream.range(0, entry.getValue())
                .mapToObj(ignored -> entry.getKey())
        );
    }
}
```

The `Stream` method streams out a sorted sequence, using `flatMap` to expand each entry (`"a": 3`) into a subsequence `["a", "a", "a"]`.

Maybe we'll get to re-use this, maybe not.

I had a notion that Java would by now have a way to "zip" a pair of Streams together, but no, you have to roll your own (or import [protonpack](https://github.com/poetix/protonpack)). Might as well do the sums while we're at it:

```java
public int sumDifferences() {
    var leftIter = left.stream().iterator();
    var rightIter = right.stream().iterator();
    var total = 0;

    while (leftIter.hasNext()) {
        total += Math.abs((leftIter.next() - rightIter.next()));
    }

    return total;
}
```

UPDATE: later, this annoyed me so much that I pinched the `ZippingSpliterator` from Protonpack, wrote an integer-optimised version of it, and rewrote the whole thing to:

```java
public int sumDifferences() {
    return Streams.zipToInt(
            left.stream(),
            right.stream(),
            (l, r) -> Math.abs(l - r)
    ).sum();
}
```

In part two, we multiply each number in the left column by the number of times that number appears in the right column, and sum the results. Now we need to do some lookups across columns.

It turns out to be fortuitous that we kept a map of counts for each column's numbers. We already have an easy way to answer the question "how many times does number *x* appear in this column?".

We expand `SortedSequence` to expose this information:

```java
public int getCount(T index) {
    return counts.getOrDefault(index, 0);
}

public Stream<Count<T>> streamCounts() {
    return counts.entrySet().stream()
            .map(e -> new Count<>(e.getKey(), e.getValue()));
}
```

and then the solution is easy to come by:

```java
public int calculateSimilarity() {
    return left.streamCounts().mapToInt(count ->
            count.item() * count.count() * right.getCount(count.item())
    ).sum();
}
```

A nice _O(n)_ implementation (given we've paid the _O(n log n)_ sorting costs already on insertion).

Sometimes in AOC the thing you do to make life easier in part 1 serendipitously helps you out in part 2, and sometimes it...doesn't.

## Day 2

My first pass at this was quick and dirty, because I had a Leaderboard to climb.

Having got my stars, I set about eliminating intermediate data structures. Each line in the puzzle input represents a series of integers, and we're interested in knowing whether the differences between integers in the series have the following properties:

1. They all have the same sign, either positive or negative.
2. They are all within the range 1-3 inclusive.

If we have an iterator containing the differences between levels, we can validate this in a single pass:

```java
private static boolean deltasAreSafe(PrimitiveIterator.OfInt deltas) {
    var sgn = 0;

    while (deltas.hasNext()) {
        var delta = deltas.next();
        var absDelta = Math.abs(delta);

        if (absDelta < 1 || absDelta > 3) return false;

        var newSgn = Integer.compare(0, delta);
        if (sgn != 0 && newSgn != sgn) return false;
        sgn = newSgn;
    }

    return true;
}
```

Constructing an iterator of deltas from an iterator of integers is easy enough:

```java
private static PrimitiveIterator.OfInt deltas(PrimitiveIterator.OfInt levels) {
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
```

You may ask, "why not use Streams here?" and the answer is that streams are awkward when you want to take the first item, then work on the remainder with a value such as `previous` that updates as you go along.

Now we have a simple way to determine whether an `integer` array of levels is safe:

```java
public boolean isSafe() {
    return deltasAreSafe(deltas(Arrays.stream(levels).iterator()));
}
```

but we can also generate all the "dampened" delta iterators without doing any array copying:

```java
public boolean isSafeWithDampening() {
    return isSafe() ||
            IntStream.range(0, levels.length)
                    .anyMatch(dropped ->
                            deltasAreSafe(
                                deltas(IntStream.range(0, levels.length)
                                    .filter(i -> i != dropped)
                                    .map(i -> levels[i])
                                    .iterator())));
}
```

I remain broadly unconvinced that Java's `Stream` API is the right one. Having `map`, `filter`, `reduce`, `collect` etc. defined on iterators would suit almost all the purposes they're actually used for just as well.