package com.codepoetics.aoc2024.firstTenDays;

import com.codepoetics.aoc2024.parsing.ResourceReader;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class Day7 {

    record Equation(long result, long[] numbers) {
        static Equation fromLine(String line) {
            var lineParts = line.split(":");
            var result = Long.parseLong(lineParts[0]);
            var numbers = Arrays.stream(lineParts[1].trim().split("\\s+"))
                    .mapToLong(Long::parseLong)
                    .toArray();
            return new Equation(result, numbers);
        }

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

        public boolean isValidWithConcat() {
            return isValidWithConcat(numbers[0], 1);
        }

        private boolean isValidWithConcat(long accumulator, int index) {
            if (index == numbers.length) {
                return result == accumulator;
            }
            if (accumulator > result) {
                return false;
            }
            return isValidWithConcat(accumulator + numbers[index], index + 1)
                    || isValidWithConcat(accumulator * numbers[index], index + 1)
                    || isValidWithConcat(concat(accumulator, numbers[index]), index + 1);
        }

        private long concat(long lhs, long rhs) {
            return Long.parseLong(String.format("%d%d", lhs, rhs));
        }
    }

    private List<Equation> equations = ResourceReader.of("/day7.txt").readLines()
            .map(Equation::fromLine)
            .toList();

    @Test
    public void part1() {
        var sumOfValidEquations = equations.stream()
                .filter(Equation::isValid)
                .mapToLong(Equation::result)
                .sum();

        System.out.println(sumOfValidEquations);
    }

    @Test
    public void part2() {
        var sumOfValidEquations = equations.stream()
                .filter(Equation::isValidWithConcat)
                .mapToLong(Equation::result)
                .sum();

        System.out.println(sumOfValidEquations);
    }


    @Test
    public void buildExpressions() {
        ResourceReader.of("/day7_test.txt").readLines()
                .map(Equation::fromLine)
                .flatMap(Equation::possibleEquations)
                .forEach(System.out::println);
    }
}
