import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.comparator.LastModifiedFileComparator;
import org.jfree.chart.ChartPanel;

import bean.ObservationTO;
import bean.TBSourceTO;
import exceptions.CoordinateOverrideException;
import exceptions.EmptyCoordinatesException;
import exceptions.EphemException;
import exceptions.InvalidFanBeamNumberException;
import exceptions.InvalidFileException;
import javafx.application.Application;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.embed.swing.SwingNode;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.effect.BlendMode;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Pair;
import manager.DBManager;
import manager.ObservationManager;
import service.DBService;
import standalones.Point;
import standalones.SMIRF_GetUniqStitches;
import standalones.Traversal;
import util.Util;

public class Showroom  extends Application{

	List<File> pointingDirs = null;

	BorderPane root = new BorderPane();

	Integer count = new Integer(0);

	File rootDir = null; 
	
	String pulsarCandidatesFile = "pulsar.candidates";
	String newCandidatesFile = "new.candidates";
	String rfiCandidatesFile = "rfi.candidates";  
	 
	String rfiColor = "#ff0000";
	String newColor = "#00ff00";
	String pulsarColor = "#0000ff";

	final Map<String, Set<String>> knownPulsarCandidates = new HashMap<>();
	
	final Map<String, Set<String>> newCandidates = new HashMap<>();
	
	final Map<String, Set<String>> rfiCandidates = new HashMap<>();
	
	final Label message = new Label("All ok.");

	final HBox thisImageViewHBox = new HBox(10);

	public Showroom(){}

