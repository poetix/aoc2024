package com.codepoetics.aoc2024;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.stream.Stream;

public class ResourceReader {

    public static ResourceReader of(String path) {
        return new ResourceReader(path);
    }

    private final String path;

    private ResourceReader(String path) {
        this.path = path;
    }

    public Stream<String> readLines() {
        var br = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(
                        getClass().getResourceAsStream(path))));

        return br.lines().onClose(() -> {
            try {
                br.close();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }
}
