package functions;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class TaskPublisher {
    private int line;
    private final List<JTextArea> statusTextArea = new ArrayList<>();
    private final StringBuilder buffer = new StringBuilder();
    private long lastFlushTime = 0;
    private static final long FLUSH_INTERVAL_MS = 500; // Flush every 500ms

    public void subscribe(JTextArea listener) {
        statusTextArea.add(listener);
    }

    public void publish(String message) {
        synchronized (buffer) {
            buffer.append(++line).append(": ").append(message).append("\n");
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastFlushTime >= FLUSH_INTERVAL_MS) {
                flushBuffer();
            }
        }
    }

    private void flushBuffer() {
        if (buffer.length() > 0) {
            String text = buffer.toString();
            SwingUtilities.invokeLater(() -> {
                statusTextArea.forEach(textArea -> {
                    textArea.append(text); // Append, never truncate
                    textArea.setCaretPosition(textArea.getDocument().getLength()); // Scroll to bottom
                });
            });
            buffer.setLength(0);
            lastFlushTime = System.currentTimeMillis();
        }
    }

    public void flush() {
        synchronized (buffer) {
            flushBuffer();
        }
    }
}