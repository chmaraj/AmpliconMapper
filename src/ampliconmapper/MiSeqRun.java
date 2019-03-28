package appliconmapper;

import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.RowConstraints;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.control.RadioButton;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.Process;
import java.lang.Runtime;
import java.util.ArrayList;

import java.util.Collections;
import java.util.List;

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.JOptionPane;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class MiSeqRun implements Page{

	private String separator = "/";
	
	private Stage primaryStage;
	private File readDirectory, outputDirectory, referenceFile, primerFile, FastQCLocation, BBToolsLocation, PicardLocation, TabletLocation;
	private File logs, qc, fastqc, trimmed, merged, fastqcRaw, bbMapSam, bbMapTSV, bbMapBam, bamSorted;
	private File[] readList;
	private ArrayList<String> sampleList = new ArrayList<String>();
	private int threads = Runtime.getRuntime().availableProcessors();
	private boolean currentlyRunning = false, mergeReads;
	private ExecutorService mainPool = null;
	private ArrayList<Process> mainProcesses = new ArrayList<Process>();
	private ArrayList<String> startPrimers = new ArrayList<String>(), endPrimers = new ArrayList<String>();
	
	public MiSeqRun(Stage primaryStage, File FastQCLocation, File BBToolsLocation, File PicardLocation, File TabletLocation) {
		this.primaryStage = primaryStage;
		this.FastQCLocation = FastQCLocation;
		this.BBToolsLocation = BBToolsLocation;
		this.PicardLocation = PicardLocation;
		this.TabletLocation = TabletLocation;
		if(System.getProperties().getProperty("os.name").contains("Windows")) {
			this.separator = "\\";
		}else {
			this.separator = "/";
		}
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
		
		Button browseInputs = new Button("Browse...");
		browseInputs.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		browseInputs.getStyleClass().add("browseButton");
		pane.add(browseInputs, 34, 5, 4, 2);
		browseInputs.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent e) {
				DirectoryChooser directoryChooser = new DirectoryChooser();
				readDirectory = directoryChooser.showDialog(primaryStage);
				readInput.setText(readDirectory.getAbsolutePath());
				readList = readDirectory.listFiles();
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
		primerField.setTooltip(new Tooltip("A text file containing custom primer sequences to trim from the reads"));
		pane.add(primerField, 2, 17, 30, 2);
		
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
		
		TextField threadField = new TextField();
		threadField.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		threadField.setAlignment(Pos.CENTER);
		threadField.setText(Integer.toString(threads - 1));
		pane.add(threadField, 41, 7, 6, 3);
		
		RadioButton mergingButton = new RadioButton();
		mergingButton.setSelected(true);
		mergingButton.setText("Merge trimmed reads?");
		HBox mergeBox = new HBox(10);
		mergeBox.setAlignment(Pos.CENTER);
		mergeBox.getChildren().add(mergingButton);
		pane.add(mergeBox, 39, 13, 10, 2);
		
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
				WelcomePage page = new WelcomePage(primaryStage, FastQCLocation, BBToolsLocation, PicardLocation, TabletLocation);
				page.run();
			}
		});		
		
		TextArea outputField = new TextArea();
		outputField.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		outputField.setEditable(false);
		pane.add(outputField, 2, 20, 46, 23);	
		
		Button proceed = new Button("Proceed");
		proceed.setId("proceedButton");
		proceed.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		pane.add(proceed, 22, 45, 4, 3);
		proceed.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent e) {
				try {
					threads = Integer.parseInt(threadField.getText());
				}catch(Exception exception) {
					alertText.setText("You need to enter a number in the Threads field");
					return;
				}
				if(readDirectory == null || outputDirectory == null || referenceFile == null || primerFile == null) {
					alertText.setText("Please enter an input file directory, a reference file, a primer file, and an output file directory");
				}else if(!checkReadsCompatible()){
					alertText.setText("Directory containing reads is either empty or contains incompatible files");
				}else if(threads >= Runtime.getRuntime().availableProcessors()){
					alertText.setText("Thread count must be at most 1 less than total number of cores available");
				}else {
					if(mergingButton.isSelected()) {
						mergeReads = true;
					}else {
						mergeReads = false;
					}
					alertText.setText("");
					RunAssemblyPipeline task = new RunAssemblyPipeline(outputField);
					Thread t = new Thread(task);
					t.start();
					currentlyRunning = true;
				}
			}
		});	
		
		Scene scene = new Scene(pane, 1000, 700);
		scene.getStylesheets().add("QCPage.css");
		primaryStage.setScene(scene);
