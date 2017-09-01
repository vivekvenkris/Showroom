import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.util.List;
import java.util.stream.Collectors;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import standalones.Point;
import standalones.Traversal;
import util.Constants;

public class PointTracer {



	private JFreeChart chart ;

	static Color[] tbColors 
	= new Color[]{	new Color(  Color.BLUE.getRed()/255.0f, Color.BLUE.getGreen()/255.0f, Color.BLUE.getBlue()/255.0f, 1f),
					new Color(	Color.CYAN.getRed()/255.0f, Color.CYAN.getGreen()/255.0f, Color.CYAN.getBlue()/255.0f, 1f),
					new Color(	Color.ORANGE.getRed()/255.0f, Color.ORANGE.getGreen()/255.0f, Color.ORANGE.getBlue()/255.0f, 1f),  
					new Color(	Color.MAGENTA.getRed()/255.0f, Color.MAGENTA.getGreen()/255.0f, Color.MAGENTA.getBlue()/255.0f, 1f),
					new Color(	Color.GREEN.getRed()/255.0f, Color.GREEN.getGreen()/255.0f, Color.GREEN.getBlue()/255.0f, 1f)};

	public PointTracer(List<Point> points, List<Point> pulsars){

		List<Point> pointsList = points.stream().filter(f -> f!=null).collect(Collectors.toList());
		
		chart = ChartFactory.createXYLineChart(	"RA/DEC points with candidates", "FanBeam Number", "NS (deg)",
				createDataSet(pointsList, pulsars), PlotOrientation.VERTICAL, true, true, false);
		chart.setBackgroundPaint( new Color(Integer.parseInt("f4f4f4", 16)));
		chart.removeLegend();

		
		XYPlot plot = chart.getXYPlot();
		plot.setDomainCrosshairVisible(true);
		plot.setRangeCrosshairVisible(true);
		plot.setBackgroundAlpha(0.5f);
		
		plot.setDomainGridlinesVisible(false);
		plot.setRangeGridlinesVisible(false);
		
		
		double size = 6.0;
		double delta = size / 2.0;
		Shape ellipse = new Ellipse2D.Double(-delta, -delta, size, size);

		XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer)plot.getRenderer();
		
		for(int i=0;i< pointsList.size(); i++) {
			
			renderer.setSeriesPaint(i, Color.GRAY);
			renderer.setSeriesShape(i, ellipse);
			renderer.setSeriesShapesVisible(i, true);
			
		}
		
		size = 14.0;
		delta = size / 2.0;
		ellipse = new Ellipse2D.Double(-delta, -delta, size, size);
		
		int c = 0;
		for(int i= pointsList.size(); i< plot.getSeriesCount(); i++) {
						
			renderer.setSeriesPaint(i, c < 4 ? tbColors[c] : tbColors[4]);
			renderer.setSeriesShape(i, ellipse);
			renderer.setSeriesFillPaint(i, c < 4 ? tbColors[c] : tbColors[4]);
			renderer.setSeriesShapesVisible(i, true);
			c++;
			
		}
		
		
		plot.getDomainAxis().setAutoRange(true);
		plot.getRangeAxis().setAutoRange(true);
		
		plot.setBackgroundPaint( new Color(Integer.parseInt("f4f4f4", 16)));
		
		chart.setBorderStroke( new BasicStroke(3.0f));
		
	}
	
	public static  void addSeries(JFreeChart chart, Point point, int pointSize){
		
		XYPlot plot = chart.getXYPlot();
		XYSeriesCollection dataset = (XYSeriesCollection) plot.getDataset();
		

		
		if(plot.getSeriesCount() > pointSize)	dataset.removeSeries(pointSize);	
		
		
		plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
		
		XYSeries series = generateSeries(point, pointSize+1);
		dataset.addSeries(series);
		
		plot.setDataset(dataset);
		
		XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
		
		double size = 12;
		double delta = size / 2.0;
		Shape ellipse = new Ellipse2D.Double(-delta, -delta, size, size);
		

		renderer.setSeriesPaint(plot.getSeriesCount()-1, 
				new Color( 1.0f,0.0f,0.0f,1f) );
		renderer.setSeriesShape(plot.getSeriesCount()-1, ellipse);
		renderer.setSeriesFillPaint(plot.getSeriesCount()-1, 
				new Color( 1.0f,0.0f,0.0f,1f));
		
		renderer.setSeriesShapesVisible(plot.getSeriesCount()-1, true);
		plot.setRenderer(renderer);

		
	}

	private XYDataset createDataSet(List<Point> points, List<Point> pulsars){

		XYSeriesCollection result = new XYSeriesCollection();
		
		
		int i=0;
		
		for(Point point: points)	result.addSeries(generateSeries(point, i++));

		for(Point point: pulsars)	result.addSeries(generateSeries(point, i++));

		return result;

	}
	
	private static XYSeries generateSeries(Point point, int index){
		
		XYSeries series = new XYSeries("point  " + index);

		for(Traversal t: point.getTraversalList()){
			
			series.add(t.getFanbeam(), new Double(t.getNs()* Constants.rad2Deg));
			
		}
		return series;
	}

	public JFreeChart getChart() {
		return chart;
	}

	public void setChart(JFreeChart chart) {
		this.chart = chart;
	}


}
