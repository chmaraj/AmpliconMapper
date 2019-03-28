package appliconmapper;

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
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.concurrent.Task;
import javafx.application.Platform;
import javafx.scene.control.Tooltip;

import java.io.File;
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

import javax.swing.JOptionPane;

public class QuickRun implements Page{
	
	private String separator = "/";
	private Stage primaryStage;
	private File readDirectory, outputDirectory, referenceFile, FastQCLocation, BBToolsLocation, PicardLocation, TabletLocation;
	private File bbMapSam, bbMapTSV, bbMapBam, bamSorted;
	private ArrayList<String> sampleList = new ArrayList<String>();
	private boolean currentlyRunning = false, runCompleted = false;
	private ExecutorService mainPool = null;
	private ArrayList<Process> mainProcesses= new ArrayList<Process>();

	public QuickRun(Stage primaryStage, File FastQCLocation, File BBToolsLocation, File PicardLocation, File TabletLocation) {
		this.primaryStage = primaryStage;
		this.FastQCLocation = FastQCLocation;
		this.BBToolsLocation = BBToolsLocation;
		this.PicardLocation = PicardLocation;
		this.TabletLocation = TabletLocation;
		if(System.getProperties().getProperty("os.name").contains("Windows")) {
			this.separator = "\\";
		}
	}
	
	public void run() {
		GridPane pane = new GridPane();
		ColumnConstraints cC = new ColumnConstraints();
		cC.setPercentWidth(5);
		List<ColumnConstraints> colCopies = Collections.nCopies(20, cC);
		pane.getColumnConstraints().addAll(colCopies);
		RowConstraints rC = new RowConstraints();
		rC.setPercentHeight(5);
		List<RowConstraints> rowCopies = Collections.nCopies(20, rC);
		pane.getRowConstraints().addAll(rowCopies);
		
		Text disclaimer = new Text();
		disclaimer.setId("quick_disclaimer");
		disclaimer.setTextAlignment(TextAlignment.CENTER);
		HBox disclaimerBox = new HBox(10);
		disclaimerBox.setAlignment(Pos.CENTER);
		disclaimerBox.getChildren().add(disclaimer);
		pane.add(disclaimerBox, 1, 0, 18, 1);
		
		Text inputPrompt = new Text("Please enter a directory which contains the raw reads");
		inputPrompt.setTextAlignment(TextAlignment.CENTER);
		HBox inputBox = new HBox(10);
		inputBox.setAlignment(Pos.CENTER);
		inputBox.getChildren().add(inputPrompt);
		pane.add(inputBox, 1, 1, 7, 1);
		
		TextField readInput = new TextField();
		readInput.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		readInput.setEditable(false);
		readInput.setTooltip(new Tooltip("A directory containing only raw sequence files in .fastq.gz, .fasta, .fa, .fna, or .ffn format"));
		pane.add(readInput, 1, 2, 11, 1);
		
		Button browseInputs = new Button("Browse...");
		browseInputs.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		browseInputs.getStyleClass().add("browseButton");
		pane.add(browseInputs, 13, 2, 2, 1);
		browseInputs.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent e) {
				DirectoryChooser directoryChooser = new DirectoryChooser();
				readDirectory = directoryChooser.showDialog(primaryStage);
				readInput.setText(readDirectory.getAbsolutePath());
			}
		});
		
		Text refPrompt = new Text("Please enter a reference file");
		refPrompt.setTextAlignment(TextAlignment.CENTER);
		HBox refBox = new HBox(10);
		refBox.setAlignment(Pos.CENTER_LEFT);
		refBox.getChildren().add(refPrompt);
		pane.add(refBox, 1, 3, 8, 1);
		
		TextField refFileField = new TextField();
		refFileField.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		refFileField.setEditable(false);
		refFileField.setTooltip(new Tooltip("A single fasta file containing the reference to align the reads to"));
		pane.add(refFileField, 1, 4, 11, 1);
		
		Button browseRef = new Button("Browse...");
		browseRef.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		browseRef.getStyleClass().add("browseButton");
		pane.add(browseRef, 13, 4, 2, 1);
		browseRef.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent e) {
				FileChooser fileChooser = new FileChooser();
				referenceFile = fileChooser.showOpenDialog(primaryStage);
				refFileField.setText(referenceFile.getAbsolutePath());
			}
		});
		
		Text outputPrompt = new Text("Please enter a directory which will receive the output contigs");
		outputPrompt.setTextAlignment(TextAlignment.CENTER);
		HBox outputBox = new HBox(10);
		outputBox.setAlignment(Pos.CENTER);
		outputBox.getChildren().add(outputPrompt);
		pane.add(outputBox, 1, 5, 8, 1);
		
		TextField outputFileField = new TextField();
		outputFileField.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		outputFileField.setEditable(false);
		outputFileField.setTooltip(new Tooltip("An empty directory which will receive the output files"));
		pane.add(outputFileField, 1, 6, 11, 1);
		
		Button browseOutputs = new Button("Browse...");
		browseOutputs.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		browseOutputs.getStyleClass().add("browseButton");
		pane.add(browseOutputs, 13, 6, 2, 1);
		browseOutputs.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent e) {
				DirectoryChooser directoryChooser = new DirectoryChooser();
				outputDirectory = directoryChooser.showDialog(primaryStage);
				outputFileField.setText(outputDirectory.getAbsolutePath());
			}
		});
		
		Text threadPrompt = new Text("Input number of\nthreads to use(1, 2, etc.)");
		threadPrompt.setTextAlignment(TextAlignment.CENTER);
		HBox threadBox = new HBox(10);
		threadBox.setAlignment(Pos.CENTER);
		threadBox.getChildren().add(threadPrompt);
		pane.add(threadBox, 16, 1, 2, 2);
		
		TextField threadField = new TextField();
		threadField.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		threadField.setAlignment(Pos.CENTER);
		threadField.setText(Integer.toString(Runtime.getRuntime().availableProcessors() - 1));
		pane.add(threadField, 16, 3, 3, 2);
		
		Button backButton = new Button("Back");
		backButton.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		pane.add(backButton, 1, 17, 2, 2);
		backButton.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent e) {
				WelcomePage page = new WelcomePage(primaryStage, FastQCLocation, BBToolsLocation, PicardLocation, TabletLocation);
				page.run();
			}
		});
		
		TextArea outputField = new TextArea();
		outputField.setEditable(false);
		pane.add(outputField, 1, 8, 18, 8);
		
		Text alertText = new Text();
		alertText.setId("alertText");
		HBox alertBox = new HBox(10);
		alertBox.setAlignment(Pos.CENTER);
		alertBox.getChildren().add(alertText);
		pane.add(alertText, 1, 16, 18, 1);
		
		Button proceed = new Button("Proceed");
		proceed.setId("proceedButton");
		proceed.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		pane.add(proceed, 9, 17, 2, 2);
		proceed.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent e) {
				int threads = 0;
				try {
					threads = Integer.parseInt(threadField.getText());
				}catch(Exception exception) {
					alertText.setText("You need to enter a number in the Threads field");
					return;
				}
				if(readDirectory == null || outputDirectory == null || referenceFile == null) {
					alertText.setText("Please enter an input file directory, a reference file, and an output file directory");
				}else if(!checkReadsCompatible()){
					alertText.setText("Directory containing reads is either empty or contains incompatible files");
				}else if(threads >= Runtime.getRuntime().availableProcessors()){
					alertText.setText("Thread count must be at most 1 less than total number of cores available");
				}else{
					alertText.setText("");
					RunPipeline run = new RunPipeline(outputField, threads);
					Thread t = new Thread(run);
					t.setDaemon(true);
					t.start();
					currentlyRunning = true;
				}
			}
		});
		
