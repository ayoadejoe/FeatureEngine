package connections;

import functions.ProgressListener;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class FileDownloader {
    /**
     * Downloads a file from the specified URL and saves it to the given local path,
     * with progress updates sent to the ProgressListener.
     *
     * @param urlString The URL of the file to download
     * @param outputFilePath The default local path where the file will be saved
     * @param listener The ProgressListener to receive download progress updates
     * @throws IOException If there’s an error during download (e.g., network issue, invalid URL)
     */
    public static void downloadFile(String urlString, String outputFilePath, ProgressListener listener) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        // Connect and get response details
        connection.connect();
        int responseCode = connection.getResponseCode();
        System.out.println("Response Code: " + responseCode);
        System.out.println("Content-Type: " + connection.getContentType());

        // Determine the filename from Content-Disposition, fallback to outputFilePath
        String filename = outputFilePath;
        String contentDisposition = connection.getHeaderField("Content-Disposition");
        if (contentDisposition != null && contentDisposition.contains("filename=")) {
            filename = contentDisposition.split("filename=")[1].replace("\"", "").trim();
            System.out.println("Filename from server: " + filename);
        } else {
            System.out.println("Using default filename: " + filename);
        }

        long totalBytes = connection.getContentLengthLong(); // Get total size if available
        System.out.println("Total bytes to download: " + totalBytes);

        // Handle the response
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (BufferedInputStream in = new BufferedInputStream(connection.getInputStream());
                 FileOutputStream fileOutputStream = new FileOutputStream(filename)) {
                byte[] buffer = new byte[1024];
                long bytesDownloaded = 0;
                int bytesRead;

                while ((bytesRead = in.read(buffer)) != -1) {
                    fileOutputStream.write(buffer, 0, bytesRead);
                    bytesDownloaded += bytesRead;
                    double percentage = (totalBytes > 0) ? (bytesDownloaded * 100.0 / totalBytes) : -1.0;
                    listener.onProgressUpdate(bytesDownloaded, totalBytes, percentage);
                }
                System.out.println("File downloaded successfully to: " + filename);
            }
        } else {
            // Read error stream if available
            StringBuilder errorMessage = new StringBuilder();
            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()))) {
                String line;
                while ((line = errorReader.readLine()) != null) {
                    errorMessage.append(line).append("\n");
                }
            }
            throw new IOException("Failed to download file. HTTP response code: " + responseCode +
                    (errorMessage.length() > 0 ? ". Error: " + errorMessage.toString() : ""));
        }

        // Close the connection
        connection.disconnect();
    }
}