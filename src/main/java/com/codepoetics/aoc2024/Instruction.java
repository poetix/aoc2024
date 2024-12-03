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

        Stream.Builder<Instruction> found = Stream.builder();

        while (matcher.find()) {
            var token = matcher.group(0);
            if (token.startsWith("mul")) {
                found.add(new Mul(
                        Integer.parseInt(matcher.group(1)),
                        Integer.parseInt(matcher.group(2))));
            } else if (token.equals("do()")) {
                found.add(new Do());
            } else {
                found.add(new Dont());
            }
        }

        return found.build();
    }

    record Mul(int lhs, int rhs) implements Instruction { }

    record Do() implements Instruction { }

    record Dont() implements Instruction { }

}
