package ampliconmapper;

import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.RowConstraints;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.event.EventHandler;
import javafx.event.ActionEvent;
import javafx.scene.input.MouseEvent;

import java.util.List;
import java.util.Collections;
import java.io.File;

public class HelpPage {
	
	private Stage primaryStage;
	private File FastQCLocation, BBToolsLocation, PicardLocation, TabletLocation;
	private String javaCall;
	
	public HelpPage(Stage primaryStage, File FastQCLocation, File BBToolsLocation, File PicardLocation, File TabletLocation, String javaCall) {
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
		
		Label banner = new Label();
		banner.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		banner.setId("banner");
		pane.add(banner, 0, 0, 50, 5);
		
		Text helpTitle = new Text("HELP");
		helpTitle.setTextAlignment(TextAlignment.CENTER);
		HBox titleBox = new HBox(10);
		titleBox.setAlignment(Pos.CENTER);
		titleBox.getChildren().add(helpTitle);
		helpTitle.setId("title");
		pane.add(titleBox, 1, 0, 48, 5);
		
		Label[] helpOptions = {new Label("Run FastQC"), 
							new Label("Quick Run"), 
							new Label("Map IonTorrent Reads"), 
							new Label("Map Illumina Reads"), 
							new Label("View in Tablet")};
		
		GridPane contentArea = new GridPane();
		contentArea.getStyleClass().add("contentArea");
		ColumnConstraints contentWidth = new ColumnConstraints();
		contentWidth.setPercentWidth(2);
		List<ColumnConstraints> contentWidthConstraints = Collections.nCopies(50, contentWidth);
		contentArea.getColumnConstraints().addAll(contentWidthConstraints);
		RowConstraints contentHeight = new RowConstraints();
		contentHeight.setPercentHeight(2);
		List<RowConstraints> contentHeightConstraints = Collections.nCopies(50, contentHeight);
		contentArea.getRowConstraints().addAll(contentHeightConstraints);
		pane.add(contentArea, 11, 6, 38, 42);
		
		for(int i = 0; i < helpOptions.length; i++) {
			helpOptions[i].getStyleClass().add("helpOption");
			helpOptions[i].setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
			helpOptions[i].setWrapText(true);
			pane.add(helpOptions[i], 1, (i*7) + 6, 10, 7);
			helpOptions[i].setOnMouseClicked(new EventHandler<MouseEvent>(){
				public void handle(MouseEvent e) {
					Label source = (Label)e.getSource();
					adjustSelection(helpOptions, source);
					adjustContent(contentArea, source);
				}
			});
		}
		
		Button backButton = new Button("Back");
		backButton.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		pane.add(backButton, 2, 44, 5, 4);
		backButton.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent e) {
				WelcomePage page = new WelcomePage(primaryStage, FastQCLocation, BBToolsLocation, PicardLocation, TabletLocation, javaCall);
				page.run();
			}
		});
		
		Scene scene = new Scene(pane, 800, 500);
		scene.getStylesheets().add("src/resources/HelpPage.css");
