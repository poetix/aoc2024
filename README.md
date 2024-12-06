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

You may ask, "why not use Streams here?" and the answer is that streams are awkward when you want to take the first item, then work on the remainder with a value such as `previous` that pageLists as you go along.

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

## Day 5

Today's puzzle is about ordering page numbers given a set of dependencies (page X must come before page Y). This resembles an interview question I used to set (order a collection of build dependencies into a build order), but is actually simpler to solve than that, as every set of pages has only one valid order.

The trick is to organise the dependencies into a lookup: for each page number, what set of pages must come before that page? Here's how we do that:

```java
public static class Ordering {

    private final Map<Integer, Set<Integer>> previousByNext = new HashMap<>();

    public void add(int prev, int next) {
        previousByNext.compute(next, (ignored, previous) -> {
            var result = previous == null ? new HashSet<Integer>() : previous;
            result.add(prev);
            return result;
        });
    }
}
```

In solving part one, I initially scanned each list of pages, checking for each page number that the preceding page numbers in the list satisfied these constraints. But once you have part two, you can use it to solve part one, so let's pretend we knew that all along.

For part two, we need to reorder invalid lists of pages. Is this a graph traversal problem? No, it's a sort. First, we need `Ordering` to tell us how _many_ of the pages in a given list are required to be previous to a given page:

```java
public long countPreviousIn(int page, Set<Integer> pages) {
    var previous = previousByNext.get(page);
    return previous == null ? 0 : previous.stream()
            .filter(pages::contains)
            .count();
}
```

Then we use that to sort the pages in our list:

```java
public PageList reorder(Ordering ordering) {
   var unorderedPages = Arrays.stream(pages).boxed().collect(Collectors.toSet());

   Comparator<Integer> byNumberOfPreviousPages = Comparator.comparing(page ->
           ordering.countPreviousIn(page, unorderedPages)
   );

   return new PageList(unorderedPages.stream()
           .sorted(byNumberOfPreviousPages)
           .mapToInt(Integer::valueOf)
           .toArray());
}
```

The reasoning here is that because the page-order dependencies determine a single correct ordering, the first page will have no pages in the list that must come before it, the second will have one, the third will have two, and so on.

Now we can retroactively figure out which lists of pages are in the right order and which aren't, with a simple equality test between the ordered and unordered page sets:

```java
private boolean isValid(Update pageList) {
    return pageList.equals(pageList.reorder(ordering));
}
```

