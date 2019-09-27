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
import javafx.scene.control.Button;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.application.Platform;
import javafx.scene.control.Tooltip;

import java.io.File;
import java.io.IOException;
import java.lang.Process;
import java.lang.Runtime;

import java.util.Collections;
import java.util.List;

public class TabletRun implements Page{
	
	private Stage primaryStage;
	private File FastQCLocation, BBToolsLocation, PicardLocation, TabletLocation;
	private File inputFile, referenceFile;

	public TabletRun(Stage primaryStage, File FastQCLocation, File BBToolsLocation, File PicardLocation, File TabletLocation) {
		this.primaryStage = primaryStage;
		this.FastQCLocation = FastQCLocation;
		this.BBToolsLocation = BBToolsLocation;
		this.PicardLocation = PicardLocation;
		this.TabletLocation = TabletLocation;
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
		
		Text inputPrompt = new Text("Please enter a mapped .bam file to visualize");
		inputPrompt.setTextAlignment(TextAlignment.CENTER);
		HBox inputBox = new HBox(10);
		inputBox.setAlignment(Pos.CENTER);
		inputBox.getChildren().add(inputPrompt);
		pane.add(inputBox, 1, 3, 14, 2);
		
		TextField inputField = new TextField();
		inputField.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		inputField.setEditable(false);
		inputField.setTooltip(new Tooltip("The .bam file to open in Tablet"));
		pane.add(inputField, 1, 5, 14, 2);
		
		Button browseInputs = new Button("Browse...");
		browseInputs.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		browseInputs.getStyleClass().add("browseButton");
		pane.add(browseInputs, 16, 5, 3, 2);
		browseInputs.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent e) {
				FileChooser chooser = new FileChooser();
				inputFile = chooser.showOpenDialog(primaryStage);
				inputField.setText(inputFile.getAbsolutePath());
			}
		});
		
		Text refPrompt = new Text("Please enter a reference file");
		refPrompt.setTextAlignment(TextAlignment.CENTER);
		HBox refBox = new HBox(10);
		refBox.setAlignment(Pos.CENTER);
		refBox.getChildren().add(refPrompt);
		pane.add(refBox, 1, 9, 14, 2);
		
		TextField refFileField = new TextField();
		refFileField.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		refFileField.setEditable(false);
		refFileField.setTooltip(new Tooltip("The reference file that the .bam file was mapped to"));
		pane.add(refFileField, 1, 11, 14, 2);
		
		Button browseRef = new Button("Browse...");
		browseRef.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		browseRef.getStyleClass().add("browseButton");
		pane.add(browseRef, 16, 11, 3, 2);
		browseRef.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent e) {
				FileChooser chooser = new FileChooser();
				referenceFile = chooser.showOpenDialog(primaryStage);
				refFileField.setText(referenceFile.getAbsolutePath());
			}
		});
		
		Button backButton = new Button("Back");
		backButton.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		pane.add(backButton, 1, 17, 3, 2);
		backButton.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent e) {
				WelcomePage page = new WelcomePage(primaryStage, FastQCLocation, BBToolsLocation, PicardLocation, TabletLocation);
				page.run();
			}
		});
		
		Text alertText = new Text();
		alertText.setId("alertText");
		HBox alertBox = new HBox(10);
		alertBox.setAlignment(Pos.CENTER);
		alertBox.getChildren().add(alertText);
		pane.add(alertText, 1, 16, 18, 1);
		
		Button proceed = new Button("Proceed");
		proceed.setId("proceedButton");
		proceed.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		pane.add(proceed, 8, 17, 4, 2);
		proceed.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent e) {
				RunTablet(inputFile, referenceFile);
			}
		});
		
		Scene scene = new Scene(pane, 500, 300);
		scene.getStylesheets().add("QCPage.css");
		primaryStage.setScene(scene);
//		assemblyPane.setGridLinesVisible(true);
		primaryStage.setTitle("Amplicon Mapper");
		primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
			public void handle(WindowEvent e) {
				Platform.exit();
				System.exit(0);
			}
		});
		primaryStage.show();
	}
	
	public void RunTablet(File inputFile, File referenceFile) {
		String fullProcessCall = TabletLocation + "\\tablet.exe " + "\"" + inputFile.getAbsolutePath() + "\"" +
							" \"" + referenceFile.getAbsolutePath() + "\"";
		try{
			Process p = Runtime.getRuntime().exec(fullProcessCall);
		}catch(IOException e) {
			e.printStackTrace();
		}
	}
}

