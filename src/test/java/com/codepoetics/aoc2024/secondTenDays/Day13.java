package com.codepoetics.aoc2024.secondTenDays;

import com.codepoetics.aoc2024.grid.Point;
import com.codepoetics.aoc2024.parsing.ResourceReader;
import com.codepoetics.aoc2024.streams.Streams;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Day13 {

    record Machine(Point buttonA, Point buttonB, Point prize) {

        public Machine adjustPart2() {
            return new Machine(buttonA, buttonB,
                    new Point(prize.x() + 10000000000000L, prize.y() + 10000000000000L));
        }

        private long determinant(long r1c1, long r1c2, long r2c1, long r2c2) {
            return r1c1 * r2c2 - r1c2 * r2c1;
        }

        public long minCost() {
            var detA = determinant(buttonA.x(), buttonB.x(), buttonA.y(), buttonB.y());
            var detAn = determinant(prize.x(), buttonB.x(), prize.y(), buttonB.y());
            var detAm = determinant(buttonA.x(), prize.x(), buttonA.y(), prize.y());

            if (detAn % detA != 0) return Long.MAX_VALUE;
            if (detAm % detA != 0) return Long.MAX_VALUE;

            var aCount = detAn / detA;
            var bCount = detAm / detA;
            return (aCount * 3) + bCount;
        }
    }

    static class MachineInterpreter implements Function<Iterator<String>, Machine> {

        private static final Pattern buttonPattern = Pattern.compile("Button [A|B]: X\\+(\\d+), Y\\+(\\d+)");
        private static final Pattern prizePattern = Pattern.compile("Prize: X=(\\d+), Y=(\\d+)");

        public static Stream<Machine> readMachines(Stream<String> input) {
            return Streams.interpret(input, new MachineInterpreter());
        }

        private static Point readPoint(String line, Pattern pattern) {
            var matcher = pattern.matcher(line);
            if (!matcher.find()) throw new IllegalArgumentException(line + " didn't match " + pattern);
            return new Point(
                    Integer.parseInt(matcher.group(1)),
                    Integer.parseInt(matcher.group(2))
            );
        }

        @Override
        public Machine apply(Iterator<String> iter) {
            var a = readPoint(iter.next(), buttonPattern);
            var b = readPoint(iter.next(), buttonPattern);
            var prize = readPoint(iter.next(), prizePattern);
            if (iter.hasNext()) iter.next();

            return new Machine(a, b, prize);
        }
    }

    @Test
    public void part1Test() {
        var totalMinCost = MachineInterpreter.readMachines(ResourceReader.of("/day13_test.txt").readLines())
                .mapToLong(Machine::minCost)
                .filter(cost -> cost != Long.MAX_VALUE)
                .sum();

        assertEquals(480, totalMinCost);
    }

    @Test
    public void part1() {
        var totalMinCost = MachineInterpreter.readMachines(ResourceReader.of("/day13.txt").readLines())
                .mapToLong(Machine::minCost)
                .filter(cost -> cost != Long.MAX_VALUE)
                .sum();

        assertEquals(35997, totalMinCost);
    }

    @Test
    public void part2() {
        var totalMinCost = MachineInterpreter.readMachines(ResourceReader.of("/day13.txt").readLines())
                .map(Machine::adjustPart2)
                .mapToLong(Machine::minCost)
                .filter(cost -> cost != Long.MAX_VALUE)
                .sum();

        assertEquals(82510994362072L, totalMinCost);
    }
}
