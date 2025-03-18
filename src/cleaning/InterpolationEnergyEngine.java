package cleaning;

import functions.TaskPublisher;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class InterpolationEnergyEngine {

    private int powerID = 0;
    private boolean idAlteration = false;
    private TaskPublisher publisher;
    private Predicate<LocalDateTime> firstRowTest = firstDatetime -> firstDatetime.getHour()>0 || firstDatetime.getMinute()>00;
    private Predicate<LocalDateTime> lastRowTest = lastDatetime -> lastDatetime.getHour()<23 || lastDatetime.getMinute()<59;
    public InterpolationEnergyEngine(String energyFile, String outputFile, TaskPublisher publisher, boolean forceFullDay) throws IOException {
        this.publisher = publisher;
        String inputFile = energyFile;
        System.out.println("File received:"+energyFile);
        //create read and write engines
        //MUST BE NIO, we can't afford to read and write files to memory as the files are huge
        //hence NIO package classes used, lazy Read/Write: reading line by line, writing line by line
        //try with resources used to implement Auto-Close, in case reader or writer hooks
        try(Stream<String> readSingleRow = Files.lines(Paths.get(inputFile));
            BufferedWriter writeSingleRow = Files.newBufferedWriter(Paths.get(outputFile))){
            System.out.println("File output:"+outputFile);
            publisher.publish("Launching streams...");
            System.out.println("Launching streams...");
            //now let's take each row and write until we reach where there is inconsistency, we would use the time as the guideline
            Iterator<String> iterateOverStream = readSingleRow.iterator();

            // Check if the file is empty
            if (!iterateOverStream.hasNext()) {
                publisher.publish("Empty CSV File");
                throw new IOException("Empty CSV File");
            }

            //after this, we need to check if the file is not empty and if it has header
            // Get and write the header
            String header = iterateOverStream.next();
            writeSingleRow.write(header);
            writeSingleRow.newLine();

            publisher.publish("Header acquired: "+header);
            System.out.println("Header acquired: "+header);
            // Check if there are any rows after the header
            if (!iterateOverStream.hasNext()) {
                return; // No rows after header
            }

            // Use an iterator or collect the stream to compare consecutive rows
            String previousRow = iterateOverStream.next();      //inicial row would be ok so write it
            PowerRecord firstRow = new PowerRecord(previousRow, 0);

            //did the entryrow start at midnight?
            if(forceFullDay)detectEntryRowGap( firstRow, writeSingleRow);

            //continue
            powerID = firstRow.id();

            writeSingleRow.write(previousRow);
            writeSingleRow.newLine();   //move to next line

            PowerRecord current = null;

            while (iterateOverStream.hasNext()){        //oya, continue to iterate over the stream
                String currentRow = iterateOverStream.next();   //row to investigate if it follows the order
                if(detectDoubleMinutes(previousRow, currentRow))continue;
                detectDateGaps(previousRow, currentRow, writeSingleRow);
                powerID++;
                current = new PowerRecord(currentRow, powerID);
                if(detectDoubleMinutes(previousRow, currentRow))continue;
                writeSingleRow.write(current.toCsvLine()); // Write the current row after checking
                writeSingleRow.newLine();
                previousRow = currentRow; // Update previousRow for the next iteration
            }

            if(idAlteration)System.out.println("Gaps were detected!");
            //acquire last row, send to check for gap
            if(forceFullDay)
            if(current != null){
                detectLastRowGap(current, writeSingleRow);
            }
            System.out.println("COmpleted");
        }
    }

    private boolean detectDoubleMinutes(String previousRow, String currentRow) {
        PowerRecord previousRecord = new PowerRecord(previousRow, 0);
        PowerRecord currentRecord = new PowerRecord(currentRow,0);
        LocalDateTime previousTime = previousRecord.time();
        LocalDateTime currentTime = currentRecord.time();

        //While not comparing against the seconds, the moment,there are two same minute, skip the record
        if (previousTime.truncatedTo(ChronoUnit.MINUTES).equals(currentTime.truncatedTo(ChronoUnit.MINUTES))) {
            return true;
        }
        return false;
    }


    /*
                int response = JOptionPane.showConfirmDialog(wattsAhead, "The first instance of this selection does not " +
                    "start at midnight as required for complete coverage, if you deliberately selected a period after midnight, " +
                    "please cancel. If not, select 'yes' for cleaning to proceed.");

            //if response is yes, then it means there are missing rows at the beginning/no power or network. In this case, we won't know which except we go back and query the row before
            //this would be a technical debt - for now, interpolate upward to midnight, we would assume power failure
     */
    private void detectEntryRowGap( PowerRecord firstRow, BufferedWriter writeSingleRow) throws IOException {
        LocalDateTime entryRowDatetime = firstRow.time();
        if(firstRowTest.test(entryRowDatetime)){
            System.out.println("The data did not start at midnight. Early data missing...");
            publisher.publish("The data did not start at midnight. Early data missing...");
            LocalDateTime midnight = firstRow.time().toLocalDate().atStartOfDay();
            long minutesBeforeMidnight = ChronoUnit.MINUTES.between(midnight, firstRow.time())+1;
            powerID = firstRow.id() - (int)minutesBeforeMidnight;
            publisher.publish(minutesBeforeMidnight+" minutes missing. Filling up data. Starting from ID:"+powerID+" to "+firstRow.id());
            PowerRecord previousInstance = PowerRecord.createFiller( midnight.minusMinutes(1), firstRow, powerID );
            PowerRecord currentInstance = firstRow;
            processPowerFailureGaps(previousInstance, currentInstance, writeSingleRow);
        }
    }

    /*int response = JOptionPane.showConfirmDialog(wattsAhead, "The last instance of this selection does not " +
                       "end at midnight as required for complete coverage, if you deliberately selected a period after midnight, " +
                       "please cancel. If not, select 'yes' for cleaning to proceed.");

               //if response is yes, then it means there are missing rows at the beginning/no power or network. In this case, we won't know which except we go back and query the row before
               //this would be a technical debt - for now, interpolate upward to midnight, we would assume power failure
                */
    private void detectLastRowGap( PowerRecord lastRow, BufferedWriter writeSingleRow) throws IOException {
        LocalDateTime lastRowDatetime = lastRow.time();
        if(lastRowTest.test(lastRowDatetime)){
            System.out.println("The data did not end at midnight. Final data missing...");
            publisher.publish("The data did not end at midnight. Final data missing...");
            LocalDateTime beforeMidnight = lastRow.time().toLocalDate().atTime(LocalTime.MAX);
            PowerRecord finalInstance = PowerRecord.createFiller( beforeMidnight.plusMinutes(1), lastRow, lastRow.id());
            PowerRecord currentInstance = lastRow;
            publisher.publish("Filling up rows. Continuing from: "+lastRow.id());
            processPowerFailureGaps(currentInstance, finalInstance, writeSingleRow);
        }

    }


    private void detectDateGaps(String previousRow, String currentRow, BufferedWriter writeSingleRow) throws IOException {
        PowerRecord previousInstance = new PowerRecord(previousRow, 0);
        PowerRecord currentInstance = new PowerRecord(currentRow, 0);

        if((currentInstance.energy1()-previousInstance.energy1()>0.15) && (currentInstance.energy2()-previousInstance.energy2()>0.15) && (currentInstance.energy3()-previousInstance.energy3()>0.15)){
            //means, there was internet downtime, so power kept accumulating locally
            //since power accumulated locally, means there was no power failure, hence, we have to interpolate
            //for this work, we will use linear interpolation
            processNetworkDowntimeGaps(previousInstance, currentInstance, writeSingleRow);
        }else{
            //if power failure, that is energy did not change, then fill with same energy values
            processPowerFailureGaps(previousInstance, currentInstance, writeSingleRow);
        }

    }

    private void processNetworkDowntimeGaps(PowerRecord previousInstance, PowerRecord currentInstance, BufferedWriter writeSingleRow) throws IOException {
        if(ChronoUnit.SECONDS.between(previousInstance.time(), currentInstance.time())>59){
            System.out.println("This is a Network failure");
            publisher.publish("Network Failure Detected at Row:"+previousInstance.id());
        }
        LocalDateTime previous = previousInstance.time();
        publisher.publish("Interpolation starting>> Row:"+previousInstance.id());
        while(ChronoUnit.SECONDS.between(previous, currentInstance.time())>59){
            powerID++;
            InterpolationEnergyFunctions interpol = new InterpolationEnergyFunctions();
            double stepValue1 = interpol.getStepValue(previousInstance.energy1(), currentInstance.energy1(), previousInstance.time(), currentInstance.time());
            double interpolatedEnergy1 = interpol.generateInterpolatedEnergy(stepValue1, previousInstance.energy1());
            double interpolatedPower1 = interpol.getPower(previousInstance.energy1(), interpolatedEnergy1);
            double interpolatedCurrent1 = interpol.getCurrent(interpolatedPower1, previousInstance.powerFactor1(), previousInstance.voltage1());

            double stepValue2 = interpol.getStepValue(previousInstance.energy2(), currentInstance.energy2(), previousInstance.time(), currentInstance.time());
            double interpolatedEnergy2 = interpol.generateInterpolatedEnergy(stepValue2, previousInstance.energy2());
            double interpolatedPower2 = interpol.getPower(previousInstance.energy2(), interpolatedEnergy2);
            double interpolatedCurrent2 = interpol.getCurrent(interpolatedPower2, previousInstance.powerFactor2(), previousInstance.voltage2());

            double stepValue3 = interpol.getStepValue(previousInstance.energy3(), currentInstance.energy3(), previousInstance.time(), currentInstance.time());
            double interpolatedEnergy3 = interpol.generateInterpolatedEnergy(stepValue3, previousInstance.energy3());
            double interpolatedPower3 = interpol.getPower(previousInstance.energy3(), interpolatedEnergy3);
            double interpolatedCurrent3 = interpol.getCurrent(interpolatedPower3, previousInstance.powerFactor3(), previousInstance.voltage3());

            PowerRecord missingRecord = PowerRecord.createInterpolatedRecord(previous.plusMinutes(1), previousInstance, powerID, interpolatedCurrent1,
                    interpolatedPower1, interpolatedEnergy1, interpolatedCurrent2, interpolatedPower2, interpolatedEnergy2, interpolatedCurrent3,
                    interpolatedPower3, interpolatedEnergy3);

            writeSingleRow.write(PowerRecord.toMissingCsvLine(missingRecord));
            writeSingleRow.newLine();   //move to next line
            previous = missingRecord.time();
            previousInstance = missingRecord;
            idAlteration = true;
        }
        publisher.publish("Interpolation completed for gap ending at Row: " + previousInstance.id());
    }


    private void processPowerFailureGaps(PowerRecord previousInstance, PowerRecord currentInstance, BufferedWriter writeSingleRow) throws IOException {
        if(ChronoUnit.SECONDS.between(previousInstance.time(), currentInstance.time())>59)System.out.println("This is a Power failure");
        LocalDateTime previous = previousInstance.time();
        publisher.publish("Interpolation in starting>> Row:"+previousInstance.id());
        while(ChronoUnit.SECONDS.between(previous, currentInstance.time())>59){
            powerID++;
            publisher.publish("Interpolation in progress>> Row:"+previousInstance.id());
            System.out.println("PowerID2:"+powerID);
            PowerRecord missingRecord = PowerRecord.createFiller(previous.plusMinutes(1), previousInstance, powerID);
            writeSingleRow.write(PowerRecord.toMissingCsvLine(missingRecord));
            writeSingleRow.newLine();   //move to next line
            previous = missingRecord.time();
            idAlteration = true;
        }
        publisher.publish("Interpolation ends>> Row:"+previousInstance.id());
    }

    public static void main(String[] a){
        try {
            new InterpolationEnergyEngine("", "", null, false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
