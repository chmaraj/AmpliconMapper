package ampliconmapper;

import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.scene.Scene;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.RowConstraints;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.concurrent.Task;
import javafx.application.Platform;
import javafx.scene.control.Tooltip;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.control.ProgressBar;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.lang.Process;
import java.lang.Runtime;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import javax.swing.JOptionPane;

public class IonTorrentMapper implements Page{
	
	private String separator = File.separator;
	private Stage primaryStage;
	private File readDirectory, outputDirectory, referenceFile, primerFile, FastQCLocation, BBToolsLocation, PicardLocation, TabletLocation;
	private File fastQCOutput, trimmed, bbMapSam, bbMapTSV, bbMapBam, bamSorted;
	private ArrayList<String> sampleList = new ArrayList<String>();
	private boolean currentlyRunning = false;
	private ExecutorService mainPool = null;
	private ArrayList<Process> mainProcesses= new ArrayList<Process>();
	private int threads = Runtime.getRuntime().availableProcessors();
	private ArrayList<String> startPrimers = new ArrayList<String>(), endPrimers = new ArrayList<String>();
	private String fileEnding = "";
	private int mapperMinLen = 200, mapperMaxLen = 450;
	private double mapperMinID = 0.95;
	private String javaCall;

	public IonTorrentMapper(Stage primaryStage, File FastQCLocation, File BBToolsLocation, File PicardLocation, File TabletLocation, String javaCall) {
		this.primaryStage = primaryStage;
		this.FastQCLocation = FastQCLocation;
		this.BBToolsLocation = BBToolsLocation;
		this.PicardLocation = PicardLocation;
		this.TabletLocation = TabletLocation;
		this.javaCall = javaCall;
	}
	
