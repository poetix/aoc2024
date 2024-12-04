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

Having got my stars, I set about eliminating intermediate data structures. Each line in the puzzle input represents a series of integers, and we're interested in knowing whether the differences between adjacent integers in the series have the following properties:

1. They all have the same sign, either positive or negative.
2. They are all within the range 1-3 inclusive.

An array of integers ("levels") is deemed "safe" if these conditions hold on their deltas. If we have an iterator containing just these deltas, we can validate this in a single pass:

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

In part 2, a sequence of levels is also safe if any subsequence from which a single item has been removed ("dampened") is safe. We can also generate all the "dampened" delta iterators without doing any array copying:

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

I remain broadly unconvinced that Java's `Stream` API is the right one for general-purpose usage. Having `map`, `filter`, `reduce`, `collect` etc. defined on iterators would suit almost all the tasks they're actually used for just as well. Parallelisation with "spliterators" seems like a special case which should have been designed for separately.

One of the things that makes Python especially good for AOC-type puzzles is the availability of a very efficient internal language for building and transforming iterators without creating intermediate collections. For comparison, here's a Python solution making use of generator functions and comprehensions:

```python
def levels(line):
  return [int(s) for s in line.split()]

with open("src/test/resources/day2.txt") as file:
  records = [levels(line) for line in file.readlines()]

def deltas(levels):
  try:
    prev = next(levels)
  except StopIteration:
    return
  for item in levels:
    yield item - prev
    prev = item

def is_safe(deltas):
  sgn = 0
  for delta in deltas:
    absDelta = abs(delta)
    if (absDelta < 1 or absDelta > 3):
      return False
    newSgn = 1 if delta > 0 else -1
    if (sgn != 0 and newSgn != sgn):
      return False
    sgn = newSgn
  return True

def dampened(levels, index):
  return (levels[i] for i in range(len(levels)) if i != index)

def is_safe_with_dampening(levels):
  if is_safe(deltas(iter(levels))):
    return True
  return any(is_safe(deltas(dampened(levels, i))) for i in range(len(levels)))

print(sum(1 for record in records if (is_safe(deltas(iter(record))))))
print(sum(1 for record in records if (is_safe_with_dampening(record))))
```

## Day 3

