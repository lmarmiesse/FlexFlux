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
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import org.jfree.ui.RefineryUtilities;
import org.math.plot.Plot3DPanel;
import org.math.plot.canvas.PlotCanvas;
import org.math.plot.plots.GridPlot3D;
import org.math.plot.plots.ScatterPlot;
import org.math.plot.render.AbstractDrawer;

import parsebionet.biodata.BioEntity;

/**
 * 
 * 
 * Class representing the result of an analysis with two varying fluxes.
 * 
 * @author lmarmiesse 18 avr. 2013
 * 
 */
public class PP3DResult extends AnalysisResult {

	/**
	 * Minimal group size to consider it is a phenotype phase.
	 */
	int minGrpsSize;

	/**
	 * Parameters of the varying variables.
	 */
	double init, end, deltaF, init2, end2, deltaF2;

	public final static Color[] COLORLIST = { Color.BLUE, Color.RED,
			Color.GREEN, Color.YELLOW, Color.PINK, Color.CYAN, Color.MAGENTA };
	private String reacName, reacName2, objName;

	/**
	 * 
	 * Each double[] is a point x,y,z with x : reac1 flux value, y = reac2 flux
	 * value and z = objective value.
	 * 
	 */

	private List<double[]> results = new ArrayList<double[]>();

	/**
	 * phenotype phases sorted by shadow-price value.
	 */
	Map<Double, List<double[]>> shadowPriceGroups;

	/**
	 * Comparator between phenotype phases.
	 */
	private PhenotypicPhaseComparator comparator;

	/**
	 * Map to get the group index of a point.
	 */
	Map<double[], Integer> pointIndex = new HashMap<double[], Integer>();
	/**
	 * Map to get the group index of a shadow-price value.
	 */
	Map<Double, Integer> groupIndex = new HashMap<Double, Integer>();

	/**
	 * Experimental values.
	 * 
	 * Used for pareto analysis, not for phenotype phase.
	 */

	private Map<Double, List<double[]>> expValues = new HashMap<Double, List<double[]>>();

	private double score;

	public PP3DResult(String objName, String reacName,
			String reacName2, int minGrpsSize, double init, double end,
			double deltaF, double init2, double end2, double deltaF2) {

		this.objName = objName;
		this.reacName = reacName;
		this.init = init;
		this.end = end;
		this.deltaF = deltaF;

		this.reacName2 = reacName2;
		this.init2 = init2;
		this.end2 = end2;
		this.deltaF2 = deltaF2;
		this.minGrpsSize = minGrpsSize;
	}