//		Button helpButton = new Button("Help");
//		helpButton.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
//		assemblyPane.add(helpButton, 17, 17, 2, 2);
//		helpButton.setOnAction(new EventHandler<ActionEvent>() {
//			public void handle(ActionEvent e) {
//				
//			}
//		});
		
		Scene scene = new Scene(pane, 800, 500);
		scene.getStylesheets().add("QCPage.css");
		primaryStage.setScene(scene);
//		assemblyPane.setGridLinesVisible(true);
		primaryStage.setTitle("Amplicon Mapper");
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
					if(!outputField.getText().isEmpty() && !runCompleted) {
						try{
							FileWriter logFile = new FileWriter(outputDirectory.getAbsolutePath() + separator + "log.txt", true);
							String[] outputLines = outputField.getText().split("\n");
							System.out.println(outputLines.length);
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
		primaryStage.show();
	}
	
	public boolean checkReadsCompatible() {
		File[] readList = readDirectory.listFiles();
		if(readList.length == 0) {
			return false;
		}
		boolean compatible = true;
		for(File entry : readList) {
			String[] splitString = entry.getName().split("\\.");
			boolean match = false;
			for(String string : splitString) {
				if(string.equals("fastq") || string.equals("fq") || string.equals("bam") || string.equals("sam")) {
					match = true;
				}
			}
			if(match == false) {
				compatible = false;
				break;
			}
		}
		if(compatible == true) {
			return true;
		}else {
			return false;
		}
	}
	
	public void createSampleList() {
		File[] readList = readDirectory.listFiles();
		for(File entry : readList) {
			String sampleID = entry.getName().replace(".fastq.gz", "");
			if(!sampleList.contains(sampleID)) {
				sampleList.add(sampleID);
			}
		}
	}
	
	public void createDirectories() {
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
		private int threads;
		
		public RunPipeline(TextArea outputField, int threads) {
			this.outputField = outputField;
			this.threads = threads;
		}
		
		public Void call() {
			createSampleList();
			createDirectories();
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
				String[] outputLines = outputField.getText().split("\n");
				for(String line : outputLines) {
					logFile.write(line + "\n");
				}
				logFile.close();
			}catch(IOException e) {
				e.printStackTrace();
			}
			currentlyRunning = false;
			runCompleted = true;
			return null;
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
			try {
				mainPool.shutdown();
				mainPool.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
			}catch(InterruptedException e) {
				e.printStackTrace();
			}
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
				String in = "\"" + readDirectory.getAbsolutePath() + separator + entry + ".fastq.gz" + "\"";
				String ref = "\"" + referenceFile.getAbsolutePath() + "\"";
				String scafstats = "\"" + bbMapTSV.getAbsolutePath() + separator + entry + "_mapping_stats.tsv" + "\"";
				String outm = "\"" + bbMapSam.getAbsolutePath() + separator + entry + "_mapped.sam" + "\"";
				String options = " in=" + in + " ref=" + ref + " scafstats=" + scafstats + " outm=" + outm + " threads=" + threads +
								" minid=0.95 overwrite=t sortstats=t trd=t nodisk=t";
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
	
	public void logMessage(TextArea outputField, String message) {
		Platform.runLater(() -> outputField.appendText("\n" + message + "\n"));
	}
}

