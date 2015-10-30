/*******************************************************************************
 * Copyright INRA
 * 
 *  Contact: ludovic.cottret@toulouse.inra.fr
 * 
 * 
 * This software is governed by the CeCILL license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 * 
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 *  In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 *  The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL license and that you accept its terms.
 ******************************************************************************/
/**
 * 28 mai 2013 
 */
package flexflux.analyses.result;

import java.awt.Color;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RefineryUtilities;

import flexflux.objective.Objective;



/**
 * 
 * Class representing the result of a simple FBA analysis.
 * 
 * @author lmarmiesse 28 mai 2013
 * 
 */
public class ParetoAnalysisResult extends AnalysisResult {

	/**
	 * List of all the 2D results associated to their scores.
	 */
	Map<PP2DResult, Double> twoDResults = new HashMap<PP2DResult, Double>();
	/**
	 * List of all the 3D results associated to their scores.
	 */
	Map<PP3DResult, Double> threeDResults = new HashMap<PP3DResult, Double>();

	/**
	 * 1D results for all the objectives.
	 * 
	 */
	Map<Objective, List<Double>> oneDResults = new HashMap<Objective, List<Double>>();

	public ParetoAnalysisResult(Map<Objective, List<Double>> oneDResults,
			Map<PP2DResult, Double> twoDResults,
			Map<PP3DResult, Double> threeDResults) {

		this.oneDResults = oneDResults;
		this.twoDResults = twoDResults;
		this.threeDResults = threeDResults;

	}

	public void writeToFile(String path) {
		
		File dir = new File(path);
		if (!dir.mkdirs()){
			System.err.println("Error : result directory was not created");
			if (!dir.canWrite()){
				System.err.println("FlexFlux cannot write in directory "+dir);
			}
		}

		// 1D results
		PrintWriter out;
		try {
			out = new PrintWriter(new File(path + "/1D.txt"));

			for (Objective obj : oneDResults.keySet()) {

				out.println(obj.getName() + "\n");

				for (double val : oneDResults.get(obj)) {

					out.println(val);
				}

				out.println();
			}

			out.close();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// 2Dresults

		int i = 1;
		for (PP2DResult res : twoDResults.keySet()) {
			res.writeToFile(path + "/2D_" + i + ".txt");
			i++;
		}

		// 3D results

		i = 1;
		for (PP3DResult res : threeDResults.keySet()) {
			res.writeToFile(path + "/3D_" + i + ".txt");
			i++;
		}

	}

	public void plot() {

		XYSeriesCollection dataset = new XYSeriesCollection();

		int i = 1;
		for (Objective obj : oneDResults.keySet()) {

			XYSeries series = new XYSeries(obj.getName());

			for (double val : oneDResults.get(obj)) {

				series.add(i, val);
			}

			dataset.addSeries(series);
			i++;
		}

		// create the chart...

		final JFreeChart chart = ChartFactory.createXYLineChart(
				"", // chart title
				"Objectives", // x axis label
				"Values", // y axis label
				dataset, // data
				PlotOrientation.VERTICAL, true, // include legend
				true, // tooltips
				false // urls
				);

		ChartPanel chartPanel = new ChartPanel(chart);

		XYPlot plot = chart.getXYPlot();

		plot.setBackgroundPaint(Color.WHITE);
		plot.setRangeGridlinePaint(Color.GRAY);
		plot.setDomainGridlinePaint(Color.GRAY);

		XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
		renderer.setLinesVisible(false);
		renderer.setShapesVisible(true);
		plot.setRenderer(renderer);

		// change the auto tick unit selection to integer units only...
		NumberAxis rangeAxis = (NumberAxis) plot.getDomainAxis();
		rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

		JFrame frame = new JFrame("Pareto analysis one dimension results");
		frame.add(chartPanel);

		frame.pack();

		RefineryUtilities.centerFrameOnScreen(frame);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setVisible(true);

		for (PP2DResult r : twoDResults.keySet()) {

			r.plot();

		}

		for (PP3DResult r : threeDResults.keySet()) {

			r.plot();

		}

	}

	@Override
	public void writeHTML(String path) {
		// TODO Auto-generated method stub
		
	}

}
