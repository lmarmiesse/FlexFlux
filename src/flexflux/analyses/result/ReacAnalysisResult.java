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
 * 18 avr. 2013 
 */
package flexflux.analyses.result;

import flexflux.analyses.PhenotypicPhaseComparator;
import flexflux.general.Vars;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Paint;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RefineryUtilities;

import parsebionet.biodata.BioEntity;


/**
 * 
 * Class representing the result of an analysis with a varying flux.
 * 
 * @author lmarmiesse 18 avr. 2013
 * 
 */
public class ReacAnalysisResult extends AnalysisResult {

	/**
	 * Minimal group size to consider it is a phenotype phase.
	 */
	int minGrpsSize;

	/**
	 * Parameters of the varying variable.
	 */
	double init, end, deltaF;

	public final static Color[] COLORLIST = { Color.BLUE, Color.RED,
			Color.GREEN, Color.YELLOW, Color.PINK, Color.CYAN, Color.MAGENTA };

	/**
	 * results.
	 */
	private Map<Double, Double> resultValues = new HashMap<Double, Double>();
	/**
	 * All flux values.
	 */
	private List<Double> fluxValues = new ArrayList<Double>();

	private String reacName, objName;

	/**
	 * phenotype phases sorted by shadow-price value.
	 */
	private Map<Double, List<Double>> shadowPriceGroups;

	/**
	 * Map to get the group index of a point.
	 */
	private Map<Double, Integer> pointIndex = new HashMap<Double, Integer>();

	/**
	 * Map to get the group index of a shadow-price value.
	 */
	private Map<Double, Integer> groupIndex = new HashMap<Double, Integer>();

	/**
	 * Comparator between phenotype phases.
	 */
	private PhenotypicPhaseComparator comparator;

	/**
	 * Experimental values.
	 * 
	 * Used for pareto analysis, not for phenotype phase.
	 */
	private Map<Double, List<Double>> expValues = new HashMap<Double, List<Double>>();

	private double score;

	public ReacAnalysisResult(String objName, String reacName,
			List<Double> fluxValues, Map<Double, Double> resultValues,
			int minGrpsSize, double init, double end, double deltaF) {
		this.fluxValues = fluxValues;
		this.resultValues = resultValues;
		this.objName = objName;
		this.reacName = reacName;

		this.minGrpsSize = minGrpsSize;
		this.init = init;
		this.end = end;
		this.deltaF = deltaF;
	}

	/**
	 * Orders the phenotype phases.
	 * 
	 */
	public void setShadowPriceGroups(Map<Double, List<Double>> shadowPriceGroups) {
		// we order the groups
		this.shadowPriceGroups = shadowPriceGroups;

		List<Double> notYetAddedGroup = new ArrayList<Double>();
		for (double group : this.shadowPriceGroups.keySet()) {
			notYetAddedGroup.add(group);
		}

		int index = 1;

		for (double x = init; x <= end; x += deltaF) {
			for (double group : this.shadowPriceGroups.keySet()) {

				if (notYetAddedGroup.contains(group)) {

					if (shadowPriceGroups.get(group).size() > minGrpsSize) {
						for (double point : shadowPriceGroups.get(group)) {

							if (point == x) {
								groupIndex.put(group, index);
								for (double point2 : shadowPriceGroups
										.get(group)) {
									pointIndex.put(point2, index);
								}
								notYetAddedGroup.remove(group);
								index++;
								break;
							}

						}
					}
				}
			}
		}
	}

	public Map<Double, List<Double>> getShadowPriceGroups() {
		return shadowPriceGroups;
	}

	public Map<Double, Integer> getGroupIndex() {
		return groupIndex;
	}

	public void setComparator(PhenotypicPhaseComparator comparator) {
		this.comparator = comparator;
	}

