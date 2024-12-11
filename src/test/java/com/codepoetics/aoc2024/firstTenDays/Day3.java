package com.codepoetics.aoc2024.firstTenDays;

import com.codepoetics.aoc2024.Instruction;
import com.codepoetics.aoc2024.MultiplierState;
import com.codepoetics.aoc2024.ResourceReader;
import org.junit.jupiter.api.Test;

public class Day3 {

    @Test
    public void part1() {
        MultiplierState state = new MultiplierState();

        ResourceReader.of("/day3.txt").readLines()
                .flatMap(Instruction::parseLine)
                .filter(instruction -> instruction instanceof Instruction.Mul)
                .forEach(state::interpret);

        System.out.println(state.getSum());
    }

    @Test
    public void part2() {
        MultiplierState state = new MultiplierState();

        ResourceReader.of("/day3.txt").readLines()
                .flatMap(Instruction::parseLine)
                .forEach(state::interpret);

        System.out.println(state.getSum());
    }

}