//		assemblyPane.setGridLinesVisible(true);
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
	
	public void parsePrimers(File primerFile) {
		try{
			String line;
			BufferedReader reader = new BufferedReader(new FileReader(primerFile));
			try{
				while((line = reader.readLine())!= null) {
					String[] fields = line.split(",");
					if(fields.length <= 1) {
						continue;
					}else if(fields[1].equals("START")) {
						startPrimers.add(fields[2].trim());
					}else if(fields[1].equals("END")) {
						endPrimers.add(fields[2].trim());
					}
				}
			}catch(IOException e) {
				e.printStackTrace();
			}
			
		}catch(FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public class RunAssemblyPipeline extends Task<Void> {
		
		private TextArea outputField;
		
		public RunAssemblyPipeline(TextArea outputField) {
			this.outputField = outputField;
		}
		
		public Void call() {
			parsePrimers(primerFile);
			logMessage(outputField, "Finished parsing primers");
			createBaseDirectories();
			logMessage(outputField,"Finished creating base directories");
			if(FastQCLocation != null) {
				runFastQCTask(outputField);
				logMessage(outputField,"Finished FastQC task");
			}
			populateSampleList();
			logMessage(outputField,"Finished creating sample list");
			runFilterByTileTask(outputField);
			logMessage(outputField,"Finished filtering by tile");
			runQualityFilterTask(outputField);
			logMessage(outputField,"Finished filtering by quality");
			runLeftTrimTask(outputField);
			logMessage(outputField, "Finished removing start primers");
			runRightTrimTask(outputField);
			logMessage(outputField, "Finished removing end primers");
			runRemoveArtifactsTask(outputField);
			logMessage(outputField,"Finished removing artifacts");
			if(mergeReads) {
				runMergeReadsTask(outputField);
			}
			RunBBMapTask(outputField, threads);
			logMessage(outputField, "Completed mapping reads");
			RunSamConvertTask(outputField, threads);
			logMessage(outputField, "Completed converting to BAM");
			RunBamSortTask(outputField, threads);
			logMessage(outputField, "Completed sorting BAM");
			RunBamIndexTask(outputField, threads);
			logMessage(outputField, "Completed indexing BAM");
			try{
				FileWriter logFile = new FileWriter(outputDirectory.getAbsolutePath() + separator + "log.txt");
				logFile.write(outputField.getText());
				logFile.close();
			}catch(IOException e) {
				e.printStackTrace();
			}
			currentlyRunning = false;
			RunTablet(bamSorted.listFiles()[0], referenceFile);
			return null;
		}
	}
	
	public void createBaseDirectories() {
		logs = new File(outputDirectory.getAbsolutePath() + separator + "logs");
		logs.mkdirs();
		qc = new File(outputDirectory.getAbsolutePath() + separator + "QC");
		qc.mkdirs();
		fastqc = new File(qc.getAbsolutePath() + separator + "fastqc");
		fastqc.mkdirs();
		fastqcRaw = new File(fastqc.getAbsolutePath() + separator + "raw");
		fastqcRaw.mkdirs();
		trimmed = new File(outputDirectory.getAbsolutePath() + separator + "trimmed");
		trimmed.mkdirs();
		if(mergeReads) {
			merged = new File(outputDirectory.getAbsolutePath() + separator + "merged");
			merged.mkdirs();
		}
		bbMapSam = new File(outputDirectory.getAbsolutePath() + separator + "bbMapSam");
		bbMapSam.mkdirs();
		bbMapTSV = new File(outputDirectory.getAbsolutePath() + separator + "bbMapTSV");
		bbMapTSV.mkdirs();
		bbMapBam = new File(outputDirectory.getAbsolutePath() + separator + "bbMapBam");
		bbMapBam.mkdirs();
		bamSorted = new File(outputDirectory.getAbsolutePath() + separator + "bamSorted");
		bamSorted.mkdirs();
	}
	
	public void populateSampleList() {
		readList = readDirectory.listFiles();
		for(File entry : readList) {
			String entryName = entry.getName();
			String sampleID = entryName.replaceAll("_R1.*", "");
			sampleID = sampleID.replaceAll("_R2.*", "");
			if(sampleList.isEmpty() || !sampleList.contains(sampleID)) {
				sampleList.add(sampleID);
			}
		}
	}
	
	public void runFastQCTask(TextArea outputField) {
		mainPool = Executors.newFixedThreadPool(threads);
		for(File entry : readList) {
			FastQCTask qcTask = new FastQCTask(outputField, entry);
			mainPool.submit(qcTask);
		}
		try {
			mainPool.shutdown();
			mainPool.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
		}catch(InterruptedException e) {
			e.printStackTrace();
		}
		logMessage(outputField,"Beginning Relocation");
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
	
	public void runFilterByTileTask(TextArea outputField) {
		FilterByTileTask filterTask = new FilterByTileTask(outputField);
		Thread t2 = new Thread(filterTask);
		t2.setDaemon(true);
		t2.start();
		try {
			t2.join();
		}catch(InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void runQualityFilterTask(TextArea outputField) {
		QualityFilterTask qualityTask = new QualityFilterTask(outputField);
		Thread t3 = new Thread(qualityTask);
		t3.setDaemon(true);
		t3.start();
		try {
			t3.join();
		}catch(InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void runLeftTrimTask(TextArea outputField) {
		LeftTrimTask task = new LeftTrimTask(outputField);
		Thread t = new Thread(task);
		t.setDaemon(true);
		t.start();
		try {
			t.join();
		}catch(InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void runRightTrimTask(TextArea outputField) {
		RightTrimTask task = new RightTrimTask(outputField);
		Thread t = new Thread(task);
		t.setDaemon(true);
		t.start();
		try {
			t.join();
		}catch(InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void runRemoveArtifactsTask(TextArea outputField) {
		RemoveArtifactsTask removeTask = new RemoveArtifactsTask(outputField);
		Thread t4 = new Thread(removeTask);
		t4.setDaemon(true);
		t4.start();
		try {
			t4.join();
		}catch(InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void runMergeReadsTask(TextArea outputField) {
		MergeReadsTask task = new MergeReadsTask(outputField);
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
			String processCall = "java -Xmx500m -classpath .:./sam-1.103.jar:./jbzip2-0.9.jar uk.ac.babraham.FastQC.FastQCApplication";
			if(System.getProperties().getProperty("os.name").contains("Windows")) {
				processCall = "java -Xmx500m -classpath .;./sam-1.103.jar;./jbzip2-0.9.jar uk.ac.babraham.FastQC.FastQCApplication";
			}
			String fullProcessCall = processCall + " \"" + entry.getAbsolutePath() + "\"";
			try {
				Process readRun = Runtime.getRuntime().exec(fullProcessCall, null, FastQCLocation);
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
	
	private class RelocateTask extends Task<Void>{
		
		public RelocateTask() {
			
		}
		
		@Override
		protected Void call() {
			readList = readDirectory.listFiles();
			for(File item : readList) {
				String itemName = item.getName();
				String[] splitString = itemName.split("\\.");
				if(splitString[(splitString.length - 1)].equals("html") || splitString[(splitString.length - 1)].equals("zip")) {
					item.renameTo(new File(fastqcRaw.getAbsolutePath() + separator + item.getName()));
				}	
			}
			return null;
		}
	}
	
	private class FilterByTileTask extends Task<Void> {
		
		private TextArea outputField;
		private String line;
		private MessageConsumer consumer;
		
		public FilterByTileTask(TextArea outputField) {
			this.outputField = outputField;
		}
		
		@Override
		protected Void call() {
			for(String entry : sampleList) {
				BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();
				this.consumer = new MessageConsumer(messageQueue, this.outputField);
				File entryDirectory = new File(trimmed.getAbsolutePath() + separator + entry);
				entryDirectory.mkdirs();
				String in1 = "", in2 = "";
				for(File item : readDirectory.listFiles()) {
					if(item.getName().contains("_R1") && item.getName().contains(entry)) {
						in1 = "\"" + item.getAbsolutePath() + "\"";
					}else if(item.getName().contains("_R2") && item.getName().contains(entry)) {
						in2 = "\"" + item.getAbsolutePath() + "\"";
					}
				}
				String out1 = "\"" + entryDirectory.getAbsolutePath() + separator + entry + "_Filtered_1P.fastq.gz" + "\"";
				String out2 = "\"" + entryDirectory.getAbsolutePath() + separator + entry + "_Filtered_2P.fastq.gz" + "\"";
				String options = " in1=" + in1 + " in2=" + in2 + " out1=" + out1 + " out2=" + out2 + " threads=" + threads + " ziplevel=9";
				String runFilterByTile = "java -ea -Xmx7g -cp ./current hiseq.AnalyzeFlowCell" + options;
				try {
					Process runFilter = Runtime.getRuntime().exec(runFilterByTile, null, BBToolsLocation);
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
	
	private class QualityFilterTask extends Task<Void>{
		
		private TextArea outputField;
		private String line;
		private MessageConsumer consumer;
		
		public QualityFilterTask(TextArea outputField) {
			this.outputField = outputField;
		}
		
		@Override
		protected Void call() {
			for(String entry : sampleList) {
				BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();
				this.consumer = new MessageConsumer(messageQueue, this.outputField);
				String in1 = "\"" + trimmed.getAbsolutePath() + separator + entry + separator + entry + "_Filtered_1P.fastq.gz" + "\"";
				String in2 = "\"" + trimmed.getAbsolutePath() + separator + entry + separator + entry + "_Filtered_2P.fastq.gz" + "\"";
				String out1 = "\"" + trimmed.getAbsolutePath() + separator + entry + separator + entry + "_Trimmed_1P.fastq.gz" + "\"";
				String out2 = "\"" + trimmed.getAbsolutePath() + separator + entry + separator + entry + "_Trimmed_2P.fastq.gz" + "\"";
				String ref = "\"" + BBToolsLocation.getAbsolutePath() + separator + "resources" + separator + "adapters.fa" + "\"";
				String options = " in=" + in1 + " in2=" + in2 + " out=" + out1 + " out2=" + out2 + 
						" ktrim=r k=23 mink=11 hdist=1 tbo tpe qtrim=lr trimq=10 minlen=64 overwrite=t ziplevel=9 ordered=t threads=" + threads + " ref=" + ref;
				String runQualityFilter = "java -ea -Xmx7g -cp ./current jgi.BBDuk" + options;
				logMessage(outputField, runQualityFilter);
				try {
					Process runFilter = Runtime.getRuntime().exec(runQualityFilter, null, BBToolsLocation);
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
				File in2File = new File(in2);
				in1File.delete();
				in2File.delete();
			}
			mainProcesses.clear();
			return null;
		}
	}
	
	private class LeftTrimTask extends Task<Void>{
		
		private TextArea outputField;
		private String line;
		private MessageConsumer consumer;
		
		public LeftTrimTask(TextArea outputField) {
			this.outputField = outputField;
		}
		
		@Override
		protected Void call() {
			for(String entry : sampleList) {
				BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();
				this.consumer = new MessageConsumer(messageQueue, this.outputField);
				String in1 = "\"" + trimmed.getAbsolutePath() + separator + entry + separator + entry + "_Trimmed_1P.fastq.gz" + "\"";
				String in2 = "\"" + trimmed.getAbsolutePath() + separator + entry + separator + entry + "_Trimmed_2P.fastq.gz" + "\"";
				String out1 = "\"" + trimmed.getAbsolutePath() + separator + entry + separator + entry + "_TrimmedL_1P.fastq.gz" + "\"";
				String out2 = "\"" + trimmed.getAbsolutePath() + separator + entry + separator + entry + "_TrimmedL_2P.fastq.gz" + "\"";
				String literal = "";
				for(String primer : startPrimers) {
					if(primer == startPrimers.get(startPrimers.size() - 1)) {
						literal += primer;
					}else {
						literal += primer + ",";
					}
				}
				String options = " in=" + in1 + " in2=" + in2 + " out=" + out1 + " out2=" + out2 + " literal=" + literal +
						" ktrim=l k=17 hdist=1 overwrite=t mink=11 threads=" + threads;
				String runQualityFilter = "java -ea -Xmx7g -cp ./current jgi.BBDuk" + options;
				logMessage(outputField, runQualityFilter);
				try {
					Process runFilter = Runtime.getRuntime().exec(runQualityFilter, null, BBToolsLocation);
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
				File in2File = new File(in2);
				in1File.delete();
				in2File.delete();
			}
			mainProcesses.clear();
			return null;
		}
	}
	
	private class RightTrimTask extends Task<Void>{
		
		private TextArea outputField;
		private String line;
		private MessageConsumer consumer;
		
		public RightTrimTask(TextArea outputField) {
			this.outputField = outputField;
		}
		
		@Override
		protected Void call() {
			for(String entry : sampleList) {
				BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();
				this.consumer = new MessageConsumer(messageQueue, this.outputField);
				String in1 = "\"" + trimmed.getAbsolutePath() + separator + entry + separator + entry + "_TrimmedL_1P.fastq.gz" + "\"";
				String in2 = "\"" + trimmed.getAbsolutePath() + separator + entry + separator + entry + "_TrimmedL_2P.fastq.gz" + "\"";
				String out1 = "\"" + trimmed.getAbsolutePath() + separator + entry + separator + entry + "_TrimmedR_1P.fastq.gz" + "\"";
				String out2 = "\"" + trimmed.getAbsolutePath() + separator + entry + separator + entry + "_TrimmedR_2P.fastq.gz" + "\"";
				String literal = "";
				for(String primer : endPrimers) {
					if(primer == endPrimers.get(endPrimers.size() - 1)) {
						literal += primer;
					}else {
						literal += primer + ",";
					}
				}
				String options = " in=" + in1 + " in2=" + in2 + " out=" + out1 + " out2=" + out2 + " literal=" + literal +
						" ktrim=r k=17 hdist=1 qtrim=lr trimq=10 overwrite=t mink=11 threads=" + threads;
				String runQualityFilter = "java -ea -Xmx7g -cp ./current jgi.BBDuk" + options;
				logMessage(outputField, runQualityFilter);
				try {
					Process runFilter = Runtime.getRuntime().exec(runQualityFilter, null, BBToolsLocation);
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
				File in2File = new File(in2);
				in1File.delete();
				in2File.delete();
			}
			mainProcesses.clear();
			return null;
		}
	}	

	private class RemoveArtifactsTask extends Task<Void>{
		
		private TextArea outputField;
		private String line;
		private MessageConsumer consumer;
		
		public RemoveArtifactsTask(TextArea outputField) {
			this.outputField = outputField;
		}
		
		@Override
		protected Void call() {
			for(String entry : sampleList) {
				BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();
				this.consumer = new MessageConsumer(messageQueue, this.outputField);
				String in1 = "\"" + trimmed.getAbsolutePath() + separator + entry + separator + entry + "_TrimmedR_1P.fastq.gz" + "\"";
				String in2 = "\"" + trimmed.getAbsolutePath() + separator + entry + separator + entry + "_TrimmedR_2P.fastq.gz" + "\"";
				String out1 = "\"" + trimmed.getAbsolutePath() + separator + entry + separator + entry + "_Cleaned_1P.fastq.gz" + "\"";
				String out2 = "\"" + trimmed.getAbsolutePath() + separator + entry + separator + entry + "_Cleaned_2P.fastq.gz" + "\"";
				String ref = "\"" + BBToolsLocation.getAbsolutePath() + separator + "resources" + separator + "phix174_ill.ref.fa.gz" + "\"";
				String options = " in=" + in1 + " in2=" + in2 + " out=" + out1 + " out2=" + out2 + 
						" ziplevel=9 k=31 ordered=t threads=" + threads + " ref=" + ref;
				String runRemoveArtifacts = "java -ea -Xmx7g -cp ./current jgi.BBDuk" + options;
				try {
					Process runRemove = Runtime.getRuntime().exec(runRemoveArtifacts, null, BBToolsLocation);
					mainProcesses.add(runRemove);
					BufferedReader stdout = new BufferedReader(new InputStreamReader(runRemove.getErrorStream()));
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
						runRemove.waitFor();
					}catch(InterruptedException e) {
						e.printStackTrace();
					}
				}catch(IOException e) {
					e.printStackTrace();
				}
				File in1File = new File(in1);
				File in2File = new File(in2);
				in1File.delete();
				in2File.delete();
			}
			mainProcesses.clear();
			return null;
		}
	}
	
	private class MergeReadsTask extends Task<Void> {
		
		private TextArea outputField;
		private String line;
		private MessageConsumer consumer;
		
		public MergeReadsTask(TextArea outputField) {
			this.outputField = outputField;
		}
		
		public Void call() {
			BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();
			consumer = new MessageConsumer(messageQueue, this.outputField);
			String memJava = "7g";
			String processCall = "java -ea -Xmx" + memJava + " -cp ./current jgi.BBMerge";
			for(String entry : sampleList) {
				File mergedDirectory = new File(merged.getAbsolutePath() + separator + entry);
				mergedDirectory.mkdirs();
				File entryDirectory = new File(trimmed.getAbsolutePath() + separator + entry);
				String in = "\"" + entryDirectory.getAbsolutePath() + separator + entry + "_Cleaned_1P.fastq.gz" + "\"";
				String in2 = "\"" + entryDirectory.getAbsolutePath() + separator + entry + "_Cleaned_2P.fastq.gz" + "\"";
				String out = "\"" + mergedDirectory.getAbsolutePath() + separator + entry + "_merged.fastq.gz" + "\"";
				String outu = "\"" + mergedDirectory.getAbsolutePath() + separator + entry + "_unmerged_R1.fastq.gz" + "\"";
				String outu2 = "\"" + mergedDirectory.getAbsolutePath() + separator + entry + "_unmerged_R2.fastq.gz" + "\"";
				String options = " in=" + in + " in2=" + in2 + " out=" + out + " outu=" + outu + " outu2=" + outu2 +
						" mininsert=0 minoverlap=12";
				String fullProcessCall = processCall + options;
				try {
					Process p = Runtime.getRuntime().exec(fullProcessCall, null, BBToolsLocation);
					mainProcesses.add(p);
					BufferedReader error = new BufferedReader(new InputStreamReader(p.getErrorStream()));
					Platform.runLater(() -> consumer.start());
					while((this.line = error.readLine()) != null) {
						try {
							messageQueue.put(this.line);
						}catch(InterruptedException e) {
							e.printStackTrace();
						}
					}
					consumer.stop();
					error.close();
					try {
						p.waitFor();
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
			String processCall = "java -ea -Xmx" + memJava + " -cp ./current align2.BBMap";
			for(String entry : sampleList) {
				String options = "";
				String ref = "\"" + referenceFile.getAbsolutePath() + "\"";
				String scafstats = "\"" + bbMapTSV.getAbsolutePath() + separator + entry + "_mapping_stats.tsv" + "\"";
				String outm = "\"" + bbMapSam.getAbsolutePath() + separator + entry + "_mapped.sam" + "\"";
				if(mergeReads) {
					File entryDirectory = new File(merged.getAbsolutePath() + separator + entry);
					String in = "\"" + entryDirectory.getAbsolutePath() + separator + entry + "_merged.fastq.gz" + "\"";
					options = " in=" + in + " ref=" + ref + " scafstats=" + scafstats + " outm=" + outm + 
									" threads=" + threads + " minid=0.95 overwrite=t sortstats=t trd=t nodisk=t";
				}else {
					File entryDirectory = new File(trimmed.getAbsolutePath() + separator + entry);
					String in = "\"" + entryDirectory.getAbsolutePath() + separator + entry + "_Cleaned_1P.fastq.gz" + "\"";
					String in2 = "\"" + entryDirectory.getAbsolutePath() + separator + entry + "_Cleaned_2P.fastq.gz" + "\"";
					options = " in=" + in + " in2=" + in2 + " ref=" + ref + " scafstats=" + scafstats + " outm=" + outm + 
									" threads=" + threads + " minid=0.95 overwrite=t sortstats=t trd=t nodisk=t";
				}
				String fullProcessCall = processCall + options;
				try {
					Process assemblyRun = Runtime.getRuntime().exec(fullProcessCall, null, BBToolsLocation);
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
			String processCall = "java -ea -Xmx" + memJava + " -jar picard.jar SamFormatConverter";
			String in = "\"" + bbMapSam.getAbsolutePath() + separator + entry + "_mapped.sam" + "\"";
			String out = "\"" + bbMapBam.getAbsolutePath() + separator + entry + ".bam" + "\"";
			String fullProcessCall = processCall + " I=" + in + " O=" + out;
			try {
				Process p = Runtime.getRuntime().exec(fullProcessCall, null, PicardLocation);
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
			String processCall = "java -ea -Xmx" + memJava + " -jar picard.jar SortSam";
			String in = "\"" + bbMapBam.getAbsolutePath() + separator + entry + ".bam" + "\"";
			String out = "\"" + bamSorted.getAbsolutePath() + separator + entry + "_sorted.bam" + "\"";
			String sortOrder = "coordinate";
			String fullProcessCall = processCall + " I=" + in + " O=" + out + " SORT_ORDER=" + sortOrder;
			try {
				Process p = Runtime.getRuntime().exec(fullProcessCall, null, PicardLocation); 
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
			String processCall = "java -ea -Xmx" + memJava + " -jar picard.jar BuildBamIndex";
			String in = "\"" + bamSorted.getAbsolutePath() + separator + entry + "_sorted.bam" + "\"";
			String out = "\"" + bamSorted.getAbsolutePath() + separator + entry + "_sorted.bam.bai" + "\"";
			String fullProcessCall = processCall + " I=" + in + " O=" + out;
			try {
				Process p = Runtime.getRuntime().exec(fullProcessCall, null, PicardLocation);
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
	
	public void RunTablet(File inputFile, File referenceFile) {
		String fullProcessCall = TabletLocation + "\\tablet.exe \"" + inputFile.getAbsolutePath() + "\"" + 
								" \"" + referenceFile.getAbsolutePath() + "\"";
		try{
			Process p = Runtime.getRuntime().exec(fullProcessCall);
		}catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	public void logMessage(TextArea outputField, String msg) {
		Platform.runLater(() -> outputField.appendText("\n##########\n" + msg + "\n##########\n"));
	}
}
