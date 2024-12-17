package com.codepoetics.aoc2024.secondTenDays;

import com.codepoetics.aoc2024.data.Lst;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Day17 {

    enum Instruction {
        ADV(true),
        BXL(false),
        BST(true),
        JNZ(false),
        BXC(false),
        OUT(true),
        BDV(true),
        CDV(true);

        private final boolean isCombo;

        Instruction(boolean isCombo) {
            this.isCombo = isCombo;
        }

        public boolean isCombo() {
            return isCombo;
        }
    }

    enum ComboOperand {
        LITERAL_0,
        LITERAL_1,
        LITERAL_2,
        LITERAL_3,
        REGISTER_A,
        REGISTER_B,
        REGISTER_C,
        RESERVED
    }

    static public String describe(int[] program) {
        StringBuilder sb = new StringBuilder();
        for (int ptr = 0; ptr < program.length; ptr += 2) {
            var instr = Instruction.values()[program[ptr]];
            var operand = instr.isCombo() ? ComboOperand.values()[program[ptr + 1]].toString() : Integer.toString(program[ptr + 1]);
            sb.append(instr).append(" ").append(operand).append("\n");
        }
        return sb.toString();
    }

    record MachineState(int ptr, long registerA, long registerB, long registerC, Lst<Long> output, boolean halted) {

        public static MachineState initialise(long registerA, long registerB, long registerC) {
            return new MachineState(0, registerA, registerB, registerC, Lst.empty(), false);
        }

        public String result() {
            return output().reverse().stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(","));
        }

        public MachineState run(int[] program) {
            var currentState = this;
            while (!currentState.halted()) {
                currentState = currentState.step(program);
            }
            return currentState;
        }

        @Override
        public String toString() {
            return "%sptr: %d, A: %d, B: %d, C: %d, output: %s".formatted(
                    halted ? "HALTED " : "",
                    ptr, registerA, registerB, registerC,
                    output.reverse().stream().map(Object::toString).collect(Collectors.joining(","))
                );
        }

        public MachineState step(int[] program) {
            if (ptr >= program.length) return halt();

            var instruction = Instruction.values()[program[ptr]];
            return interpret(instruction, program[ptr + 1]);
        }

        private long combo(int operand) {
            return switch(ComboOperand.values()[operand]) {
                case LITERAL_0 -> 0L;
                case LITERAL_1 -> 1L;
                case LITERAL_2 -> 2L;
                case LITERAL_3 -> 3L;
                case REGISTER_A -> registerA;
                case REGISTER_B -> registerB;
                case REGISTER_C -> registerC;
                case RESERVED -> throw new UnsupportedOperationException();
            };
        }

        public MachineState interpret(Instruction instruction, int operand) {
            long interpretedOperand = instruction.isCombo() ? combo(operand) : operand;

            return switch(instruction) {
                case ADV -> adv(interpretedOperand);
                case BXL -> bxl(interpretedOperand);
                case BST -> bst(interpretedOperand);
                case JNZ -> jnz(interpretedOperand);
                case BXC -> bxc(interpretedOperand);
                case OUT -> out(interpretedOperand);
                case BDV -> bdv(interpretedOperand);
                case CDV -> cdv(interpretedOperand);
            };
        }

        private MachineState updateA(long newA) {
            return new MachineState(ptr + 2, newA, registerB, registerC, output, false);
        }

        private MachineState updateB(long newB) {
            return new MachineState(ptr + 2, registerA, newB, registerC, output, false);
        }

        private MachineState updateC(long newC) {
            return new MachineState(ptr + 2, registerA, registerB, newC, output, false);
        }

        private MachineState noop() {
            return new MachineState(ptr + 2, registerA, registerB, registerC, output, false);
        }

        private MachineState halt() {
            return new MachineState(ptr, registerA, registerB, registerC, output, true);
        }

        private long divABy(long operand) {
            return registerA >> operand;
        }

        private MachineState adv(long comboOperand) {
            return updateA(divABy(comboOperand));
        }

        private MachineState bxl(long operand) {
            return updateB(registerB ^ operand);
        }

        private MachineState bst(long comboOperand) {
            return updateB(comboOperand & 7);
        }

        private MachineState jnz(long operand) {
            if (registerA == 0) return noop();
            return new MachineState((int) operand, registerA, registerB, registerC, output, false);
        }

        private MachineState bxc(long ignored) {
            return updateB(registerB ^ registerC);
        }

        private MachineState out(long comboOperand) {
            return new MachineState(ptr + 2, registerA, registerB, registerC, output.add(comboOperand & 7), false);
        }

        private MachineState bdv(long comboOperand) {
            return updateB(divABy(comboOperand));
        }

        private MachineState cdv(long comboOperand) {
            return updateC(divABy(comboOperand));
        }
    }

    @Test
    public void part1Test() {
        assertEquals("4,6,3,5,6,3,5,2,1,0",
                MachineState.initialise(729, 0, 0)
                        .run(new int[] { 0, 1, 5, 4, 3, 0}).result());
    }

    private final int[] program = {2,4,1,4,7,5,4,1,1,4,5,5,0,3,3,0 };

    @Test
    public void part1() {
        assertEquals("7,0,7,3,4,1,3,0,1",
                MachineState.initialise(25986278, 0, 0)
                        .run(program)
                        .result());
    }


    @Test
    public void part2() {
        Set<Long> candidates = new HashSet<>();
        candidates.add(0L);

        for (int j = program.length - 1; j >= 0; j--) {
            Set<Long> newCandidates = new HashSet<>();
            var digit = program[j];
            for (long current : candidates) {
                for (int i = 0; i < 8; i++) {
                    long next = (current << 3) + i;

                    var result = test(next);
                    if (result.output().size() == program.length - j
                            && result.output().last() == digit) {
                        newCandidates.add(next);
                    }
                }
            }
            candidates = newCandidates;
        }

        var min = candidates.stream().mapToLong(l -> l).min().orElseThrow();
        assertEquals(156985331222018L, min);
    }

    private MachineState test(long a) {
        var initialState = MachineState.initialise(a, 0, 0);
        return initialState.run(program);
    }

    @Test
    public void describeProgram() {
        System.out.println(describe(program));
    }

    /**
     * BST REGISTER_A
     * BXL 4
     * CDV REGISTER_B
     * BXC 1
     * BXL 4
     * OUT REGISTER_B
     * ADV LITERAL_3
     * JNZ 0
     */
    @Test
    public void overflow() {
        var initialState = MachineState.initialise((long) Integer.MAX_VALUE << 1, 0, 0);
        initialState.run(new int[] {2,4,1,4,7,5,4,1,1,4,5,5,0,3,3,0 });
    }
}
