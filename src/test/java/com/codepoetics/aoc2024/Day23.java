package com.codepoetics.aoc2024;

import com.codepoetics.aoc2024.data.Lst;
import com.codepoetics.aoc2024.parsing.ResourceReader;
import org.junit.jupiter.api.Test;

import java.util.*;
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
            return maxBronKerbosch(Lst.empty(), Lst.of(connections.keySet()), Lst.empty());
        }

        public List<String> maximalClique2() {
            var cliques = connections.keySet().stream().map(s -> {
                List<String> clique =  new ArrayList<>();
                clique.add(s);
                return clique;
            }).toList();

            return cliques.stream().peek(clique ->
                connections.forEach((node, neighbours) -> {
                    if (neighbours.containsAll(clique)) {
                        clique.add(node);
                    }
                })
            ).max(Comparator.comparing(List::size)).orElseThrow();
        }

        private Lst<String> maxBronKerbosch(Lst<String> r, Lst<String> p, Lst<String> x) {
            if (p.isEmpty() && x.isEmpty()) {
                return r;
            }

            Lst<String> max = Lst.empty();
            var pivot = Stream.concat(p.stream(), x.stream()).max(Comparator.comparing(v ->
                    connections.get(v).size())).orElseThrow();
            var pivotNeighbours = connections.get(pivot);

            while (!p.isEmpty()) {
                var v = p.head();

                if (!pivotNeighbours.contains(v)) {
                    var neighbours = connections.get(v);

                    var candidate = maxBronKerbosch(r.add(v), intersect(p, neighbours), intersect(x, neighbours));
                    if (candidate.size() > max.size()) max = candidate;
                }
                
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
    public void bothParts() {
        var network = Network.of(ResourceReader.of("/day23.txt").readLines());

        var part1 = network.groupsOfThree()
                        .filter(group -> group.stream().anyMatch(s -> s.startsWith("t")))
                        .count();
        assertEquals(1077L, part1);

        assertEquals("bc,bf,do,dw,dx,ll,ol,qd,sc,ua,xc,yu,zt",
                String.join(",", network.maximalClique2()));
    }



}
