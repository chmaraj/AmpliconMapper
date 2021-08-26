package ampliconmapper;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.scene.control.TextArea;
import javafx.application.Platform;

public class DegenMethods {

	private static HashMap<Character, Character[]> degenerates = new HashMap<Character, Character[]>();
	private static Pattern degenRegex;
	
	public static void processPrimers(File primerFile, ArrayList<String> startPrimers, ArrayList<String> endPrimers, TextArea outputField) {
	
		degenerates.put('R', new Character[] {'A', 'G'});
		degenerates.put('Y', new Character[] {'C', 'T'});
		degenerates.put('S', new Character[] {'G', 'C'});
		degenerates.put('W', new Character[] {'A', 'T'});
		degenerates.put('K', new Character[] {'G', 'T'});
		degenerates.put('M', new Character[] {'A', 'C'});
		degenerates.put('B', new Character[] {'G', 'C', 'T'});
		degenerates.put('D', new Character[] {'A', 'G', 'T'});
		degenerates.put('H', new Character[] {'A', 'C', 'T'});
		degenerates.put('V', new Character[] {'A', 'C', 'G'});
		degenerates.put('N', new Character[] {'A', 'C', 'G', 'T'});
	
		// Need to generate the degen Regex
		// Unfortunately cannot convert directly from Object[] to char[], or even from Character[] to char[]
		Character[] degenCharArray = degenerates.keySet().toArray(new Character[degenerates.keySet().size()]);
		char[] charDegen = new char[degenCharArray.length];
		for(int i = 0; i < charDegen.length; i++) {
			charDegen[i] = (char)degenCharArray[i];
		}
		String degenRegexString = String.join("", new String(charDegen));
		degenRegex = Pattern.compile("[" + degenRegexString + "]");
		
		// This regex will find any incompatible characters in the primer sequences
		Pattern regex = Pattern.compile("[^ATCGRYSWKMBDHVN]");
		try (BufferedReader reader = new BufferedReader(new FileReader(primerFile))){
			String line = "";
			while((line = reader.readLine()) != null) {
				if(!line.trim().isEmpty()) {
					String[] entry = line.trim().split(",");
					
					// Check for illegal bases
					Matcher matcher = regex.matcher(entry[2]);
					if(matcher.find()) {
						Platform.runLater(() -> outputField.appendText("Primer sequence contains incompatible characters:\n" + entry[0] + "\n"));
						return;
					}
					
					// If the sequence contains degenerated bases, create sequences for all possible iterations
					Matcher degenMatcher = degenRegex.matcher(entry[2]);
					if(degenMatcher.find()) {
						ArrayList<String> expandedSeq = expandDegenerated(entry[2], 0, new ArrayList<String>());
						
						// Remove the original entry which contained degenerate bases, replace with all the possible sequences
						for(String sequence : expandedSeq) {
							if(entry[1].equals("START")) {
								startPrimers.add(sequence);
							}else if(entry[1].equals("END")) {
								endPrimers.add(sequence);
							}
						}
					}else {
						if(entry[1].equals("START")) {
							startPrimers.add(entry[2]);
						}else if(entry[1].equals("END")) {
							endPrimers.add(entry[2]);
						}
					}
				}
			}
		}catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	// Expand the sequences that contain degenerate bases into every possibility
	public static ArrayList<String> expandDegenerated(String seq, int index, ArrayList<String> primerContainer){		
		
		char[] seqChars = seq.toCharArray();
		// Due to recursive nature, need to keep going from where we left off
		for(int i = index; i < seq.length(); i++) {
			char c = seqChars[i];
			
			// Check if the current character is contained in the list of degenerate bases. If so, replace this instance with each possible base.
			if(degenerates.keySet().contains(c)) {
				for(char s : degenerates.get(c)) {
					String newSeq = seq.replaceFirst(Character.toString(c), Character.toString(s));
					Matcher matcher = degenRegex.matcher(newSeq);
					
					// Check the resulting primers.
					// If more degenerate bases are found, do the same as above, but starting from the base following the one just replaced.
					if(matcher.find()) {
						int j = i + 1;
						ArrayList<String> expanded = expandDegenerated(newSeq, j, primerContainer);
						
					// If no more degenerate bases are found, we have reached the end, and can add the sequence to the list to be returned.
					}else {
						primerContainer.add(newSeq);
					}
				}
			}
		}
		return primerContainer;
	}
}
