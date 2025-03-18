package cleaning;

import gui.WattsAhead;
import objects.ElectricalComponents;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import javax.swing.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ElectricalCleaning {
	ElectricalComponents component;
	List<ElectricalComponents> listComponent = new ArrayList<>();
	public ElectricalCleaning(String inputFile) throws IOException {
		File filePath = new File(inputFile);
		int count = 0;
			try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
				String line;
				boolean isHeader = true;
				while ((line = br.readLine()) != null) {
					String[] rowData = line.split(","); // Split by comma
					count++;
					if(count == 0)continue; 	//header

					component = new ElectricalComponents();

					if(count == 10) break;
					for(String c: rowData) {

						System.out.println(count+"> "+ c);
					}
				}
			} catch (IOException e) {
				JOptionPane.showMessageDialog(null, "Error loading CSV file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
			}


	}

}
