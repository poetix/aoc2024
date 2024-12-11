package com.codepoetics.aoc2024.firstTenDays;

import com.codepoetics.aoc2024.ResourceReader;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Day5 {

    record PageList(int[] pages) {

        @Override
        public boolean equals(Object other) {
            return other instanceof PageList &&
                    Arrays.equals(pages, ((PageList) other).pages);
        }

        public int middleValue() {
           return pages[pages.length / 2];
       }

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
    }

    public static class Ordering {

        private final Map<Integer, Set<Integer>> previousByNext = new HashMap<>();

        public void add(int prev, int next) {
            previousByNext.compute(next, (ignored, previous) ->{
                var result = previous == null ? new HashSet<Integer>() : previous;
                result.add(prev);
                return result;
            });
        }

        public long countPreviousIn(int page, Set<Integer> pages) {
            var previous = previousByNext.get(page);
            return previous == null ? 0 : previous.stream()
                    .filter(pages::contains)
                    .count();
        }
    }

    record Puzzle(Ordering ordering, List<PageList> pageLists) {

        public static Puzzle fromFile(Stream<String> lines) {
            var iter = lines.iterator();

            Ordering ordering = new Ordering();

            while (iter.hasNext()) {
                String orderingStr = iter.next().trim();
                if (orderingStr.isEmpty()) break;
                String[] parts = orderingStr.split("\\|");
                ordering.add(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
            }

            List<PageList> pageLists = new ArrayList<>();
            while (iter.hasNext()) {
                pageLists.add(new PageList(
                        Arrays.stream(iter.next().trim().split(","))
                                .mapToInt(Integer::parseInt)
                                .toArray()));
            }

            return new Puzzle(ordering, pageLists);
        }

        private boolean isValid(PageList pageList) {
            return pageList.equals(pageList.reorder(ordering));
        }

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
    }

    @Test
    public void parsePuzzle() {
        var puzzle = Puzzle.fromFile(ResourceReader.of("/day5.txt").readLines());

        System.out.println(puzzle.validUpdatesSum());
        System.out.println(puzzle.reorderedUpdatesSum());
    }
}
