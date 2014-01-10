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
 * 28 mars 2013 
 */
package parsebionet.utils.flexconflux.analyses.result;

import java.awt.Color;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RefineryUtilities;

import parsebionet.utils.flexconflux.Vars;

/**
 * Class representing the result of a time dependent FBA analysis.
 * 
 * @author lmarmiesse 28 mars 2013
 * 
 */
public class RFBAResult extends AnalysisResult {

	/**
	 * Map with key : time and value : map of entities and their values
	 */
	Map<Double, Map<String, Double>> resultMap = new HashMap<Double, Map<String, Double>>();

	/**
	 * All times.
	 */
	List<Double> times = new ArrayList<Double>();

	/**
	 * List of concerned entities.
	 */
	Set<String> entities = new HashSet<String>();

	/**
	 * 
	 * Add a value for all entities at a given time.
	 * 
	 * @param time
	 *            The time to add this value.
	 * @param valuesMap
	 *            A map with all entities and their values.
	 */
	public void addValues(double time, Map<String, Double> valuesMap) {

		if (entities.size() == 0) {
			for (String s : valuesMap.keySet()) {
				entities.add(s);
			}
		}

		times.add(time);

		resultMap.put(time, valuesMap);

	}

	/**
	 * 
	 * @param time
	 *            The time to get the values from.
	 * @return map containing all entities and their values.
	 */
	public Map<String, Double> getValuesforTime(double time) {

		return resultMap.get(time);

	}

	public void writeToFile(String path) {

		try {
			PrintWriter out = new PrintWriter(new File(path));

			String line = "\t";
			for (String s : entities) {

				line += s + "\t";

			}
			out.println(line);

			for (Double time : times) {

				line = Vars.round(time) + "\t";
				for (String s : entities) {

					line += Vars.round(resultMap.get(time).get(s)) + "\t";

				}
				out.println(line);

			}
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void plot() {

		JPanel panel = new JPanel();
		JScrollPane sp = new JScrollPane(panel);
		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));

		// one chart by entity

		Color[] colors = new Color[] { Color.RED, Color.GREEN, Color.BLUE };
		int index = 0;
		for (String s : entities) {

			XYSeriesCollection dataset = new XYSeriesCollection();

			XYSeries series = new XYSeries(s);

			for (Double time : times) {
				series.add(time, resultMap.get(time).get(s));
			}

			final JFreeChart chart = ChartFactory.createXYLineChart(s, // chart
																		// title
					"Time (h)", // domain axis label
					"Value", // range axis label
					dataset, // data
					PlotOrientation.VERTICAL, // orientation
					true, // include legend
					true, // tooltips
					false // urls
					);

			chart.setBackgroundPaint(Color.white);

			XYPlot plot = (XYPlot) chart.getPlot();

			plot.setBackgroundPaint(Color.WHITE);
			plot.setRangeGridlinePaint(Color.GRAY);
			plot.setDomainGridlinePaint(Color.GRAY);

			plot.getRenderer().setSeriesPaint(0, colors[index % colors.length]);

			index++;

			ChartPanel chartPanel = new ChartPanel(chart);

			dataset.addSeries(series);

			panel.add(chartPanel);
			panel.add(new JSeparator());

		}

		Dimension d = panel.getComponent(0).getPreferredSize();
		d.height *= 2;

		sp.getViewport().setPreferredSize(d);

		JFrame frame = new JFrame("rFBA results");

		frame.add(sp);
		frame.pack();
		RefineryUtilities.centerFrameOnScreen(frame);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);

	}

}