	@Override
	public void start(Stage primaryStage) throws Exception {
		
		root.setOnMouseClicked(f -> {
			root.requestFocus();
		});


		String rootDirName = "/smirf/results/";
		rootDir = new File(rootDirName);
		pointingDirs = Arrays.asList( rootDir.listFiles( Util.directoryFileFilter)).stream().sorted(Comparator.comparing(f -> ((File)f).getName())).collect(Collectors.toList());;	
		
	

		final List<File> imageFiles = new ArrayList<>();
		
		final List<PDMP> pdmpList = new ArrayList<>();

		final List<Image> images = new ArrayList<>();
		
		final List<Candidate> candidates = new ArrayList<>();
		
		final List<Point> points = new ArrayList<>();

		final ImageView thisImageView = new ImageView();
		
		thisImageViewHBox.getChildren().add(thisImageView);
		HBox.setHgrow(thisImageView, Priority.ALWAYS);
		thisImageViewHBox.setStyle("-fx-border-color: #f0f0f0; -fx-border-width: 5 ; ");

		
		final Label counterLabel = new Label();

		final VBox pdmpBox = new VBox(10);

		final Button previous = new Button("Previous");

		final Button next = new Button("Next");

		final Button makeDriven = new Button("Mark observation as processed.");

		final ComboBox<File> pointingBox = new ComboBox<>(FXCollections.observableArrayList(pointingDirs));
		
		final TextField pointingName = new TextField("");

		final ComboBox<File> utcBox = new ComboBox<>();

		final Button refresh = new Button("refresh");

		
		final Label observationInformation = new Label("Init");
		
		final List<Point> pulsarsInBeam = new ArrayList<>();
		
		final TabPane pulsarPane = new TabPane();
		
		final LabelWithTextAndButton pdmpCommand = 
				new LabelWithTextAndButton("PDMP command:", "");
		
		final Button rfi = new Button("RFI (X)");
		
		final Button pulsar = new Button("Pulsar (S)");

		final Button newCandiate = new Button("New Candidate (C)");
		
		final Button resetCandidateCategory = new Button("Reset (R)");
		
		final LabelWithTextAndButton gotoCandidate = new LabelWithTextAndButton("Go to candidate", "","Go"); 
		

		
        final SwingNode pointsChart = new SwingNode();
        pointsChart.setVisible(true);

		message.setTextFill(Paint.valueOf("red"));

		message.setText(pointingBox.getItems().size() + "pointings found");
		
		final TextArea pointTA = new TextArea();
		pointTA.setWrapText(true);
		pointTA.setVisible(false); 
		 
		final TextArea pulsarGuesserTA = new TextArea(); 
		pulsarGuesserTA.setWrapText(true);
		pulsarGuesserTA.setVisible(false); 
		
		final TextArea birdiesTA = new TextArea(); 
		birdiesTA.setWrapText(true);
		birdiesTA.setVisible(false); 
		
		
 


		pointingBox.setCellFactory(Util.fileNameViewFactory); 
		pointingBox.setButtonCell((ListCell<File>) Util.fileNameViewFactory.call(null)); 
		pointingBox.setUserData(Util.directoryFileFilter);
 
		utcBox.setCellFactory(Util.fileNameViewFactory);
		utcBox.setButtonCell((ListCell<File>) Util.fileNameViewFactory.call(null)); 
		utcBox.setUserData(Util.directoryFileFilter);


		LabelWithTextAndButton rootDirLWT = new LabelWithTextAndButton("Results directory", rootDirName,"Get pointings");	

		final ToggleGroup pointingShortlistGroup = new ToggleGroup();

		RadioButton all = new RadioButton("All");
		all.setToggleGroup(pointingShortlistGroup);
		all.setUserData("All");
		all.setSelected(true);

		RadioButton unprocessed = new RadioButton("Unprocessed");
		unprocessed.setUserData("Undriven");
		unprocessed.setToggleGroup(pointingShortlistGroup);

		RadioButton processed = new RadioButton("Processed");
		processed.setUserData("Driven");
		processed.setToggleGroup(pointingShortlistGroup);


		rootDirLWT.getButton().setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				System.err.println("Loading new root directory");
				rootDir = new File(rootDirLWT.getTextField().getText());

				FileFilter thisFilter  = null;
				FileFilter utcFilter  = null;
				switch (pointingShortlistGroup.getSelectedToggle().getUserData().toString()) {
				case "All":

					thisFilter  = Util.directoryFileFilter;
					utcFilter 	= Util.directoryFileFilter;
					break;

				case "Undriven":

					thisFilter  = Util.unprocessedPointingFilter;
					utcFilter 	= Util.unProcessedUTCFilter;
					break;

				case "Driven":

					thisFilter = Util.processedPointingFilter;
					utcFilter 	= Util.processedUTCFilter;
					break;

				}

				File[] files = rootDir.listFiles( thisFilter);
				Arrays.sort(files, LastModifiedFileComparator.LASTMODIFIED_COMPARATOR);
				pointingDirs = Arrays.asList( files).stream().sorted(Comparator.comparing(f -> ((File)f).getName())).collect(Collectors.toList());	
				
				pointingBox.getItems().clear();
				pointingBox.setItems(FXCollections.observableArrayList(pointingDirs));
				pointingBox.setUserData(utcFilter);
				
				thisImageView.setVisible(false);
				pdmpCommand.setVisible(false);
				pointTA.setVisible(false);
				pointingBox.setValue(null);
				utcBox.setValue(null);
				pointingBox.fireEvent(new ActionEvent());
				message.setText(files.length + " pointings found");

			}
		});


		pointingBox.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				
				File pointingDir = pointingBox.getValue();
			
				if(pointingDir ==null ) return;
				
				pointingName.setText(pointingDir.getName());


				FileFilter filter = (FileFilter) pointingBox.getUserData();
				utcBox.getItems().clear();

				File[] files = pointingDir.listFiles(filter);
				Arrays.sort(files, LastModifiedFileComparator.LASTMODIFIED_COMPARATOR);

				List<File> utcDirs = Arrays.asList(files);

				utcBox.setItems(FXCollections.observableArrayList(utcDirs));
				thisImageView.setVisible(false);
				pdmpCommand.setVisible(false);
				pointTA.setVisible(false);
				pdmpBox.setVisible(false);
				pulsarGuesserTA.setVisible(false);
				birdiesTA.setVisible(false);
				if(utcDirs.size() == 1){
					
					File utc = utcDirs.get(0);
					utcBox.setValue(utc);
					
				}

				message.setText(files.length+ " Observations found.");
				
				

			}
		});

		utcBox.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {

				message.setText("");

				File utcDir = utcBox.getValue();
				if(utcDir == null) return;
				
				File carsDir = new File(utcDir.getAbsolutePath()+ Util.pathSeparator + "cars");
				
				List<File> pngList = Arrays.asList(carsDir.listFiles(Util.pngFileFilter));
				
				List<String> birdieList = new ArrayList<>();
				List<String> birdiePeriods = new ArrayList<>();
				
				birdieList = Arrays.asList(utcDir.listFiles(Util.birdieFileFilter))
												.stream()
												.map( f -> {
													try {
														//System.err.println("File: " + f.getAbsolutePath());
														return Files.readAllLines(f.toPath());
													} catch (IOException e2) {
														e2.printStackTrace();
														return null;
													}
												} )
												.filter( f -> f != null)
												.flatMap(List::stream)
												.collect(Collectors.toList());
				
				
				
				//System.err.println("birdies list:" + birdieList);
				
				Candidate.loadMap(utcDir);
				
				File pdmpPosn = new File(carsDir,"pdmp.posn");
				
				if(pdmpPosn.exists()) {
					
					Stream<String> pdmpPosnStream = null;
					try {
						pdmpPosnStream = Files.lines(Paths.get(pdmpPosn.getAbsolutePath()));
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
						return;
					}
					
					pdmpList.addAll(pdmpPosnStream.map(f -> {
						try {
							return new PDMP(f);
						} catch (InvalidFileException e) {
							e.printStackTrace();
							return null;
						}
					}).sorted(Comparator.comparing(PDMP::getBestSNR).reversed())
					.filter(f -> f!=null ).collect(Collectors.toList()));
					
					pdmpPosnStream.close();
					
					pngList = pngList
								.stream()
								.filter(f -> pdmpList.contains(PDMP.dummy(f.getName().replaceAll(".png", ".car"))))
								.sorted(Comparator.comparing(
									f -> {
										File file = ((File)f);
										String name = file.getName().replaceAll(".png", ".car");
										return pdmpList.get(pdmpList.indexOf(PDMP.dummy(name))).getBestSNR();
									}).reversed())
								.collect(Collectors.toList());
				}
					
				


				images.clear();
				images.addAll(pngList.stream().map( f -> {
					try {
						return new Image(f.toURI().toURL().toExternalForm());
					} catch (MalformedURLException e) {
						message.setText(e.getMessage());
						e.printStackTrace();
						return null;
					}
				} ).filter(f-> f!=null).collect(Collectors.toList()));
				

				imageFiles.clear();
				imageFiles.addAll(pngList);
				
				
				candidates.clear();
				candidates.addAll(
						pngList.stream().map(f -> {
							String name = f.getName().replaceAll(".png", ".car");
							PDMP pdmp = pdmpList.get(pdmpList.indexOf(PDMP.dummy(name)));
							return new Candidate(f.getName(), pdmp);
							}).collect(Collectors.toList()));
				
				points.clear();
				points.addAll(candidates.stream().map(c -> c.getPoint()).collect(Collectors.toList()));
				
				thisImageViewHBox.setStyle("-fx-border-color: " + "#f0f0f0" + "; -fx-border-width: 5 ;");
				
				
				loadCandidateFiles(utcDir);
				
				ObservationTO observationTO = DBManager.getObservationByUTC(utcDir.getName());
				
				
				try {
					
					
					observationTO.getTiedBeamSources().addAll( new ObservationManager()
							.getTBSourcesForObservation(observationTO, null)
							.stream()
							.filter( t -> !observationTO.getTiedBeamSources().contains(t))
							.collect(Collectors.toList()));
					
					observationInformation.setText(observationTO.toString());
					
					pulsarsInBeam.clear(); 
					pulsarsInBeam.addAll( new SMIRF_GetUniqStitches().getPointForPulsars(observationTO) ) ;
					
					pulsarPane.getTabs().clear();
					populateTabs(pulsarPane, observationTO);
					
					
		
				} catch (EmptyCoordinatesException | CoordinateOverrideException | InvalidFanBeamNumberException | EphemException e) {
					e.printStackTrace(); 
					message.setText(e.getMessage());
				} 
				
				
				pointsChart.setContent(new ChartPanel(new PointTracer(points,pulsarsInBeam).getChart()));
				pointsChart.getContent().repaint();

				count = 0; 
				if(images.size() > 0){ 
					
					thisImageView.setImage(images.get(count));
					thisImageView.setUserData(imageFiles.get(count).getName());
					
					pdmpCommand.setValue("pdmp -mc 40 " + imageFiles.get(count)
					.getAbsolutePath().replaceAll(".png", ".car"));
					
					pointTA.setText(candidates.get(count).getPoint().getFBPercents());
					//pointTA.setText(candidates.get(count).getCandidateLine());
					pulsarGuesserTA.setText( PulsarGuesser.guessPulsar(candidates.get(count).getPmdp()));
					birdiesTA.setText(String.join("\n", birdieList));
					birdiesTA.setEditable(false);
					PointTracer.addSeries(((ChartPanel)pointsChart.getContent()).getChart(),points.get(count),pulsarsInBeam.size() + points.size());
					pointsChart.getContent().repaint();
	
					counterLabel.setText( (count+1) +"/"+images.size()); 
					thisImageView.setVisible(true); 
					pdmpCommand.setVisible(true);
					pdmpBox.setVisible(true);
					pointTA.setVisible(true);
					pulsarGuesserTA.setVisible(true);
					birdiesTA.setVisible(true);

					makeDriven.setVisible(!new File(utcDir,Util.carsDotDriven).exists());
					
				}
				else{

					message.setText("utc=" + utcDir.getName() + "had no candidate PNGs.");

				}
				

			}
		});
		
		
		rfi.setOnAction( new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				
				Set<String> files = rfiCandidates.getOrDefault(utcBox.getValue().getName(), new HashSet<>());
				files.add(thisImageView.getUserData().toString());
				
				knownPulsarCandidates.getOrDefault(utcBox.getValue().getName(), new HashSet<>()).remove(new File(thisImageView.getUserData().toString()).getName());
				newCandidates.getOrDefault(utcBox.getValue().getName(), new HashSet<>()).remove(new File(thisImageView.getUserData().toString()).getName());
				
				rfiCandidates.put(utcBox.getValue().getName(), files);
			
				message.setText("Marked candidate as RFI.");
				
				thisImageViewHBox.setStyle("-fx-border-color: " + rfiColor + "; -fx-border-width: 5 ;");
				
//				System.err.println("# RFI candidate.. known -- RFI -- NEW");
//				System.err.println(knownPulsarCandidates);
//				System.err.println(rfiCandidates);
//				System.err.println(newCandidates);
				
			}
		});
		
		
		pulsar.setOnAction( new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				
				Set<String> files = knownPulsarCandidates.getOrDefault(utcBox.getValue().getName(), new HashSet<>());
				files.add(thisImageView.getUserData().toString());
				

				rfiCandidates.getOrDefault(utcBox.getValue().getName(), new HashSet<>()).remove(new File(thisImageView.getUserData().toString()).getName());
				newCandidates.getOrDefault(utcBox.getValue().getName(), new HashSet<>()).remove(new File(thisImageView.getUserData().toString()).getName());
				
				knownPulsarCandidates.put(utcBox.getValue().getName(), files);
				
				message.setText("Marked candidate as known pulsar.");
				
				thisImageViewHBox.setStyle("-fx-border-color: " + pulsarColor + "; -fx-border-width: 5 ;");
				
//				System.err.println("# known candidate..  known -- RFI -- NEW");
//				System.err.println(knownPulsarCandidates);
//				System.err.println(rfiCandidates);
//				System.err.println(newCandidates);
			
			}
		});
		
		newCandiate.setOnAction( new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				
				Set<String> files = newCandidates.getOrDefault(utcBox.getValue().getName(), new HashSet<>());
				files.add(thisImageView.getUserData().toString());
				

				knownPulsarCandidates.getOrDefault(utcBox.getValue().getName(), new HashSet<>()).remove(new File(thisImageView.getUserData().toString()).getName());
				rfiCandidates.getOrDefault(utcBox.getValue().getName(), new HashSet<>()).remove(new File(thisImageView.getUserData().toString()).getName());
				
				newCandidates.put(utcBox.getValue().getName(), files);
				
				message.setText("Marked candidate as new candidate.");

				thisImageViewHBox.setStyle("-fx-border-color: " + newColor + "; -fx-border-width: 5 ;");
				
//				System.err.println("# New candidate.. known -- RFI -- NEW");
//				System.err.println(knownPulsarCandidates);
//				System.err.println(rfiCandidates);
//				System.err.println(newCandidates);

			
			}
		});
		
		resetCandidateCategory.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				message.setText(" Reset candidate category");

				thisImageViewHBox.setStyle("-fx-border-color: #f0f0f0; -fx-border-width: 5 ;");
				
				knownPulsarCandidates.getOrDefault(utcBox.getValue().getName(), new HashSet<>()).remove(new File(thisImageView.getUserData().toString()).getName());
				rfiCandidates.getOrDefault(utcBox.getValue().getName(), new HashSet<>()).remove(new File(thisImageView.getUserData().toString()).getName());
				newCandidates.getOrDefault(utcBox.getValue().getName(), new HashSet<>()).remove(new File(thisImageView.getUserData().toString()).getName());