	public void run() {
		GridPane pane = new GridPane();
		ColumnConstraints cC = new ColumnConstraints();
		cC.setPercentWidth(2);
		List<ColumnConstraints> colCopies = Collections.nCopies(50, cC);
		pane.getColumnConstraints().addAll(colCopies);
		RowConstraints rC = new RowConstraints();
		rC.setPercentHeight(2);
		List<RowConstraints> rowCopies = Collections.nCopies(50, rC);
		pane.getRowConstraints().addAll(rowCopies);
		
		Text disclaimerText = new Text("Disclaimer: this pipeline will create a variety of directories within the designated output directory. \n"
				+ "Also please note that this pipeline will run for an extended period, so try to run overnight if possible.");
		disclaimerText.setId("assembly_disclaimer");
		disclaimerText.setTextAlignment(TextAlignment.CENTER);	
		HBox disclaimerBox = new HBox(10);
		disclaimerBox.setAlignment(Pos.CENTER);
		disclaimerBox.getChildren().add(disclaimerText);
		pane.add(disclaimerBox, 0, 0, 50, 2);
		
		Text inputPrompt = new Text("Please enter a directory which contains the raw reads");
		inputPrompt.setTextAlignment(TextAlignment.LEFT);
		HBox inputBox = new HBox(10);
		inputBox.setAlignment(Pos.CENTER_LEFT);
		inputBox.getChildren().add(inputPrompt);
		pane.add(inputBox, 2, 3, 20, 2);
		
		TextField readInput = new TextField();
		readInput.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		readInput.setEditable(false);
		readInput.setTooltip(new Tooltip("A directory containing only raw read sequences in .fastq.gz, .fasta, .fa, .fna, or .ffn format"));
		pane.add(readInput, 2, 5, 30, 2);
		
		readInput.setOnDragOver(new EventHandler<DragEvent>() {
			public void handle(DragEvent e) {
				if(e.getGestureSource() != readInput && e.getDragboard().hasFiles()) {
					e.acceptTransferModes(TransferMode.COPY_OR_MOVE);
				}
				e.consume();
			}
		});
		readInput.setOnDragDropped(new EventHandler<DragEvent>(){
			public void handle(DragEvent e) {
				Dragboard db = e.getDragboard();
				boolean success = false;
				if(db.hasFiles()) {
					List<File> listFiles = db.getFiles();
					readDirectory = listFiles.get(0);
					readInput.setText(readDirectory.getAbsolutePath());
					success = true;
				}
				e.setDropCompleted(success);
				e.consume();
			}
		});
		
		Button browseInputs = new Button("Browse...");
		browseInputs.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		browseInputs.getStyleClass().add("browseButton");
		pane.add(browseInputs, 34, 5, 4, 2);
		browseInputs.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent e) {
				DirectoryChooser directoryChooser = new DirectoryChooser();
				readDirectory = directoryChooser.showDialog(primaryStage);
				readInput.setText(readDirectory.getAbsolutePath());
			}
		});
		
		Text refPrompt = new Text("Please enter a reference file");
		refPrompt.setTextAlignment(TextAlignment.LEFT);
		HBox refBox = new HBox(10);
		refBox.setAlignment(Pos.CENTER_LEFT);
		refBox.getChildren().add(refPrompt);
		pane.add(refBox, 2, 7, 20, 2);
		
		TextField refFileField = new TextField();
		refFileField.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		refFileField.setEditable(false);
		refFileField.setTooltip(new Tooltip("A fasta file containing the reference(s) to map reads to"));
		pane.add(refFileField, 2, 9, 30, 2);
		
		refFileField.setOnDragOver(new EventHandler<DragEvent>() {
			public void handle(DragEvent e) {
				if(e.getGestureSource() != refFileField && e.getDragboard().hasFiles()) {
					e.acceptTransferModes(TransferMode.COPY_OR_MOVE);
				}
				e.consume();
			}
		});
		refFileField.setOnDragDropped(new EventHandler<DragEvent>(){
			public void handle(DragEvent e) {
				Dragboard db = e.getDragboard();
				boolean success = false;
				if(db.hasFiles()) {
					List<File> listFiles = db.getFiles();
					referenceFile = listFiles.get(0);
					refFileField.setText(referenceFile.getAbsolutePath());
					success = true;
				}
				e.setDropCompleted(success);
				e.consume();
			}
		});
		
		Button browseRef = new Button("Browse...");
		browseRef.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		browseRef.getStyleClass().add("browseButton");
		pane.add(browseRef, 34, 9, 4, 2);
		browseRef.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent e) {
				FileChooser fileChooser = new FileChooser();
				referenceFile = fileChooser.showOpenDialog(primaryStage);
				refFileField.setText(referenceFile.getAbsolutePath());
			}
		});
		
		Text outputPrompt = new Text("Please enter a directory which will receive the output");
		outputPrompt.setTextAlignment(TextAlignment.LEFT);
		HBox outputBox = new HBox(10);
		outputBox.setAlignment(Pos.CENTER_LEFT);
		outputBox.getChildren().add(outputPrompt);
		pane.add(outputBox, 2, 11, 20, 2);
		
		TextField outputFileField = new TextField();
		outputFileField.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		outputFileField.setEditable(false);
		outputFileField.setTooltip(new Tooltip("An empty directory which will receive the output files"));
		pane.add(outputFileField, 2, 13, 30, 2);
		
		outputFileField.setOnDragOver(new EventHandler<DragEvent>() {
			public void handle(DragEvent e) {
				if(e.getGestureSource() != outputFileField && e.getDragboard().hasFiles()) {
					e.acceptTransferModes(TransferMode.COPY_OR_MOVE);
				}
				e.consume();
			}
		});
		outputFileField.setOnDragDropped(new EventHandler<DragEvent>(){
			public void handle(DragEvent e) {
				Dragboard db = e.getDragboard();
				boolean success = false;
				if(db.hasFiles()) {
					List<File> listFiles = db.getFiles();
					outputDirectory = listFiles.get(0);
					outputFileField.setText(outputDirectory.getAbsolutePath());
					success = true;
				}
				e.setDropCompleted(success);
				e.consume();
			}
		});
		
		Button browseOutputs = new Button("Browse...");
		browseOutputs.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		browseOutputs.getStyleClass().add("browseButton");
		pane.add(browseOutputs, 34, 13, 4, 2);
		browseOutputs.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent e) {
				DirectoryChooser directoryChooser = new DirectoryChooser();
				outputDirectory = directoryChooser.showDialog(primaryStage);
				outputFileField.setText(outputDirectory.getAbsolutePath());
			}
		});
		
		Text primerText = new Text("Please enter a file containing your custom primers");
		primerText.setTextAlignment(TextAlignment.CENTER);
		HBox primerBox = new HBox(10);
		primerBox.setAlignment(Pos.CENTER_LEFT);
		primerBox.getChildren().add(primerText);
		pane.add(primerBox, 2, 15, 20, 2);
		
		TextField primerField = new TextField();
		primerField.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		primerField.setEditable(false);
		primerField.setTooltip(new Tooltip("A text file containing custom primers to trim from the ends of the reads"));
		pane.add(primerField, 2, 17, 30, 2);
		
		primerField.setOnDragOver(new EventHandler<DragEvent>() {
			public void handle(DragEvent e) {
				if(e.getGestureSource() != primerField && e.getDragboard().hasFiles()) {
					e.acceptTransferModes(TransferMode.COPY_OR_MOVE);
				}
				e.consume();
			}
		});
		primerField.setOnDragDropped(new EventHandler<DragEvent>(){
			public void handle(DragEvent e) {
				Dragboard db = e.getDragboard();
				boolean success = false;
				if(db.hasFiles()) {
					List<File> listFiles = db.getFiles();
					primerFile = listFiles.get(0);
					primerField.setText(primerFile.getAbsolutePath());
					success = true;
				}
				e.setDropCompleted(success);
				e.consume();
			}
		});
		
		Button primerButton = new Button("Browse...");
		primerButton.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		primerButton.getStyleClass().add("browseButton");
		pane.add(primerButton, 34, 17, 4, 2);
		primerButton.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent e) {
				FileChooser chooser = new FileChooser();
				primerFile = chooser.showOpenDialog(primaryStage);
				primerField.setText(primerFile.getAbsolutePath());
			}
		});
		
		Text threadPrompt = new Text("Input number of\nthreads to use(1, 2, etc.)");
		threadPrompt.setTextAlignment(TextAlignment.CENTER);
		HBox threadBox = new HBox(10);
		threadBox.setAlignment(Pos.CENTER);
		threadBox.getChildren().add(threadPrompt);
		pane.add(threadBox, 40, 4, 8, 3);
		
		ComboBox<Integer> threadField = new ComboBox<Integer>();
		threadField.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		for(int i = 0; i < Runtime.getRuntime().availableProcessors() - 1; i++) {
			threadField.getItems().add(i + 1);
		}
		threadField.getSelectionModel().selectLast();
		threadField.setVisibleRowCount(3);
		pane.add(threadField, 41, 7, 6, 2);
		
		Button convertButton = new Button("Convert .BAM");
		convertButton.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		pane.add(convertButton, 41, 11, 6, 3);
		convertButton.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent e) {
				FileChooser chooser = new FileChooser();
				File bamFile = null;
				try{
					bamFile = chooser.showOpenDialog(primaryStage);
				}catch(NullPointerException exception) {
					exception.printStackTrace();
				}
				String[] fullProcessCall = new String[] {javaCall, "-ea", "-Xmx7g", "-cp", "./current", "jgi.ReformatReads",
						"in=" + bamFile.getAbsolutePath(), "out=" + bamFile.getAbsolutePath().split("\\.")[0] + ".fastq.gz",
						"ow=t", "zl=9"};
				try {
					Process p = new ProcessBuilder(fullProcessCall).directory(BBToolsLocation).start();
					try {
						p.waitFor();
					}catch(InterruptedException exception) {
						exception.printStackTrace();
					}
				}catch(IOException exception) {
					exception.printStackTrace();
				}
			}
		});
		
		Text statsPrompt = new Text("Mapped reads:\nMin Length, Max Length, Min Identity");
		statsPrompt.setTextAlignment(TextAlignment.CENTER);
		HBox statsBox = new HBox(10);
		statsBox.setAlignment(Pos.CENTER);
		statsBox.getChildren().add(statsPrompt);
		pane.add(statsBox, 39, 14, 8, 3);
		
		TextField minField = new TextField();
		minField.setAlignment(Pos.CENTER);
		minField.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		minField.setText(Integer.toString(mapperMinLen));
		pane.add(minField, 40, 17, 2, 2);
		
		TextField maxField = new TextField();
		maxField.setAlignment(Pos.CENTER);
		maxField.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		maxField.setText(Integer.toString(mapperMaxLen));
		pane.add(maxField, 43, 17, 2, 2);
		
		TextField idField = new TextField();
		idField.setAlignment(Pos.CENTER);
		idField.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		idField.setText(Double.toString(mapperMinID));
		pane.add(idField, 46, 17, 2, 2);
		
		Text alertText = new Text();
		alertText.getStyleClass().add("alertText");
		HBox alertBox = new HBox(10);
		alertBox.setAlignment(Pos.CENTER);
		alertBox.getChildren().add(alertText);
		pane.add(alertBox, 1, 44, 48, 1);
		
		Button backButton = new Button("Back");
		backButton.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		pane.add(backButton, 2, 45, 4, 3);
		backButton.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent e) {
				WelcomePage page = new WelcomePage(primaryStage, FastQCLocation, BBToolsLocation, PicardLocation, TabletLocation, javaCall);
				page.run();
			}
		});		
		
		TextArea outputField = new TextArea();
		outputField.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		outputField.setEditable(false);
		pane.add(outputField, 2, 20, 46, 23);
		
		ProgressBar progress = new ProgressBar();
		progress.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		
		Button proceed = new Button("Proceed");
		proceed.setId("proceedButton");
		proceed.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		pane.add(proceed, 22, 45, 4, 3);
		proceed.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent e) {
				try {
					threads = threadField.getSelectionModel().getSelectedItem();
				}catch(Exception exception) {
					alertText.setText("You need to enter a number in the Threads field");
					e.consume();
				}
				try {
					mapperMinLen = Integer.parseInt(minField.getText());
					mapperMaxLen = Integer.parseInt(maxField.getText());
				}catch(Exception exception) {
					alertText.setText("The minimum and maximum read length fields must contain numbers");
					e.consume();
				}
				try {
					mapperMinID = Double.parseDouble(idField.getText());
				}catch(Exception exception) {
					alertText.setText("The minimum read identity field must contain numbers");
					e.consume();
				}
				if(readDirectory == null || outputDirectory == null || referenceFile == null || primerFile == null) {
					alertText.setText("Please enter an input file directory, a reference file, a primer file, and an output file directory");
					e.consume();
				}else if(!checkReadsCompatible()){
					alertText.setText("Directory containing reads is either empty or contains incompatible files. " +
								"If the problem is a .bam file, convert it to fastq.gz first");
					e.consume();
				}else if(threads >= Runtime.getRuntime().availableProcessors()){
					alertText.setText("Thread count must be at most 1 less than total number of cores available");
					e.consume();
				}else if(mapperMinLen <= 0 || mapperMaxLen <= 0) {
					alertText.setText("Minimum and maximum read lengths must be positive integers");
					e.consume();
				}else if(mapperMinID <= 0 || mapperMinID > 1) {
					alertText.setText("Minimum identity of mapped reads must be a number between 0 and 1");
					e.consume();
				}else if(!referenceCompatible(outputField)){
					alertText.setText("Reference file contains incompatible characters");
					e.consume();
				}else {
					alertText.setText("");
					pane.add(progress, 2, 43, 46, 1);
					RunPipeline task = new RunPipeline(outputField);
					progress.progressProperty().bind(task.progressProperty());
					Thread t = new Thread(task);
					t.start();
					currentlyRunning = true;
				}
			}
		});	
		
		Scene scene = new Scene(pane, 1000, 700);
		scene.getStylesheets().add("src/resources/QCPage.css");
