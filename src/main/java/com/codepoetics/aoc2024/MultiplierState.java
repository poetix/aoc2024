package com.codepoetics.aoc2024;

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