	public void writeToFile(String path) {

		try {
			PrintWriter out = new PrintWriter(new File(path));
			out.println("Reaction flux result\n");

			out.println("Flux : " + reacName);
			out.println("Objective : " + objName + "\n");

			out.println("Flux\tObjective");

			for (Double value : fluxValues) {
				out.println(Vars.round(value) + "\t"
						+ Vars.round(resultValues.get(value)));
			}

			out.println();

			out.println("Experimental values");

			for (Double value : expValues.keySet()) {

				for (Double value2 : expValues.get(value)) {
					out.println(Vars.round(value) + "\t" + Vars.round(value2));
				}

			}

			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void plot() {

		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));

		// one chart by group

		Map<Integer, Integer> correspGroup = new HashMap<Integer, Integer>();

		XYSeriesCollection dataset = new XYSeriesCollection();
		int index = 0;
		XYSeries series = new XYSeries("");
		for (double point : fluxValues) {

			series.add(point, resultValues.get(point));
			correspGroup.put(index, pointIndex.get(point));

			index++;
		}

		dataset.addSeries(series);

		if (!expValues.isEmpty()) {
			XYSeries expSeries = new XYSeries("Experimental values");
			if (!expValues.isEmpty()) {
				for (Double d : expValues.keySet()) {
					for (Double d2 : expValues.get(d)) {
						expSeries.add(d, d2);
					}
				}
			}

			dataset.addSeries(expSeries);
		}

		final JFreeChart chart = ChartFactory.createXYLineChart("", // chart
				// title
				reacName, // domain axis label
				objName, // range axis label
				dataset, // data
				PlotOrientation.VERTICAL, // orientation
				true, // include legend
				true, // tooltips
				false // urls
				);

		XYPlot plot = (XYPlot) chart.getPlot();

		plot.setBackgroundPaint(Color.WHITE);
		plot.setRangeGridlinePaint(Color.GRAY);
		plot.setDomainGridlinePaint(Color.GRAY);

		XYLineAndShapeRenderer renderer = new MyRenderer(true, false,
				correspGroup);

		plot.setRenderer(0, renderer);

		if (!expValues.isEmpty()) {
			renderer.setSeriesLinesVisible(1, false);
			renderer.setSeriesShapesVisible(1, true);
			renderer.setSeriesPaint(1, Color.BLUE);
		}

		ChartPanel chartPanel = new ChartPanel(chart);

		panel.add(chartPanel);

		JPanel fvaPanel = new JPanel();
		fvaPanel.setLayout(new BoxLayout(fvaPanel, BoxLayout.PAGE_AXIS));

		for (int i = 1; i <= groupIndex.size(); i++) {

			Color color = COLORLIST[i % COLORLIST.length];

			JPanel groupPanel = new JPanel();
			groupPanel
					.setLayout(new BoxLayout(groupPanel, BoxLayout.PAGE_AXIS));

			List<BioEntity> newEssentialReactions = comparator
					.getNewEssentialEntities().get(i);

			List<BioEntity> noLongerEssentialReactions = comparator
					.getNoLongerEssentialEntities().get(i);

			JPanel colorPanel = new JPanel();

			colorPanel.setBackground(color);

			groupPanel.add(colorPanel);
			groupPanel
					.add(new JLabel("Phenotypic phase " + i + ", "
							+ newEssentialReactions.size()
							+ " new essential reactions"));

			fvaPanel.add(groupPanel);

			if (newEssentialReactions.size() > 0) {
				fvaPanel.add(new JScrollPane(comparator
						.getNewEssentialEntitiesPanel().get(i)));
			}

			fvaPanel.add(new JLabel("Phenotypic phase " + i + ", "
					+ noLongerEssentialReactions.size()
					+ " no longer essential reactions"));

			if (noLongerEssentialReactions.size() > 0) {
				fvaPanel.add(fvaPanel.add(new JScrollPane(comparator
						.getNoLongerEssentialEntitiesPanel().get(i))));
			}

		}

		JScrollPane fvaScrollPane = new JScrollPane(fvaPanel);

		JFrame frame = new JFrame("Results");

		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, panel,
				fvaScrollPane);

		frame.setContentPane(splitPane);

		panel.setPreferredSize(new Dimension(600, 600));
		panel.setMinimumSize(new Dimension(600, 600));

