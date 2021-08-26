package ampliconmapper;

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

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Collections;

import javax.swing.JOptionPane;

import java.nio.file.Path;
import java.nio.file.Paths;

import java.awt.Desktop;
import java.net.URI;

public class Entrance extends Application {
	
	private Stage primaryStage;
	private static File FastQCLocation = null, BBToolsLocation = null, PicardLocation = null, TabletLocation = null, JavaLocation = null;
	private static String javaCall = null;
	public static String sep = File.separator;

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
//		scene.getStylesheets().add(Entrance.class.getResource("/resources/Splash.css").toString()); // for running in Eclipse
		scene.getStylesheets().add("src/resources/Splash.css");
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
		Application.launch(Entrance.class);
	}
	
	private class CheckDependenciesWindows extends Task<Void>{
		
		public CheckDependenciesWindows() {
			
		}
		
		@Override
		public Void call() {
			updateMessage("Finding Java");
			findJava();
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
			WelcomePage page = new WelcomePage(primaryStage, FastQCLocation, BBToolsLocation, PicardLocation, TabletLocation, javaCall);
			Platform.runLater(() -> page.run());
			return null;
		}
	}
	
	private static void findJava() {
		String codeLocation = Entrance.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		String codeParent = codeLocation;
		try{
			codeParent = (new File(codeLocation)).getCanonicalPath();
		}catch(IOException e) {
			e.printStackTrace();
		}
		Path dir = Paths.get(codeParent);
//		Path dir = new File("C:\\Users\\ChmaraJ\\Desktop").toPath();
		Find.Finder finder3;
		if(System.getProperties().getProperty("os.name").contains("Windows")) {
			finder3 = new Find.Finder("**AmpliconMapper_java_runtime_windows", dir);
		}else {
			finder3 = new Find.Finder("**AmpliconMapper_java_runtime_linux", dir);
		}
		for(Path path : finder3.run()) {
			File javapath = path.toFile();
			if(javapath.isDirectory()) {
				for(File item : javapath.listFiles()) {
					if(item.isDirectory()) {
						for(File item2 : item.listFiles()) {
							if(System.getProperties().getProperty("os.name").contains("Windows")) {
								if(item2.getName().equals("java.exe")) {
									JavaLocation = item.getAbsoluteFile();
								}
							}else {
								if(item2.getName().equals("java")) {
									JavaLocation = item.getAbsoluteFile();
								}
							}
						}
					}
				}
			}
		}
		if(System.getProperties().getProperty("os.name").contains("Windows")) {
			javaCall = JavaLocation.getAbsolutePath() + sep + "java.exe";
		}else {
			javaCall = JavaLocation.getAbsolutePath() + sep + "java";
		}
	}
	
	
	private static boolean findFastQCWindows() {
		String codeLocation = Entrance.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		String codeParent = codeLocation;
		try {
			codeParent = (new File(codeLocation)).getCanonicalPath();
		}catch(IOException e) {
			e.printStackTrace();
		}
		Path dir = Paths.get(codeParent);
//		Path dir = new File("C:\\Users\\ChmaraJ\\Desktop").toPath();
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
		String codeLocation = Entrance.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		String codeParent = codeLocation;
		try {
			codeParent = (new File(codeLocation)).getCanonicalPath();
		}catch(IOException e) {
			e.printStackTrace();
		}
		Path dir = Paths.get(codeParent);
//		Path dir = new File("C:\\Users\\ChmaraJ\\Desktop").toPath();
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
		String codeLocation = Entrance.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		String codeParent = codeLocation;
		try {
			codeParent = (new File(codeLocation)).getCanonicalPath();
		}catch(IOException e) {
			e.printStackTrace();
		}
		Path dir = Paths.get(codeParent);
//		Path dir = new File("C:\\Users\\ChmaraJ\\Desktop").toPath();
		Find.Finder finder = new Find.Finder("**picard.jar", dir);
		for(Path path : finder.run()) {
			PicardLocation = path.getParent().toFile();
			return true;
		}
		return false;
	}
	
	private static boolean findTablet() {
		String codeLocation = Entrance.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		String codeParent = codeLocation;
		try {
			codeParent = (new File(codeLocation)).getCanonicalPath();
		}catch(IOException e) {
			e.printStackTrace();
		}
		Path dir = Paths.get(codeParent);
//		Path dir = new File("C:\\Users\\ChmaraJ\\Desktop").toPath();
		if(System.getProperties().getProperty("os.name").contains("Windows")) {
			Find.Finder finder = new Find.Finder("**tablet.exe", dir);
			for(Path path : finder.run()) {
				TabletLocation = path.getParent().toFile();
				return true;
			}
		}else {
			Find.Finder finder = new Find.Finder("**tablet", dir);
			for(Path path : finder.run()) {
				TabletLocation = path.getParent().toFile();
				return true;
			}
		}
		return false;
	}
	
	@Override
	public void stop() {
		System.out.println("Stopping Application");
	}
}