//		scene.getStylesheets().add(IonTorrentMapper.class.getResource("/resources/QCPage.css").toString()); // for running in Eclipse
		primaryStage.setScene(scene);
//		pane.setGridLinesVisible(true);
		primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
			public void handle(WindowEvent e) {
				if(currentlyRunning) {
					if(JOptionPane.showConfirmDialog(null, 
													"Exiting now will cause the pipeline to exit early, likely corrupting any data currently being output.\n" +
													"Are you sure you want to exit now?",
													"Exit CFIAssembly",
													JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION){
						try{
							FileWriter logFile = new FileWriter(outputDirectory.getAbsolutePath() + separator + "log.txt", true);
							String[] outputLines = outputField.getText().split("\n");
							for(String line : outputLines) {
								logFile.write(line + "\n");
							}
							logFile.close();
						}catch(IOException exception) {
							exception.printStackTrace();
						}
						if(mainPool != null && !mainPool.isTerminated()) {
							mainPool.shutdownNow();
						}
						for(Process p : mainProcesses) {
							if(p.isAlive()) {
								p.destroyForcibly();
							}
						}
						Platform.exit();
						System.exit(0);
					}else {
						e.consume();
					}
				}else {
					if(!outputField.getText().isEmpty()) {
						try{
							FileWriter logFile = new FileWriter(outputDirectory.getAbsolutePath() + separator + "log.txt", true);
							String[] outputLines = outputField.getText().split("\n");
							for(String line : outputLines) {
								logFile.write(line + "\n");
							}
							logFile.close();
						}catch(IOException exception) {
							exception.printStackTrace();
						}
					}
					Platform.exit();
					System.exit(0);
				}
			}
		});
		primaryStage.setTitle("Amplicon Mapper");
		primaryStage.show();
	}
	
	public boolean checkReadsCompatible() {
		File[] readList = readDirectory.listFiles();
		if(readList.length == 0) {
			return false;
		}
		for(File entry : readList) {
			String[] splitName = entry.getName().split("\\.");
			if(!entry.getName().contains("fastq.gz") && !entry.getName().contains("fastq") && !entry.getName().contains("fasta") &&
					!splitName[splitName.length - 1].equals("fa") && !splitName[splitName.length - 1].equals("fna")) {
				return false;
			}
		}
		return true;
	}
	
	public boolean referenceCompatible(TextArea outputField) {
		try (BufferedReader reader = new BufferedReader(new FileReader(referenceFile))) {
			String line;
			Pattern regex = Pattern.compile("[^0-9A-Za-z\\s!#$%&+./:;?@^_|~>\\=-]");
			try {
				while((line = reader.readLine()) != null) {
					Matcher matcher = regex.matcher(line);
					if(!line.isEmpty() && matcher.find()){
						AccessoryMethods.logMessage(outputField, "Incompatible character(s) in line:" + line);
						return false;
					}
				}
			}catch(IOException e) {
				e.printStackTrace();
				return false;
			}
			reader.close();
		}catch(IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public void createSampleList() {
		File[] readList = readDirectory.listFiles();
		String[] queryFile = readList[0].getName().split("\\.");
		if(queryFile[queryFile.length - 1].equals("fastq")) {
			fileEnding = ".fastq";
		}else if(queryFile[queryFile.length - 1].equals("gz") && queryFile[queryFile.length - 2].equals("fastq")) {
			fileEnding = ".fastq.gz";
		}
		for(File entry : readList) {
			String sampleID = entry.getName().replace(fileEnding, "");
			if(!sampleList.contains(sampleID)) {
				sampleList.add(sampleID);
			}
		}
	}
	
	public void createDirectories() {
		fastQCOutput = new File(outputDirectory.getAbsolutePath() + separator + "fastqc");
		fastQCOutput.mkdirs();
		trimmed = new File(outputDirectory.getAbsolutePath() + separator + "trimmed");
		trimmed.mkdirs();
		bbMapSam = new File(outputDirectory.getAbsolutePath() + separator + "bbMapSam");
		bbMapSam.mkdirs();
		bbMapTSV = new File(outputDirectory.getAbsolutePath() + separator + "bbMapTSV");
		bbMapTSV.mkdirs();
		bbMapBam = new File(outputDirectory.getAbsolutePath() + separator + "bbMapBam");
		bbMapBam.mkdirs();
		bamSorted = new File(outputDirectory.getAbsolutePath() + separator + "bamSorted");
		bamSorted.mkdirs();
	}
	
	private class RunPipeline extends Task<Void>{
		
		private TextArea outputField;
		
		public RunPipeline(TextArea outputField) {
			this.outputField = outputField;
		}
		
		public Void call() {
			createSampleList();
			updateProgress(1, 13);
			AccessoryMethods.logMessage(outputField, "Completed sample list");
			createDirectories();
			updateProgress(2, 13);
			AccessoryMethods.logMessage(outputField, "Completed creating directories");
			DegenMethods.processPrimers(primerFile, startPrimers, endPrimers, outputField);
			updateProgress(3, 13);
			AccessoryMethods.logMessage(outputField, "Completed parsing of primer file");
			if(FastQCLocation != null) {
				RunFastQCTask(outputField, threads);
				updateProgress(4, 13);
				AccessoryMethods.logMessage(outputField, "Completed FastQC");
			}
			RunAdapterTrimTask(outputField, threads);
			updateProgress(5, 13);
			AccessoryMethods.logMessage(outputField, "Completed removing adapter sequence");
			RunLeftTrimTask(outputField, threads);
			updateProgress(6, 13);
			AccessoryMethods.logMessage(outputField, "Completed removing start primers");
			RunRightTrimTask(outputField, threads);
			updateProgress(7, 13);
			AccessoryMethods.logMessage(outputField, "Completed removing end primers");
			RunBBMapTask(outputField, threads);
			updateProgress(8, 13);
			AccessoryMethods.logMessage(outputField, "Completed mapping reads");
			AccessoryMethods.makeConsolidatedTSV(referenceFile, bbMapTSV, outputDirectory);
			AccessoryMethods.logMessage(outputField, "Consolidated Reports");
			RunSamConvertTask(outputField, threads);
			updateProgress(9, 13);
			AccessoryMethods.logMessage(outputField, "Completed converting to BAM");
			RunBamSortTask(outputField, threads);
			updateProgress(10, 13);
			AccessoryMethods.logMessage(outputField, "Completed sorting BAM");
			RunBamIndexTask(outputField, threads);
			updateProgress(11, 13);
			AccessoryMethods.logMessage(outputField, "Completed indexing BAM");
			AccessoryMethods.makeQALog(new File(outputDirectory.getAbsolutePath() + "\\QALog.txt"),
					WelcomePage.version, mapperMinLen, mapperMaxLen, mapperMinID, outputDirectory, referenceFile, readDirectory, primerFile);
			try{
				FileWriter logFile = new FileWriter(outputDirectory.getAbsolutePath() + separator + "log.txt");
				String[] outputLines = outputField.getText().split("\n");
				for(String line : outputLines) {
					logFile.write(line + "\n");
				}
				logFile.close();
			}catch(IOException e) {
				e.printStackTrace();
			}
			updateProgress(12, 13);
			currentlyRunning = false;
			AccessoryMethods.logMessage(outputField, "Done");
			RunTablet(bamSorted.listFiles()[0], referenceFile);
			updateProgress(13, 13);
			return null;
		}
	}
	
	public void RunFastQCTask(TextArea outputField, int threads) {
		mainPool = Executors.newFixedThreadPool(threads);
		for(File entry : readDirectory.listFiles()) {
			FastQCTask qcTask = new FastQCTask(outputField, entry);
			mainPool.submit(qcTask);
		}
		try {
			mainPool.shutdown();
			mainPool.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
		}catch(InterruptedException e) {
			e.printStackTrace();
		}
		AccessoryMethods.logMessage(outputField,"Beginning Relocation");
		RelocateTask task = new RelocateTask();
		Thread t = new Thread(task);
		t.setDaemon(true);
		t.start();
		try {
			t.join();
		}catch(InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void RunAdapterTrimTask(TextArea outputField, int threads) {
		AdapterTrimTask qualityTask = new AdapterTrimTask(outputField, threads);
		Thread t3 = new Thread(qualityTask);
		t3.setDaemon(true);
		t3.start();
		try {
			t3.join();
		}catch(InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void RunLeftTrimTask(TextArea outputField, int threads) {
		LeftTrimTask task = new LeftTrimTask(outputField, threads);
		Thread t = new Thread(task);
		t.setDaemon(true);
		t.start();
		try {
			t.join();
		}catch(InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void RunRightTrimTask(TextArea outputField, int threads) {
		RightTrimTask task = new RightTrimTask(outputField, threads);
		Thread t = new Thread(task);
		t.setDaemon(true);
		t.start();
		try {
			t.join();
		}catch(InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void RunBBMapTask(TextArea outputField, int threads) {
		BBMapTask task = new BBMapTask(outputField, threads);
		Thread t = new Thread(task);
		t.setDaemon(true);
		t.start();
		try {
			t.join();
		}catch(InterruptedException exception) {
			exception.printStackTrace();
		}
	}
	
	public void RunSamConvertTask(TextArea outputField, int threads) {
		mainPool = Executors.newFixedThreadPool(threads);
		for(String entry : sampleList) {
			SamConvertTask task = new SamConvertTask(outputField, entry);
			mainPool.submit(task);
		}
		try {
			mainPool.shutdown();
			mainPool.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
		}catch(InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void RunBamSortTask(TextArea outputField, int threads) {
		mainPool = Executors.newFixedThreadPool(threads);
		for(String entry : sampleList) {
			SamSortTask task = new SamSortTask(outputField, entry);
			mainPool.submit(task);
		}
		try {
			mainPool.shutdown();
			mainPool.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
		}catch(InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void RunBamIndexTask(TextArea outputField, int threads) {
		mainPool = Executors.newFixedThreadPool(threads);
		for(String entry : sampleList) {
			BamIndexTask task = new BamIndexTask(outputField, entry);
			mainPool.submit(task);
		}
		try {
			mainPool.shutdown();
			mainPool.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
		}catch(InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void RunTablet(File inputFile, File referenceFile) {
		String[] fullProcessCall = new String[] {TabletLocation + "/tablet", inputFile.getAbsolutePath(),
				referenceFile.getAbsolutePath()};
		if(System.getProperties().getProperty("os.name").contains("Windows")) {
			fullProcessCall = new String[] {TabletLocation + "\\tablet.exe", inputFile.getAbsolutePath(),
					referenceFile.getAbsolutePath()};
		}
		try{
			Process p = new ProcessBuilder(fullProcessCall).start();
		}catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	private class FastQCTask extends Task<Void>{
		
		private TextArea outputField;
		private File entry;
		private String line;
		private MessageConsumer consumer;
		
		public FastQCTask(TextArea outputField, File entry) {
			this.outputField = outputField;
			this.entry = entry;
		}
			
		@Override
		protected Void call() {
			BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();
			this.consumer = new MessageConsumer(messageQueue, this.outputField);
			String[] fullProcessCall = new String[] {javaCall, "-Xmx500m", "-classpath", ".:./sam-1.103.jar:./jbzip2-0.9.jar",
					"uk.ac.babraham.FastQC.FastQCApplication", entry.getAbsolutePath()};
			if(System.getProperties().getProperty("os.name").contains("Windows")) {
				fullProcessCall = new String[] {javaCall, "-Xmx500m", "-classpath", ".;./sam-1.103.jar;./jbzip2-0.9.jar",
						"uk.ac.babraham.FastQC.FastQCApplication", entry.getAbsolutePath()};
			}
			try {
				Process readRun = new ProcessBuilder(fullProcessCall).directory(FastQCLocation).start();
				BufferedReader stdout = new BufferedReader(new InputStreamReader(readRun.getErrorStream()));
				Platform.runLater(() -> this.consumer.start());
				while((this.line = stdout.readLine()) != null) {
					try{
						messageQueue.put(this.line);
					}catch(InterruptedException e) {
						e.printStackTrace();
					}
				}
				this.consumer.stop();
				stdout.close();
				try{
					readRun.waitFor();
				}catch(InterruptedException e) {
					e.printStackTrace();
				}
			}catch(IOException e){
				e.printStackTrace();
			}
			return null;
		}
	}
	
	private class AdapterTrimTask extends Task<Void>{
		
		private TextArea outputField;
		private String line;
		private MessageConsumer consumer;
		
		public AdapterTrimTask(TextArea outputField, int threads) {
			this.outputField = outputField;
		}
		
		@Override
		protected Void call() {
			for(String entry : sampleList) {
				BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();
				this.consumer = new MessageConsumer(messageQueue, this.outputField);
				String in1 = readDirectory.getAbsolutePath() + separator + entry + fileEnding;
				String out1 = trimmed.getAbsolutePath() + separator + entry + "_Trimmed.fastq.gz";
				String literal = "ATCACCGACTGCCCATAGAGAGG";
				String[] runQualityFilter = new String[] {javaCall, "-ea", "-Xmx7g", "-cp", "./current", "jgi.BBDuk", 
						"in=" + in1, "out=" + out1, "ktrim=r", "k=23", "mink=11", "hdist=1", "tbo", "tpe", "qtrim=lr", 
						"trimq=10", "minlen=64", "overwrite=t", "ziplevel=9", "ordered=t", "threads=" + threads, "literal=" + literal};
				AccessoryMethods.logMessage(outputField, String.join(" ", runQualityFilter));
				try {
					Process runFilter = new ProcessBuilder(runQualityFilter).directory(BBToolsLocation).start();
					mainProcesses.add(runFilter);
					BufferedReader stdout = new BufferedReader(new InputStreamReader(runFilter.getErrorStream()));
					Platform.runLater(() -> this.consumer.start());
					while((this.line = stdout.readLine()) != null) {
						try{
							messageQueue.put(this.line);
						}catch(InterruptedException e) {
							e.printStackTrace();
						}
					}
					this.consumer.stop();
					stdout.close();
					try {
						runFilter.waitFor();
					}catch(InterruptedException e) {
						e.printStackTrace();
					}
				}catch(IOException e) {
					e.printStackTrace();
				}
			}
			mainProcesses.clear();
			return null;
		}
	}
	
	private class LeftTrimTask extends Task<Void>{
		
		private TextArea outputField;
		private String line;
		private MessageConsumer consumer;
		
		public LeftTrimTask(TextArea outputField, int threads) {
			this.outputField = outputField;
		}
		
		@Override
		protected Void call() {
			for(String entry : sampleList) {
				BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();
				this.consumer = new MessageConsumer(messageQueue, this.outputField);
				String in1 = trimmed.getAbsolutePath() + separator + entry + "_Trimmed.fastq.gz";
				String out1 = trimmed.getAbsolutePath() + separator + entry + "_TrimmedL.fastq.gz";
				String literal = "";
				for(String primer : startPrimers) {
					if(primer == startPrimers.get(startPrimers.size() - 1)) {
						literal += primer;
					}else {
						literal += primer + ",";
					}
				}
				String[] runQualityFilter = new String[] {javaCall, "-ea", "-Xmx7g", "-cp", "./current", "jgi.BBDuk", 
						"in=" + in1, "out=" + out1, "literal=" + literal, "ktrim=l", "k=17", "hdist=1", "overwrite=t",
						"mink=11", "threads=" + threads};
				AccessoryMethods.logMessage(outputField, String.join(" ", runQualityFilter));
				try {
					Process runFilter = new ProcessBuilder(runQualityFilter).directory(BBToolsLocation).start();
					mainProcesses.add(runFilter);
					BufferedReader stdout = new BufferedReader(new InputStreamReader(runFilter.getErrorStream()));
					Platform.runLater(() -> this.consumer.start());
					while((this.line = stdout.readLine()) != null) {
						try{
							messageQueue.put(this.line);
						}catch(InterruptedException e) {
							e.printStackTrace();
						}
					}
					this.consumer.stop();
					stdout.close();
					try {
						runFilter.waitFor();
					}catch(InterruptedException e) {
						e.printStackTrace();
					}
				}catch(IOException e) {
					e.printStackTrace();
				}
				File in1File = new File(in1);
				in1File.delete();
			}
			mainProcesses.clear();
			return null;
		}
	}
	
	private class RightTrimTask extends Task<Void>{
		
		private TextArea outputField;
		private String line;
		private MessageConsumer consumer;
		
		public RightTrimTask(TextArea outputField, int threads) {
			this.outputField = outputField;
		}
		
		@Override
		protected Void call() {
			for(String entry : sampleList) {
				BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();
				this.consumer = new MessageConsumer(messageQueue, this.outputField);
				String in1 = trimmed.getAbsolutePath() + separator + entry + "_TrimmedL.fastq.gz";
				String out1 = trimmed.getAbsolutePath() + separator + entry + "_TrimmedR.fastq.gz";
				String literal = "";
				for(String primer : endPrimers) {
					if(primer == endPrimers.get(endPrimers.size() - 1)) {
						literal += primer;
					}else {
						literal += primer + ",";
					}
				}
				String[] runQualityFilter = new String[] {javaCall, "-ea", "-Xmx7g", "-cp", "./current", "jgi.BBDuk", "in=" + in1, 
						"out=" + out1, "literal=" + literal, "ktrim=r", "k=17", "hdist=1", "qtrim=lr", "trimq=10", "overwrite=t", 
						"mink=11", "threads=" + threads, "minlen=" + mapperMinLen, "maxlen=" + mapperMaxLen};
				AccessoryMethods.logMessage(outputField, String.join(" ", runQualityFilter));
				try {
					Process runFilter = new ProcessBuilder(runQualityFilter).directory(BBToolsLocation).start();
					mainProcesses.add(runFilter);
					BufferedReader stdout = new BufferedReader(new InputStreamReader(runFilter.getErrorStream()));
					Platform.runLater(() -> this.consumer.start());
					while((this.line = stdout.readLine()) != null) {
						try{
							messageQueue.put(this.line);
						}catch(InterruptedException e) {
							e.printStackTrace();
						}
					}
					this.consumer.stop();
					stdout.close();
					try {
						runFilter.waitFor();
					}catch(InterruptedException e) {
						e.printStackTrace();
					}
				}catch(IOException e) {
					e.printStackTrace();
				}
				File in1File = new File(in1);
				in1File.delete();
			}
			mainProcesses.clear();
			return null;
		}
	}	
	
	private class RelocateTask extends Task<Void>{
		
		public RelocateTask() {
			
		}
		
		@Override
		protected Void call() {
			File[] readList = readDirectory.listFiles();
			for(File item : readList) {
				String itemName = item.getName();
				String[] splitString = itemName.split("\\.");
				if(splitString[(splitString.length - 1)].equals("html") || splitString[(splitString.length - 1)].equals("zip")) {
					item.renameTo(new File(fastQCOutput.getAbsolutePath() + separator + item.getName()));
				}	
			}
			return null;
		}
	}
	
	private class BBMapTask extends Task<Void> {
		
		private TextArea outputField;
		private String line;
		private MessageConsumer consumer;
		private int threads;
		
		public BBMapTask(TextArea outputField, int threads) {
			this.outputField = outputField;
			this.threads = threads;
		}
		
		@Override
		protected Void call() {
			BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();
			consumer = new MessageConsumer(messageQueue, this.outputField);
			String memJava = "7g";
			for(String entry : sampleList) {
				String in = trimmed.getAbsolutePath() + separator + entry + "_TrimmedR.fastq.gz";
				String ref = referenceFile.getAbsolutePath();
				String scafstats = bbMapTSV.getAbsolutePath() + separator + entry + "_mapping_stats.tsv";
				String outm = bbMapSam.getAbsolutePath() + separator + entry + "_mapped.sam";
				String[] fullProcessCall = new String[] {javaCall, "-ea", "-Xmx" + memJava, "-cp", "./current", "align2.BBMap",
						"in=" + in, "ref=" + ref, "scafstats=" + scafstats, "outm=" + outm, "threads=" + threads,
						"maxlen=" + mapperMaxLen, "minid=" + mapperMinID, "overwrite=t", "sortstats=t", "trd=t", "nodisk=t"};
				try {
					Process assemblyRun = new ProcessBuilder(fullProcessCall).directory(BBToolsLocation).start();
					mainProcesses.add(assemblyRun);
					BufferedReader error = new BufferedReader(new InputStreamReader(assemblyRun.getErrorStream()));
					Platform.runLater(() -> consumer.start());
					while((this.line = error.readLine())!= null) {
						try{
							messageQueue.put(this.line);
						}catch(InterruptedException e) {
							e.printStackTrace();
						}
					}
					consumer.stop();
					error.close();
					try {
						assemblyRun.waitFor();
					}catch(InterruptedException e) {
						e.printStackTrace();
					}
				}catch(IOException e) {
					e.printStackTrace();
				}
			}
			return null;
		}
	}
	
	private class SamConvertTask extends Task<Void> {
		
		private TextArea outputField;
		private String line;
		private MessageConsumer consumer;
		private String entry;
		
		public SamConvertTask(TextArea outputField, String entry) {
			this.outputField = outputField;
			this.entry = entry;
		}
		
		@Override
		public Void call() {
			BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();
			consumer = new MessageConsumer(messageQueue, outputField);
			String memJava = "7g";
			String in = bbMapSam.getAbsolutePath() + separator + entry + "_mapped.sam";
			String out = bbMapBam.getAbsolutePath() + separator + entry + ".bam";
			String[] fullProcessCall = new String[] {javaCall, "-ea", "-Xmx" + memJava, "-jar", "picard.jar", "SamFormatConverter",
					"I=" + in, "O=" + out};
			try {
				Process p = new ProcessBuilder(fullProcessCall).directory(PicardLocation).start();
				BufferedReader stdout = new BufferedReader(new InputStreamReader(p.getErrorStream()));
				Platform.runLater(() -> consumer.start());
				while((this.line = stdout.readLine()) != null) {
					try {
						messageQueue.put(this.line);
					}catch(InterruptedException e) {
						e.printStackTrace();
					}
				}
				consumer.stop();
				stdout.close();
				try {
					p.waitFor();
				}catch(InterruptedException e) {
					e.printStackTrace();
				}
			}catch(IOException e) {
				e.printStackTrace();
			}
			return null;
		}
	}
	
	private class SamSortTask extends Task<Void> {
		
		private TextArea outputField;
		private String line;
		private MessageConsumer consumer;
		private String entry;
		
		public SamSortTask(TextArea outputField, String entry) {
			this.outputField = outputField;
			this.entry = entry;
		}
		
		@Override
		public Void call() {
			BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();
			consumer = new MessageConsumer(messageQueue, this.outputField);
			String memJava = "7g";
			String in = bbMapBam.getAbsolutePath() + separator + entry + ".bam";
			String out = bamSorted.getAbsolutePath() + separator + entry + "_sorted.bam";
			String sortOrder = "coordinate";
			String[] fullProcessCall = new String[] {javaCall, "-ea", "-Xmx" + memJava, "-jar", "picard.jar", "SortSam",
					"I=" + in, "O=" + out, "SORT_ORDER=" + sortOrder};
			try {
				Process p = new ProcessBuilder(fullProcessCall).directory(PicardLocation).start(); 
				BufferedReader stdout = new BufferedReader(new InputStreamReader(p.getErrorStream()));
				Platform.runLater(() -> consumer.start());
				while((this.line = stdout.readLine()) != null) {
					try{
						messageQueue.put(this.line);
					}catch(InterruptedException e) {
						e.printStackTrace();
					}
				}
				consumer.stop();
				stdout.close();
				try {
					p.waitFor();
				}catch(InterruptedException e) {
					e.printStackTrace();
				}
			}catch(IOException e) {
				e.printStackTrace();
			}
			return null;
		}
	}
	
	private class BamIndexTask extends Task<Void> {
		
		private TextArea outputField;
		private String line;
		private MessageConsumer consumer;
		private String entry;
		
		public BamIndexTask(TextArea outputField, String entry) {
			this.outputField = outputField;
			this.entry = entry;
		}
		
		@Override
		public Void call() {
			BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();
			consumer = new MessageConsumer(messageQueue, outputField);
			String memJava = "7g";
			String in = bamSorted.getAbsolutePath() + separator + entry + "_sorted.bam";
			String out = bamSorted.getAbsolutePath() + separator + entry + "_sorted.bam.bai";
			String[] fullProcessCall = new String[] {javaCall, "-ea", "-Xmx" + memJava, "-jar", "picard.jar", "BuildBamIndex",
					"I=" + in, "O=" + out};
			try {
				Process p = new ProcessBuilder(fullProcessCall).directory(PicardLocation).start();
				BufferedReader stdout = new BufferedReader(new InputStreamReader(p.getErrorStream()));
				Platform.runLater(() -> consumer.start());
				while((this.line = stdout.readLine()) != null) {
					try {
						messageQueue.put(this.line);
					}catch(InterruptedException e) {
						e.printStackTrace();
					}
				}
				consumer.stop();
				stdout.close();
				try {
					p.waitFor();
				}catch(InterruptedException e) {
					e.printStackTrace();
				}
			}catch(IOException e) {
				e.printStackTrace();
			}
			return null;
		}
	}
}

