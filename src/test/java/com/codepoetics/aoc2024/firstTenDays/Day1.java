package com.codepoetics.aoc2024.firstTenDays;

import com.codepoetics.aoc2024.parsing.ResourceReader;
import com.codepoetics.aoc2024.parsing.TwinListParser;
import org.junit.jupiter.api.Test;

public class Day1 {

    @Test
    public void day1Part1() {
        var parser = new TwinListParser();

        parser.parse(ResourceReader.of("/day1.txt").readLines());

        System.out.println(parser.sumDifferences());
    }

    @Test
    public void day1Part2() {
        var parser = new TwinListParser();

        parser.parse(ResourceReader.of("/day1.txt").readLines());

        System.out.println(parser.calculateSimilarity());
    }
}
