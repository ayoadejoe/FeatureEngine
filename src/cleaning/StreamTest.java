package cleaning;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class StreamTest {


    public static void main(String[] args) {
        String inputFile = "Requested.csv";
        String outputFile = "output_filled.csv";

        try (Stream<String> lines = Files.lines(Paths.get(inputFile));
             BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputFile))) {
            // Write header
            String header = lines.findFirst().orElseThrow(() -> new IOException("Empty CSV"));
            writer.write(header);
            writer.newLine();

            // Process data lines with gap filling
            try (Stream<String> dataLines = Files.lines(Paths.get(inputFile)).skip(1)) {
                GapFillingSpliterator gapFiller = new GapFillingSpliterator(dataLines.spliterator());
                Stream<PowerRecord> filledStream = StreamSupport.stream(gapFiller, false);

                filledStream.forEach(record -> {
                    try {
                        writer.write(record.toCsvLine());
                        writer.newLine();
                    } catch (IOException e) {
                        System.err.println("Error writing: " + e.getMessage());
                    }
                });
            }
            System.out.println("Filled CSV written to " + outputFile);
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}