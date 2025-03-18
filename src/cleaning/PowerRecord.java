package cleaning;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
// Record for a CSV row
public record PowerRecord(int id, String deviceId, LocalDateTime time, double voltage1, double current1,
                          double power1, double energy1, double frequency1, double powerFactor1,
                          double voltage2, double current2, double power2, double energy2,
                          double frequency2, double powerFactor2, double voltage3, double current3,
                          double power3, double energy3, double frequency3, double powerFactor3,
                          double kiloWattHr, double rates, double vat, double cost, String data) {

    // Non-canonical constructor that delegates to the canonical constructor
    public PowerRecord(String line, int id) {
        this(parseLine(line, id)); // Delegate to canonical constructor
    }

    // Private constructor to handle parsed values and delegate
    private PowerRecord(Object[] parsed) {
        this((int) parsed[0], (String) parsed[1], (LocalDateTime) parsed[2],
                (double) parsed[3], (double) parsed[4], (double) parsed[5], (double) parsed[6],
                (double) parsed[7], (double) parsed[8], (double) parsed[9], (double) parsed[10],
                (double) parsed[11], (double) parsed[12], (double) parsed[13], (double) parsed[14],
                (double) parsed[15], (double) parsed[16], (double) parsed[17], (double) parsed[18],
                (double) parsed[19], (double) parsed[20], (double) parsed[21], (double) parsed[22],
                (double) parsed[23], (double) parsed[24], (String) parsed[25]);
    }

    public static PowerRecord generateRow(int id, String deviceId, LocalDateTime time, PowerRecord firstRow) {
        PowerRecord fillerRecord = new PowerRecord(
                id, deviceId, time,
                firstRow.voltage1, firstRow.current1, firstRow.power1, firstRow.energy1, firstRow.frequency1, firstRow.powerFactor1,
                firstRow.voltage2, firstRow.current2, firstRow.power2, firstRow.energy2, firstRow.frequency2, firstRow.powerFactor2,
                firstRow.voltage3, firstRow.current3, firstRow.power3, firstRow.energy3, firstRow.frequency3, firstRow.powerFactor3,
                firstRow.kiloWattHr, firstRow.rates, firstRow.vat, firstRow.cost, firstRow.data
        );

        //System.out.println("Missing record:"+missingRecord);
        return fillerRecord;
    }

    // Static method to parse the line into an array of values
    private static Object[] parseLine(String line, int id) {
        String[] fields = line.split(",");
        if (fields.length < 26) throw new IllegalArgumentException("Malformed row: " + line);
        return new Object[] {
                id>0?id:Integer.parseInt(fields[0]), //id
                //Integer.parseInt(fields[0]), // id
                fields[1], // deviceId
               // LocalDateTime.parse(fields[2], DateTimeFormatter.ofPattern("yyyy-M-d HH:mm")), // time
                LocalDateTime.parse(fields[2].replace("\"", ""), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                Double.parseDouble(fields[3]), // voltage1
                Double.parseDouble(fields[4]), // current1
                Double.parseDouble(fields[5]), // power1
                Double.parseDouble(fields[6]), // energy1
                Double.parseDouble(fields[7]), // frequency1
                Double.parseDouble(fields[8]), // powerFactor1
                Double.parseDouble(fields[9]), // voltage2
                Double.parseDouble(fields[10]), // current2
                Double.parseDouble(fields[11]), // power2
                Double.parseDouble(fields[12]), // energy2
                Double.parseDouble(fields[13]), // frequency2
                Double.parseDouble(fields[14]), // powerFactor2
                Double.parseDouble(fields[15]), // voltage3
                Double.parseDouble(fields[16]), // current3
                Double.parseDouble(fields[17]), // power3
                Double.parseDouble(fields[18]), // energy3
                Double.parseDouble(fields[19]), // frequency3
                Double.parseDouble(fields[20]), // powerFactor3
                Double.parseDouble(fields[21]), // kiloWattHr
                Double.parseDouble(fields[22]), // rates
                Double.parseDouble(fields[23]), // vat
                Double.parseDouble(fields[24]), // cost
                fields[25] // data
        };
    }

    public String toCsvLine() {
        return String.format("%d,%s,%s,%.1f,%.2f,%.1f,%.2f,%.1f,%.2f,%.1f,%.2f,%.1f,%.2f,%.1f,%.2f,%.1f,%.2f,%.1f,%.1f,%.1f,%.2f,%.6f,%.2f,%.3f,%.8f,%s",
                id, deviceId, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(time),
                voltage1, current1, power1, energy1, frequency1, powerFactor1,
                voltage2, current2, power2, energy2, frequency2, powerFactor2,
                voltage3, current3, power3, energy3, frequency3, powerFactor3,
                kiloWattHr, rates, vat, cost, data);
    }

    public static String toMissingCsvLine(PowerRecord missingRecord) {
        return String.format("%d,%s,%s,%.1f,%.2f,%.1f,%.2f,%.1f,%.2f,%.1f,%.2f,%.1f,%.2f,%.1f,%.2f,%.1f,%.2f,%.1f,%.1f,%.1f,%.2f,%.6f,%.2f,%.3f,%.8f,%s",
                missingRecord.id, missingRecord.deviceId, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(missingRecord.time),
                missingRecord.voltage1, missingRecord.current1, missingRecord.power1, missingRecord.energy1, missingRecord.frequency1, missingRecord.powerFactor1,
                missingRecord.voltage2, missingRecord.current2, missingRecord.power2, missingRecord.energy2, missingRecord.frequency2, missingRecord.powerFactor2,
                missingRecord.voltage3, missingRecord.current3, missingRecord.power3, missingRecord.energy3, missingRecord.frequency3, missingRecord.powerFactor3,
                missingRecord.kiloWattHr, missingRecord.rates, missingRecord.vat, missingRecord.cost, missingRecord.data);
    }

    // Create a filler record for a gap
    public static PowerRecord createFiller(LocalDateTime time, PowerRecord previous, int powerID) {
        //System.out.println("In powerrecord creator");
        String line = "";

        PowerRecord missingRecord = new PowerRecord(
                powerID, "Power Failure", time, 0.0, 0.0, 0.0, previous.energy1(), 0.0, 0.0,
                0.0, 0.0, 0.0, previous.energy2(), 0.0, 0.0,
                0.0, 0.0, 0.0, previous.energy3(), 0.0, 0.0,
                0.0, 0.0, 0.0, 0.0, "-"
        );

        //System.out.println("Missing record:"+missingRecord);
        return missingRecord;
    }

    public static PowerRecord createInterpolatedRecord(LocalDateTime time, PowerRecord previous, int powerID, double current1, double power1, double energy1,
                                                       double current2, double power2, double energy2,
                                                       double current3, double power3, double energy3) {
        //System.out.println("In powerrecord creator");
        String line = "";

        PowerRecord missingRecord = new PowerRecord(
                powerID, "Network Failure", time, previous.voltage1, current1, power1, energy1, previous.frequency1, previous.powerFactor1,
                previous.voltage2, current2, power2, energy2, previous.frequency2, previous.powerFactor2,
                previous.voltage3, current3, power3, energy3, previous.frequency3, previous.powerFactor3,
                previous.kiloWattHr, previous.rates, previous.vat, previous.cost, previous.data
        );

        //System.out.println("Missing record:"+missingRecord);
        return missingRecord;
    }
}