		frame.setSize(600, 1000);
		RefineryUtilities.centerFrameOnScreen(frame);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);

	}

	/**
	 * Adds a value to the results.
	 * 
	 * 
	 * @param value
	 *            The flux value.
	 * 
	 * @param res
	 *            The objective value.
	 */
	public synchronized void addValue(double value, double res) {

		fluxValues.add(value);
		resultValues.put(value, res);

	}

	/**
	 * Normalizes the values.
	 * 
	 * Used for pareto analysis, not for phenotype phase.
	 * 
	 * @param xLB
	 *            Lower bound of X.
	 * @param xUB
	 *            Upper bound of X.
	 * @param invertX
	 *            True if X is minimized in the objective.
	 * @param yLB
	 *            Lower bound of Y.
	 * @param yUB
	 *            Upper bound of Y.
	 * @param invertY
	 *            True if Y is minimized in the objective.
	 */
	public void normalizeValues(double xLB, double xUB, boolean invertX,
			double yLB, double yUB, boolean invertY) {

		Map<Double, Double> newResultValues = new HashMap<Double, Double>();
		List<Double> newFluxValues = new ArrayList<Double>();

		for (double x : fluxValues) {

			double newX = (x - xLB) / (xUB - xLB);

			if (invertX) {
				newX = 1 - newX;
			}

			double newY = (resultValues.get(x) - yLB) / (yUB - yLB);
			if (invertY) {
				newY = 1 - newY;
			}

			newFluxValues.add(newX);
			newResultValues.put(newX, newY);

		}

		fluxValues = newFluxValues;
		resultValues = newResultValues;

		Map<Double, List<Double>> newExpValues = new HashMap<Double, List<Double>>();

		for (double x : expValues.keySet()) {

			double newX = (x - xLB) / (xUB - xLB);

			if (invertX) {
				newX = 1 - newX;
			}

			List<Double> newYs = new ArrayList<Double>();

			for (double y : expValues.get(x)) {

				double newY = (y - yLB) / (yUB - yLB);
				if (invertY) {
					newY = 1 - newY;
				}

				newYs.add(newY);

			}

			newExpValues.put(newX, newYs);
		}

		expValues = newExpValues;

	}

	/**
	 * 
	 * Class used to set the right colors and sized for the plot.
	 * 
	 * @author lmarmiesse 11 juin 2013
	 * 
	 */
	private class MyRenderer extends XYLineAndShapeRenderer {

		Map<Integer, Integer> correspGroup;

		public Color[] COLORLIST = { Color.BLUE, Color.RED, Color.GREEN,
				Color.YELLOW, Color.PINK, Color.CYAN, Color.MAGENTA };

		public MyRenderer(boolean lines, boolean shapes,
				Map<Integer, Integer> correspGroup) {
			super(lines, shapes);
			this.correspGroup = correspGroup;
		}

		@Override
		public Paint getItemPaint(int row, int col) {

			if (row == 1) {
				return Color.BLUE;
			}

			if (pointIndex.isEmpty())
				return Color.RED;

			if (correspGroup.get(col) != null) {
				return COLORLIST[correspGroup.get(col) % COLORLIST.length];
			}
			return Color.BLACK;

		}
	}

	/**
	 * Adds an experimental value.
	 * 
	 * 
	 * 
	 * Used for pareto analysis, not for phenotype phase.
	 * 
	 * @param a
	 *            The experimental value for X.
	 * @param b
	 *            The experimental value for Y.
	 */
	public void addExpValue(double a, double b) {
		if (expValues.containsKey(a)) {
			expValues.get(a).add(b);
		} else {

			List<Double> d = new ArrayList<Double>();
			d.add(b);

			expValues.put(a, d);
		}
	}

	/**
	 * 
	 * Adds up all the distance of the experimental values to the calculated
	 * line.
	 * 
	 * The distance is the distance between the experimental value and the
	 * closest point on the pareto line.
	 * 
	 * Used for pareto analysis, not for phenotype phase.
	 * 
	 * 
	 */
	public void calculateScore() {

		double score = 0.0;

		for (Double d : expValues.keySet()) {

			for (Double d2 : expValues.get(d)) {

				double minDistance = Math
						.sqrt(Math.pow(d - fluxValues.get(0), 2)
								+ Math.pow(
										d2
												- resultValues.get(fluxValues
														.get(0)), 2));

				for (double point : fluxValues) {

					double distance = Math.sqrt(Math.pow(d - point, 2)
							+ Math.pow(d2 - resultValues.get(point), 2));

					if (distance < minDistance) {
						minDistance = distance;
					}
				}

				score += minDistance;
			}
		}

		this.score = score;

	}

	/**
	 * @return The score.
	 */
	public double getScore() {

		return score;

	}
}
