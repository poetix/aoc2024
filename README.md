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
import com.codepoetics.aoc2024.grid.Direction;

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

## Day 13

I got stuck on this one, and asked an LLM for help finding a solution. It gave me an answer that worked, but that I didn't entirely understand. Later on, taking my daughter to nursery, I figured out the real nature of the problem, and wondered whether I could work back from that to understanding why the LLM's answer was the right one.

Let's forget for a moment about the fact that you can only push a button a whole-numbered number of times, and think of the two buttons as  _vectors_. One way to think of a vector is as the combination of a direction and a velocity. The two vectors (1, 2) and (3, 6) describe the same direction at different velocities, whereas the vectors (1, 2) and (6, 3) describe different directions.

Starting at the origin (0, 0) we want to reach a point (x, y). We have two vectors we can scale by separate amounts _n_ and _m_ and add together to get there. Now one of the following must be true:

* The two vectors point in the same direction:
  * However we scale them, when we add them together we get a new vector pointing in that direction.
  * Either there is something we can multiply that vector by to get (x, y) or there isn't:
    * If there isn't, we can't reach the prize.
    * If there is, there are _many_ combinations of Button A and Button B that will get us there, _but_ either:
      * Button A moves the claw more than 3 times as fast as button B, so the extra cost is worth it and we should just push A for the cheapest win, or
      * Button A moves the claw less than three times as fast as button B, in which case we should just push button B.
* The two vectors point in different directions:
  * If there is a solution:
    * We will reach it along a line passing through (x, y) in the direction described by the second vector.
    * This line will intersect with the line passing through the origin in the direction described by the first vector.
    * This point of intersection is the _only_ solution.

Now as it happens my puzzle input had no pairs of vectors pointing in the same direction, so it boiled down to finding the line intersections in every case. There also weren't any cases where you'd have to move claw A _backwards_ before moving claw B _forwards_ (or vice versa), which further simplified things.

What did the LLM tell me? It told me to use some matrix maths. The problem could be pictured as a matrix equation:

```math
\begin{bmatrix}
x_1 & x_2 \\
y_1 & y_2
\end{bmatrix}
\begin{bmatrix}
n \\
m
\end{bmatrix}
=
\begin{bmatrix}
x_3 \\
y_3
\end{bmatrix}.
```

The first step was to find the _determinant_ of the matrix on the left:

```math
\text{If } A = \begin{bmatrix} a & b \\ c & d \end{bmatrix}, \text{ then } \det(A) = ad - bc.
```

