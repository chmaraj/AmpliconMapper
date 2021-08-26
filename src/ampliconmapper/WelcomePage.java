package ampliconmapper;

import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.RowConstraints;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.control.Button;
import javafx.event.EventHandler;
import javafx.event.ActionEvent;

import java.util.List;

import java.util.Collections;
import java.io.File;
import java.util.ArrayList;

/* This application is intended for use on company laptops for researchers who want to
 * do their own sequence analysis, or who want to do read QC or a quick and dirty assembly.
 */
public class WelcomePage {
	
	private Stage primaryStage;
	public static final String version = "v0.310";
	private File FastQCLocation, BBToolsLocation, PicardLocation, TabletLocation;
	private String javaCall;
	
	public WelcomePage(Stage primaryStage, File FastQCLocation, File BBToolsLocation, File PicardLocation, File TabletLocation, String javaCall) {
		this.primaryStage = primaryStage;
		this.FastQCLocation = FastQCLocation;
		this.BBToolsLocation = BBToolsLocation;
		this.PicardLocation = PicardLocation;
		this.TabletLocation = TabletLocation;
		this.javaCall = javaCall;
	}
	
	public void run() {
		GridPane welcomePane = new GridPane();
		ColumnConstraints cC = new ColumnConstraints();
		cC.setPercentWidth(5);
		List<ColumnConstraints> colCopies = Collections.nCopies(20, cC);
		welcomePane.getColumnConstraints().addAll(colCopies);
		RowConstraints rC = new RowConstraints();
		rC.setPercentHeight(5);
		List<RowConstraints> rowCopies = Collections.nCopies(20, rC);
		welcomePane.getRowConstraints().addAll(rowCopies);
		
		Label background = new Label();
		background.setId("welcomeBanner");
		background.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		welcomePane.add(background, 0, 0, 20, 4);
		
		Text title = new Text("Amplicon Mapper");
		title.setId("welcomePageTitle");
		HBox titleBox = new HBox(10);
		titleBox.getChildren().add(title);
		titleBox.setAlignment(Pos.CENTER);
		welcomePane.add(titleBox, 1, 2);
		
		Text version = new Text(WelcomePage.version);
		version.setId("version");
		HBox versionBox = new HBox(10);
		versionBox.setAlignment(Pos.CENTER);
		versionBox.getChildren().add(version);
		welcomePane.add(versionBox, 18, 19, 2, 1);
		
		Label image = new Label();
		image.setId("backgroundImage");
		image.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		welcomePane.add(image, 0, 4, 20, 16);
		
		//Keep Button Generation concise
		ButtonHandle[] buttons = new ButtonHandle[] {
				new ButtonHandle("Read QC") {public void run() {QCPage page = new QCPage(primaryStage, FastQCLocation, BBToolsLocation, PicardLocation,
																TabletLocation, javaCall); page.run();}},
				new ButtonHandle("Quick Run") {public void run() {QuickRun page = new QuickRun(primaryStage,
																FastQCLocation, BBToolsLocation, PicardLocation, TabletLocation, javaCall); page.run();}},
				new ButtonHandle("Map IonTorrent Reads") {public void run() {IonTorrentMapper page = new IonTorrentMapper(primaryStage, 
																FastQCLocation, BBToolsLocation, PicardLocation, TabletLocation, javaCall); page.run();}},
				new ButtonHandle("Map Illumina Reads") {public void run() {MiSeqRun page = new MiSeqRun(primaryStage, 
																FastQCLocation, BBToolsLocation, PicardLocation, TabletLocation, javaCall); page.run();}},
				new ButtonHandle("View in Tablet") {public void run() {TabletRun page = new TabletRun(primaryStage, FastQCLocation, BBToolsLocation,
																PicardLocation, TabletLocation, javaCall); page.run();}},
				new ButtonHandle("Help") {public void run() {HelpPage page = new HelpPage(primaryStage, FastQCLocation, BBToolsLocation,
																PicardLocation, TabletLocation, javaCall); page.run();}}
		};
		
		ArrayList<Button> buttonsList = new ArrayList<Button>();
		
		for(int i = 0; i < 3; i++) {
			for(int j = 0; j < 2; j++) {
				ButtonHandle handle = buttons[(i * 2) + j];
				Button button = new Button(handle.getName());
				buttonsList.add(button);
				button.getStyleClass().add("subtitle");
				button.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
				welcomePane.add(button, (j*9) + 2, (i*4) + 6, 7, 3);
				button.setOnAction(new EventHandler<ActionEvent>() {
					public void handle(ActionEvent e) {
						handle.run();
					}
				});
			}
		}
		
		for(Button button : buttonsList) {
			if(button.getText() == "Read QC" && FastQCLocation == null) {
				button.setDisable(true);
			}else if((button.getText() == "Quick Run" || button.getText() == "Map IonTorrent Reads" || button.getText() == "Map Illumina Reads") && 
					(BBToolsLocation == null || PicardLocation == null)) {
				button.setDisable(true);
			}else if(button.getText() == "View in Tablet" && TabletLocation == null) {
				button.setDisable(true);
			}
		}
		
//		Text versionUpdate = new Text("**A new version of this software is available**");
//		versionUpdate.setStyle("-fx-fill: red; -fx-font-weight: bold; -fx-font-size: 16px;");
//		HBox updateBox = new HBox(10);
//		updateBox.setAlignment(Pos.CENTER);
//		updateBox.getChildren().add(versionUpdate);
//		if(!AccessoryMethods.checkVersion()) {
//			welcomePane.add(updateBox, 1, 19, 18, 1);
//		}
		
		Scene scene = new Scene(welcomePane, 800, 500);
//		scene.getStylesheets().add(WelcomePage.class.getResource("/resources/WelcomePage.css").toString());
		scene.getStylesheets().add("src/resources/WelcomePage.css");
		primaryStage.setScene(scene);
		primaryStage.setTitle("Amplicon Mapper");
		primaryStage.show();
	}
	
	private abstract class ButtonHandle {
		
		private String name;
		
		public ButtonHandle(String name) {
			this.name = name;
		}
		
		public String getName() {
			return this.name;
		}
		
		public abstract void run();
	}
}
