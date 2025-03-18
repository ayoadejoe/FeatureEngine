package cleaning;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class LinearInterpolator {

    private DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");


    private LocalDateTime dateTime = null;

    public boolean interpolate(String inputFile, String outputFile) throws IOException {
        try(Stream<String> readSingleRow = Files.lines(Paths.get(inputFile));
            BufferedWriter writeSingleRow = Files.newBufferedWriter(Paths.get(outputFile))){
            Iterator<String> iterateOverStream = readSingleRow.iterator();

            // Check if the file is empty
            if (!iterateOverStream.hasNext()) {
                throw new IOException("Empty CSV File");
            }

            // Get and write the header
            String header = iterateOverStream.next();
            writeSingleRow.write(header);
            writeSingleRow.newLine();

            // Check if there are any rows after the header
            if (!iterateOverStream.hasNext()) {
                return false; // No rows after header
            }
            

            while(iterateOverStream.hasNext()) {
                String[] row = iterateOverStream.next().split(",", 2);
                String dtime = row[0].replace("\"","");
                System.out.println(dtime);
                dateTime = LocalDateTime.parse(dtime, inputFormatter);
                String remnant = row[1];
                IntStream.range(0, 60).mapToObj(count -> dateTime.plusMinutes(count).format(inputFormatter) + "," + remnant).forEach(csvrow -> {
                    try {
                        writeSingleRow.write(csvrow);
                        writeSingleRow.newLine();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

            }
            return true;
        }

    }

}
