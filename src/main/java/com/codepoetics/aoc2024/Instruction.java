package com.codepoetics.aoc2024;

import java.util.regex.Pattern;
import java.util.stream.Stream;

public sealed interface Instruction permits
        Instruction.Mul,
        Instruction.Do,
        Instruction.Dont {

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

    record Mul(int lhs, int rhs) implements Instruction { }

    record Do() implements Instruction { }

    record Dont() implements Instruction { }

}
