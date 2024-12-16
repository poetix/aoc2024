package com.codepoetics.aoc2023;

import com.codepoetics.aoc2024.parsing.ResourceReader;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.regex.Pattern;

public class Day23 {

    record Nanobot(long x, long y, long z, long range) {
        static final Pattern pattern = Pattern.compile("pos=<(-?\\d+),(-?\\d+),(-?\\d+)>, r=(\\d+)");

        static Nanobot of(String line) {
            var matcher = pattern.matcher(line);
            matcher.find();

            return new Nanobot(
                    Long.parseLong(matcher.group(1)),
                    Long.parseLong(matcher.group(2)),
                    Long.parseLong(matcher.group(3)),
                    Long.parseLong(matcher.group(4))
            );
        }
    }

    record NanobotPair(Nanobot first, Nanobot second, long manhattanDistance) {
        static NanobotPair of(Nanobot first, Nanobot second) {
            long manhattanDistance = Math.abs(first.x() - second.x()) +
                    Math.abs(first.y() - second.y()) +
                    Math.abs(first.z() - second.z());
            return new NanobotPair(first, second, manhattanDistance);
        }
    }

    @Test
    public void part1() {
        var nanobots = ResourceReader.of("/2023_day23.txt").readLines()
                .map(Nanobot::of)
                .toList();

        var biggestRange = nanobots.stream().max(Comparator.comparing(Nanobot::range)).orElseThrow();
        var withinRange = nanobots.stream()
                .map(n -> NanobotPair.of(n, biggestRange))
                .filter(p -> p.manhattanDistance <= biggestRange.range());

        System.out.println(withinRange.count());
    }


}