At last, an opportunity to use [sealed interfaces](https://docs.oracle.com/en/java/javase/17/language/sealed-classes-and-interfaces.html) and [record-matching](https://docs.oracle.com/en/java/javase/23/language/record-patterns.html) inside `switch`.

The task at hand is relatively simple: pick out of a each line everything that looks like either `mul(x, y)`, `do()` or `don't()`, then feed these instructions to a very simple little state machine:

Here's the sealed interface:

```java
public sealed interface Instruction permits
        Instruction.Mul,
        Instruction.Do,
        Instruction.Dont {
    
    record Mul(int lhs, int rhs) implements Instruction { }

    record Do() implements Instruction { }

    record Dont() implements Instruction { }

}
```

And here's the state machine:

```java
public final class MultiplierState {

    private boolean enabled = true;
    private int sum = 0;

    public void interpret(Instruction instruction) {
        switch (instruction) {
            case Instruction.Do() -> enabled = true;
            case Instruction.Dont() -> enabled = false;
            case Instruction.Mul(int lhs, int rhs) -> {
                if (enabled) sum += (lhs * rhs);
            }
        }
    }

    public int getSum() {
        return sum;
    }
}
```

We could have done this the old-fashioned way, by having `MultiplierState` expose `enable()`, `disable()` and `addIfEnabled(int value)` methods, and writing our `Instruction`s like this:

```java
import com.codepoetics.aoc2024.MultiplierState;

interface Instruction {
    void apply(MultiplierState state);
}

record Do() implements Instruction {
    @Override
    public void apply(MultiplierState state) {
        state.enable();
    }
}

record Dont() implements Instruction {
    @Override
    public void apply(MultiplierState state) {
        state.disable();
    }
}

record Mul(int lhs, int rhs) implements Instruction {
    @Override
    public void apply(MultiplierState state) {
        state.addIfEnabled(lhs * rhs);
    }
}
```

So, which is better? Well, in this case, I like that the `sealed` interface and the `switch`-based interpreter makes `MultiplierState` a black box which accepts instructions and interprets them according to its own internal logic, rather than exposing its mechanisms for external clients to tinker with.

I should mention the other classic OOP approach, which is to use the [Visitor Pattern](https://en.wikipedia.org/wiki/Visitor_pattern):

```java
import com.codepoetics.aoc2024.MultiplierState;

interface Instruction {
    void accept(MultiplierStateVisitor state);
}

record Mul(int lhs, int rhs) implements Instruction {
    @Override
    public void accept(MultiplierStateVisitor visitor) {
        state.visit(this);
    }
}

record Do() implements Instruction {
    @Override
    public void accept(MultiplierStateVisitor visitor) {
        state.visit(this);
    }
}

record Dont() implements Instruction {
    @Override
    public void accept(MultiplierStateVisitor visitor) {
        state.visit(this);
    }
}

interface MultiplierStateVisitor {
    void visit(Mul mulInstr);
    void visit(Do doInstr);
    void visit(Dont dontInstr);
}
```

Here, the visitor still gets to encapsulate what each instruction type actually does to its internal state, but the mechanics of dispatch feel kind of clunky. The general consensus seems to be that [sum types](https://en.wikipedia.org/wiki/Algebraic_data_type) like sealed interfaces make this approach unnecessary in most cases.

Other than that, the only other thing of note is the [regex](https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html) with subgroups, which is a handy thing to know how to do:

```java
Pattern PATTERN = Pattern.compile("mul\\((\\d+),(\\d+)\\)|do\\(\\)|don't\\(\\)");

static Stream<Instruction> parseLine(String line) {
    var matcher = PATTERN.matcher(line);

    return matcher.results().map(result -> {
        var token = result.group(0);
        if (token.startsWith("mul")) {
            return new Mul(
                    Integer.parseInt(result.group(1)),
                    Integer.parseInt(result.group(2)));
        } else if (token.equals("do()")) {
            return new Do();
        } else {
            return new Dont();
        }
    });
}
```

By wrapping the digits inside `mul\((\d+),(\d+)\)` in brackets, we identify them as sub-groups which can be pulled out of the match result, e.g.

```
result.group(0) == "mul(123,456)
result.group(1) == "123"
result.group(2) == "456"
```

That's it for today; if past years are anything to go by, later challenges may involve building more complex state machines with more instruction types, so this is a good pattern to have under one's belt.

## Day 4

Hello to the first Grid question of the season. Anticipating some of the things you typically have to do with these, I implemented a couple of utility classes, `Point`, `Direction`, `SparseGrid` and `DenseGrid`.

A `DenseGrid<T>` represents a grid's contents with a nested array, while a `SparseGrid<T>` uses a map of `Point`s to `T`s. I wrote and used `SparseGrid` first, wanting to be lazy about bounds-checking, then realised I could implement a dense grid with the same contract and use that instead.

Anyway, there's not much to say about this one really. We look for a string starting at a given `Point` and seeking in a given `Direction` like this:

```java
private boolean seek(String s, Point position, Direction direction) {
    Point cursor = position;
    for (char c : s.toCharArray()) {
        Character charAt = grid.get(cursor);
        if (charAt == null || c != charAt) return false;
        cursor = direction.addTo(cursor);
    }
    return true;
}
```

Once you have that, counting "XMAS"-es is trivial:

```java
private long countXmasesAt(Point position) {
    return Arrays.stream(Direction.values())
            .filter(direction -> seek("XMAS", position, direction))
            .count();
}

public long countXmases() {
    return grid.populatedPositions().mapToLong(this::countXmasesAt).sum();
}
```

and counting "X-MAS"-es is not much less trivial:

```java
import com.codepoetics.aoc2024.Direction;

public boolean hasCrossAt(Point position) {
    return Stream.of(Direction.NORTHEAST, Direction.NORTHWEST)
            .allMatch(diagonal ->
                    seek("MAS", diagonal.addTo(position), diagonal.inverse())) ||
                    seek("MAS", diagonal.inverse().addTo(position), diagonal);
}

public long countCrosses() {
    return grid.populatedSquares()
            .filter(entry -> entry.contents() == 'A'
                    && hasCrossAt(entry.position()))
            .count();
}
```