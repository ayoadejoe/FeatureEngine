package connections;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class JsonCsvConnect implements Runnable {
    private static final String API_KEY = "4e7962db4cmshee7a6cf5f283a50p1567c8jsn3404f0a1b14c"; // Replace with your actual API key

    private static final String OUTPUT_CSV = "weather_data_ikeja.csv";
    private static final double LAT = 6.6018; // Ikeja, Lagos
    private static final double LON = 3.3515;
    private static final int ALT = 0; // Approximate altitude
    private static final LocalDate START_DATE = LocalDate.of(2023, 8, 1);
    private static final LocalDate END_DATE = LocalDate.now(); // Today, March 9, 2025
    private static final int DAYS_PER_REQUEST = 31;

    @Override
    public void run() {
        try {
            List<String> allJsonResponses = fetchAllJsonData();
            System.out.println("Combining and converting all responses");
            convertJsonToCsv(allJsonResponses);
            System.out.println("CSV conversion completed successfully");
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private List<String> fetchAllJsonData() throws IOException {
        List<String> responses = new ArrayList<>();
        LocalDate currentStart = START_DATE;
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;

        while (!currentStart.isAfter(END_DATE)) {
            LocalDate currentEnd = currentStart.plusDays(DAYS_PER_REQUEST - 1);
            if (currentEnd.isAfter(END_DATE)) {
                currentEnd = END_DATE;
            }

            String url = String.format(
                    "https://meteostat.p.rapidapi.com/point/hourly?lat=%f&lon=%f&alt=%d&start=%s&end=%s",
                    LAT, LON, ALT, currentStart.format(formatter), currentEnd.format(formatter)
            );
            String jsonResponse = fetchJsonFromApi(url);
            responses.add(jsonResponse);
            System.out.println("Fetched data for " + currentStart + " to " + currentEnd);

            // Add delay to avoid rate limiting (optional, adjust as needed)
            try {
                Thread.sleep(1000); // 1-second delay between requests
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            currentStart = currentEnd.plusDays(1);
        }
        return responses;
    }

    private String fetchJsonFromApi(String apiUrl) throws IOException {
        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("x-rapidapi-host", "meteostat.p.rapidapi.com");
        conn.setRequestProperty("x-rapidapi-key", API_KEY);
        conn.setRequestProperty("Accept", "application/json");

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
                StringBuilder error = new StringBuilder();
                String line;
                while ((line = errorReader.readLine()) != null) {
                    error.append(line);
                }
                throw new IOException("HTTP " + responseCode + ": " + error.toString());
            }
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        } finally {
            conn.disconnect();
        }
    }

    private void convertJsonToCsv(List<String> jsonResponses) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        Set<String> headers = new LinkedHashSet<>();
        List<JsonNode> allRecords = new ArrayList<>();

        for (String json : jsonResponses) {
            JsonNode rootNode = mapper.readTree(json);
            JsonNode hourlyData = rootNode.path("data");
            if (!hourlyData.isArray()) {
                throw new IOException("Expected array in 'data' field");
            }
            for (JsonNode node : hourlyData) {
                Iterator<String> fieldNames = node.fieldNames();
                while (fieldNames.hasNext()) {
                    headers.add(fieldNames.next());
                }
                allRecords.add(node);
            }
        }
        List<String> headerList = new ArrayList<>(headers);

        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(OUTPUT_CSV))) {
            writer.write(String.join(",", headerList));
            writer.newLine();

            int rowCount = 0;
            for (JsonNode node : allRecords) {
                StringBuilder row = new StringBuilder();
                for (String header : headerList) {
                    JsonNode value = node.path(header);
                    if (value.isMissingNode() || value.isNull()) {
                        row.append(",");
                    } else if (value.isNumber()) {
                        row.append(value.asDouble()).append(",");
                    } else {
                        row.append("\"").append(value.asText().replace("\"", "\"\"")).append("\",");
                    }
                }
                String rowString = row.substring(0, row.length() - 1);
                writer.write(rowString);
                writer.newLine();
                rowCount++;
            }
            System.out.println("Wrote " + rowCount + " rows");
        }
    }

    public static void main(String[] args) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        JsonCsvConnect converter = new JsonCsvConnect();
        executor.submit(converter);
        executor.shutdown();
        System.out.println("Conversion task submitted");
    }
}