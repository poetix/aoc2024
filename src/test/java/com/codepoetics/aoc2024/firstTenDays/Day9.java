package com.codepoetics.aoc2024.firstTenDays;

import com.codepoetics.aoc2024.ResourceReader;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Day9 {

    public record Blank(int position, int length) {
        public Blank remainderAfterPopulatingWith(FileRecord record) {
            return new Blank(
                    position + record.length(),
                    length - record.length());
        }
    }

    public record FileRecord(int fileId, int position, int length) {
        public FileRecord moveTo(int newPosition) {
            return new FileRecord(fileId, newPosition, length);
        }

        public FileRecord resizeTo(int newLength) {
            return new FileRecord(fileId, position, newLength);
        }

        public long checksum() {
            return fileId * length *
                    ((position * 2L) + length - 1) / 2L;
        }
    }

    static class BlankTable {

        private final NavigableMap<Integer, NavigableSet<Blank>> blanksByLengthAndPosition = new TreeMap<>();

        public void add(Blank blank) {
            blanksByLengthAndPosition.compute(blank.length(), (ignored, v) -> {
                var s = v == null ? new TreeSet<>(Comparator.comparing(Blank::position)) : v;
                s.add(blank);
                return s;
            });
        }

        private void prune(int maxPosition) {
            var lengthIter = blanksByLengthAndPosition.values().iterator();

            while (lengthIter.hasNext()) {
                var positions = lengthIter.next();
                var positionIter = positions.descendingIterator();

                while (positionIter.hasNext()) {
                    if (positionIter.next().position() < maxPosition) break;
                    positionIter.remove();
                }

                if (positions.isEmpty()) {
                    lengthIter.remove();
                }
            }
        }

        public Optional<Blank> takeFirst(int minLength, int maxPosition) {
            prune(maxPosition);
            return blanksByLengthAndPosition.subMap(minLength, 10).entrySet().stream()
                    .min(Comparator.comparing(e -> e.getValue().getFirst().position()))
                    .map(e -> e.getValue().removeFirst());
        }
    }

    record DiscFiles(List<FileRecord> fileRecords, List<Blank> blanks) {

        public static DiscFiles of(String descriptor) {
            List<FileRecord> fileRecords = new ArrayList<>();
            List<Blank> blanks = new ArrayList<>();

            var ints = descriptor.chars()
                    .map(c -> Integer.parseInt(String.valueOf((char) c)))
                    .iterator();

            int fileId = 0;
            int pos = 0;

            while (ints.hasNext()) {
                var fileLength = ints.nextInt();
                if (fileLength > 0) {
                    fileRecords.add(new FileRecord(fileId++, pos, fileLength));
                    pos += fileLength;
                }

                if (!ints.hasNext()) break;

                var blankLength = ints.nextInt();
                if (blankLength > 0) {
                    blanks.add(new Blank(pos, blankLength));
                    pos += blankLength;
                }
            }

            return new DiscFiles(fileRecords, blanks);
        }

        public BlockCompactor blockCompactor() {
            return new BlockCompactor(fileRecords, blanks);
        }

        public FileCompactor fileCompactor() {
            BlankTable blankTable = new BlankTable();
            blanks.forEach(blankTable::add);

            return new FileCompactor(fileRecords, blankTable);
        }

    }

    static class BlockCompactor {

        static class FileRecords {

            public static FileRecords iteratingOver(List<FileRecord> records) {
                return new FileRecords(records.reversed().iterator());
            }

            private final Iterator<FileRecord> iterator;
            private FileRecord current;

            private FileRecords(Iterator<FileRecord> iterator) {
                this.iterator = iterator;
                this.current = iterator.hasNext() ? iterator.next() : null;
            }

            public boolean canCompactInto(Blanks blanks) {
                return current != null && blanks.hasCapacityBefore(current.position());
            }

            public FileRecord fillBlankIn(Blanks blanks, int blankSize) {
                var compacted = blankSize > current.length()
                        ? blanks.accept(current)
                        : blanks.accept(current.resizeTo(blankSize));

                current = compacted.length() < current.length()
                    ? current.resizeTo(current.length() - blankSize)
                    : iterator.hasNext() ? iterator.next() : null;

                return compacted;
            }

            public long checksumRemainder() {
                long checksum = 0;
                if (current != null) checksum += current.checksum();

                while (iterator.hasNext()) {
                    checksum += iterator.next().checksum();
                }

                return checksum;
            }
        }

        static class Blanks {

            static Blanks iteratingOver(List<Blank> blanks) {
                return new Blanks(blanks.iterator());
            }

            private final Iterator<Blank> iterator;
            private Blank current;

            private Blanks(Iterator<Blank> iterator) {
                this.iterator = iterator;
                this.current = iterator.hasNext() ? iterator.next() : null;
            }

            private int capacity() {
                return current == null ? 0 : current.length();
            }

            public FileRecord compactNext(FileRecords fileRecords) {
                return fileRecords.fillBlankIn(this, capacity());
            }

            public boolean hasCapacityBefore(int position) {
                return current != null && current.position() < position;
            }

            public FileRecord accept(FileRecord record) {
                var result = record.moveTo(current.position());

                current = record.length() < capacity()
                        ? current.remainderAfterPopulatingWith(record)
                        : iterator.hasNext() ? iterator.next() : null;

                return result;
            }
        }

        private final FileRecords fileRecords;
        private final Blanks blanks;

        BlockCompactor(List<FileRecord> fileRecords, List<Blank> blanks) {
            this.fileRecords = FileRecords.iteratingOver(fileRecords);
            this.blanks = Blanks.iteratingOver(blanks);
        }

        public long compactAndChecksum() {
            long checksum = 0;

            while (fileRecords.canCompactInto(blanks)) {
                checksum += (blanks.compactNext(fileRecords)).checksum();
            }

            return checksum + fileRecords.checksumRemainder();
        }

    }

    static class FileCompactor {

        private final List<FileRecord> fileRecords;
        private final BlankTable blankTable;

        FileCompactor(List<FileRecord> fileRecords, BlankTable blankTable) {
            this.fileRecords = fileRecords;
            this.blankTable = blankTable;
        }

        public long compactByFileAndChecksum() {
            var result = fileRecords.reversed().stream()
                    .map(this::tryMoveRecord)
                    .mapToLong(FileRecord::checksum)
                    .sum();
            return result;
        }

        private FileRecord tryMoveRecord(FileRecord record) {
            return blankTable.takeFirst(record.length(), record.position())
                    .map(blank -> moveRecord(record, blank))
                    .orElse(record);
        }

        private FileRecord moveRecord(FileRecord record, Blank blank) {
            var remainingLength = blank.length() - record.length();

            if (remainingLength > 0) {
                blankTable.add(
                        new Blank(
                                blank.position() + record.length(),
                                remainingLength));
            }

            return record.moveTo(blank.position());
        }
    }

    private final DiscFiles test = DiscFiles.of("2333133121414131402");
    private final DiscFiles real = DiscFiles.of(
            ResourceReader.of("/day9.txt").readLines().findFirst().orElseThrow()
    );

    @Test
    public void blockCompactor() {
        assertEquals(1928, test.blockCompactor().compactAndChecksum());
        assertEquals(6401092019345L, real.blockCompactor().compactAndChecksum());
    }

    @Test
    public void fileCompactor() {
        assertEquals(2858, test.fileCompactor().compactByFileAndChecksum());
        assertEquals(6431472344710L, real.fileCompactor().compactByFileAndChecksum());
    }

    @Test
    public void checksum() {
        assertEquals(90, new FileRecord(2, 1, 9).checksum());
    }

}