	/**
	 * 
	 * @param value
	 */
	public synchronized void addValue(double[] value) {

		results.add(value);

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
	 * @param zLB
	 *            Lower bound of Z.
	 * @param zUB
	 *            Upper bound of Z.
	 * @param invertZ
	 *            True if Z is minimized in the objective.
	 */
	public void normalizeValues(double xLB, double xUB, boolean invertX,
			double yLB, double yUB, boolean invertY, double zLB, double zUB,
			boolean invertZ) {

		List<double[]> newResults = new ArrayList<double[]>();

		for (double[] point : results) {

			double newX = (point[0] - xLB) / (xUB - xLB);
			if (invertX) {
				newX = 1 - newX;
			}
			double newY = (point[1] - yLB) / (yUB - yLB);
			if (invertY) {
				newY = 1 - newY;
			}
			double newZ = (point[2] - zLB) / (zUB - zLB);
			if (invertZ) {
				newZ = 1 - newZ;
			}

			newResults.add(new double[] { newX, newY, newZ });

		}

		results = newResults;

		Map<Double, List<double[]>> newExpValues = new HashMap<Double, List<double[]>>();

		for (double x : expValues.keySet()) {

			double newZ = (x - zLB) / (zUB - zLB);
			if (invertZ) {
				newZ = 1 - newZ;
			}

			List<double[]> points = new ArrayList<double[]>();

			for (double[] point : expValues.get(x)) {

				double newX = (point[0] - xLB) / (xUB - xLB);
				if (invertX) {
					newX = 1 - newX;
				}
				double newY = (point[1] - yLB) / (yUB - yLB);
				if (invertY) {
					newY = 1 - newY;
				}

				points.add(new double[] { newX, newY });
			}

			newExpValues.put(newZ, points);

		}

		expValues = newExpValues;

	}

	/**
	 * Orders the phenotype phases.
	 * 
	 */
	public void setShadowPriceGroups(
			Map<Double, List<double[]>> shadowPriceGroups) {

		// we order the groups
		this.shadowPriceGroups = shadowPriceGroups;

		List<Double> notYetAddedGroup = new ArrayList<Double>();
		for (double group : this.shadowPriceGroups.keySet()) {
			if (shadowPriceGroups.get(group).size() > minGrpsSize) {
				notYetAddedGroup.add(group);
			}
		}

		int index = 1;

		double y = init2;
		for (double x = end; x >= init; x -= deltaF) {
			for (double group : this.shadowPriceGroups.keySet()) {

				if (notYetAddedGroup.contains(group)) {

					if (shadowPriceGroups.get(group).size() > minGrpsSize) {
						for (double[] point : shadowPriceGroups.get(group)) {

							if (point[0] == x && point[1] == y) {
								groupIndex.put(group, index);
								for (double[] point2 : shadowPriceGroups
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
		double x = init;
		for (y = init2; y <= end2; y += deltaF2) {
			for (double group : this.shadowPriceGroups.keySet()) {
				if (shadowPriceGroups.get(group).size() > minGrpsSize) {

					if (notYetAddedGroup.contains(group)) {

						for (double[] point : shadowPriceGroups.get(group)) {

							if (point[0] == x && point[1] == y) {
								groupIndex.put(group, index);
								for (double[] point2 : shadowPriceGroups
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

		// if we didnt get the groups, we add them anyway
		while (!notYetAddedGroup.isEmpty()) {

			double group = notYetAddedGroup.get(0);
			groupIndex.put(group, index);
			for (double[] point2 : shadowPriceGroups.get(group)) {
				pointIndex.put(point2, index);
			}
			notYetAddedGroup.remove(group);
			index++;

		}

	}

	public Map<Double, List<double[]>> getShadowPriceGroups() {

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
			out.println("Two reactions fluxes result\n");

			out.println("Flux1 : " + reacName);
			out.println("Flux2 : " + reacName2);
			out.println("Objective : " + objName + "\n");
			out.println("Flux1\tFlux2\tObjective");

			for (double[] value : results) {
				out.println(Vars.round(value[0]) + "\t" + Vars.round(value[1])
						+ "\t" + Vars.round(value[2]));
			}

			out.println();
			out.println("Experimental values");

			for (Double value : expValues.keySet()) {

				for (double[] value2 : expValues.get(value)) {
					out.println(Vars.round(value) + "\t"
							+ Vars.round(value2[0]) + "\t"
							+ Vars.round(value2[1]));
				}

			}

			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void plot() {

		List<double[][]> plots = new ArrayList<double[][]>();

		double[] x = new double[results.size()];
		double[] y = new double[results.size()];
		double[] z = new double[results.size()];

		int i = 0;
		for (double[] values : results) {

			x[i] = values[0];
			y[i] = values[1];
			z[i] = values[2];
			i++;
		}

		plots.add(new double[][] { x, y, z });

		// create your PlotPanel (you can use it as a JPanel) with a legend at
		// SOUTH
		Plot3DPanel plot = new Plot3DPanel("SOUTH");

		plot.setAxisLabel(0, reacName);
		plot.setAxisLabel(1, reacName2);
		plot.setAxisLabel(2, objName);

		// we create the plot
		drawGridPlot(plot, "", plots.get(0)[0], plots.get(0)[1],
				plots.get(0)[2]);

		// if there are experimental values to plot
		if (!expValues.isEmpty()) {

			int size = 0;
			for (double objValue : expValues.keySet()) {
				
				size =+ expValues.get(objValue).size();
				
			}

			double[][] data = new double[size][3];

			int k = 0;
			for (double objValue : expValues.keySet()) {

				for (double[] point : expValues.get(objValue)) {

					data[k][2] = objValue;

					data[k][0] = point[0];
					data[k][1] = point[1];

					k++;
				}
			}

			ScatterPlot expPlot = new ScatterPlot("Experimental values",
					Color.BLUE, 1, 4, data);

			plot.addPlot(expPlot);

		}

		// put the PlotPanel in a JFrame like a JPanel
		JFrame frame = new JFrame("Phenotypic phase analysis results");

		if (expValues.size() > 0) {
			frame.setTitle("Pareto analysis three dimensions results");
		}

		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));

		// size for the image
		plot.setSize(600, 600);

		mainPanel.add(plot);
		frame.pack();
		frame.add(mainPanel);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		// savePng(plot, new File("/home/lmarmiesse/Documents/tests/test.png"));

		// if we want to display the frame

		JPanel fvaPanel = new JPanel();
		fvaPanel.setLayout(new BoxLayout(fvaPanel, BoxLayout.PAGE_AXIS));

		for (int index = 1; index <= groupIndex.size(); index++) {

			Color color = COLORLIST[index % COLORLIST.length];

			JPanel groupPanel = new JPanel();
			groupPanel
					.setLayout(new BoxLayout(groupPanel, BoxLayout.PAGE_AXIS));

			List<BioEntity> newEssentialReactions = comparator
					.getNewEssentialEntities().get(index);

			List<BioEntity> noLongerEssentialReactions = comparator
					.getNoLongerEssentialEntities().get(index);

			JPanel colorPanel = new JPanel();

			colorPanel.setBackground(color);

			groupPanel.add(colorPanel);
			groupPanel
					.add(new JLabel("Phenotypic phase " + index + ", "
							+ newEssentialReactions.size()
							+ " new essential reactions"));

			fvaPanel.add(groupPanel);

			if (newEssentialReactions.size() > 0) {
				fvaPanel.add(new JScrollPane(comparator
						.getNewEssentialEntitiesPanel().get(index)));
			}

			fvaPanel.add(new JLabel("Phenotypic phase " + index + ", "
					+ noLongerEssentialReactions.size()
					+ " no longer essential reactions"));

			if (noLongerEssentialReactions.size() > 0) {
				fvaPanel.add(fvaPanel.add(new JScrollPane(comparator
						.getNoLongerEssentialEntitiesPanel().get(index))));
			}

		}

		plot.setPreferredSize(new Dimension(600, 600));
		
		if (groupIndex.size() > 0) {
			JScrollPane fvaScrollPane = new JScrollPane(fvaPanel);

			fvaScrollPane.setPreferredSize(new Dimension(600, 300));

			JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
					mainPanel, fvaScrollPane);
			splitPane.setDividerLocation(600);

			frame.setContentPane(splitPane);
			frame.setSize(600, 900);

		}
		else{
			frame.setContentPane(plot);
			frame.setSize(600, 600);
		}

		RefineryUtilities.centerFrameOnScreen(frame);
		frame.setVisible(true);

	}

	/**
	 * Save the plot as a PBG image file.
	 * 
	 * @param panel
	 *            panel to save.
	 * @param file
	 *            Path to the file to write.
	 */
	public void savePng(Plot3DPanel panel, File file) {

		panel.plotToolBar.setVisible(false);

		BufferedImage bi = new BufferedImage(panel.getWidth(),
				panel.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = bi.createGraphics();

		panel.print(graphics);
		graphics.dispose();

		try {
			ImageIO.write((RenderedImage) bi, "PNG", file);

			System.err.println("successfully saved PNG image : "
					+ file.getAbsolutePath());
		} catch (IllegalArgumentException ex) {
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		panel.plotToolBar.setVisible(true);

	}

	/**
	 * Draws a 3D Grid plot
	 * 
	 * @param plot
	 *            plot to add the grid plot to.
	 * @param name
	 *            Name of the plot.
	 * @param x
	 *            X values.
	 * @param y
	 *            Y values.
	 * @param z
	 *            Z values.
	 */
	public void drawGridPlot(Plot3DPanel plot, String name, double[] x,
			double[] y, double[] z) {

		// different possible values for x
		List<Double> xValues = new ArrayList<Double>();

		for (int i = 0; i < x.length; i++) {
			if (!xValues.contains(x[i])) {
				xValues.add(x[i]);
			}
		}

		double[] xGrid = new double[xValues.size()];
		for (int i = 0; i < xGrid.length; i++) {

			xGrid[i] = xValues.get(i);
		}

		// different possible values for y
		List<Double> yValues = new ArrayList<Double>();

		for (int i = 0; i < x.length; i++) {
			if (!yValues.contains(y[i])) {
				yValues.add(y[i]);
			}
		}

		double[] yGrid = new double[yValues.size()];
		for (int i = 0; i < yGrid.length; i++) {
			yGrid[i] = yValues.get(i);
		}

		Arrays.sort(yGrid);
		Arrays.sort(xGrid);

		double[][] zGrid = new double[y.length][x.length];

		for (int i = 0; i < xGrid.length; i++) {
			for (int j = 0; j < yGrid.length; j++) {
				for (int k = 0; k < x.length; k++) {

					if (xGrid[i] == x[k] && yGrid[j] == y[k]) {
						zGrid[j][i] = z[k];
						break;
					}
				}
			}
		}

		MyPlot gridPlot = new MyPlot(name, null, xGrid, yGrid, zGrid);

		// gridPlot.draw_lines = false;
		gridPlot.draw_points = false;
		// gridPlot.fill_shape = false;

		plot.addPlot(gridPlot);
	}

	protected Color getNewColor(PlotCanvas plotCanvas) {
		return COLORLIST[plotCanvas.plots.size() % COLORLIST.length];
	}

	/**
	 * Class to create a personalized grid plot.
	 * 
	 * @author lmarmiesse 11 juin 2013
	 * 
	 */
	public class MyPlot extends GridPlot3D {

		double[] X;

		double[] Y;

		double[][] Z;

		private double[][] XYZ_list;

		public boolean draw_points = true;

		public MyPlot(String n, Color c, double[] _X, double[] _Y, double[][] _Z) {
			super(n, Color.WHITE, _X, _Y, _Z);

			X = _X;
			Y = _Y;
			Z = _Z;
			buildXYZ_list();
		}

		public void plot(AbstractDrawer draw, Color c) {
			if (!visible)
				return;

			draw.setColor(Color.BLACK);

			if (draw_lines) {
				draw.setLineType(AbstractDrawer.CONTINOUS_LINE);
				for (int i = 0; i < X.length; i++)
					for (int j = 0; j < Y.length - 1; j++) {

						int index = areSameGroup(new double[] { X[i], Y[j] },
								new double[] { X[i], Y[j + 1] });

						if (index != 0) {
							draw.setColor(COLORLIST[index % COLORLIST.length]);
							draw.drawLine(
									new double[] { X[i], Y[j], Z[j][i] },
									new double[] { X[i], Y[j + 1], Z[j + 1][i] });
						} else {
							draw.setColor(Color.BLACK);
							draw.drawLine(
									new double[] { X[i], Y[j], Z[j][i] },
									new double[] { X[i], Y[j + 1], Z[j + 1][i] });
						}

					}

				for (int j = 0; j < Y.length; j++)
					for (int i = 0; i < X.length - 1; i++) {
						int index = areSameGroup(new double[] { X[i], Y[j] },
								new double[] { X[i + 1], Y[j] });

						if (index != 0) {
							draw.setColor(COLORLIST[index % COLORLIST.length]);
							draw.drawLine(
									new double[] { X[i], Y[j], Z[j][i] },
									new double[] { X[i + 1], Y[j], Z[j][i + 1] });
						} else {
							draw.setColor(Color.BLACK);
							draw.drawLine(
									new double[] { X[i], Y[j], Z[j][i] },
									new double[] { X[i + 1], Y[j], Z[j][i + 1] });
						}
					}

			}
			// draw points
			if (draw_points) {
				draw.setColor(Color.BLACK);
				draw.setDotType(AbstractDrawer.ROUND_DOT);
				draw.setDotRadius(AbstractDrawer.DEFAULT_DOT_RADIUS);
				for (int i = 0; i < X.length; i++) {
					for (int j = 0; j < Y.length; j++) {

						for (double[] point : pointIndex.keySet()) {

							if (point[0] == X[i] && point[1] == Y[j]) {
								draw.setColor(COLORLIST[pointIndex.get(point)
										% COLORLIST.length]);
								break;
							}

						}
						draw.drawDot(new double[] { X[i], Y[j], Z[j][i] });
						draw.setColor(Color.BLACK);
					}
				}
			}
			draw.setColor(Color.BLACK);

			if (fill_shape) {
				for (int j = 0; j < Y.length - 1; j++)
					for (int i = 0; i < X.length - 1; i++) {

						int index = areSameGroup(new double[] { X[i], Y[j] },
								new double[] { X[i + 1], Y[j], }, new double[] {
										X[i + 1], Y[j + 1] }, new double[] {
										X[i], Y[j + 1] });

						if (index != 0) {
							draw.setColor(COLORLIST[index % COLORLIST.length]);
						} else {
							draw.setColor(Color.BLACK);
						}

						draw.fillPolygon(0.2f, new double[] { X[i], Y[j],
								Z[j][i] }, new double[] { X[i + 1], Y[j],
								Z[j][i + 1] }, new double[] { X[i + 1],
								Y[j + 1], Z[j + 1][i + 1] }, new double[] {
								X[i], Y[j + 1], Z[j + 1][i] });
					}
			}
		}

		private void buildXYZ_list() {
			XYZ_list = new double[X.length * Y.length][3];
			for (int i = 0; i < X.length; i++) {
				for (int j = 0; j < Y.length; j++) {
					XYZ_list[i + (j) * X.length][0] = X[i];
					XYZ_list[i + (j) * X.length][1] = Y[j];
					XYZ_list[i + (j) * X.length][2] = Z[j][i];
				}
			}
		}

		public int areSameGroup(double[]... points) {

			int index = -1;
			for (double[] p : points) {

				for (double[] point : pointIndex.keySet()) {

					if (point[0] == p[0] && point[1] == p[1]) {
						if (index == -1) {
							index = pointIndex.get(point);
						} else {

							if (index != pointIndex.get(point)) {
								return 0;
							}

						}

						break;
					}

				}

			}

			if (index != -1)
				return index;

			return 0;
		}
	}

	/**
	 * 
	 * Adds an experimental value.
	 * 
	 * Used for pareto analysis, not for phenotype phase.
	 * 
	 * 
	 * @param val1
	 *            The experimental value for X.
	 * @param val2
	 *            The experimental value for Y.
	 * @param val3
	 *            The experimental value for Z.
	 */
	public void addExpValue(double val1, double val2, double val3) {
		// val1 : obj value
		if (expValues.containsKey(val1)) {

			expValues.get(val1).add(new double[] { val2, val3 });

		} else {

			List<double[]> list = new ArrayList<double[]>();
			list.add(new double[] { val2, val3 });
			expValues.put(val1, list);
		}

	}

	/**
	 * 
	 * Adds up all the distance of the experimental values to the calculated
	 * surface.
	 * 
	 * The distance is the distance between the experimental value and the
	 * closest point on the pareto surface.
	 * 
	 * Used for pareto analysis, not for phenotype phase.
	 * 
	 */
	public void calculateScore() {

		double score = 0.0;

		for (Double d : expValues.keySet()) {

			for (double[] expPoint : expValues.get(d)) {

				double minDistance = Math.sqrt(Math.pow(
						expPoint[0] - results.get(0)[0], 2)
						+ Math.pow(expPoint[1] - results.get(0)[1], 2)
						+ Math.pow(d - results.get(0)[2], 2));

				for (double[] point : results) {

					double distance = Math.sqrt(Math.pow(
							expPoint[0] - point[0], 2)
							+ Math.pow(expPoint[1] - point[1], 2)
							+ Math.pow(d - point[2], 2));

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
	 * 
	 * Adds up all the distance of the experimental values to the calculated
	 * line.
	 * 
	 * Used for pareto analysis, not for phenotype phase.
	 * 
	 * @return The score.
	 */
	public Double getScore() {
		return score;
	}

}
