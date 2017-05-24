import java.awt.Color;
import java.awt.TextField;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
import exceptions.InvalidFanBeamNumberException;
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
import javafx.scene.control.ToggleGroup;
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
import standalones.Point;
import standalones.SMIRF_GetUniqStitches;
import util.Util;

public class Showroom  extends Application{

	List<File> pointingDirs = null;

	BorderPane root = new BorderPane();

	Integer count = new Integer(0);

	File rootDir = null;


	public Showroom(){}

	@Override
	public void start(Stage primaryStage) throws Exception {


		String rootDirName = "/smirf/results/";
		rootDir = new File(rootDirName);
		pointingDirs = Arrays.asList( rootDir.listFiles( Util.directoryFileFilter));	

		final List<File> imageFiles = new ArrayList<>();

		final List<Image> images = new ArrayList<>();
		
		final List<Candidate> candidates = new ArrayList<>();
		
		final List<Point> points = new ArrayList<>();

		final ImageView thisImageView = new ImageView();
		
		final Label counterLabel = new Label();

		final VBox pdmpBox = new VBox(10);

		final Button previous = new Button("Previous");

		final Button next = new Button("Next");

		final Button makeDriven = new Button("Mark observation as processed.");

		final ComboBox<File> pointingBox = new ComboBox<>(FXCollections.observableArrayList(pointingDirs));

		final ComboBox<File> utcBox = new ComboBox<>();

		final Button refresh = new Button("refresh");

		final Label message = new Label("All ok.");
		
		final Label observationInformation = new Label("Init");
		
		final List<Point> pulsarsInBeam = new ArrayList<>();
		
		final TabPane pulsarPane = new TabPane();
		
		final LabelWithTextAndButton pdmpCommand = 
				new LabelWithTextAndButton("PDMP command:", "");
		
		

		
        final SwingNode pointsChart = new SwingNode();
        pointsChart.setVisible(true);

		message.setTextFill(Paint.valueOf("red"));

		message.setText(pointingBox.getItems().size() + "pointings found");
		
		final TextArea pointTA = new TextArea();
		pointTA.setWrapText(true);
		pointTA.setVisible(false); 


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
				pointingDirs = Arrays.asList( files);	
				
				pointingBox.getItems().clear();
				pointingBox.setItems(FXCollections.observableArrayList(pointingDirs));
				pointingBox.setUserData(utcFilter);
				
				thisImageView.setVisible(false);
				pdmpCommand.setVisible(false);
				pointTA.setVisible(false);
				pointingBox.setValue(null);
				utcBox.setValue(null);
				pointingBox.fireEvent(new ActionEvent());
				message.setText(files.length + "pointings found");

			}
		});


		thisImageView.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
			
			String name = thisImageView.getUserData().toString().split("\\.")[0];
			
			
			if(e.isPrimaryButtonDown()){
				
				
			}
			else if(e.isSecondaryButtonDown()){
				
			}
			
		});

		




		pointingBox.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				File pointingDir = pointingBox.getValue();

				if(pointingDir ==null ) return;

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

				message.setText(files.length+ " Observations found.");

			}
		});

		utcBox.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {

				File utcDir = utcBox.getValue();
				if(utcDir == null) return;
				File carsDir = new File(utcDir.getAbsolutePath()+ Util.pathSeparator + "cars");


				
				List<File> pngList = Arrays.asList(carsDir.listFiles(Util.pngFileFilter));
				
				Candidate.loadMap(utcDir);

				
				File pdmpPer = new File(carsDir,"pdmp.per");
				
				if(pdmpPer.exists()){

					Map<String, Double> pdmpSNRs = new HashMap<>();
					
					try {
						BufferedReader br = new BufferedReader( new FileReader( pdmpPer));
						String line="";
						
						while((line= br.readLine()) !=null ) {
							
							String[] chunks = line.trim().split("\\s+");
							if(chunks.length != 12) {
								System.err.println("length of line: '" + line +"' was not 12 but was" + chunks.length);
								continue;
							}
							pdmpSNRs.put(chunks[7].trim(), Double.parseDouble(chunks[6]));
							
						}
						
						br.close();
						
					} catch (FileNotFoundException e1) {
						e1.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
					
					
					
					
					pngList = pngList
							.stream()
							.filter(f -> pdmpSNRs.get(f.getName().replaceAll(".png", ".car"))!=null)
							.sorted(Comparator.comparing(f -> pdmpSNRs.get(((File)f).getName().replaceAll(".png", ".car"))).reversed())
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
				} ).collect(Collectors.toList()));
				

				imageFiles.clear();
				imageFiles.addAll(pngList);
				
				
				candidates.clear();
				candidates.addAll(pngList.stream().map(f -> new Candidate(f.getName())).collect(Collectors.toList()));
				
				points.clear();
				points.addAll(candidates.stream().map(c -> c.getPoint()).collect(Collectors.toList()));
				
				
				
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
					populateTabs(pulsarPane, observationTO.getTiedBeamSources());
					
					
		
				} catch (EmptyCoordinatesException | CoordinateOverrideException | InvalidFanBeamNumberException e) {
					e.printStackTrace(); 
					message.setText(e.getMessage());
				} 
				
				
				pointsChart.setContent(new ChartPanel(new PointTracer(points,pulsarsInBeam).getChart()));
				pointsChart.getContent().repaint();

				count = 0; 
				if(images.size() > 0){ 
					
					thisImageView.setImage(images.get(count));
					thisImageView.setUserData(imageFiles.get(count).getName());
					
					pdmpCommand.setValue("pdmp " + imageFiles.get(count)
					.getAbsolutePath().replaceAll(".png", ".car"));
					
					pointTA.setText(points.get(count).toString());
					
					PointTracer.addSeries(((ChartPanel)pointsChart.getContent()).getChart(),points.get(count),pulsarsInBeam.size() + points.size());
					pointsChart.getContent().repaint();
	
					counterLabel.setText( (count+1) +"/"+images.size());
					thisImageView.setVisible(true);
					pdmpCommand.setVisible(true);
					pdmpBox.setVisible(true);
					pointTA.setVisible(true);
	
					makeDriven.setVisible(!new File(utcDir,Util.carsDotDriven).exists());
					
					
									
					}
					else{
						message.setText("utc=" + utcDir.getName() + "had no candidate PNGs.");
					}
	
					



			}
		});



		previous.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				if( count - 1 >= 0 ){
					thisImageView.setImage(images.get(--count));
					thisImageView.setUserData(imageFiles.get(count).getName());
					
					pdmpCommand.setValue("pdmp " + imageFiles.get(count)
					.getAbsolutePath().replaceAll(".png", ".car"));
					
					counterLabel.setText( (count+1) +"/"+images.size());
				
					pointTA.setText(points.get(count).toString());
					PointTracer.addSeries(((ChartPanel)pointsChart.getContent()).getChart(),points.get(count),pulsarsInBeam.size() +  points.size());
					pointsChart.getContent().repaint();


				}
			}
		});


		next.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				if( count + 1 < images.size() ){
					thisImageView.setImage(images.get(++count));
					thisImageView.setUserData(imageFiles.get(count).getName());
					
					pdmpCommand.setValue("pdmp " + imageFiles.get(count)
					.getAbsolutePath().replaceAll(".png", ".car"));
					
					counterLabel.setText( (count+1) +"/"+images.size());
					pointTA.setText(points.get(count).toString());
					PointTracer.addSeries(((ChartPanel)pointsChart.getContent()).getChart(),points.get(count),pulsarsInBeam.size() +  points.size());
					pointsChart.getContent().repaint();

				}

			}
		});


		makeDriven.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				File utc = utcBox.getValue();
				File carsDotDriven = new File(utc,Util.carsDotDriven);
				try {
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

		pdmpCommand.gethBox().setHgrow(pdmpCommand.getTextField(), Priority.ALWAYS);
		pdmpBox.getChildren().addAll(thisImageView,new HBox(10,previous,counterLabel,next, makeDriven),pdmpCommand.gethBox());
		pdmpBox.setVisible(false);

		VBox controlBox = new VBox(10,new Label(),new HBox(10, new Label("Type:"), all, unprocessed, processed),rootDirLWT.gethBox(), 
				new HBox(10,new Label("select pointing: "),pointingBox, new Label("Select UTC:"), utcBox,refresh));

		Label title = new Label("SHOWROOM");
		Label subTitle =  new Label("THE SMIRF CANDIDATE VIEWER");
		title.setFont(Font.font("monaco",FontWeight.EXTRA_BOLD, 24));
		subTitle.setFont(Font.font("monaco",FontWeight.EXTRA_BOLD, 16));

		title.setAlignment(Pos.CENTER);
		subTitle.setAlignment(Pos.CENTER);

		VBox top = new VBox(10,title,subTitle,controlBox);
		top.setAlignment(Pos.CENTER);
		
		VBox right = new VBox(10, pointsChart, pulsarPane);
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
	
	
	
	public void populateTabs(TabPane tabPane, List<TBSourceTO> tbSourceTOs){
		int i=0;
		for(TBSourceTO tbSourceTO: tbSourceTOs){
			
			Color c = i < 4 ? PointTracer.tbColors[i] :PointTracer.tbColors[4];
			
			Tab tab = new Tab();
			tab.setText(tbSourceTO.getPsrName());
			tab.setStyle("-fx-background-color: "
					+  String.format("rgba(%d,%d,%d,0.2)", c.getRed(), c.getGreen(), c.getBlue())   +";");
			
			System.err.println(tab.getStyle());
		
			
		    final TableView<Pair<String, Object>> table = new TableView<>();
		    
		    System.err.println(tbSourceTO.getAngleRA() + " " + tbSourceTO.getAngleDEC() + " " + tbSourceTO.getFluxAt843MHz());

			table.getItems().add( new Pair<String, Object>("RA:", tbSourceTO.getAngleRA().toHHMMSS()));
			table.getItems().add( new Pair<String, Object>("DEC:", tbSourceTO.getAngleDEC().toDDMMSS()));
			table.getItems().add( new Pair<String, Object>("Flux:", tbSourceTO.getFluxAt843MHz().toString()));
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