If the determinant was non-zero that meant there was a single solution, and we could use [Cramer's Rule](https://en.wikipedia.org/wiki/Cramer%27s_rule) to find the values of _n_ and _m_ (how many times to push buttons A and B respectively).

OK, so what does this mean?

Well, first of all, how can we tell if two vectors point in the same direction? Let's say our vectors are (x1, y1) and (x2, y2). They point in the same direction if there is some _n_ such that n * x1 = x2 and n * y1 = y2. In that case, the determinant will be zero!

```math
\text{If } A = \begin{bmatrix} x_1 & nx_1 \\ y_1 & ny_1 \end{bmatrix}, \text{ then } \det(A) = (x_1 . n . y_1) - (n . x_1 . y_1) = 0.
```

In the case that the determinant is always non-zero (as it was for my puzzle input), we just apply Cramer's Rule every time:

```math
n = \frac{\det(A_n)}{\det(A)}, \quad m = \frac{\det(A_m)}{\det(A)},
```

where:
```math
A_n = \begin{bmatrix}
x_3 & x_2 \\
y_3 & y_2
\end{bmatrix}, \quad
A_m = \begin{bmatrix}
x_1 & x_3 \\
y_1 & y_3
\end{bmatrix}.
```

If we expand this out for _n_ and _m_, we get:

```math
n = \frac{ x_3y_2 - x_2y_3 }{ x_1y_2 - x_2y_1 }, \quad
m = \frac{ y1_y3 - x_3y_1 }{ x1_y2 - x_2y_1 }
```

where

```math
n(x_1, y_1)
```
just happens to be the intersection of the two lines, and

```math
n(x_1, y_1) + m(x_2, y_2) = (x_3, y_3)
```

Now we just need to check that _n_ and _m_ are positive whole numbers and we have our unique answer.

## Day 14

I wrote part 1 in the expectation that if you had to quadrisect a region once to get the first answer, you might need to quadrisect it again (and again) to get the second.

```java
record BoundingBox(Point topLeft, Point dimensions) {

    public Stream<BoundingBox> quadrisect() {
        var newDimensions = new Point(dimensions.x() / 2, dimensions().y() / 2);
        return Stream.of(
                new BoundingBox(topLeft, newDimensions),
                new BoundingBox(topLeft().plus(new Point(newDimensions.x() + 1, 0)), newDimensions),
                new BoundingBox(topLeft().plus(new Point(0, newDimensions.y() + 1)), newDimensions),
                new BoundingBox(topLeft().plus(new Point(newDimensions.x() + 1, newDimensions.y() + 1)), newDimensions)
        );
    }

    public long countIn(SortedMap<Long, SortedMap<Long, Long>> map) {
        return map.subMap(topLeft.x(), topLeft.x() + dimensions.x())
                .values().stream()
                .mapToLong(byY ->
                        byY.subMap(topLeft.y(), topLeft().y() + dimensions.y())
                                .values().stream().mapToLong(l -> l).sum()
                ).sum();
    }
}
```

Note that the `countIn` method accepts a map of counts by grid position which is already sorted by position, meaning we can efficiently grab the sub-maps of positions that lie within a given range.

For part 2, I added a `density` method:

```java
public double density(SortedMap<Long, SortedMap<Long, Long>> map) {
    return ((double) countIn(map)) / (dimensions().x() * dimensions().y());
}
```

This enables us to search for regions of the grid that have a suspiciously higher-than-average density of points in them, like this:

```java
public boolean containsClusterAfter(int moves) {
    var counts = countsOfPositionsAfter(moves);
    double avgDensity = frame.density(counts);
    double threshold = avgDensity * 8.0;

    return frame.quadrisect()
            .flatMap(BoundingBox::quadrisect)
            .flatMap(BoundingBox::quadrisect)
            .anyMatch(q -> q.density(counts) > threshold);
}
```

This wasn't my initial solution, as my commit history here will attest - I pinched the "search for a continuous row of robots" approach from a Redditor. But I like this approach better, because it re-uses what we built for part 1.

## Day 15

What we really care about, in this puzzle, is the positions of the boxes. And in fact for part 1 and part 2 we care about them in essentially the same way, as expressed by the following interface:

```java
interface BoxSet {
    boolean containsBox(Point position);
    boolean canMoveBoxAt(Point position, Direction direction, Predicate<Point> hasWall);
    void moveBoxAt(Point position, Direction direction);
    long score();
    String represent(int width, int height, Point botPosition, Predicate<Point> hasWall);
}
```

When we try to move the robot, here's what happens to it and the boxes:

```java
public BoxPuzzleState afterMove(Direction direction, Predicate<Point> hasWall) {
    var newPosition = direction.addTo(botPosition);

    if (hasWall.test(newPosition)) return this;
    if (!boxPositions.containsBox(newPosition)) return new BoxPuzzleState(newPosition, boxPositions);
    if (!boxPositions.canMoveBoxAt(newPosition, direction, hasWall)) return this;

    boxPositions.moveBoxAt(newPosition, direction);
    return new BoxPuzzleState(newPosition, boxPositions);
}
```

We can't move if there's a wall in the way. If there's neither a wall nor a box in the way, we just move into the new position. If there's a box in the way but we can't move it, we stay put. If there's a box in the way and we can move it, we move it then move the bot into its old position.

(Note: `BoxPuzzleState` was originally meant to be immutable, i.e. we would never destructively modify `boxPositions` but rather create a new copy each time. That was because I anticipated having to do some backtracking, which in the event wasn't necessary, so here we just update the box positions...).

That's nice and simple. Now all we need is an implementation of `BoxSet` for part 1:

```java
static class SingleCellBoxSet implements BoxSet {

    private final Set<Point> boxPositions;

    SingleCellBoxSet(Set<Point> boxPositions) {
        this.boxPositions = boxPositions;
    }

    @Override
    public boolean containsBox(Point position) {
        return boxPositions.contains(position);
    }

    @Override
    public boolean canMoveBoxAt(Point position, Direction direction, Predicate<Point> hasWall) {
        var afterMove = direction.addTo(position);
        if (hasWall.test(afterMove)) return false;

        return !containsBox(afterMove) || canMoveBoxAt(afterMove, direction, hasWall);
    }

    @Override
    public void moveBoxAt(Point position, Direction direction) {
        boxPositions.remove(position);
        var newPosition = direction.addTo(position);
        while (containsBox(newPosition)) {
            newPosition = direction.addTo(newPosition);
        }
        boxPositions.add(newPosition);
    }

    @Override
    public long score() {
        return boxPositions.stream().mapToLong(p -> p.y() * 100 + p.x()).sum();
    }

    @Override
    public String represent(int width, int height, Point botPosition, Predicate<Point> hasWall) {
        // draw the map with these boxes placed in it
    }
}
```

We make the simplifying assumption that if we have to push a whole row or column of boxes we don't actually have to "move" each individual box - we can take the one off the front of the line and put it at the back.

Part 2 is naturally a little more involved:

```java
static class DualCellBoxSet implements BoxSet {

        private final Map<Point, Point> boxPositions;

        DualCellBoxSet(Map<Point, Point> boxPositions) {
            this.boxPositions = boxPositions;
        }

        @Override
        public boolean containsBox(Point position) {
            return boxPositions.containsKey(position);
        }

        @Override
        public boolean canMoveBoxAt(Point position, Direction direction, Predicate<Point> hasWall) {
            var boxStart = boxPositions.get(position);
            var newLeft = direction.addTo(boxStart);
            var newRight = Direction.EAST.addTo(newLeft);

            if (hasWall.test(newLeft) || hasWall.test(newRight)) return false;

            var boxAtNewLeft = boxPositions.get(newLeft);
            var boxAtNewRight = boxPositions.get(newRight);

            if (!(boxAtNewLeft == null
                    || boxAtNewLeft.equals(boxStart)
                    || canMoveBoxAt(boxAtNewLeft, direction, hasWall))) return false;

            return boxAtNewRight == null
                    || boxAtNewRight.equals(boxStart)
                    || canMoveBoxAt(boxAtNewRight, direction, hasWall);
        }

        @Override
        public void moveBoxAt(Point position, Direction direction) {
            var movedBoxLeft = boxPositions.get(position);
            var movedBoxRight = Direction.EAST.addTo(movedBoxLeft);

            boxPositions.remove(movedBoxLeft);
            boxPositions.remove(movedBoxRight);

            var newLeft = direction.addTo(movedBoxLeft);
            var newRight = direction.addTo(movedBoxRight);

            if (containsBox(newLeft)) moveBoxAt(newLeft, direction);
            if (containsBox(newRight)) moveBoxAt(newRight, direction);

            boxPositions.put(newLeft, newLeft);
            boxPositions.put(newRight, newLeft);
        }

        @Override
        public long score() {
            return boxPositions.values().stream().distinct().mapToLong(p ->
                    p.y() * 100 + p.x()).sum();
        }

        @Override
        public String represent(int width, int height, Point botPosition, Predicate<Point> hasWall) {
            // etc
        }
    }
```

Here we keep a map of grid positions containing boxes to the position of the "left" side of each box. When we want to move a box, we have to test that both sides can be moved, and if each side touches a different box then we have to move both before we move the box we're currently moving. It is all, as you can see, very recursive.

Nice and fast, this one. Exercise for the reader: double the height, too.

## Day 16

Well.

It would have helped to have a shortest path algorithm implementation on hand, because who doesn't need one of those from time to time, and my efforts at building one _in situ_ were haphazard having got up at 5AM GMT to race my colleagues to the stars. But we got there in the end.

Let's suppose for a moment that we have an implementation of Dijkstra's shortest path algorithm after all, and work from there. What does our graph look like? It's a graph of reindeer states, where a reindeer's state (like that of a Guard, some days ago) consists of being in a particular place and facing in a particular direction. The possible transitions from such a state are to move forward, at a cost of one, or to turn left, or right, at a cost of 1,000.

Building the graph of states isn't too hard, in that case. First we build up a `SparseGrid` from the empty squares in the maze:

```java
AtomicReference<Point> start = new AtomicReference<>();
AtomicReference<Point> end = new AtomicReference<>();

Grid<Boolean> paths = SparseGrid.of(lines, (p, c) -> switch(c){
    case '#' -> null;
    case 'S' -> {
        start.set(p);
        yield true;
    }
    case 'E' -> {
        end.set(p);
        yield true;
    }
    default -> true;
});
```

then we fill the graph will all the possible combinations of being in a position and facing in a direction, calculating the weights and next states for things it would make sense to do in each state:

```java
paths.populatedPositions()
    .flatMap(p -> Direction.nsew().map(d -> new PathStep(p, d)))
    .forEach(step -> {
        if (paths.getOrDefault(step.ahead(), false)) {
            graph.add(step, step.goForward(), 1);
        }
        if (paths.getOrDefault(step.turnLeft().ahead(), false)) {
            graph.add(step, step.turnLeft(), 1000);
        }
        if (paths.getOrDefault(step.turnRight().ahead(), false)) {
            graph.add(step, step.turnRight(), 1000);
        }
    });
```

Notice that we don't put in a "turning" edge if it would turn the reindeer to face a wall, since the only things to do from there would be to turn back again, or turn one more time in the same direction and start retracing your steps. Either of those moves would be "inefficient" from the point of view of finding the shortest path, so we rule them out of consideration.

Now we ask the magic algorithm to find the distance from the start (facing East) to every node in the graph, recording for each node the "precursor" nodes that were covered by a shortest path reaching that node. (How we do this is the hard part, and it's hidden away inside `WeightedGraph` - we'll take a look in a second).

```java
var distanceMap = graph.distanceMap(new PathStep(start, Direction.EAST));
var distances = distanceMap.distances();
```

Given these results, we have a little more work to do to get our answers. There is more than one way to be standing at the endpoint - likely two, if it's in the top-right corner of the maze - so we need to find the shorter of the shortest paths to both:

```java
var endpoints = Direction.nsew()
        .map(d -> new PathStep(end, d))
        .filter(distances::containsKey)
        .toList();

var shortestPathLength = endpoints.stream()
        .mapToLong(distances::get)
        .min().orElseThrow();
```

Now we need to consider all of the paths that might lead to any of the endpoints that are reachable via a shortest path, extract and deduplicate the positions they traverse, and count them up:

```java
var pointsOnShortestPaths = endpoints.stream()
        .filter(d -> distances.get(d) == shortestPathLength)
        .flatMap(distanceMap::getPathsTo)
        .flatMap(Lst::stream)
        .map(PathStep::position)
        .distinct()
        .count();

return new Result(shortestPathLength, pointsOnShortestPaths);
```

So, there's magic happening inside `graph.distanceMap(start)`, and further magic happening inside `distanceMap.getPathsTo(end)`. Let's take a look inside `WeightedGraph`.

The main part of the work is the implementation of Dijkstra's algorithm here:

```java
private class DistanceMapCalculationContext {
  private final Map<T, Long> distances = new HashMap<>();
  private final Map<T, Set<T>> precursors = new HashMap<>();

  private final PriorityQueueSet<T> queue = new PriorityQueueSet<>();

  public DistanceMap<T> distanceMap(T start) {
    initialise(start);
    populateDistanceMap();
    return new DistanceMap<T>(distances, precursors);
  }

  private void initialise(T start) {
    data.indices().forEach(vertex -> {
      var score = vertex.equals(start) ? 0 : Long.MAX_VALUE;
      queue.add(score, vertex);
      distances.put(vertex, score);
    });
  }

  private void populateDistanceMap() {
    while (!queue.isEmpty()) {
      T source = queue.removeFirst();

      var distU = distances.get(source);
      if (distU == Long.MAX_VALUE) return;

      data.get(source)
              .filter(weighted -> queue.contains(weighted.target()))
              .forEach(weighted ->
                      updateScores(source, weighted.target(), distU + weighted.weight())
              );
    }
  }

  private void updateScores(T source, T target, long newWeight) {
    var distV = distances.get(target);

    if (newWeight > distV) return;
    distances.put(target, newWeight);

    if (newWeight < distV) {
      queue.reprioritise(distV, newWeight, target);
      Set<T> newPrecursors = new HashSet<>();
      newPrecursors.add(source);
      precursors.put(target, newPrecursors);
      return;
    }

    precursors.computeIfAbsent(target, ignored -> new HashSet<>()).add(source);
  }
}
```

Check the [Wikipedia article](https://en.wikipedia.org/wiki/Dijkstra%27s_algorithm) if you want to know where most of this is cribbed from. The most important part is what we do to `precursors` when overwriting a previously-estimated shortest path. For each node, the `precursors` collection holds the set of all the immediate precursor nodes through which a shortest path passes on its way to that node. If we've shortened the estimate, we overwrite the precursors for that node with a set containing just the node we came from. If we find that another node can reach the target node with the same score, we add our precursor node to the existing set. This effectively "fuses" the two paths, when we come to traverse the precursors later on to get the complete set of paths.

Overall this was a bit of a slog; you're much better off re-using an existing graph library if you have one you know well. But next time, I will!

## Day 17

Here I arguably did over-engineer, designing a part 1 solution that could have accommodated backtracking, infinite loop detection, even (with a tweak to include the current program in the state) self-modifying code. You never know what part 2 is going to throw at you; but as in fact it was none of those things, the interpreter I built was definitely overkill.

I'm not going to discuss the part 2 solution here, because I didn't come up with it independently (thanks, Reddit) - basically, the program describes a loop with the initial value of the A register being shifted right by three bits each time until it hits 0, and neither of the other register values carries any meaning from one loop cycle to the next, so it becomes a question of building up an initial value three bits at a time from the intended output. In a way I was hoping for something _much_ nastier. Maybe tomorrow...

An aside: the `Lst` class is starting to be useful for all those cases where I want a stack-like object I can make many copies of (for branching) without having to do array-copying. Even though strictly speaking I didn't need that here.

## Day 18

You know how I mentioned earlier that it would be handy to have a weighted graph / shortest path algorithm already in the bag next time a problem requiring one came around? Well, it came around sooner than expected. Part 1 was _trivial_:

```java
private static long calculateShortestPathWith(Set<Point> obstacles) {
    WeightedGraph<Point> graph = new WeightedGraph<>();
    Predicate<Point> isUnobstructed = p -> !obstacles.contains(p);

    IntStream.range(0, 71).boxed().flatMap(x ->
            IntStream.range(0, 71)
                    .mapToObj(y -> new Point(x, y))
                    .filter(isUnobstructed)
    ).forEach(unobstructed ->
        unobstructed.adjacents()
                .filter(isUnobstructed)
                .forEach(adjacent -> graph.add(unobstructed, adjacent, 1))
    );

    return graph.distanceMap(new Point(0, 0)).distances().get(new Point(70, 70));
}
```

What about part 2? Well, there's a slow way, which takes a bit under 6 seconds on an unwarmed JVM:

```java
private static Point findFirstBlockadingObstacleSlow(Iterator<Point> obstaclesIter) {
    Set<Point> obstaclesSoFar = new HashSet<>();

    while (obstaclesIter.hasNext()) {
        var obstacle = obstaclesIter.next();
        obstaclesSoFar.add(obstacle);
        if (calculateShortestPathWith(obstaclesSoFar) == Long.MAX_VALUE) return obstacle;
    }

    throw new IllegalStateException("No blockading obstacle found");
}
```

If I'd just done that first I could have nabbed the second star super-quick, but for some reason I thought it would be unreasonably slow, so I set about finding a quicker way.

Our path from the top left to the bottom right is blocked if there is any connected group of obstacles which runs from one edge of the map to another (unless it's left to bottom, or right to top, which don't bother us).

Let's start by defining a `ConnectedObstacleGroup`, which tracks a collection of connected obstacles plus which edges of the map it's connected to:

```java
record ConnectedObstacleGroup(
        Set<Point> points,
        boolean meetsLeftEdge,
        boolean meetsRightEdge,
        boolean meetsTopEdge,
        boolean meetsBottomEdge) {

  static ConnectedObstacleGroup empty() {
    return new ConnectedObstacleGroup(new HashSet<>(),
            false, false, false, false);
  }

  public boolean isConnectedTo(Point point) {
    return Stream.of(Direction.values()).anyMatch(d -> points.contains(d.addTo(point)));
  }

  public ConnectedObstacleGroup fuse(ConnectedObstacleGroup other) {
    points.addAll(other.points);
    return new ConnectedObstacleGroup(points,
            meetsLeftEdge || other.meetsLeftEdge,
            meetsRightEdge || other.meetsRightEdge,
            meetsTopEdge || other.meetsTopEdge,
            meetsBottomEdge || other.meetsBottomEdge);
  }

  public boolean isBlockade() {
    return (meetsLeftEdge && (meetsTopEdge || meetsRightEdge))
            || (meetsTopEdge && meetsBottomEdge)
            || (meetsBottomEdge && meetsRightEdge);
  }

  public ConnectedObstacleGroup add(Point p) {
    points.add(p);
    return new ConnectedObstacleGroup(points,
            meetsLeftEdge || (p.x() == 0 && p.y() > 0),
            meetsRightEdge || (p.x() == 70),
            meetsTopEdge || (p.y() == 0 && p.x() > 0),
            meetsBottomEdge || p.y() == 70 && p.x() < 70);
  }
}
```

If a new obstacle connects to a single obstacle group, it gets added to it. If it connects to more than one, they are fused together and the point is added to the fused group. If it connects to none, it is put into a new group:

```java
private static Point findFirstBlockadingObstacle(Collection<Point> obstacles) {
  Set<ConnectedObstacleGroup> connectedGroups = new HashSet<>();

  for (Point point : obstacles) {
    List<ConnectedObstacleGroup> connectedToPoint = connectedGroups.stream()
            .filter(group -> group.isConnectedTo(point))
            .toList();

    connectedToPoint.forEach(connectedGroups::remove);
    ConnectedObstacleGroup containingGroup = connectedToPoint.stream()
            .reduce(ConnectedObstacleGroup::fuse)
            .orElseGet(ConnectedObstacleGroup::empty)
            .add(point);
    connectedGroups.add(containingGroup);

    if (containingGroup.isBlockade()) {
      return point;
    }
  }

  throw new IllegalStateException("No obstacle blockades the route");
}
```

That runs in around 45ms on a warmed-up JVM.

## Day 18: Update

A redditor kindly pointed out that my `ConnectedObstacleGroup` was potentially very inefficient (all the copying of set elements on fusion could be really costly), and I was directed to seek out the Disjoint Set data structure:

```java
public class DisjointSet<T> {

    private static final int LESS_THAN = -1;
    private static final int GREATER_THAN = 1;

    private final Map<T, T> parents = new HashMap<>();
    private final Map<T, Integer> ranks = new HashMap<>();

    public void add(T element) {
        parents.put(element, element);
        ranks.put(element, 0);
    }

    @SafeVarargs
    public final void addAll(T... elements) {
        Arrays.stream(elements).forEach(this::add);
    }

    public void connect(T first, T second) {
        T root1 = findRoot(first);
        T root2 = findRoot(second);

        if (root1.equals(root2)) return;

        switch(Integer.compare(ranks.get(root1), ranks.get(root2))) {
            case LESS_THAN -> parents.put(root1, root2);
            case GREATER_THAN-> parents.put(root2, root1);
            default -> {
                parents.put(root2, root1);
                ranks.put(root1, ranks.get(root1) + 1);
            }
        }
    }

    public boolean isConnected(T first, T second) {
        return findRoot(first).equals(findRoot(second));
    }

    public boolean contains(T element) {
        return parents.containsKey(element);
    }

    private T findRoot(T element) {
        if (!parents.get(element).equals(element)) {
            parents.put(element, findRoot(parents.get(element))); // Path compression: flatten the tree
        }
        return parents.get(element);
    }
}
```

This has the needed property that connections between elements are transitive: if A is connected to B, and B is connected to C, then A is connected to C. Our modified solution creates some "anchor" points representing each of the edges, to which any point on an edge is connected, and then tests for connections between anchor points:

```java
static final class ConnectedObstacleGroup {

    private final DisjointSet<Point> points;
    private final Point leftAnchor;
    private final Point rightAnchor;
    private final Point topAnchor;
    private final Point bottomAnchor;

    ConnectedObstacleGroup() {
        points = new DisjointSet<>();

        leftAnchor = new Point(-1, 0);
        rightAnchor = new Point(71, 0);
        topAnchor = new Point(0, -1);
        bottomAnchor = new Point(0, 71);

        points.addAll(leftAnchor, rightAnchor, topAnchor, bottomAnchor);
    }

    public boolean isBlockadeAfterAdding(Point point) {
      points.add(point);
  
      if (point.x() == 0) points.connect(leftAnchor, point);
      if (point.x() == 70) points.connect(rightAnchor, point);
      if (point.y() == 0) points.connect(topAnchor, point);
      if (point.y() == 70) points.connect(bottomAnchor, point);
  
      Arrays.stream(Direction.values())
              .map(d -> d.addTo(point))
              .filter(points::contains)
              .forEach(adjacent -> points.connect(point, adjacent));
  
      return points.isConnected(leftAnchor, topAnchor)
              || points.isConnected(leftAnchor, rightAnchor)
              || points.isConnected(topAnchor, bottomAnchor)
              || points.isConnected(rightAnchor, bottomAnchor);
    }
}
```

Our part 2 solver is now a simple "feed these values to this consumer until it returns true" loop:

```java
private static Point findFirstBlockadingObstacle(Collection<Point> obstacles) {
    ConnectedObstacleGroup connectedObstacles = new ConnectedObstacleGroup();
    return obstacles.stream()
            .filter(connectedObstacles::isBlockadeAfterAdding)
            .findFirst()
            .orElseThrow();
}
```

I don't particularly like that we're updating the state of `connectedObstacles` inside `filter`, which ought really to be a pure function, but there isn't a haltable `forEach` or `reduce` on Java's `Stream`. I suppose I could always have written a `for`-loop instead.

## Day 19

Induction (if we can make the rest of the pattern after matching the start, then we can make the whole thing) plus memoisation (keep track of how many ways there are to make each possible remainder) ftw. Should have got this one super-quick, as it's super-easy.

There are two approaches we can take to the memoisation. One, which I favoured, is to stuff all the suffix scores in a `Map<String, Long>`, which also means you can re-use them from one item to the next. This is the cache-using, recursive version:

```java
private long countPossible(String checking) {
    var knownCount = knownPossible.get(checking);
    if (knownCount != null) return knownCount;

    long possibleCount = 0L;
    for (String subPattern : availablePatterns) {
        if (subPattern.length() > checking.length()) break;

        if (checking.startsWith(subPattern)) {
            if (subPattern.length() == checking.length()) {
                possibleCount += 1;
                break;
            }

            var remainder = checking.substring(subPattern.length());
            possibleCount += countPossible(remainder);
        }
    }

    knownPossible.put(checking, possibleCount);
    return possibleCount;
}
```

(note that we can't just use `Map.computeIfAbsent` here because the function's recursion means we'll get a `ConcurrentModificationException`).

The other approach is the dynamic programming approach, where we populate intermediate calculations into an array and work backwards from the end in such a way that "remainder" results are always available when we need them:

```java
private long countPossibleDynamic(String checking) {
    for (int i = checking.length() - 1; i >= 0; i--) {
        long count = 0;
        long maxLength = checking.length() - i;
        for (String subPattern: availablePatterns) {
            if (checking.startsWith(subPattern, i)) {
                count += subPattern.length() == maxLength
                        ? 1
                        : counts[i + subPattern.length()];
            }
        }
        counts[i] = count;
    }
    return counts[0];
}
```

(the array, `counts`, is pre-dimensioned to fit the largest of the strings we're examining). No recursion here - just a nested loop.

I had a vague notion this might be faster then the other way, but it isn't noticeably. We have to do essentially the same number of calculations either way (we never have to re-do the count for any suffix, whichever approach we use). It just happens that the first way lets us re-use in later cases suffixes we already calculated in former ones.

## Day 20

This really is the year of Dijkstra!

Given that we already know how to compute the complete distance map of distances from any point in the maze to the end, we can identify the value of any prospective cheat as the difference between the distances of the start and end positions, minus the cost in extra moves of warping from start to end:

```java
private long cheatValue(Point start, Point end) {
    var beforeDistance = distanceMap.get(start);
    var afterDistance = distanceMap.get(end);

    var cost = end.manhattanDistanceFrom(start) - 1;

    return beforeDistance - (afterDistance + cost);
}
```

Given an allowance _n_ of cheat moves, the cheat position offsets worth considering are those with a Manhattan distance of _2 <= distance <= n_. Let's pre-calculate those:

```java
private List<Point> diamondOffsets(int size) {
    Point origin = new Point(0, 0);
    return IntStream.range(-size, size + 1).boxed().flatMap(x ->
            IntStream.range(-size, size + 1).boxed().map(y -> new Point(x, y)))
            .filter(p -> {
                var distance = p.manhattanDistanceFrom(origin);
                return 2 <= distance && distance <= size;
            }).toList();
}
```

Now we run through the positions on the map for which we have distances calculate, look at all the offsets for each of those positions which are also on the map, calculate their cheat values and count up the ones with a cheat value of 100 or more:

```java
public long cheats(int diamondSize) {
    var offsets = diamondOffsets(diamondSize);

    return distanceMap.keySet().stream()
            .mapToLong(before ->
                offsets.stream()
                        .map(before::plus)
                        .filter(this::isOnPath)
                        .filter(after -> cheatValue(before, after) >= 100)
                        .count()
            ).sum();
}

private boolean isOnPath(Point p) {
    return distanceMap.getOrDefault(p, Long.MAX_VALUE) != Long.MAX_VALUE;
}
```

That gives us all we need to solve parts 1 (with a diamond size of 2) and 2 (with a diamond size of 20).