We just need to equip `PageList` with a working `equals` method (Java isn't clever enough to notice when an array-typed field will break the one it generates for `record`s), and extract the middle values so we can add them up to get our puzzle answers:

```java
 @Override
public boolean equals(Object other) {
    return other instanceof PageList &&
            Arrays.equals(pages, ((PageList) other).pages);
}

public int middleValue() {
    return pages[pages.length / 2];
}
```

and then it's the usual map/filter/sum:

```java
public long validUpdatesSum() {
    return pageLists.stream()
            .filter(this::isValid)
            .mapToInt(PageList::middleValue)
            .sum();
}

public long reorderedUpdatesSum() {
    return pageLists.stream()
            .filter(pageList -> !isValid(pageList))
            .map(pageList -> pageList.reorder(ordering()))
            .mapToInt(PageList::middleValue)
            .sum();
}
```

Did I start out by writing the graph traversal algorithm from my interview question, then realise I was being silly and throw it away again? I did. But actually it gave me a useful intuition, which was to build the initial lookup map. Only the traversal itself turned out to be unnecessary.

## Day 6

Welcome again to our friend `SparseGrid`! I found this puzzle _tricksy_, to the point where having reached a successful solution I then tried to tidy it up and managed to break it quite mysteriously.

Part 1 is simple enough, and we have some of the bits we need already:

```java
public Set<Point> guardPath() {
    Set<Point> visited = new HashSet<>();

    Point position = initialGuardPosition;
    Direction direction = Direction.NORTH;

    while (inGrid(position)) {
        Point nextPosition = direction.addTo(position);

        while (hasObstacle(nextPosition)) {
            direction = direction.rotate90Right();
            nextPosition = direction.addTo(position);
        }

        visited.add(position);
        position = nextPosition;
    }

    return visited;
}

public int guardPathSize() {
    return guardPath().size();
}
```

Simple enough: start at the initial position, facing North. Until you leave the grid, keep moving in the direction you're facing, unless there's an obstacle there, in which case rotate 90 degrees to the right until there isn't and then move. Keep track of the positions you've visited in a set, so we don't count multiple visits to the same position (i.e. moving in different directions) more than once.

To reach a naive solution we just need to try placing an obstacle at every position in the discovered route except the starting position, then running the route from the beginning and seeing if it gets into a loop with that obstacle in place. To do the loop detection we need to keep track not only of which positions we've visited, but which direction we were going in when we visited them (since routes do sometimes cross over or double back on themselves). Here's what that looks like:

```java
record PathStep(Point position, Direction direction) {}

private boolean endsInLoop(Point obstacle) {
    Set<PathStep> visited = new HashSet<>();

    Point position = initialGuardPosition;
    Direction direction = Direction.NORTH;

    while (inGrid(position)) {
        PathStep step = new PathStep(position, direction);
        if (visited.contains(step)) return true;
        visited.add(step);

        var nextPosition = direction.addTo(position);
        while (hasObstacle(nextPosition) || nextPosition.equals(obstacle)) {
            direction = direction.rotate90Right();
            nextPosition = direction.addTo(position);
        }

        position = nextPosition;
    }

    return false;
}

public long countObstaclePositions() {
    return guardPath().stream()
            .filter(p -> !p.equals(initialGuardPosition) &&
                    endsInLoop(p))
            .distinct()
            .count();
}
```

Can we do a little better than that, in terms of efficiency? Well, yes, but here devils lie in wait.

The basic idea is that as we run through the path placing obstacles, we are already building up some of the set of path steps that the guard will have taken before reaching each obstacle we try to place, so we don't need to re-run the path from the initial position to check if an obstacle sends the guard into a loop. We can rewrite `endsInLoop` to take this already-gathered information into account:

```java
private boolean endsInLoop(Set<PathStep> knownPath, PathStep currentStep, Point obstacle) {
    Set<PathStep> newSteps = new HashSet<>();

    while (inGrid(currentStep.position()) ) {
        Direction nextDirection = currentStep.direction();
        Point nextPosition = nextDirection.addTo(currentStep.position());

        while (hasObstacle(nextPosition) || obstacle.equals(nextPosition)) {
            nextDirection = nextDirection.rotate90Right();
            nextPosition = nextDirection.addTo(currentStep.position());
        }

        newSteps.add(currentStep);
        currentStep = new PathStep(nextPosition, nextDirection);
        if (knownPath.contains(currentStep) || newSteps.contains(currentStep)) return true;
    }

    return false;
}
```

The `knownPath` parameter captures steps we've taken already; the `currentStep` parameter captures where we are and what direction we're facing. We proceed from there, looking in both the `knownPath` and `newSteps` collections to see if we've looped back on ourselves.

To make this work, we need our `guardPath` function to return an ordered list of `PathStep`s rather than just a set of positions visited:

```java
public List<PathStep> guardPath2() {
    List<PathStep> path = new ArrayList<>();

    PathStep currentStep = new PathStep(initialGuardPosition, Direction.NORTH);

    while (inGrid(currentStep.position)) {
        var nextDirection = currentStep.direction();
        Point nextPosition = nextDirection.addTo(currentStep.position());

        while (hasObstacle(nextPosition)) {
            nextDirection = nextDirection.rotate90Right();
            nextPosition = nextDirection.addTo(currentStep.position());
        }

        path.add(currentStep);
        currentStep = new PathStep(nextPosition, nextDirection);
    }

    return path;
}
```

And now comes the bit where I mysteriously messed it up. What's wrong with this function?

```java
public int countObstaclePositions2(List<PathStep> guardPath) {
    Set<PathStep> pathSoFar = new HashSet<>();
    Set<Point> obstacles = new HashSet<>();

    PathStep currentStep = guardPath.getFirst();
    for (PathStep nextStep : guardPath.subList(1, guardPath.size())) {
        if (endsInLoop(pathSoFar, currentStep, nextStep.position())) {
            obstacles.add(nextStep.position());
        }
        pathSoFar.add(currentStep);
        currentStep = nextStep;
    }

    return obstacles.size();
}
```

It looks fine! It returns the same result for the test input!! It returns a different answer for the full input!!!

The problem is this: whether an obstacle sends us into a loop depends on what direction we're facing when we hit it. If a path crosses over itself, then an obstacle encountered later in the path will send us in a different direction to an obstacle encountered earlier in the path, with potentially different results. We can get a false positive if, on first encounter with the obstacle, the guard would take a non-looping path, but if they proceeded through it and came up against it from a different direction later in their path it would send them in to a loop.

So, to eliminate these false positives we have to eliminate obstacle positions we've already tried:

```java
public int countObstaclePositions2(List<PathStep> guardPath) {
    Set<PathStep> pathSoFar = new HashSet<>();
    Set<Point> obstacles = new HashSet<>();
    Set<Point> tried = new HashSet<>();

    PathStep currentStep = guardPath.getFirst();
    for (PathStep nextStep : guardPath.subList(1, guardPath.size())) {
        if (!tried.contains(nextStep.position) &&
                endsInLoop(pathSoFar, currentStep, nextStep.position())) {
            obstacles.add(nextStep.position());
        }
        tried.add(nextStep.position);
        pathSoFar.add(currentStep);
        currentStep = nextStep;
    }

    return obstacles.size();
}
```

_Now_ it works. But for the sake of getting the star, you might as well go with the naive version - there's a difference of perhaps 250ms in it.