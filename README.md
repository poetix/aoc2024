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

The task at hand is relatively simple: pick out of each line everything that looks like either `mul(x, y)`, `do()` or `don't()`, then feed these instructions to a very simple little state machine:

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

private boolean endsInLoop(Point newObstacle) {
    Set<PathStep> visited = new HashSet<>();

    Point position = initialGuardPosition;
    Direction direction = Direction.NORTH;

    while (inGrid(position)) {
        PathStep step = new PathStep(position, direction);
        if (visited.contains(step)) return true;
        visited.add(step);

        var nextPosition = direction.addTo(position);
        while (hasObstacle(nextPosition) || nextPosition.equals(newObstacle)) {
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
private boolean endsInLoop(Set<PathStep> knownPath, PathStep currentStep, Point newObstacle) {
    Set<PathStep> newSteps = new HashSet<>();

    while (inGrid(currentStep.position()) ) {
        Direction nextDirection = currentStep.direction();
        Point nextPosition = nextDirection.addTo(currentStep.position());

        while (hasObstacle(nextPosition) || nextPosition.equals(newObstacle)) {
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

## Day 7

A software engineer has a problem which they attempt to solve using recursion. Now a software engineer has a problem with they attempt to solve using recursion and a software engineer has a problem which they attempt to solve using recursion. Now a software engineer has a problem which they attempt to solve using recursion and a software engineer has a problem which they attempt to solve using recursion and a software engineer has a problem which they attempt to solve using recursion and a software engineer has a problem which they attempt to solve using recursion...

This is one of those "pause for breath" problems. Here's the nub:

```java
record Equation(long result, long[] numbers) {

    public boolean isValid() {
        return isValid(numbers[0], 1);
    }

    public boolean isValid(long accumulator, int index) {
        if (index == numbers.length) {
            return result == accumulator;
        }
        if (accumulator > result) {
            return false;
        }
        return isValid(accumulator + numbers[index], index + 1)
                || isValid(accumulator * numbers[index], index + 1);
    }
}
```

Not that it makes much difference, but the `if (accumulator > result)` test lets us short-circuit the recursion if it's already impossible to get a valid answer.

I really thought they were going to make us do operator precedence rules in part 2. How would you do that? Split recursion and evaluation: use the recursion to build up complete expressions (`a + b * c` etc), and then evaluate them. A good use for `flatMap`, in fact:

```java
public Stream<String> possibleEquations() {
    return possibleExpressions(0).map(s -> result == evaluate(s)
        ? result + " == " + s
        : result + " != " + s);
}

private Stream<String> possibleExpressions(int index) {
    if (index == numbers.length - 1) return Stream.of(Long.toString(numbers[index]));
    return Stream.of(" + ", " * ").flatMap(operator ->
            possibleExpressions(index + 1).map(s -> numbers[index] + operator + s)
    );
}
```

Here's a simple left-to-right (so no operator precedence) evaluator for the expressions returned by `possibleExpressions`:

```java
private long evaluate(String expression) {
    String[] parts = expression.split("\\s+");
    return evaluate(Long.parseLong(parts[0]), parts, 1);
}

private long evaluate(long lhs, String[] parts, int index) {
    if (index == parts.length) return lhs;
    var rhs = Long.parseLong(parts[index + 1]);
    var newLhs = switch (parts[index]) {
        case "+" -> lhs + rhs;
        case "*" -> lhs * rhs;
        default -> 0;
    };
    return evaluate(newLhs, parts, index + 2);
}
 ```

If we run `possibleEquations` against the test input, we get:

```
190 != 10 + 19
190 == 10 * 19
3267 != 81 + 40 + 27
3267 == 81 + 40 * 27
3267 == 81 * 40 + 27
3267 != 81 * 40 * 27
83 != 17 + 5
83 != 17 * 5
156 != 15 + 6
156 != 15 * 6
7290 != 6 + 8 + 6 + 15
7290 != 6 + 8 + 6 * 15
7290 != 6 + 8 * 6 + 15
7290 != 6 + 8 * 6 * 15
7290 != 6 * 8 + 6 + 15
7290 != 6 * 8 + 6 * 15
7290 != 6 * 8 * 6 + 15
7290 != 6 * 8 * 6 * 15
161011 != 16 + 10 + 13
161011 != 16 + 10 * 13
161011 != 16 * 10 + 13
161011 != 16 * 10 * 13
192 != 17 + 8 + 14
192 != 17 + 8 * 14
192 != 17 * 8 + 14
192 != 17 * 8 * 14
21037 != 9 + 7 + 18 + 13
21037 != 9 + 7 + 18 * 13
21037 != 9 + 7 * 18 + 13
21037 != 9 + 7 * 18 * 13
21037 != 9 * 7 + 18 + 13
21037 != 9 * 7 + 18 * 13
21037 != 9 * 7 * 18 + 13
21037 != 9 * 7 * 18 * 13
292 != 11 + 6 + 16 + 20
292 != 11 + 6 + 16 * 20
292 == 11 + 6 * 16 + 20
292 != 11 + 6 * 16 * 20
292 != 11 * 6 + 16 + 20
292 != 11 * 6 + 16 * 20
292 != 11 * 6 * 16 + 20
292 != 11 * 6 * 16 * 20
```

Exercise for the reader: expand this to include the "||" concatenation operator. Exercise for the masochistic reader: make `evaluate` apply operator precedence rules.

## Day 8

We are certainly getting our money's worth out of `SparseGrid`. For this puzzle we are interested in groups of items on the grid: we want the positions of all those antennae represented by the same character. Let's add a default method to the `Grid` interface to provide this (regardless of the underlying implementation):

```java
default Map<T, List<Point>> populatedPositionsByContents() {
    return populatedSquares()
            .collect(Collectors.groupingBy(
                    Grid.Square::contents,
                    Collectors.mapping(Grid.Square::position, Collectors.toList())));
}
```

Now, for every list of positions of antennae of the same type, we want all possible pairs of antennae. Finding all the pairs in a list is a generic enough task that I'm going to put it in my `Streams` utility class:

```java
public static <I, T> Stream<T> pairsIn(List<I> items, BiFunction<I, I, T> toPair) {
    return IntStream.range(0, items.size()).boxed().flatMap(i -> {
        var first = items.get(i);
        return IntStream.range(i + 1, items.size()).mapToObj(j ->
                toPair.apply(first, items.get(j)));
    });
}
```

and then use it like so:

```java
private Stream<AntennaPair> antennaPairs(List<Point> positions) {
    return Streams.pairsIn(positions, AntennaPair::new);
}
```

We can use this to get a `Stream` of all possible pairs of antennae of the same type within the grid:

```java
private Stream<AntennaPair> antennaPairs() {
    return antennae.populatedPositionsByContents().values().stream()
            .flatMap(this::antennaPairs);
}
```

Now all we need to do is find the antinode positions for any given pair of antennae. We beef up `Point` with a couple of methods to help with this:

```java
public Point minus(Point other) {
    return new Point(x - other.x, y - other. y);
}

public Point plus(Point other) {
    return new Point(x + other.x, y + other.y);
}
```

Finding the Part 1 antinodes for any `AntennaPair` is now simple:

```java
public record AntennaPair(Point p1, Point p2) {
    Stream<Point> antinodesPart1(Predicate<Point> inBounds) {
        return Stream.of(
                        p2.plus(p2).minus(p1),
                        p1.plus(p1).minus(p2))
                .filter(inBounds);
    }
}
```

Part 2 is not much harder:

```java
Stream<Point> antinodesPart2(Predicate<Point> inBounds) {
    return Stream.concat(
            nodesFromTo(p1, p2, inBounds),
            nodesFromTo(p2, p1, inBounds));
}

private Stream<Point> nodesFromTo(Point from, Point to, Predicate<Point> inBounds) {
    var delta = to.minus(from);
    var nextAntinode = from;

    Stream.Builder<Point> result = Stream.builder();
    while (inBounds.test(nextAntinode)) {
        result.add(nextAntinode);
        nextAntinode = nextAntinode.plus(delta);
    }
    return result.build();
}
```

Putting it altogether to count unique antinode positions:

```java
public long countAntinodePositionsPart1() {
    return antennaPairs()
            .flatMap(pair -> pair.antinodesPart1(grid::isInBounds))
            .distinct()
            .count();
}

public long countAntinodePositionsPart2() {
    return antennaPairs()
            .flatMap(pair -> pair.antinodesPart2(grid::isInBounds))
            .distinct()
            .count();
}
```

This is, once again, a very `Stream` / `flatMap`-heavy approach. We break down the problem into a series of transformations of collections of things, grouping, filtering, de-duplicating and so on until we have the answer we want.

We don't have to create too many intermediate collections - there's a `Map` for grouping the antennae into by type, and `Stream.Builder` is probably populating a `List` of some sort under the hood, but everything else is value-by-value transformation on streams of values.

If you're used to writing nested `for`-loops, or are forced into doing so by [perverse language choices](https://go.dev/), then some of this might seem quite strange. It's a very functional style of programming, close to the way you might tackle this sort of problem in Haskell (`Stream.flatMap` is the Java equivalent of the "bind" operator `>>=` on Haskell's `List` monad). What it gets you, I think, is a way to break the problem down into small, easily-understood parts and then glue them together using a well-understood idiom.

## Day 9

GraalVM on an M2 Macbook Air is very forgiving of sub-optimal solutions, but somewhere deep down I _know_ that the Gods of [Big O Notation](https://en.wikipedia.org/wiki/Big_O_notation) are offended by my laxity.

So, I confess, my initial part 1 solution populated a big array of individual disk blocks and moved them around one block at a time, and actually this was fine. And my initial part 2 solution searched from left-to-right through a list of blank spaces until it found one of the right size for each file it was trying to relocate, and again, this was fine.

Also I calculated the checksum by running across the array and adding up the file ids attached to each block, multiplied by the block index; again, _fine_.

But we _can_ do better, and we _should_. We introduce a `FileRecord` which may represent the whole or a part of a file, and knows how to calculate the checksum for that whole or part:

```java
public record FileRecord(int fileId, int position, int length) {
    public long checksum() {
        return fileId * length *
                ((position * 2L) + length - 1) / 2L;
    }
}
```

Sometimes AOC asks you to remember some high-school math. Here we are using a formula (which I googled) for summing _N_ integers from _A_ to _B_ inclusive. This alone lets us off a few microseconds in Performance Purgatory.

By breaking `FileRecord`s up and moving the sub-ranges around we can compact everything without having to build or update an array of individual blocks: instead our unit of interest is the `FileRecord` itself. Here's the core of the part 1 algorithm:

```java
public long compactAndChecksum() {
    long checksum = 0;

    while (fileRecords.canCompactInto(blanks)) {
        checksum += (blanks.compactNext(fileRecords)).checksum();
    }

    return checksum + fileRecords.checksumRemainder();
}
```

Both `fileRecords` and `blanks` have to do a little bit of state-keeping, since either a file record or a blank might be split up during compaction. Given a current file record, and a current blank:

* If the current file record is larger than the current blank, then we split the current file record into a part which gets moved into the blank, filling it up, and a remainder which becomes the new current record.
* Otherwise, we move the entire current file record into the current blank, but if there is blank space left over then we update the current blank to be this remainder.

Here's the class that manages file records:

```java
static class FileRecords {

    public static FileRecords iteratingOver(List<FileRecord> records) {
        return new FileRecords(records.reversed().iterator());
    }

    private final Iterator<FileRecord> iterator;
    private FileRecord current;

    private FileRecords(Iterator<FileRecord> iterator) {
        this.iterator = iterator;
        this.current = iterator.hasNext() ? iterator.next() : null;
    }

    public boolean canCompactInto(Blanks blanks) {
        return current != null && blanks.hasCapacityBefore(current.position());
    }

    public FileRecord compactInto(Blanks blanks, int capacity) {
        var compacted = capacity > current.length()
                ? blanks.accept(current)
                : blanks.accept(current.resizeTo(capacity));

        current = compacted.length() < current.length()
                ? current.resizeTo(current.length() - capacity)
                : iterator.hasNext() ? iterator.next() : null;

        return compacted;
    }

    public long checksumRemainder() {
        long checksum = 0;
        if (current != null) checksum += current.checksum();

        while (iterator.hasNext()) {
            checksum += iterator.next().checksum();
        }

        return checksum;
    }
}
```

(This is somewhat wilfully written in as much of a "tell, don't ask" style as possible, without getters).

The class managing blanks is similar - it wraps an iterator, and allows the "head" of the iterator to be modified if an incoming file record doesn't completely consume a blank, or moved on to the next blank if it does:

```java
static class Blanks {

    static Blanks iteratingOver(List<Blank> blanks) {
        return new Blanks(blanks.iterator());
    }

    private final Iterator<Blank> iterator;
    private Blank current;

    private Blanks(Iterator<Blank> iterator) {
        this.iterator = iterator;
        this.current = iterator.hasNext() ? iterator.next() : null;
    }

    private int capacity() {
        return current == null ? 0 : current.length();
    }

    public FileRecord compactNext(FileRecords fileRecords) {
        return fileRecords.compactInto(this, capacity());
    }

    public boolean hasCapacityBefore(int position) {
        return current != null && current.position() < position;
    }

    public FileRecord accept(FileRecord record) {
        var result = record.moveTo(current.position());

        current = record.length() < capacity()
                ? current.remainderAfterPopulatingWith(record)
                : iterator.hasNext() ? iterator.next() : null;

        return result;
    }
}
```

You can see a sort of dance between the two where `fileRecords.canCompactInto(blanks)` sends its current position to `blanks.hasCapacityBefore(position)` to find out whether there are any unused blank regions to the left of the current file record. Similarly, rather than _ask_ing `blanks` what the size of the current blank region is, we make a call to `blanks.compactNext(fileRecords)` which calls `fileRecords.fillBlankIn(blanks, blankSize)` _tell_ing it what size of `FileRecord` it can accept.

This brings part 1 down to around 1-2ms, since we don't have to build any intermediate collections and are just iterating in two directions across two collections - left-to-right over `blanks`, and right-to-left over `fileRecords` - spitting out compacted records and summing their checksums as we go. The Gods of Big O Notation smile upon us.

Part 2 is rather different. The core of it is this:

```java
static class FileCompactor {

    private final List<FileRecord> fileRecords;
    private final BlankTable blankTable;

    FileCompactor(List<FileRecord> fileRecords, BlankTable blankTable) {
        this.fileRecords = fileRecords;
        this.blankTable = blankTable;
    }

    public long compactByFileAndChecksum() {
        return fileRecords.reversed().stream()
                .map(this::tryMoveRecord)
                .mapToLong(FileRecord::checksum)
                .sum();
    }

    private FileRecord tryMoveRecord(FileRecord record) {
        return blankTable.takeFirst(record.length(), record.position())
                .map(blank -> moveRecord(record, blank))
                .orElse(record);
    }

    private FileRecord moveRecord(FileRecord record, Blank blank) {
        var remainingLength = blank.length() - record.length();

        if (remainingLength > 0) {
            blankTable.add(
                    new Blank(
                            blank.position() + record.length(),
                            remainingLength));
        }

        return record.moveTo(blank.position());
    }
}
```

We reverse iterate through our file records, attempting to move each into the first suitable-sized blank in our blank table, and updating the blank table with leftover space as we go. All the important book-keeping is actually going on inside `BlankTable`, which keeps track of what blanks are where and, most importantly, tries to find the next usable one efficiently. Here's what that looks like:

```java
static class BlankTable {

    private final NavigableMap<Integer, NavigableSet<Blank>> blanksByLengthAndPosition = new TreeMap<>();

    public void add(Blank blank) {
        blanksByLengthAndPosition.compute(blank.length(), (ignored, v) -> {
            var s = v == null ? new TreeSet<>(Comparator.comparing(Blank::position)) : v;
            s.add(blank);
            return s;
        });
    }

    private void prune(int maxPosition) {
        var lengthIter = blanksByLengthAndPosition.values().iterator();

        while (lengthIter.hasNext()) {
            var positions = lengthIter.next();
            var positionIter = positions.descendingIterator();

            while (positionIter.hasNext()) {
                if (positionIter.next().position() < maxPosition) break;
                positionIter.remove();
            }

            if (positions.isEmpty()) {
                lengthIter.remove();
            }
        }
    }

    public Optional<Blank> takeFirst(int minLength, int maxPosition) {
        prune(maxPosition);
        return blanksByLengthAndPosition.subMap(minLength, 10).entrySet().stream()
                .min(Comparator.comparing(e -> e.getValue().getFirst().position()))
                .map(e -> e.getValue().removeFirst());
    }
}
```

The important thing here is that the `NavigableMap` storing `blanksByLengthAndPosition` organises the blanks in the table into `NavigableSet`s, ordered by position, which in turn are ordered by length. So we ask (using `subMap`, which partitions the binary tree keeping the keys in order) for blanks of length at least 3, and get ordered sets of blanks of length 3, 4, 6 etc - whatever's left in the table. Then we pick the set that has the initial blank with the lowest position number, remove that blank from that set and return it.

The `prune` method does some necessary tidying as we go along: it throws away blanks we can't use any more (because we're now considering file records to the left of where they start), and any empty sets, so that if there are no blanks of size 3 in the table, there is no entry (rather than an empty set of blanks) under "3" in the table.

For comparison, here's a much simpler `LinearSearchBlankTable`, which doesn't do any of this organising:

```java
static class LinearSearchBlankTable {

    private SortedSet<Blank> blanks = new TreeSet<>(Comparator.comparing(Blank::position));

    public void add(Blank blank) {
        blanks.add(blank);
    }

    public Optional<Blank> takeFirst(int minLength, int maxPosition) {
        return blanks.stream()
                .filter(b -> b.length() >= minLength && b.position() < maxPosition)
                .findFirst()
                .map(blank -> {
                    blanks.remove(blank);
                    return blank;
                });
    }

}
```

Substituting this in takes the part 2 run time from around 40+ms to around 200+ms, because we keep having to zoom along from the beginning to find the first eligible blank.

All of this does feel like a _weighty_ solution to the problem - there's so much more code here than I wrote for previous puzzles - and I do wonder whether I'm missing something more elegant.

## Day 10

Not much at all to say about this one, except that it was about the shortest path possible from part 1 to part 2 (I lost a bit of time through not having read the description properly, and incorrectly submitting part 2's answer for part 1...)

I made an assumption about memoisation - that we would want to cache the valid trails from every visited point, as we would surely revisit them. As it happens, no, not really: turns out it's cheaper not to build and store a bunch of collections but just to `Stream.flatMap` your way through all the paths and de-dup at the end.

A LISP-like cons cell is a useful thing to have if you're often making copies of a collection with one item prepended to it. We keep track of the final item to save traversals:

```java
record Trail(Point head, Point last, Trail tail) {
    static Trail of(Point head) {
        return new Trail(head, head, null);
    }

    public Trail cons(Point head) {
        return new Trail(head, last, this);
    }
}
```

Here's how it's used in building the trails:

```java
public Stream<Trail> trailsFrom(Point start) {
    int value = valueAt(start);
    if (value == 9) return Stream.of(Trail.of(start));

    var nextValue = value + 1;
    return getAdjacent(start)
            .filter(adjacentPoint -> valueAt(adjacentPoint) == nextValue)
            .flatMap(this::trailsFrom)
            .map(trail -> trail.cons(start));
}
```

and here's how we calculate our results:

```java
public long sumScores() {
    return starts.stream().mapToLong(start ->
        trailsFrom(start)
                .map(Trail::last)
                .distinct()
                .count())
        .sum();
}

public long sumRatings() {
    return starts.stream().mapToLong(start ->
        trailsFrom(start)
                .distinct()
                .count())
    .sum();
}
```

You might think "shouldn't we de-dup as we go along so the final iteration set is smaller?" and the answer is that it makes _next to no difference at all_.

At this point, a complete run of days 1-10 without any warming up takes around 0.8 seconds. The slowest by far are days 6 (216ms) and 7 (469ms) - all the rest range between 4 and 39ms for both parts.

## Day 11

First puzzle in which the naive solution to part 1 explodes unmanageably in part 2. Yay! I could have seen this coming, but also I'm chasing Leaderboard positions so I did things the dumb-as-rocks way first because it was quick to implement. 

There are two ways of getting part 2 to finish in a reasonable amount of time. The first is to work with a table of counts of stones by number. This enables you to reduce duplicates of the same number to a single entry in the table, and process them all in one go, nicely containing the explosion.

To give you some idea of how effective that is, at the end of the 75th generation using this method we had observed 3900 distinct stone values, with the greatest number of stones of a single value at any one time being 13,167,382,607,633. Part 2 using this method takes around 30ms.

The second way is a dynamic-programming inspired approach. For any given stone value, the number of stones we will have after _n_ generations of blinking at just that stone and its descendants is equal to the number of stones we will have after blinking _n - 1_ times at each of its immediate children. This suggests a memoisation approach: remember the count for each stone number and generation number, and populate the cache starting with the last generation number and working backwards.

Here's what that looks like, storing the counts in primitive `long` arrays for speedy access:

```java
static class MemoisingStoneAutomaton {

    private final Map<Long, long[]> countsByStoneAndTimes = new HashMap<>();

    public long countAllAfter(Stream<Long> stones, int times) {
        return stones.mapToLong(
                stone -> countAfter(stone, times)
        ).sum();
    }
    
    private long countAfter(long stone, int times) {
        if (times == 0) return 1;

        long[] memoised = countsByStoneAndTimes.computeIfAbsent(stone, (ignored) -> new long[75]);
        var nextIndex = times - 1;
        var result = memoised[nextIndex];
        if (result > 0) return result;

        result = countAfterUncached(stone, times);
        memoised[nextIndex] = result;
        return result;
    }

    private long countAfterUncached(long stone, int times) {
        if (stone == 0) {
            return countAfter(1L, times - 1);
        }

        String repr = Long.toString(stone);
        if (repr.length() % 2 == 0) {
            String l = repr.substring(0, repr.length() / 2);
            String r = repr.substring(repr.length() / 2);
            return countAfter(Long.parseLong(l), times - 1) +
                        countAfter(Long.parseLong(r), times - 1);
        }

        return countAfter(stone * 2024, times - 1);
    }
}
```

This was the fastest I could make it, coming in at around 12ms with a bit of warmup for part 2.

I took a count of cache misses (130,224) and cache hits (69562), which may seem underwhelming, but each "hit" means we potentially escape a _lot_ of recursion...

## Day 12

Today's puzzle breaks down into a number of reasonably straightforward sub-tasks.

1. Find all the connected subgraphs of the graph where the nodes are co-ordinates in the grid, and there is an edge between any two co-ordinates with the same grid symbol.
2. (Part 1) Measure the perimeter of each connected subgraph, and sum these measurements.
3. (Part 2) Count the distinct verticss of the perimeter of each connected subgraph, and sum these counts.

It seems a lot of people did part 2 by counting corners, which makes sense. I did it another way, which ran very slightly faster in my implementation. If you take the squares outside but adjacent to each connected region (which I already did to get my answer for part 1), you can find "fence segments" either North, South, East or West of each square. Group these segments by latitude or longitude, then order them, and you can scan each vertical or horizontal line of segments counting breaks to obtain a final total of continuous runs of fence segments.

Here's what that looks like:

```java
long perimeterSides() {
    return Direction.nsew().mapToLong(this::perimeterSidesForDirection).sum();
}

private long perimeterSidesForDirection(Direction d) {
    Function<Point, Integer> index = d.isVertical() ? Point::y : Point::x;
    Function<Point, Integer> subIndex = d.isVertical() ? Point::x : Point::y;

    Map<Integer, SortedSet<Integer>> byDirection = perimeterPoints.stream()
            .filter(p -> points.contains(d.addTo(p)))
            .collect(groupingBy(
                    index,
                    mapping(subIndex, toCollection(TreeSet::new))));

    return byDirection.values().stream().mapToLong(this::countDiscrete).sum();
}

private long countDiscrete(SortedSet<Integer> positions) {
    int count = 1;
    var iter = positions.iterator();
    var current = iter.next();
    while (iter.hasNext()) {
        var next = iter.next();
        if (next - current > 1) count++;
        current = next;
    }
    return count;
}
```

For each of N, S, E and W we find all the perimeter points that have a point within the region adjacent to them in that direction. We organise these by co-ordinate, based on whether the fence segments for that position are vertical or horizontal, and sorting as we go so that we can traverse from left to right, or top to bottom, in order looking for breaks.

I tried re-using the "deltas of a `PrimitiveIterator.OfInt`" code from day 2 to do `countDiscrete` with, and lol:

```java
private long countDiscrete(SortedSet<Integer> positions) {
    var deltas = Iterators.deltas(positions.stream().mapToInt(i -> i).iterator());
    var spliterator = Spliterators.spliterator(deltas, positions.size(), Spliterator.SIZED & Spliterator.ORDERED);
    return StreamSupport.intStream(spliterator, false).filter(d -> d > 1).count() + 1L;
}
```

It was slower, too.

UPDATE: Someone clever pointed out that the number of vertical sides must be equal to the number of horizontal sides, which means we can do half as many counts and simplify the logic a little:

```java
long perimeterSides() {
    return Stream.of(Direction.WEST, Direction.EAST)
            .mapToLong(this::perimeterSidesForDirection)
            .sum() * 2;
}

private long perimeterSidesForDirection(Direction d) {
    Map<Integer, SortedSet<Integer>> byDirection = perimeterPoints.stream()
            .filter(p -> points.contains(d.addTo(p)))
            .collect(groupingBy(
                    Point::x,
                    mapping(Point::y, toCollection(TreeSet::new))));

    return byDirection.values().stream().mapToLong(this::countDiscrete).sum();
}
```

A pretty nice result!