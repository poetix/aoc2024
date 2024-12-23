package com.codepoetics.aoc2024;

import com.codepoetics.aoc2024.data.Lst;
import com.codepoetics.aoc2024.parsing.ResourceReader;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Day23 {

    static class Network {

        static Network of(Stream<String> lines) {
            Network network = new Network();
            lines.forEach(line -> {
                var parts = line.split("-");
                network.connect(parts[0], parts[1]);
                network.connect(parts[1], parts[0]);
            });
            return network;
        }

        private final SortedMap<String, SortedSet<String>> connections = new TreeMap<>();

        private void connect(String a, String b) {
            connections.computeIfAbsent(a, ignored -> new TreeSet<>()).add(b);
        }

        public Stream<List<String>> groupsOfThree() {
            return connections.entrySet().stream().flatMap(e -> {
                var a = e.getKey();
                var neighbours = e.getValue();

                return neighbours.subSet(a + "z", "zzz").stream().flatMap(b ->
                   connections.get(b).subSet(b + "z", "zzz").stream()
                            .filter(neighbours::contains)
                            .map(c -> List.of(a, b, c)));
            });
        }

        public Lst<String> maximalClique() {
            return maxBronKerbosch(Lst.empty(), Lst.of(connections.keySet().stream()), Lst.empty());
        }

        private Lst<String> maxBronKerbosch(Lst<String> r, Lst<String> p, Lst<String> x) {
            if (p.isEmpty() && x.isEmpty()) {
                return r;
            }

            Lst<String> max = Lst.empty();
            while (!p.isEmpty()) {
                var v = p.head();
                var neighbours = connections.get(v);

                var candidate = maxBronKerbosch(r.add(v), intersect(p, neighbours), intersect(x, neighbours));
                if (candidate.size() > max.size()) max = candidate;

                p = p.tail();
                x = x.add(v);
            }

            return max;
        }

        private Lst<String> intersect(Lst<String> a, Set<String> neighbours) {
            return a.filter(neighbours::contains);
        }

    }
    @Test
    public void part1() {
        var network = Network.of(ResourceReader.of("/day23.txt").readLines());

        var part1 = network.groupsOfThree()
                        .filter(group -> group.stream().anyMatch(s -> s.startsWith("t")))
                        .count();
        assertEquals(1077L, part1);

        var largest = network.maximalClique();
        assertEquals("bc,bf,do,dw,dx,ll,ol,qd,sc,ua,xc,yu,zt",
                largest.stream().collect(Collectors.joining(",")));
    }



}
