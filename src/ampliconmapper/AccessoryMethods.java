package ampliconmapper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;

import java.util.HashMap;

import java.util.ArrayList;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import javafx.application.Platform;
import javafx.scene.control.TextArea;

public class AccessoryMethods {

	public static void makeConsolidatedTSV(File refFile, File inputDir, File outputDir) {
		String line;
		HashMap<String, HashMap<String, Integer>> consolidated = new HashMap<String, HashMap<String, Integer>>(); 
		ArrayList<String> header = new ArrayList<String>();
		
		// Read the reference file, and put the reference IDs as keys in the consolidated dictionary
		try(BufferedReader reader = new BufferedReader(new FileReader(refFile))) {
			while((line = reader.readLine()) != null) {
				if(line.startsWith(">")) {
					String entry = line.split(">")[1];
					consolidated.put(entry, new HashMap<String, Integer>());
				}
			}
		}catch(IOException e) {
			e.printStackTrace();
		}
		
		header.add("Reference");
		
		// Read through each tsv in the input directory
		for(File file : inputDir.listFiles()) {
			String sampleName = file.getName().split(".tsv")[0];
			header.add(sampleName);
			try(BufferedReader reader = new BufferedReader(new FileReader(file))){
				while((line = reader.readLine()) != null) {
					if(line.startsWith("#name")) {
						continue;
					}
					// {#name, %unambiguousReads, unambiguousMB, %ambiguousReads, ambiguousMB, unambiguousReads, ambiguousReads, assignedReads, assignedBases}
					String[] lines = line.split("\t");
					
					// The tsv from BLAST will produce truncated references if they contain spaces
					String refHit = lines[0];
					for(String ref : consolidated.keySet()) {
						if(ref.contains(refHit)) {
							refHit = ref;
						}
					}
					int numReads = Integer.parseInt(lines[7]);
					consolidated.get(refHit).put(sampleName, numReads);
				}
			}catch(IOException e) {
				e.printStackTrace();
			}
		}
		
		try(FileWriter writer = new FileWriter(outputDir + "\\consolidated_report.tsv")) {
			// Write out the header for the tsv file
			writer.write(String.join("\t", header));
			writer.write(System.getProperty("line.separator"));
			
			// Get the references
			String[] keys = consolidated.keySet().toArray(new String[consolidated.keySet().size()]);
			for(String ref : keys) {
				
				// Each reference is associated with a number of assignedReads from each sample (can be 0) 
				HashMap<String, Integer> currentReference = consolidated.get(ref);
				String[] currentSamples = currentReference.keySet().toArray(new String[currentReference.keySet().size()]);
				
				// Have to set up the current line to be written, so start with a bunch of empty cells
				ArrayList<String> currentLine = new ArrayList<String>();
				for(int i = 0; i < header.size(); i++) {
					currentLine.add("");
				}
				
				// Set the first cell to be the reference, then set any other cells to be their appropriate value
				currentLine.set(0, ref);
				for(String sample : currentSamples) {
					int index = header.indexOf(sample);
					currentLine.set(index, Integer.toString(currentReference.get(sample)));
				}
				
				// Write out the line
				writer.write(String.join("\t", currentLine));
				writer.write("\n");
			}
		}catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void makeQALog(File qLog, String version, int minLen, int maxLen, double minID, File outputDir, File refFile, File inputDir, File primerFile) {
		try(FileWriter writer = new FileWriter(qLog)) {
			String sep = System.getProperty("line.separator");
			writer.write("Amplicon Mapper Version: " + version);
			writer.write(sep);
			DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
			Date date = new Date();
			writer.write("Date run: " + dateFormat.format(date));
			writer.write(sep);
			writer.write("Run by user: " + System.getProperty("user.name"));
			writer.write(sep);
			writer.write("Settings: minLength=" + Integer.toString(minLen) + " maxLength=" + Integer.toString(maxLen) + " minIdentity=" + Double.toString(minID));
			writer.write(sep);
			writer.write("Output Folder: " + outputDir.getAbsolutePath());
			writer.write(sep);
			writer.write("Reference File: " + refFile.getAbsolutePath());
			writer.write(sep);
			writer.write("Primer File: " + primerFile.getAbsolutePath());
			writer.write(sep);
			writer.write("Input File(s) :");
			for(File file : inputDir.listFiles()) {
				writer.write(sep);
				writer.write(file.getAbsolutePath());
			}
			writer.close();
		}catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	public static boolean checkVersion() {
		try{
			URL url = new URL("https://github.com/chmaraj/AmpliconMapper/releases");
			try{
				URLConnection connection = url.openConnection();
				InputStream in = connection.getInputStream();
				BufferedReader reader = new BufferedReader(new InputStreamReader(in));
				String line = "";
				int latestVersion = 0;
				while((line = reader.readLine()) != null) {
					if(line.contains("<a href=\"/chmaraj/AmpliconMapper/releases/tag")){
						if(line.contains("</a>")) {
							String version_name = line.split(">")[1];
							version_name = version_name.split("<")[0];
							version_name = version_name.split(" ")[version_name.split(" ").length - 1];
							version_name = version_name.substring(1, version_name.length());
							String[] splitVersion = version_name.split("\\.");
							version_name = String.join("", splitVersion);
							if(Integer.parseInt(version_name) > latestVersion) {
								latestVersion = Integer.parseInt(version_name);
							}
						}
					}
				}
				String currentVersion = String.join("", WelcomePage.version.substring(1, WelcomePage.version.length()).split("\\."));
				if(Integer.parseInt(currentVersion) == latestVersion) {
					return true;
				}
			}catch(IOException e) {
				e.printStackTrace();
			}
		}catch(MalformedURLException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public static void logMessage(TextArea outputField, String msg) {
		Platform.runLater(() -> outputField.appendText("\n" + msg + "\n"));
	}
}
