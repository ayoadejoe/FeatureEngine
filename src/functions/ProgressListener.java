package functions;

@FunctionalInterface
public interface ProgressListener {
    void onProgressUpdate(long bytesDownloaded, long totalBytes, double percentage);
}