//		scene.getStylesheets().add(HelpPage.class.getResource("/resources/HelpPage.css").toString()); // for running in Eclipse
//		pane.setGridLinesVisible(true);
		primaryStage.setScene(scene);
		primaryStage.setTitle("CFIAssembly");
		primaryStage.show();
	}
	
	public void adjustSelection(Label[] helpOptions, Label source) {
		for(Label option : helpOptions) {
			option.getStyleClass().clear();
			if(option.equals(source)){
				option.getStyleClass().add("selected");
			}else {
				option.getStyleClass().add("helpOption");
			}
		}
	}
	
	public void adjustContent(GridPane contentArea, Label source) {
		
		contentArea.getChildren().clear();
		
		switch (source.getText()) {
			case "Run FastQC":
				createFastQCPage(contentArea);
				break;
			case "Quick Run":
				createQuickRunPageOne(contentArea);
				break;
			case "Map IonTorrent Reads":
				createIonTorrentPageOne(contentArea);
				break;
			case "Map Illumina Reads":
				createIlluminaPageOne(contentArea);
				break;
			case "View in Tablet":
				createTabletPage(contentArea);
				break;
			default:
				break;
		}
	}
	
	public void createFastQCPage(GridPane contentArea) {
		Label helpFastQC = new Label();
		helpFastQC.getStyleClass().add("subtitle");
		helpFastQC.setText("Run FastQC");
		helpFastQC.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		contentArea.add(helpFastQC, 0, 0, 50, 5);
		
		Label helpText = new Label();
		helpText.getStyleClass().add("helpText");
		helpText.setText("This selection is intended as a first look at the quality of the raw read sequences produced by "
				+ "your sequencing run. The pipeline will produce files containing statistics relevant to the input raw sequence reads. "
				+ "Opening the output .html file(s) will open the file in your browser.");
		helpText.setWrapText(true);
		helpText.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		contentArea.add(helpText, 2, 5, 46, 7);
		
		Label inputOption = new Label();
		inputOption.getStyleClass().add("subsubtitle");
		inputOption.setText("Input Field");
		inputOption.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		contentArea.add(inputOption, 2, 12, 10, 5);
		
		Label inputHelp = new Label();
		inputHelp.getStyleClass().add("helpText");
		inputHelp.setText("The input directory (folder) which contains your raw read sequences. The directory must only contain "
				+ "raw reads in .fastq.gz, .fastq, .fasta, .fa, .fna, or .ffn format, or the program will not allow you to proceed. "
				+ "The directory can also contain either one or multiple files, as all files within the directory will be analyzed.");
		inputHelp.setWrapText(true);
		inputHelp.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		contentArea.add(inputHelp, 2, 17, 46, 7);
		
		Label outputOption = new Label();
		outputOption.getStyleClass().add("subsubtitle");
		outputOption.setText("Output Field");
		outputOption.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		contentArea.add(outputOption, 2, 24, 10, 5);
		
		Label outputHelp = new Label();
		outputHelp.getStyleClass().add("helpText");
		outputHelp.setText("The output directory (folder) which will receive the output of the program run. The directory can contain "
				+ "files already, it does not need to be empty.");
		outputHelp.setWrapText(true);
		outputHelp.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		contentArea.add(outputHelp, 2, 29, 46, 5);
		
		Label threadOption = new Label();
		threadOption.getStyleClass().add("subsubtitle");
		threadOption.setText("Thread Field");
		threadOption.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		contentArea.add(threadOption, 2, 34, 10, 5);
		
		Label threadHelp = new Label();
		threadHelp.getStyleClass().add("helpText");
		threadHelp.setText("The number of threads to use for parallel processing. Essentially how many files can be analyzed at the same "
				+ "time, speeding up wait time significantly. This value must be (and defaults to) at most one less than the number of "
				+ "cores in your computer, as one thread must be reserved for running the user interface. For example, if your computer has "
				+ "four cores, you can run at most 3 threads at once.");
		threadHelp.setWrapText(true);
		threadHelp.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		contentArea.add(threadHelp, 2, 39, 46, 10);
	}
	
	public void createQuickRunPageOne(GridPane contentArea) {
		Label helpFastQC = new Label();
		helpFastQC.getStyleClass().add("subtitle");
		helpFastQC.setText("Quick Run");
		helpFastQC.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		contentArea.add(helpFastQC, 0, 0, 50, 5);
		
		Label helpText = new Label();
		helpText.getStyleClass().add("helpText");
		helpText.setText("This selection is intended as a first look at the mapping of the raw read sequences produced by "
				+ "your sequencing run. The pipeline will first map your reads to the reference, and the output file will "
				+ "be reformatted to a .bam file, then sorted and indexed for viewing in programs like Tablet. This pipeline "
				+ "will not trim or filter your raw reads prior to mapping.");
		helpText.setWrapText(true);
		helpText.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		contentArea.add(helpText, 2, 5, 46, 10);
		
		Label inputOption = new Label();
		inputOption.getStyleClass().add("subsubtitle");
		inputOption.setText("Input Field");
		inputOption.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		contentArea.add(inputOption, 2, 15, 10, 5);
		
		Label inputHelp = new Label();
		inputHelp.getStyleClass().add("helpText");
		inputHelp.setText("The input directory (folder) which contains your raw read sequences. The directory must only contain "
				+ "raw reads in .fastq.gz, .fastq, .fasta, .fa, .fna, or .ffn format, or the program will not allow you to proceed. "
				+ "The directory can also contain either one or multiple files, as all files within the directory will be analyzed.");
		inputHelp.setWrapText(true);
		inputHelp.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		contentArea.add(inputHelp, 2, 20, 46, 7);
		
		Label referenceOption = new Label();
		referenceOption.getStyleClass().add("subsubtitle");
		referenceOption.setText("Reference Field");
		referenceOption.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		contentArea.add(referenceOption, 2, 27, 10, 5);
		
		Label refHelp = new Label();
		refHelp.getStyleClass().add("helpText");
		refHelp.setText("The reference file that the raw reads will be mapped to. This file must be in .fasta format, or the "
				+ "program will crash. You may only select one reference file per run.");
		refHelp.setWrapText(true);
		refHelp.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		contentArea.add(refHelp, 2, 32, 46, 5);
		
		Label pageCount = new Label();
		pageCount.getStyleClass().add("pageCount");
		pageCount.setText("1 / 2");
		pageCount.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		contentArea.add(pageCount, 23, 45, 4, 2);
		
		Button next = new Button(">");
		next.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		contentArea.add(next, 27, 45, 4, 2);
		next.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent e) {
				contentArea.getChildren().clear();
				createQuickRunPageTwo(contentArea);
			}
		});
	}
	
	public void createQuickRunPageTwo(GridPane contentArea) {
		Label helpQuickRun = new Label();
		helpQuickRun.getStyleClass().add("subtitle");
		helpQuickRun.setText("Quick Run");
		helpQuickRun.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		contentArea.add(helpQuickRun, 0, 0, 50, 5);
		
		Label outputOption = new Label();
		outputOption.getStyleClass().add("subsubtitle");
		outputOption.setText("Output Field");
		outputOption.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		contentArea.add(outputOption, 2, 5, 10, 5);
		
		Label outputHelp = new Label();
		outputHelp.getStyleClass().add("helpText");
		outputHelp.setText("The output directory that will receive the files output from the program run. Multiple subfolders will be "
				+ "generated in the output directory to assist in organizing the output files. The selected output directory does not "
				+ "need to be empty.");
		outputHelp.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		outputHelp.setWrapText(true);
		contentArea.add(outputHelp, 2, 10, 46, 7);
		
		Label threadOption = new Label();
		threadOption.getStyleClass().add("subsubtitle");
		threadOption.setText("Thread field");
		threadOption.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		contentArea.add(threadOption, 2, 17, 10, 5);
		
		Label threadHelp = new Label();
		threadHelp.getStyleClass().add("helpText");
		threadHelp.setText("The number of threads to use for parallel processing. Essentially how many files can be analyzed at the same "
				+ "time, speeding up wait time significantly. This value must be (and defaults to) at most one less than the number of "
				+ "cores in your computer, as one thread must be reserved for running the user interface. For example, if your computer has "
				+ "four cores, you can run at most 3 threads at once.");
		threadHelp.setWrapText(true);
		threadHelp.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		contentArea.add(threadHelp, 2, 22, 46, 10);
		
		Label pageCount = new Label();
		pageCount.getStyleClass().add("pageCount");
		pageCount.setText("2 / 2");
		pageCount.setPrefSize(Double.MAX_VALUE,Double.MAX_VALUE);
		contentArea.add(pageCount, 23, 45, 4, 2);
		
		Button back = new Button("<");
		back.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		contentArea.add(back, 19, 45, 4, 2);
		back.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent e) {
				contentArea.getChildren().clear();
				createQuickRunPageOne(contentArea);
			}
		});
	}
	
	public void createIonTorrentPageOne(GridPane contentArea) {
		Label helpIonTorrent = new Label();
		helpIonTorrent.getStyleClass().add("subtitle");
		helpIonTorrent.setText("Map IonTorrent Reads");
		helpIonTorrent.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		contentArea.add(helpIonTorrent, 0, 0, 50, 5);
		
		Label helpText = new Label();
		helpText.getStyleClass().add("helpText");
		helpText.setText("This pipeline is intended to fully map input raw read files to the reference file submitted. The reads are first trimmed of "
				+ "adapters, then custom PCR primers are trimmed. Reads are then mapped to the reference, converted to .bam format, and subsequently "
				+ "sorted and indexed for viewing in Tablet. One file will then be opened in Tablet automatically.");
		helpText.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		helpText.setWrapText(true);
		contentArea.add(helpText, 2, 5, 46, 10);
		
		Label inputOption = new Label();
		inputOption.getStyleClass().add("subsubtitle");
		inputOption.setText("Input Field");
		inputOption.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		contentArea.add(inputOption, 2, 15, 10, 5);
		
		Label inputHelp = new Label();
		inputHelp.getStyleClass().add("helpText");
		inputHelp.setText("The input directory (folder) which contains your raw read sequences. The directory must only contain " + 
				"raw reads in .fastq.gz, .fastq, .fasta, .fa, .fna, or .ffn format, or the program will not allow you to proceed. " + 
				"The directory can also contain either one or multiple files, as all files within the directory will be analyzed.");
		inputHelp.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		inputHelp.setWrapText(true);
		contentArea.add(inputHelp, 2, 20, 46, 7);
		
		Label referenceOption = new Label();
		referenceOption.getStyleClass().add("subsubtitle");
		referenceOption.setText("Reference Field");
		referenceOption.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		contentArea.add(referenceOption, 2, 27, 10, 5);
		
		Label refHelp = new Label();
		refHelp.getStyleClass().add("helpText");
		refHelp.setText("The reference file that the raw reads will be mapped to. This file must be in .fasta format, or the "
				+ "program will crash. You may only select one reference file per run.");
		refHelp.setWrapText(true);
		refHelp.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		contentArea.add(refHelp, 2, 32, 46, 5);
		
		Label pageCount = new Label();
		pageCount.getStyleClass().add("pageCount");
		pageCount.setText("1 / 3");
		pageCount.setPrefSize(Double.MAX_VALUE,Double.MAX_VALUE);
		contentArea.add(pageCount, 23, 45, 4, 2);
		
		Button next = new Button(">");
		next.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		contentArea.add(next, 27, 45, 4, 2);
		next.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent e) {
				contentArea.getChildren().clear();
				createIonTorrentPageTwo(contentArea);
			}
		});
		
		Button last = new Button(">>");
		last.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		contentArea.add(last, 32, 45, 4, 2);
		last.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent e) {
				contentArea.getChildren().clear();
				createIonTorrentPageThree(contentArea);
			}
		});
	}
	
	public void createIonTorrentPageTwo(GridPane contentArea) {
		Label helpIonTorrent = new Label();
		helpIonTorrent.getStyleClass().add("subtitle");
		helpIonTorrent.setText("Map IonTorrent Reads");
		helpIonTorrent.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		contentArea.add(helpIonTorrent, 0, 0, 50, 5);
		
		Label outputOption = new Label();
		outputOption.getStyleClass().add("subsubtitle");
		outputOption.setText("Output Field");
		outputOption.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		contentArea.add(outputOption, 2, 5, 10, 5);
		
		Label outputHelp = new Label();
		outputHelp.getStyleClass().add("helpText");
		outputHelp.setText("The output directory that will receive the files output from the program run. Multiple subfolders will be "
				+ "generated in the output directory to assist in organizing the output files. The selected output directory does not "
				+ "need to be empty.");
		outputHelp.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		outputHelp.setWrapText(true);
		contentArea.add(outputHelp, 2, 10, 46, 7);
		
		Label threadOption = new Label();
		threadOption.getStyleClass().add("subsubtitle");
		threadOption.setText("Thread field");
		threadOption.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		contentArea.add(threadOption, 2, 17, 10, 5);
		
		Label threadHelp = new Label();
		threadHelp.getStyleClass().add("helpText");
		threadHelp.setText("The number of threads to use for parallel processing. Essentially how many files can be analyzed at the same "
				+ "time, speeding up wait time significantly. This value must be (and defaults to) at most one less than the number of "
				+ "cores in your computer, as one thread must be reserved for running the user interface. For example, if your computer has "
				+ "four cores, you can run at most 3 threads at once.");
		threadHelp.setWrapText(true);
		threadHelp.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		contentArea.add(threadHelp, 2, 22, 46, 10); 
		
		Label convertOption = new Label();
		convertOption.getStyleClass().add("subsubtitle");
		convertOption.setText("Convert .BAM");
		convertOption.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		contentArea.add(convertOption, 2, 32, 10, 5);
		
		Label convertHelp = new Label();
		convertHelp.getStyleClass().add("helpText");
		convertHelp.setText("Converts a .bam file to a .fastq.gz file.");
		convertHelp.setWrapText(true);
		convertHelp.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		contentArea.add(convertHelp, 2, 37, 46, 3);
		
		Label pageCount = new Label();
		pageCount.getStyleClass().add("pageCount");
		pageCount.setText("2 / 3");
		pageCount.setPrefSize(Double.MAX_VALUE,Double.MAX_VALUE);
		contentArea.add(pageCount, 23, 45, 4, 2);
		
		Button back = new Button("<");
		back.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		contentArea.add(back, 19, 45, 4, 2);
		back.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent e) {
				contentArea.getChildren().clear();
				createIonTorrentPageOne(contentArea);
			}
		});
		
		Button next = new Button(">");
		next.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		contentArea.add(next, 27, 45, 4, 2);
		next.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent e) {
				contentArea.getChildren().clear();
				createIonTorrentPageThree(contentArea);
			}
		});
	}
	
	public void createIonTorrentPageThree(GridPane contentArea) {
		Label helpIonTorrent = new Label();
		helpIonTorrent.getStyleClass().add("subtitle");
		helpIonTorrent.setText("Map IonTorrent Reads");
		helpIonTorrent.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		contentArea.add(helpIonTorrent, 0, 0, 50, 5);
		
		Label customOption = new Label();
		customOption.getStyleClass().add("subsubtitle");
		customOption.setText("Custom Primers Field");
		customOption.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		contentArea.add(customOption, 2, 10, 15, 5);
		
		Label customHelp = new Label();
		customHelp.getStyleClass().add("helpText");
		customHelp.setText("A text file containing information on custom primers used during amplicon PCR. The format is:\n\n"
				+ "NAME,POSITION,SEQUENCE\n\n"
				+ "NAME -> free form field. Try to avoid spaces and special characters.\n"
				+ "POSITION -> START or END. START will do a left trim, END will do a right trim.\n"
				+ "SEQUENCE -> the primer sequence. eg. ATCGCGTAGCAATTTCCGC\n\n"
				+ "Further information on the construction of your custom primer text file can be found in the "
				+ "\"Custom_Primer_File_Guide.txt\" file that came with your copy of this program.");
		customHelp.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		customHelp.setWrapText(true);
		contentArea.add(customHelp, 2, 15, 46, 25);
		
		Label pageCount = new Label();
		pageCount.getStyleClass().add("pageCount");
		pageCount.setText("3 / 3");
		pageCount.setPrefSize(Double.MAX_VALUE,Double.MAX_VALUE);
		contentArea.add(pageCount, 23, 45, 4, 2);
		
		Button first = new Button("<<");
		first.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		contentArea.add(first, 14, 45, 4, 2);
		first.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent e) {
				contentArea.getChildren().clear();
				createIonTorrentPageOne(contentArea);
			}
		});
		
		Button back = new Button("<");
		back.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		contentArea.add(back, 19, 45, 4, 2);
		back.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent e) {
				contentArea.getChildren().clear();
				createIonTorrentPageTwo(contentArea);
			}
		});
	}
	
	public void createIlluminaPageOne(GridPane contentArea) {
		Label helpIllumina = new Label();
		helpIllumina.getStyleClass().add("subtitle");
		helpIllumina.setText("Map Illumina Reads");
		helpIllumina.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		contentArea.add(helpIllumina, 0, 0, 50, 5);
		
		Label helpText = new Label();
		helpText.getStyleClass().add("helpText");
		helpText.setText("This pipeline is intended to fully map input raw read files to the reference file submitted. The reads are first trimmed of "
				+ "adapters, then custom PCR primers are trimmed. Reads are then mapped to the reference, converted to .bam format, and subsequently "
				+ "sorted and indexed for viewing in Tablet. One file will then be opened in Tablet automatically.");
		helpText.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		helpText.setWrapText(true);
		contentArea.add(helpText, 2, 5, 46, 10);
		
		Label inputOption = new Label();
		inputOption.getStyleClass().add("subsubtitle");
		inputOption.setText("Input Field");
		inputOption.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		contentArea.add(inputOption, 2, 15, 10, 5);
		
		Label inputHelp = new Label();
		inputHelp.getStyleClass().add("helpText");
		inputHelp.setText("The input directory (folder) which contains your raw read sequences. The directory must only contain " + 
				"raw reads in .fastq.gz, .fastq, .fasta, .fa, .fna, or .ffn format, or the program will not allow you to proceed. " + 
				"The directory can also contain either one or multiple files, as all files within the directory will be analyzed.");
		inputHelp.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		inputHelp.setWrapText(true);
		contentArea.add(inputHelp, 2, 20, 46, 7);
		
		Label referenceOption = new Label();
		referenceOption.getStyleClass().add("subsubtitle");
		referenceOption.setText("Reference Field");
		referenceOption.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		contentArea.add(referenceOption, 2, 27, 10, 5);
		
		Label refHelp = new Label();
		refHelp.getStyleClass().add("helpText");
		refHelp.setText("The reference file that the raw reads will be mapped to. This file must be in .fasta format, or the "
				+ "program will crash. You may only select one reference file per run.");
		refHelp.setWrapText(true);
		refHelp.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		contentArea.add(refHelp, 2, 32, 46, 5);
		
		Label pageCount = new Label();
		pageCount.getStyleClass().add("pageCount");
		pageCount.setText("1 / 3");
		pageCount.setPrefSize(Double.MAX_VALUE,Double.MAX_VALUE);
		contentArea.add(pageCount, 23, 45, 4, 2);
		
		Button next = new Button(">");
		next.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		contentArea.add(next, 27, 45, 4, 2);
		next.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent e) {
				contentArea.getChildren().clear();
				createIlluminaPageTwo(contentArea);
			}
		});
		
		Button last = new Button(">>");
		last.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		contentArea.add(last, 32, 45, 4, 2);
		last.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent e) {
				contentArea.getChildren().clear();
				createIlluminaPageThree(contentArea);
			}
		});
	}
	
	public void createIlluminaPageTwo(GridPane contentArea) {
		Label helpIllumina = new Label();
		helpIllumina.getStyleClass().add("subtitle");
		helpIllumina.setText("Map Illumina Reads");
		helpIllumina.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		contentArea.add(helpIllumina, 0, 0, 50, 5);
		
		Label outputOption = new Label();
		outputOption.getStyleClass().add("subsubtitle");
		outputOption.setText("Output Field");
		outputOption.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		contentArea.add(outputOption, 2, 5, 10, 5);
		
		Label outputHelp = new Label();
		outputHelp.getStyleClass().add("helpText");
		outputHelp.setText("The output directory that will receive the files output from the program run. Multiple subfolders will be "
				+ "generated in the output directory to assist in organizing the output files. The selected output directory does not "
				+ "need to be empty.");
		outputHelp.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		outputHelp.setWrapText(true);
		contentArea.add(outputHelp, 2, 10, 46, 7);
		
		Label threadOption = new Label();
		threadOption.getStyleClass().add("subsubtitle");
		threadOption.setText("Thread field");
		threadOption.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		contentArea.add(threadOption, 2, 17, 10, 5);
		
		Label threadHelp = new Label();
		threadHelp.getStyleClass().add("helpText");
		threadHelp.setText("The number of threads to use for parallel processing. Essentially how many files can be analyzed at the same "
				+ "time, speeding up wait time significantly. This value must be (and defaults to) at most one less than the number of "
				+ "cores in your computer, as one thread must be reserved for running the user interface. For example, if your computer has "
				+ "four cores, you can run at most 3 threads at once.");
		threadHelp.setWrapText(true);
		threadHelp.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		contentArea.add(threadHelp, 2, 22, 46, 10); 
		
		Label pageCount = new Label();
		pageCount.getStyleClass().add("pageCount");
		pageCount.setText("2 / 3");
		pageCount.setPrefSize(Double.MAX_VALUE,Double.MAX_VALUE);
		contentArea.add(pageCount, 23, 45, 4, 2);
		
		Button back = new Button("<");
		back.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		contentArea.add(back, 19, 45, 4, 2);
		back.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent e) {
				contentArea.getChildren().clear();
				createIlluminaPageOne(contentArea);
			}
		});
		
		Button next = new Button(">");
		next.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		contentArea.add(next, 27, 45, 4, 2);
		next.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent e) {
				contentArea.getChildren().clear();
				createIlluminaPageThree(contentArea);
			}
		});
	}
	
	public void createIlluminaPageThree(GridPane contentArea) {
		Label helpIllumina = new Label();
		helpIllumina.getStyleClass().add("subtitle");
		helpIllumina.setText("Map Illumina Reads");
		helpIllumina.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		contentArea.add(helpIllumina, 0, 0, 50, 5);
		
		Label mergeOption = new Label();
		mergeOption.getStyleClass().add("subsubtitle");
		mergeOption.setText("Merge Option");
		mergeOption.setPrefSize(Double.MAX_VALUE,Double.MAX_VALUE);
		contentArea.add(mergeOption, 2, 5, 10, 5);
		
		Label mergeHelp = new Label();
		mergeHelp.getStyleClass().add("helpText");
		mergeHelp.setText("Selecting this option (selected by default) adds additional steps to the pipeline whereby the program attempts to"
				+ "merge overlapping paired reads prior to mapping, which hopefully increases the mapping quality.");
		mergeHelp.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		mergeHelp.setWrapText(true);
		contentArea.add(mergeHelp, 2, 10, 46, 7);
		
		Label customOption = new Label();
		customOption.getStyleClass().add("subsubtitle");
		customOption.setText("Custom Primers Field");
		customOption.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		contentArea.add(customOption, 2, 17, 15, 5);
		
		Label customHelp = new Label();
		customHelp.getStyleClass().add("helpText");
		customHelp.setText("A text file containing information on custom primers used during amplicon PCR. The format is:\n\n"
				+ "NAME,POSITION,SEQUENCE\n\n"
				+ "NAME -> free form field. Try to avoid spaces and special characters.\n"
				+ "POSITION -> START or END. START will do a left trim, END will do a right trim.\n"
				+ "SEQUENCE -> the primer sequence. eg. ATCGCGTAGCAATTTCCGC\n\n"
				+ "Further information on the construction of your custom primer text file can be found in the "
				+ "\"Custom_Primer_File_Guide.txt\" file that came with your copy of this program.");
		customHelp.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		customHelp.setWrapText(true);
		contentArea.add(customHelp, 2, 22, 46, 22);
		
		Label pageCount = new Label();
		pageCount.getStyleClass().add("pageCount");
		pageCount.setText("3 / 3");
		pageCount.setPrefSize(Double.MAX_VALUE,Double.MAX_VALUE);
		contentArea.add(pageCount, 23, 45, 4, 2);
		
		Button first = new Button("<<");
		first.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		contentArea.add(first, 14, 45, 4, 2);
		first.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent e) {
				contentArea.getChildren().clear();
				createIlluminaPageOne(contentArea);
			}
		});
		
		Button back = new Button("<");
		back.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		contentArea.add(back, 19, 45, 4, 2);
		back.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent e) {
				contentArea.getChildren().clear();
				createIlluminaPageTwo(contentArea);
			}
		});
	}
	
	public void createTabletPage(GridPane contentArea) {
		Label helpTablet = new Label();
		helpTablet.getStyleClass().add("subtitle");
		helpTablet.setText("View in Tablet");
		helpTablet.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		contentArea.add(helpTablet, 0, 0, 50, 5);
		
		Label helpText = new Label();
		helpText.getStyleClass().add("helpText");
		helpText.setText("This selection is intended to provide a simplified, user-interface driven method of opening a mapped file "
				+ "in Tablet for viewing. All that is required is the input file and the reference used during the mapping.");
		helpText.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		helpText.setWrapText(true);
		contentArea.add(helpText, 2, 5, 46, 7);
		
		Label inputOption = new Label();
		inputOption.getStyleClass().add("subsubtitle");
		inputOption.setText("Input Field");
		inputOption.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		contentArea.add(inputOption, 2, 12, 10, 5);
		
		Label inputHelp = new Label();
		inputHelp.getStyleClass().add("helpText");
		inputHelp.setText("The input file to be viewed in Tablet. The file should be a .bam file, and should be contained in the same "
				+ "directory as its associated .bam.bai index file. If the files were produced using one of the mapping pipelines "
				+ "provided in this program, the files will be contained in the same directory already.");
		inputHelp.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		inputHelp.setWrapText(true);
		contentArea.add(inputHelp, 2, 17, 46, 10);
		
		Label referenceOption = new Label();
		referenceOption.getStyleClass().add("subsubtitle");
		referenceOption.setText("Reference Field");
		referenceOption.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		contentArea.add(referenceOption, 2, 27, 10, 5);
		
		Label refHelp = new Label();
		refHelp.getStyleClass().add("helpText");
		refHelp.setText("The reference file against which the mapped reads will be compared. This reference file should be the same "
				+ "one that was used to map the reads for the given sample in the first place. If the mapped file was produced using "
				+ "one of the pipelines contained in this program, the reference file will be the same as the one that was provided "
				+ "to the pipeline.");
		refHelp.setWrapText(true);
		refHelp.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		contentArea.add(refHelp, 2, 32, 46, 10);
	}

}

