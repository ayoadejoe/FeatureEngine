package merger;

import functions.TaskPublisher;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Fuser {
    private DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public Fuser(String energyFilePath, String weatherFilePath, String economicFilePath, String outputFilePath, TaskPublisher promptPublisher) throws IOException {
        promptPublisher.publish("Fusion initialising...");
        try (BufferedReader energyReader = Files.newBufferedReader(Path.of(energyFilePath));
             BufferedReader weatherReader = Files.newBufferedReader(Path.of(weatherFilePath));
             BufferedReader economicReader = Files.newBufferedReader(Path.of(economicFilePath));
             BufferedWriter outputWriter = Files.newBufferedWriter(Path.of(outputFilePath))) {

            // Read and merge headers
            String[] energyheader = energyReader.readLine().split(",");
            String[] weatherheader = weatherReader.readLine().split(",");
            String[] economicheader = economicReader.readLine().split(",", 2);

            // Customize header merging
            String energymerged = IntStream.range(0, 22)
                    .mapToObj(i -> energyheader[i])
                    .collect(Collectors.joining(","));

            String weathermerged = IntStream.range(0, 11)
                    .filter(i -> i != 0 && i != 5 && i != 10)
                    .mapToObj(i -> weatherheader[i])
                    .collect(Collectors.joining(","));

            String combinedHeaders = energymerged + "," + weathermerged + "," + economicheader[1] + "," + "Availability";
            promptPublisher.publish("Expected Features:"+combinedHeaders);
            outputWriter.write(combinedHeaders);
            outputWriter.newLine();

            promptPublisher.publish("Working... Please wait");
            AtomicInteger y = new AtomicInteger();
            economicReader.lines().forEach(line -> {
                try {
                    String[] economicRow = line.split(",", 2);
                    String[] weatherRow = weatherReader.readLine().split(",");
                    String[] energyRow = energyReader.readLine().split(",");

                    // Merge energy data
                    String mergedEnergy = IntStream.range(0, 22)
                            .mapToObj(i -> energyRow[i])
                            .collect(Collectors.joining(","));

                    // Merge weather data
                    String mergedWeather = IntStream.range(0, 11)
                            .filter(i -> i != 0 && i != 5 && i != 10)
                            .mapToObj(i -> weatherRow[i])
                            .collect(Collectors.joining(","));

                    String combinedInstances = mergedEnergy + "," + mergedWeather + "," + economicRow[1] + "," + availabilityChecker(energyRow);
                    outputWriter.write(combinedInstances);
                    outputWriter.newLine();
                    y.getAndIncrement();

                    // Flush periodically for large files
                    if (y.get() % 1000 == 0) {
                        outputWriter.flush();
                        System.out.println("Flushed at row: " + y.get()); // Debug
                    }
                } catch (IOException e) {
                    System.out.println("Error at row " + y.get() + ": " + e.getMessage());
                    promptPublisher.publish("Error at row " + y.get() + ": " + e.getMessage());
                    throw new RuntimeException(e);
                }
            });

            // Final flush
            outputWriter.flush();
            promptPublisher.publish("Processed rows: " + y.get());
            System.out.println("Processed rows: " + y.get());
        } // Auto-closes all resources here
    }

    private String availabilityChecker(String[] energyheader) {
        String availability = "0";
        double kwhr = 0;

        try {
            String khr = energyheader[21];
            if (khr != null && !khr.trim().isEmpty()) {
                kwhr = Double.parseDouble(khr.trim());
            }
        } catch (IllegalArgumentException | ArrayIndexOutOfBoundsException x) {
            x.printStackTrace();
            kwhr = 0;
        }

        if (kwhr > 0) {
            availability = "1";
        }
        return availability;
    }

    public static void main(String[] args) {
        try {
            new Fuser("duplicate_clean_data.csv", "cleaned_ikeja_weather_data.csv",
                    "clean_nigeria_economic_data.csv", "untrained_availability_data.csv", null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}