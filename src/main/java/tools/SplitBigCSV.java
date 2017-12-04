package tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

public class SplitBigCSV {

	public String path = "";
	
	public void split() throws IOException {
		int end = path.lastIndexOf('.');
		int begin = path.substring(0, end).lastIndexOf('/');
		String fileName = path.substring(begin+1, end);
		
		int files = 0;
		long charCount = 0;
		
		File file = new File("/sdpdata1/github/mysql-2016-09-05/" + fileName + files + ".csv");  
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));  
        CSVWriter csvWriter = new CSVWriter(writer, ',');  
		
		CSVReader reader = new CSVReader(new BufferedReader(new FileReader(path)), ',');
		String[] nextLine = null;
		while ((nextLine = reader.readNext()) != null) {
			csvWriter.writeNext(nextLine);
			for (int i = 0; i < nextLine.length; i++) {
				charCount += nextLine[i].length();
			}
			
			if (charCount > 3000000000L) {
				writer.close();
				csvWriter.close();
				
				files++;
				file =  new File("/sdpdata1/github/mysql-2016-09-05/" + fileName + files + ".csv"); 
				writer = new BufferedWriter(new FileWriter(file));
				csvWriter = new CSVWriter(writer, ',');
				
				charCount = 0;
				System.gc();
			}
		}
		reader.close();
		csvWriter.close();
	}
	
	public static void main(String[] args) throws IOException {
		SplitBigCSV sbc = new SplitBigCSV();
		sbc.path = args[0];
		sbc.split();
	}
}
