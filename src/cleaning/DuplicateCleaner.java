package cleaning;

import functions.TaskPublisher;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.stream.Stream;

public class DuplicateCleaner {
    private static final String PREFERRED_DEVICE = "D8:F1:5B:11:4D:08";
    private final TaskPublisher publisher;

    public DuplicateCleaner(String energyFilePath, String weatherFilePath, String economicFilePath, String outputFilePath, TaskPublisher publisher) throws IOException {
        this.publisher = publisher;
        try (Stream<String> readSingleRow = Files.lines(Paths.get(energyFilePath));
             BufferedWriter writeSingleRow = Files.newBufferedWriter(Paths.get(outputFilePath))) {

            publisher.publish("Starting DuplicateCleaner for file: " + energyFilePath);
            Iterator<String> iterateOverStream = readSingleRow.iterator();

            if (!iterateOverStream.hasNext()) {
                publisher.publish("Empty CSV File detected");
                throw new IOException("Empty CSV File");
            }

            // Write header
            String header = iterateOverStream.next();
            writeSingleRow.write(header);
            writeSingleRow.newLine();
            publisher.publish("Header written: " + header);

            if (!iterateOverStream.hasNext()) {
                publisher.publish("No data rows after header, exiting");
                return; // Only header exists
            }

            // Keep track of the last written record
            String lastWrittenRow = null;
            PowerRecord lastWrittenRecord = null;
            String currentRow = iterateOverStream.next();
            int duplicatesFound = 0;

            while (true) {
                PowerRecord currentRecord = new PowerRecord(currentRow, 0);
                LocalDateTime currentMinute = currentRecord.time().truncatedTo(ChronoUnit.MINUTES);

                if (lastWrittenRecord != null) {
                    LocalDateTime lastMinute = lastWrittenRecord.time().truncatedTo(ChronoUnit.MINUTES);

                    if (currentMinute.equals(lastMinute)) {
                        String currentDevice = currentRow.split(",")[1];
                        String lastDevice = lastWrittenRow.split(",")[1];

                        if (currentDevice.equals(PREFERRED_DEVICE) && !lastDevice.equals(PREFERRED_DEVICE)) {
                            lastWrittenRow = currentRow;
                            lastWrittenRecord = currentRecord;
                            duplicatesFound++;
                            publisher.publish("Duplicate found at " + currentMinute + ", preferring device " + PREFERRED_DEVICE);
                        }
                    } else {
                        writeSingleRow.write(lastWrittenRow);
                        writeSingleRow.newLine();
                        lastWrittenRow = currentRow;
                        lastWrittenRecord = currentRecord;
                    }
                } else {
                    lastWrittenRow = currentRow;
                    lastWrittenRecord = currentRecord;
                }

                if (!iterateOverStream.hasNext()) {
                    writeSingleRow.write(lastWrittenRow);
                    writeSingleRow.newLine();
                    publisher.publish("Processed all rows, duplicates found: " + duplicatesFound);
                    break;
                }

                currentRow = iterateOverStream.next();
            }
            publisher.publish("DuplicateCleaner completed, output written to: " + outputFilePath);
        }
    }

    public static void main(String[] args) {
        try {
            new DuplicateCleaner("cleaned_Raw_Energy.csv", "cleaned_ikeja_weather_data.csv",
                    "clean_nigeria_economic_data.csv", "duplicate_clean_data.csv", new TaskPublisher());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}