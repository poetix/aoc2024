package com.codepoetics.aoc2024.arithmetic;

public class GCD {

    public static long lcm(long a, long y) {
        return a * (y / gcd(a, y));
    }

    public static long gcd(long x, long y) {
        var a = Math.max(x, y);
        var b = Math.min(x, y);
        var r = b;
        while(a % b != 0)
        {
            r = a % b;
            a = b;
            b = r;
        }
        return r;
    }
}
