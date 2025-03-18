package connections;

import javax.swing.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class ClientConnect extends SwingWorker<Void, String> {
    private final String urlString;
    private final String startDate;
    private final String endDate;
    private final String outputFilePath;
    private final JTextArea logUpdate;
    private final JLabel energyAvailable;
    private final JProgressBar progressBar;
    private final Runnable onDownloadComplete; // Callback function

    public ClientConnect(String urlString, String startDate, String endDate, String outputFilePath,
            JTextArea logUpdate, JLabel energyAvailable, JProgressBar progressBar, Runnable onDownloadComplete) {
		this.urlString = urlString;
		this.startDate = startDate;
		this.endDate = endDate;
		this.outputFilePath = outputFilePath;
		this.logUpdate = logUpdate;
		this.energyAvailable = energyAvailable;
		this.progressBar = progressBar;
		this.onDownloadComplete = onDownloadComplete; // Store callback
	}

	@Override
    protected Void doInBackground() {
        try {
            // Properly encode query parameters
            String encodedStartDate = URLEncoder.encode(startDate, StandardCharsets.UTF_8.toString());
            String encodedEndDate = URLEncoder.encode(endDate, StandardCharsets.UTF_8.toString());

            // Build full request URL with encoded parameters
            String fullUrl = urlString + "?start_date=" + encodedStartDate + "&end_date=" + encodedEndDate;
            publish("Requesting: " + fullUrl);

            // Open HTTP connection
            URL url = new URL(fullUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            // Get total file size from Content-Length (if available)
            int fileSize = conn.getContentLength();
            boolean sizeAvailable = fileSize > 0;
            publish("File size: " + (sizeAvailable ? fileSize / 1024 + " KB" : "Unknown size"));
            String prog = "";
            // Show progress bar activity (Indeterminate mode)
            SwingUtilities.invokeLater(() -> {
                progressBar.setIndeterminate(true);
                progressBar.setString("Fetching...");
            });

            // Check server response
            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (InputStream inputStream = conn.getInputStream();
                     FileOutputStream fileOutputStream = new FileOutputStream(outputFilePath)) {

                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    long totalBytesRead = 0;
                    long startTime = System.currentTimeMillis();

                    publish("Downloading CSV Data...");
                    SwingUtilities.invokeLater(() -> {
                        progressBar.setIndeterminate(false);
                        progressBar.setString("Downloading...");
                        if (sizeAvailable) {
                            progressBar.setMaximum(10000000);
                            progressBar.setValue(0);
                        }
                    });

                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        fileOutputStream.write(buffer, 0, bytesRead);
                        totalBytesRead += bytesRead;

                        // Update progress bar dynamically
                        if (sizeAvailable) {
                            int progress = (int) ((totalBytesRead * 100) / fileSize);
                            setProgress(progress);
                            prog = "Progress:"+ progress + "%";
                            progressBar.setValue(progress);
                            progressBar.setString("Progress:"+ progress + "%");
                            publish("Progress: " + progress + "%");
                        } else {
                        	
                            publish("Downloaded: " + (totalBytesRead / 1024) + " KB");
                            prog = "Downloaded: " + (totalBytesRead / 1024) + " KB";
                            progressBar.setString("Downloaded:" + prog);
                            progressBar.setValue((int)(totalBytesRead / 102400));
                        }

                        // Compute download speed & ETA
                        long elapsedTime = System.currentTimeMillis() - startTime;
                        if (elapsedTime > 500) {
                            double speed = totalBytesRead / (elapsedTime / 1000.0); // Bytes per second
                            double eta = (sizeAvailable) ? ((fileSize - totalBytesRead) / speed) : -1; // Estimated time left in seconds
                            String speedText = "Speed: " + String.format("%.2f", speed / 1024) + " KB/s";
                            String etaText = (eta > 0) ? "ETA: " + (int) eta + "s" : "";
                            prog+=" -->"+speedText + " | " + etaText;
                            progressBar.setString( prog);
                            publish(speedText + " | " + etaText);
                        }
                    }

                    publish("CSV file downloaded successfully: " + outputFilePath);
                    energyAvailable.setText("-->"+(int)(totalBytesRead/1000000)+"MB CSV file available");
                    setProgress(100);
                }
            } else {
                publish("Failed to download CSV. Server response code: " + responseCode);
            }
            conn.disconnect();
        } catch (IOException e) {
            publish("Error: " + e.getMessage());
        }
        return null;
    }

    @Override
    protected void process(java.util.List<String> chunks) {
        // Updates UI in real-time
        SwingUtilities.invokeLater(() -> {
            for (String message : chunks) {
                logUpdate.append("\n" + message);
                logUpdate.setCaretPosition(logUpdate.getDocument().getLength()); // Auto-scroll log
            }
        });
    }

    @Override
    protected void done() {
        SwingUtilities.invokeLater(() -> {
            progressBar.setValue(100);
            progressBar.setString("Completed");
            progressBar.setIndeterminate(false);

            if (onDownloadComplete != null) {
                onDownloadComplete.run(); // Execute callback
            }
        });
    }
}
