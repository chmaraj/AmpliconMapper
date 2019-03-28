package appliconmapper;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.RowConstraints;
import javafx.scene.control.Label;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.control.ProgressBar;
import javafx.concurrent.Task;
import javafx.scene.layout.HBox;
import javafx.geometry.Pos;

import java.lang.Process;
import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

import javax.swing.JOptionPane;

import java.nio.file.Path;
import java.nio.file.Paths;

import java.awt.Desktop;
import java.net.URI;

public class Entrance extends Application {
	
	private Stage primaryStage;
	private static File FastQCLocation = null, BBToolsLocation = null, PicardLocation = null, TabletLocation = null;

	public void start(Stage primaryStage) {
		this.primaryStage = primaryStage;
		GridPane pane = new GridPane();
		ColumnConstraints cC = new ColumnConstraints();
		cC.setPercentWidth(5);
		List<ColumnConstraints> colCopies = Collections.nCopies(20, cC);
		pane.getColumnConstraints().addAll(colCopies);
		RowConstraints rC = new RowConstraints();
		rC.setPercentHeight(5);
		List<RowConstraints> rowCopies = Collections.nCopies(20, rC);
		pane.getRowConstraints().addAll(rowCopies);
		
		Label banner = new Label();
		banner.setId("banner");
		banner.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		pane.add(banner, 0, 0, 20, 4);
		
		Text title = new Text("Amplicon Mapper");
		title.setId("title");
		title.setTextAlignment(TextAlignment.LEFT);
		pane.add(title, 1, 1, 20, 2);
		
		Text progressText = new Text();
		progressText.setTextAlignment(TextAlignment.CENTER);
		progressText.setId("progress_text");
		HBox textBox = new HBox(10);
		textBox.setAlignment(Pos.CENTER);
		textBox.getChildren().add(progressText);
		pane.add(textBox, 2, 12, 16, 1);
		
		ProgressBar progress = new ProgressBar();
		progress.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		pane.add(progress, 2, 14, 16, 1);
		
		Scene scene = new Scene(pane, 500, 300);
		scene.getStylesheets().add("Splash.css");
		primaryStage.setScene(scene);
		primaryStage.setTitle("Amplicon Mapper");
		primaryStage.show();
		
		CheckDependenciesWindows task = new CheckDependenciesWindows();
		progressText.textProperty().bind(task.messageProperty());
		progress.progressProperty().bind(task.progressProperty());
		Thread t = new Thread(task);
		t.start();
	}
	
	public static void main(String[] args) {		
		Entrance entrance = new Entrance();
		Application.launch(entrance.getClass());
	}
	
	private class CheckDependenciesWindows extends Task<Void>{
		
		public CheckDependenciesWindows() {
			
		}
		