//				System.err.println("# Reset candidate.. known -- RFI -- NEW");
//				System.err.println(knownPulsarCandidates);
//				System.err.println(rfiCandidates);
//				System.err.println(newCandidates);
			}
		});
		

		previous.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				if( count - 1 >= 0 ){
					thisImageView.setImage(images.get(--count));
					thisImageView.setUserData(imageFiles.get(count).getName());
					
					pdmpCommand.setValue("pdmp -mc 40 " + imageFiles.get(count)
					.getAbsolutePath().replaceAll(".png", ".car"));
					
					setImageBorder( utcBox.getValue(), imageFiles.get(count));

					
					counterLabel.setText( (count+1) +"/"+images.size());
				
					pointTA.setText(candidates.get(count).getPoint().getFBPercents());
					pulsarGuesserTA.setText( PulsarGuesser.guessPulsar(candidates.get(count).getPmdp()));
					//pointTA.setText(points.get(count).toString());
					PointTracer.addSeries(((ChartPanel)pointsChart.getContent()).getChart(),points.get(count),pulsarsInBeam.size() +  points.size());
					pointsChart.getContent().repaint();
					
					message.setText("");



				}
			}
		});
		
		
		gotoCandidate.getButton().setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				
				int c = Integer.parseInt(gotoCandidate.getTextField().getText());
				
				if( c < 1 || c > images.size()) {
					message.setText("Invalid candidate number to go to.");
					return;
				}
				
				
				count = c-1;
				
				thisImageView.setImage(images.get(count));
				thisImageView.setUserData(imageFiles.get(count).getName());

				pdmpCommand.setValue("pdmp -mc 40 " + imageFiles.get(count)
				.getAbsolutePath().replaceAll(".png", ".car"));

				setImageBorder( utcBox.getValue(), imageFiles.get(count));


				counterLabel.setText( (count+1) +"/"+images.size());
				
				pointTA.setText(candidates.get(count).getPoint().getFBPercents());
				pulsarGuesserTA.setText( PulsarGuesser.guessPulsar(candidates.get(count).getPmdp()));

				//pointTA.setText(points.get(count).toString());
				PointTracer.addSeries(((ChartPanel)pointsChart.getContent()).getChart(),points.get(count),pulsarsInBeam.size() +  points.size());
				pointsChart.getContent().repaint();

				message.setText("");




			}
		});


		next.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				if( count + 1 < images.size() ){ 
					thisImageView.setImage(images.get(++count));
					thisImageView.setUserData(imageFiles.get(count).getName());
					
					pdmpCommand.setValue("pdmp -mc 40 " + imageFiles.get(count)
					.getAbsolutePath().replaceAll(".png", ".car"));
					
					setImageBorder( utcBox.getValue(), imageFiles.get(count));
					
					counterLabel.setText( (count+1) +"/"+images.size());
					
					pointTA.setText(candidates.get(count).getPoint().getFBPercents());
					pulsarGuesserTA.setText( PulsarGuesser.guessPulsar(candidates.get(count).getPmdp()));

					//pointTA.setText(points.get(count).toString());
					PointTracer.addSeries(((ChartPanel)pointsChart.getContent()).getChart(),points.get(count),pulsarsInBeam.size() +  points.size());
					pointsChart.getContent().repaint();
					
					message.setText("");

				}

			}
		});


		makeDriven.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				File utc = utcBox.getValue();
				File carsDotDriven = new File(utc,Util.carsDotDriven);
				
				int categorized = knownPulsarCandidates.getOrDefault(utc.getName(), new HashSet<>()).size() + 
						 rfiCandidates.getOrDefault(utc.getName(), new HashSet<>()).size() + 
						 newCandidates.getOrDefault(utc.getName(), new HashSet<>()).size();
				
				if(categorized < imageFiles.size()){
					
					message.setText("Error: Not fully categorized. Categorized only" + categorized + " of " + imageFiles.size() );
					return;
				}
				
				else if(categorized > imageFiles.size()){
					message.setText("Problem with code. Ask Vivek. " + categorized + " " + imageFiles.size());
					return;
				}
				
				try {
					
					saveCandidateFiles(utcBox.getValue());
					FileUtils.touch(carsDotDriven);
					
					System.err.println("Successfully touched " + carsDotDriven.getAbsolutePath() );
					message.setText("Successfully makeed as processed. " );

				} catch (IOException e) {
					message.setText(e.getMessage());
					e.printStackTrace();
				}

			}
		});

		refresh.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {

				rootDirLWT.getButton().fire();

			}
		});
		

		HBox.setHgrow(pdmpCommand.getTextField(), Priority.ALWAYS);
		pdmpBox.getChildren().addAll(thisImageViewHBox,
				new HBox(10,previous,counterLabel,next, gotoCandidate.gethBox()), 
				new HBox(10,new Label("categorize:"), rfi, pulsar, newCandiate, resetCandidateCategory,makeDriven)  ,
				pdmpCommand.gethBox(),
				new HBox(10, new Label("Points string"), pointTA));
		pdmpBox.setVisible(false);

		VBox controlBox = new VBox(10,new Label(),
									  new HBox(10, new Label("Type:"), all, unprocessed, processed, rootDirLWT.gethBox()), 
									  new HBox(10,new Label("select pointing: "),pointingBox, new Label("Select UTC:"), utcBox,refresh,new HBox(10,pointingName))
									);
		
		Label title = new Label("SHOWROOM");
		Label subTitle =  new Label("THE SMIRF CANDIDATE VIEWER");
		title.setFont(Font.font("monaco",FontWeight.EXTRA_BOLD, 24));
		subTitle.setFont(Font.font("monaco",FontWeight.EXTRA_BOLD, 16));

		title.setAlignment(Pos.CENTER);
		subTitle.setAlignment(Pos.CENTER);

		VBox top = new VBox(10,title,subTitle,controlBox);
		top.setAlignment(Pos.CENTER);
		
		VBox right = new VBox(10, pulsarGuesserTA,birdiesTA, pointsChart, pulsarPane);
		right.setAlignment(Pos.CENTER);

		root.setTop(top);
		root.setCenter(pdmpBox);
		root.setBottom(message);
		root.setRight(right);
		root.setManaged(false);
		// top right bottom left
		Insets insets = new Insets(20, 20, 0, 25);
		BorderPane.setMargin(top, insets );
		BorderPane.setMargin(controlBox, insets );
		BorderPane.setMargin(pdmpBox, insets );
		BorderPane.setMargin(pointTA, insets ); 
		BorderPane.setMargin(pointsChart, new Insets(20, 25, 0, 0) ); 
		BorderPane.setMargin(pulsarPane, new Insets(20, 25, 0, 0) ); 
		BorderPane.setMargin(right, new Insets(20, 25, 0, 0) );  
		

		Scene scene = new Scene(root,1600,1200);
		
		scene.setOnKeyPressed(new EventHandler<KeyEvent>() {

			@Override
			public void handle(KeyEvent event) {
				
				switch (event.getCode()) {
				
				case A:
					previous.fire();
					break;
					
				case D:
					next.fire();
					break;	
				
				case S:
					pulsar.fire();
					break;
				
				case X:
					rfi.fire();
					break;
				
				case C:
					newCandiate.fire();
					break;
				
				case R:
					resetCandidateCategory.fire();
					break;
									
				default:
					break;
				}
							
			}
		});
		primaryStage.setScene(scene);
		
		//primaryStage.setResizable(false);
		primaryStage.setTitle("SMIRF CAR SHOWROOM");
		primaryStage.show();
	}
	
	public void setImageBorder( File utcDir,  File imageFile){
		
//		System.err.println("# known -- RFI -- NEW");
//		System.err.println(knownPulsarCandidates);
//		System.err.println(rfiCandidates);
//		System.err.println(newCandidates);
				

		 
		if(knownPulsarCandidates.getOrDefault(utcDir.getName(), new HashSet<>()).contains(imageFile.getName())){
			thisImageViewHBox.setStyle("-fx-border-color: " + pulsarColor + "; -fx-border-width: 5 ;");
		}
		
		
		else if(rfiCandidates.getOrDefault(utcDir.getName(), new HashSet<>()).contains(imageFile.getName())){
			thisImageViewHBox.setStyle("-fx-border-color: " + rfiColor + "; -fx-border-width: 5 ;");

		}
		
		else if(newCandidates.getOrDefault(utcDir.getName(), new HashSet<>()).contains(imageFile.getName())){
			thisImageViewHBox.setStyle("-fx-border-color: " + newColor + "; -fx-border-width: 5 ;");

		}
		else{
			thisImageViewHBox.setStyle("-fx-border-color: " + "#f0f0f0" + "; -fx-border-width: 5 ;");

		}
		
		

	}
	
	public void populateTabs(TabPane tabPane, ObservationTO observationTO){
		
		List<TBSourceTO> tbSourceTOs = observationTO.getTiedBeamSources();
		if(tbSourceTOs == null) return;
		int i=0;
		for(TBSourceTO tbSourceTO: tbSourceTOs){
			
			Color c = i < 4 ? PointTracer.tbColors[i] :PointTracer.tbColors[4];
			
			Tab tab = new Tab();
			tab.setText(tbSourceTO.getPsrName());
			tab.setStyle("-fx-background-color: "
					+  String.format("rgba(%d,%d,%d,0.2)", c.getRed(), c.getGreen(), c.getBlue())   +";");
			
		
			
		    final TableView<Pair<String, Object>> table = new TableView<>();
		    

			table.getItems().add( new Pair<String, Object>("RA:", tbSourceTO.getAngleRA().toHHMMSS()));
			table.getItems().add( new Pair<String, Object>("DEC:", tbSourceTO.getAngleDEC().toDDMMSS()));
			table.getItems().add( new Pair<String, Object>("Flux:", tbSourceTO.getFluxAt843MHz().toString()));
			table.getItems().add( new Pair<String, Object>("DM:", tbSourceTO.getDM().toString()));
			table.getItems().add( new Pair<String, Object>("P0:", tbSourceTO.getP0().toString()));
			table.getItems().add( new Pair<String, Object>("F0:", tbSourceTO.getF0().toString()));
			table.getItems().add( new Pair<String, Object>("TB SNR:",
					DBService.getPulsarSNRForObs(observationTO.getUtc(), tbSourceTO.getPsrName()).toString()));
			 
			String harmonics = "";
			
			for(int h = -8 ; h<= 8; h++ ){
				harmonics += String.format("%.6f \n", tbSourceTO.getP0()* Math.pow(2, h));
			}
			
			table.getItems().add( new Pair<String, Object>("Harmonic periods:", harmonics));

			
			table.getItems().add( new Pair<String, Object>("Eph:", tbSourceTO.getEphemerides()));
			
			
			TableColumn<Pair<String, Object>, String> nameColumn = new TableColumn<>("Name");
			TableColumn<Pair<String, Object>, Object> valueColumn = new TableColumn<>("Value");
			valueColumn.setSortable(false);

			nameColumn.setCellValueFactory(new PairKeyFactory());
			valueColumn.setCellValueFactory(new PairValueFactory());

			table.getColumns().setAll(nameColumn, valueColumn);
			
			valueColumn.setCellFactory(new Callback<TableColumn<Pair<String, Object>, Object>, TableCell<Pair<String, Object>, Object>>() {
				@Override
				public TableCell<Pair<String, Object>, Object> call(TableColumn<Pair<String, Object>, Object> column) {
					return new PairValueCell();
				}
			});
			
			tab.setContent(table);
			 
			tabPane.getTabs().add(tab);
			i++;
		}
		
		tabPane.setPrefWidth(200);
	}
	

	public static void main(String[] args) {
		args =new String("-r /smirf/results").split(" ");
		launch(args);
	}

	public void parseArgs(String[] args) {

		System.err.println(Arrays.asList(args));

		CommandLine commandLine;
		CommandLineParser parser = new DefaultParser();
		Options options = new Options();

		Option rootDirOption = new Option("r", "roor_dir", true, "Root directory of candidates");
		options.addOption(rootDirOption);

		try {

			commandLine = parser.parse(options, args);

			String root = "/smirf/results/";

			if(hasOption(commandLine, rootDirOption)) root = getValue(commandLine, rootDirOption);

			File rootDir = new File(root);
			pointingDirs = Arrays.asList( rootDir.listFiles( Util.directoryFileFilter));			

		} catch (Exception e) {
			e.printStackTrace();
		}


	}
	
	public void saveCandidateFiles(File utcDir){
		
	try {
		OpenOption[] options = new OpenOption[] { StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING };

		
		Files.write(new File(utcDir, pulsarCandidatesFile).toPath(),
				knownPulsarCandidates.getOrDefault(utcDir.getName(), new HashSet<>())
				.stream()
				.map(s -> new File(utcDir+"/cars/"+ s.replace(".png", ".car")).getAbsolutePath())
				.collect(Collectors.toList()),
				options);
		
		Files.write(new File(utcDir, newCandidatesFile).toPath(),
				newCandidates.getOrDefault(utcDir.getName(), new HashSet<>())
				.stream()
				.map(s -> new File(utcDir+"/cars/"+ s.replace(".png", ".car")).getAbsolutePath())
				.collect(Collectors.toList()),
				options);
		
		Files.write(new File(utcDir, rfiCandidatesFile).toPath(),
				rfiCandidates.getOrDefault(utcDir.getName(), new HashSet<>())
				.stream()
				.map(s -> new File(utcDir+"/cars/"+ s.replace(".png", ".car")).getAbsolutePath())
				.collect(Collectors.toList()),
				options);
		
		
	} catch (IOException e) {
		message.setText(e.getMessage());
		e.printStackTrace();
	}	
		
	}
	
	public void loadCandidateFiles(File utcDir){
		try {
			
			File file = null;
			
			knownPulsarCandidates.clear();
			newCandidates.clear();
			rfiCandidates.clear();
			
			if((file = new File(utcDir, pulsarCandidatesFile)).exists()){
				
				knownPulsarCandidates.put( utcDir.getName(),  Files.lines(file.toPath())
						.map(s -> s.replace(".car", ".png"))
						.collect(Collectors.toList())
						.stream()
						.filter(s -> new File(s).exists())
						.map(s -> new File(s).getName())
						.collect(Collectors.toSet()) );	
			}
			
			if((file = new File(utcDir,newCandidatesFile)).exists()){
				
				newCandidates.put( utcDir.getName(),  Files.lines(file.toPath())
						.map(s -> s.replace(".car", ".png"))
						.collect(Collectors.toList())
						.stream()
						.filter(s -> new File(s).exists())
						.map(s -> new File(s).getName())
						.collect(Collectors.toSet()) );
			}
			
			if((file = new File(utcDir,rfiCandidatesFile)).exists()){

				rfiCandidates.put( utcDir.getName(),  Files.lines(file.toPath())
						.map(s -> s.replace(".car", ".png"))
						.collect(Collectors.toList())
						.stream()
						.filter(s -> new File(s).exists())
						.map(s -> new File(s).getName())
						.collect(Collectors.toSet()) );
			}

			
		} catch (IOException e1) {
			message.setText(e1.getMessage());
			e1.printStackTrace();
		}

	}
	public static void help(Options options){
		HelpFormatter formater = new HelpFormatter();
		formater.printHelp("Main", options);
	}

	public static String getValue(CommandLine line, Option option){
		return line.getOptionValue(option.getOpt());
	}

	public static boolean hasOption(CommandLine line, Option option){
		return line.hasOption(option.getOpt());
	}


	
	class PairKeyFactory implements Callback<TableColumn.CellDataFeatures<Pair<String, Object>, String>, ObservableValue<String>> {
	    @Override
	    public ObservableValue<String> call(TableColumn.CellDataFeatures<Pair<String, Object>, String> data) {
	        return new ReadOnlyObjectWrapper<>(data.getValue().getKey());
	    }
	}

	class PairValueFactory implements Callback<TableColumn.CellDataFeatures<Pair<String, Object>, Object>, ObservableValue<Object>> {
	    @SuppressWarnings("unchecked")
	    @Override
	    public ObservableValue<Object> call(TableColumn.CellDataFeatures<Pair<String, Object>, Object> data) {
	        Object value = data.getValue().getValue();
	        return (value instanceof ObservableValue)
	                ? (ObservableValue) value
	                : new ReadOnlyObjectWrapper<>(value);
	    }
	}

	class PairValueCell extends TableCell<Pair<String, Object>, Object> {
	    @Override
	    protected void updateItem(Object item, boolean empty) {
	        super.updateItem(item, empty);

	        if (item != null) {
	            if (item instanceof String) {
	                setText((String) item);
	                setGraphic(null);
	            } else if (item instanceof Integer) {
	                setText(Integer.toString((Integer) item));
	                setGraphic(null);
	            } else if (item instanceof Boolean) {
	                CheckBox checkBox = new CheckBox();
	                checkBox.setSelected((boolean) item);
	                setGraphic(checkBox);
	            } else if (item instanceof Image) {
	                setText(null);
	                ImageView imageView = new ImageView((Image) item);
	                imageView.setFitWidth(100);
	                imageView.setPreserveRatio(true);
	                imageView.setSmooth(true);
	                setGraphic(imageView);
	            } else {
	                setText("N/A");
	                setGraphic(null);
	            }
	        } else {
	            setText(null);
	            setGraphic(null);
	        }
	    }
	}

	
	
}