		@Override
		public Void call() {
			updateMessage("Finding Java");
			if(!findJavaWindows()) {
				JOptionPane.showMessageDialog(null, "No Java found in PATH. Please put Java in PATH prior to running.");
				System.exit(-1);
			}
			updateProgress(1, 4);
			updateMessage("Finding FastQC");
			if(!findFastQCWindows()) {
				JOptionPane.showMessageDialog(null, "No FastQC found. Please download and unpack FastQC, located here:" +
								"https://www.bioinformatics.babraham.ac.uk/projects/fastqc/.\nAlternatively, if you have" +
								"FastQC downloaded, ensure that the containing folder is entitled 'FastQC'");
				if(Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
					try {
						Desktop.getDesktop().browse(new URI("https://www.bioinformatics.babraham.ac.uk/projects/fastqc/"));
					}catch(Exception exception) {
						exception.printStackTrace();
					}
				}
			}
			updateProgress(2, 4);
			updateMessage("Finding BBTools");
			if(!findBBToolsWindows()) {
				JOptionPane.showMessageDialog(null, "No BBTools Suite found. Please download and unpack BBTools, located here:" +
								"https://sourceforge.net/projects/bbmap/. \n Alternatively, if you have BBMap downloaded," +
								"ensure that the containing folder is entitled 'bbMap'");
				if(Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
					try {
						Desktop.getDesktop().browse(new URI("https://sourceforge.net/projects/bbmap/"));
					}catch(Exception exception) {
						exception.printStackTrace();
					}
				}
			}
			updateProgress(3, 4);
			updateMessage("Finding Picard");
			if(!findPicard()) {
				JOptionPane.showMessageDialog(null, "No Picard found. Please download Picard, located here:" +
								"https://broadinstitute.github.io/picard.\nAlternatively, if you have Picard downloaded," +
								"ensure that the jar is located in your home drive.");
				if(Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
					try {
						Desktop.getDesktop().browse(new URI("https://broadinstitute.github.io/picard"));
					}catch(Exception exception) {
						exception.printStackTrace();
					}
				}
			}
			updateProgress(4, 4);
			updateMessage("Finding Tablet");
			if(!findTablet()) {
				JOptionPane.showMessageDialog(null, "No Tablet found. Please download and install Tablet, located here:" +
								"https://ics.hutton.ac.uk/tablet/download-tablet.\nAlternatively, if you have Tablet downloaded," +
								"ensure that the exe is located in your home drive.");
				if(Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
					try {
						Desktop.getDesktop().browse(new URI("https://ics.hutton.ac.uk/tablet/download-tablet/"));
					}catch(Exception exception) {
						exception.printStackTrace();
					}
				}
			}
			if(FastQCLocation == null && (BBToolsLocation == null || PicardLocation == null)) {
				JOptionPane.showMessageDialog(null, "No FastQC and no BBTools or Picard located. Exiting");
				System.exit(-1);
			}
			WelcomePage page = new WelcomePage(primaryStage, FastQCLocation, BBToolsLocation, PicardLocation, TabletLocation);
			Platform.runLater(() -> page.run());
			return null;
		}
	}
	
	private static boolean findJavaWindows() {
		ArrayList<String> results = new ArrayList<String>();
		try{
			String line;
			Process p = Runtime.getRuntime().exec("java -version");
			BufferedReader stdout = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			while((line = stdout.readLine()) != null) {
				results.add(line);
			}
		}catch(IOException e) {
			e.printStackTrace();
		}
		if(results.get(0).contains("java version")) {
			return true;
		}
		return false;
	}
	
	
	private static boolean findFastQCWindows() {
//		Path dir = Paths.get(System.getProperties().getProperty("user.dir"));
		Path dir = new File("C:\\Users\\ChmaraJ\\Desktop").toPath();
		Find.Finder finder = new Find.Finder("**FastQC", dir);
		for(Path path : finder.run()) {
			File directory = path.toFile();
			for(File file : directory.listFiles()) {
				if(file.getName().contains("run_fastqc.bat")) {
					FastQCLocation = path.toFile();
					return true;
				}
			}
		}		
		return false;
	}
	
	
	
	private static boolean findBBToolsWindows() {
//		Path dir = Paths.get(System.getProperties().getProperty("user.dir"));
		Path dir = new File("C:\\Users\\ChmaraJ\\Desktop").toPath();
		Find.Finder finder = new Find.Finder("**bbmap", dir);
		for(Path path: finder.run()) {
			File directory = path.toFile();
			for(File file : directory.listFiles()) {
				if(file.getName().contains("tadpole.sh")) {
					BBToolsLocation = path.toFile();
					return true;
				}
			}
		}
		return false;
	}
	
	private static boolean findPicard() {
//		Path dir = Paths.get(System.getProperties().getProperty("user.dir"));
		Path dir = new File("C:\\Users\\ChmaraJ\\Desktop").toPath();
		Find.Finder finder = new Find.Finder("**picard.jar", dir);
		for(Path path : finder.run()) {
			PicardLocation = path.getParent().toFile();
			return true;
		}
		return false;
	}
	
	private static boolean findTablet() {
//		Path dir = Paths.get(System.getProperties().getProperty("user.dir"));
		Path dir = new File("C:\\Users\\ChmaraJ\\Desktop").toPath();
		Find.Finder finder = new Find.Finder("**tablet.exe", dir);
		for(Path path : finder.run()) {
			TabletLocation = path.getParent().toFile();
			return true;
		}
		return false;
	}
	
	@Override
	public void stop() {
		System.out.println("Stopping Application");
	}
}

