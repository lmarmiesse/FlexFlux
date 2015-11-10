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
 * 13 mars 2013 
 */
package flexflux.analyses.result;

import flexflux.general.Vars;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.jfree.ui.RefineryUtilities;

import parsebionet.biodata.BioChemicalReaction;
import parsebionet.biodata.BioEntity;

/**
 * 
 * Class representing the result of an FVA analysis.
 * 
 * @author lmarmiesse 13 mars 2013
 * 
 */
public class FVAResult extends AnalysisResult {

	/**
	 * Value of the original objective value that was used as a constraint.
	 */
	private double objValue;

	/**
	 * 
	 * Map containig all result values.
	 */
	protected Map<BioEntity, double[]> map; 
	TableRowSorter<TableModel> sorter;

	/**
	 * Field to search for entities in the plot.
	 */
	private JTextField searchField;
	/**
	 * Field to search for entities in the plot.
	 */
	private JTextField minField;
	/**
	 * Field to search for entities in the plot.
	 */
	private JTextField maxField;

	/**
	 * Table with all results.
	 */
	private JTable resultTable;

	public FVAResult(double objValue) {
		this.objValue = objValue;
		
		map = new HashMap<BioEntity, double[]>();
	}

	/**
	 * 
	 * Adds a line to the plot.
	 * 
	 * @param entity
	 *            The entity to add
	 * @param values
	 *            Its values.
	 */
	public synchronized void addLine(BioEntity entity, double[] values) {

		map.put(entity, values);
	}

	/**
	 * Set the minimum value of an entity.
	 * 
	 * @param entity
	 *            The entity to set.
	 * @param min
	 *            Its minimum value.
	 */
	public synchronized void setMin(BioEntity entity, double min) {

		if (map.containsKey(entity)) {
			map.put(entity, new double[] { min, map.get(entity)[1] });
		} else {
			map.put(entity, new double[] { min, 0 });
		}
	}

	/**
	 * Set the maximum value of an entity.
	 * 
	 * @param entity
	 *            The entity to set.
	 * @param max
	 *            Its maximum value.
	 */
	public synchronized void setMax(BioEntity entity, double max) {

		if (map.containsKey(entity)) {
			map.put(entity, new double[] { map.get(entity)[0], max });
		} else {
			map.put(entity, new double[] { 0, max });
		}
	}

	/**
	 * Get the values for an entity
	 * 
	 * @param ent
	 *            The entity to get.
	 * @return a double[] with the minimum value in [0] and the maximum value in
	 *         [1].
	 */
	public double[] getValuesForEntity(BioEntity ent) {
		return map.get(ent);

	}

	public double getObjValue() {
		return objValue;
	}

	public Map<BioEntity, double[]> getMap() {
		return map;
	}

	/**
	 * 
	 * @return The list of essential reactions.
	 */
	public List<BioEntity> getEssentialReactions() {

		List<BioEntity> essentials = new ArrayList<BioEntity>();

		for (BioEntity entity : map.keySet()) {

			if (map.get(entity)[0] < 0 && map.get(entity)[1] < 0
					&& Math.abs(map.get(entity)[1] - 0) > Math.pow(10, -Vars.decimalPrecision)) {
				essentials.add(entity);

			} else if (map.get(entity)[0] > 0 && map.get(entity)[1] > 0
					&& Math.abs(map.get(entity)[0] - 0) > Math.pow(10, -Vars.decimalPrecision)) {
				essentials.add(entity);
			}

		}

		return essentials;

	}
	
	
	/**
	 * 
	 * @return all reactions that have min and max equal to 0
	 */
	public HashMap<String, BioEntity> getZeroFluxReactions() {
		
		HashMap<String, BioEntity> zeroFluxReactions = new HashMap<String, BioEntity>();
		
		for (BioEntity entity : map.keySet()) {
			
			if (Math.abs(map.get(entity)[0]) <= Math.pow(10, -Vars.decimalPrecision) && Math.abs(map.get(entity)[1]) <= Math.pow(10, -Vars.decimalPrecision)) {
				zeroFluxReactions.put(entity.getId(), entity);
			}

		}
		
		return zeroFluxReactions;
		
		
	}
	
	
	public void writeToFile(String path) {
		try {
			PrintWriter out = new PrintWriter(new File(path));
			out.println("FVA result\n");
			out.println("obj : " + Vars.round(objValue) + "(+-"
					+ Vars.libertyPercentage + "%)");

			out.println("Name\tmin\tmax");

			for (BioEntity entity : map.keySet()) {

				out.println(entity.getId() + "\t"
						+ Vars.round(map.get(entity)[0]) + "\t"
						+ Vars.round(map.get(entity)[1]));

			}
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void plot() {

		resultTable = new JTable(0, 2);
		
		String[] columnNames = { "Entity name", "Min", "Max" };
		Object[][] data = new Object[map.size()][columnNames.length];

		int i = 0;
		for (BioEntity ent : map.keySet()) {

			data[i] = new Object[] { ent.getId(), map.get(ent)[0],
					map.get(ent)[1] };

			i++;
		}

		DefaultTableModel model = new MyTableModel(data, columnNames);
		resultTable.setModel(model);

		sorter = new TableRowSorter<TableModel>(resultTable.getModel());
		resultTable.setRowSorter(sorter);

		JPanel northPanel = new JPanel();
		northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.PAGE_AXIS));

		northPanel.add(new JLabel("obj : " + Vars.round(objValue) + "(±"
				+ Vars.libertyPercentage + "%)"));

		JPanel centerPanel = new JPanel();
		centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.PAGE_AXIS));

		JPanel searchPanel = new JPanel(new FlowLayout());
		searchPanel.add(new JLabel("Search for an entity : "));

		MyDocumentListener mdl = new MyDocumentListener();
		// name search
		searchField = new JTextField(10);
		searchField.getDocument().addDocumentListener(mdl);
		searchPanel.add(searchField);

		northPanel.add(searchPanel);

		// min and max search
		JPanel searchPanel2 = new JPanel(new FlowLayout());
		searchPanel2.add(new JLabel("Min : "));
		minField = new JTextField(5);
		minField.setText("-999999");
		minField.getDocument().addDocumentListener(mdl);
		searchPanel2.add(minField);
		searchPanel2.add(new JLabel("Max : "));
		maxField = new JTextField(5);
		maxField.setText("999999");
		maxField.getDocument().addDocumentListener(mdl);
		searchPanel2.add(maxField);

		northPanel.add(searchPanel2);
		centerPanel.add(new JScrollPane(resultTable));

		JFrame frame = new JFrame("FVA results");

		frame.add(northPanel, BorderLayout.PAGE_START);
		frame.add(centerPanel, BorderLayout.CENTER);
		frame.pack();
		RefineryUtilities.centerFrameOnScreen(frame);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);

	}

	/**
	 * Update the table when a search is made.
	 */
	private void updateTable(TableRowSorter<TableModel> sorter,
			DocumentEvent arg0) {
		String text = searchField.getText();
		double min = -999999;

		try {
			min = Double.parseDouble(minField.getText());
		} catch (Exception e) {
		}

		double max = 999999;

		try {
			max = Double.parseDouble(maxField.getText());
		} catch (Exception e) {
		}

		if (sorter.getModelRowCount() != 0) {

			List<RowFilter<TableModel, Object>> filters = new ArrayList<RowFilter<TableModel, Object>>();

			RowFilter<TableModel, Object> firstFilter = null;
			RowFilter<TableModel, Object> secondFilter = null;
			RowFilter<TableModel, Object> thirdFilter = null;

			// case insensitive
			if (text.length() != 0) {
				firstFilter = RowFilter.regexFilter(
						"(?i)" + Pattern.quote(text), 0);
			}

			secondFilter = RowFilter.numberFilter(
					RowFilter.ComparisonType.BEFORE, max, 2);

			thirdFilter = RowFilter.numberFilter(
					RowFilter.ComparisonType.AFTER, min, 1);

			if (firstFilter != null) {
				filters.add(firstFilter);
			}
			filters.add(secondFilter);
			filters.add(thirdFilter);

			sorter.setRowFilter(RowFilter.andFilter(filters));

		}

	}

	class MyDocumentListener implements DocumentListener {
		String newline = "\n";

		public void changedUpdate(DocumentEvent arg0) {
			updateTable(sorter, arg0);
		}

		public void insertUpdate(DocumentEvent arg0) {
			updateTable(sorter, arg0);
		}

		public void removeUpdate(DocumentEvent arg0) {
			updateTable(sorter, arg0);
		}
	}

	/**
	 * Creates a JPanel for reactions.
	 * 
	 * @param essentialReactions
	 *            Essential reaction.
	 * @return a JPanel with each essential reaction, their pathway, minimum and
	 *         maximum values.
	 */
	public JPanel getReactionsPanel(List<BioEntity> essentialReactions) {

		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));

		String[] columnNames = { "Entity name", "Pathway", "Min", "Max" };

		Object[][] data = new Object[essentialReactions.size()][columnNames.length];

		int i = 0;

		for (BioEntity ent : essentialReactions) {

			String pathwayName = "";

			try {

				for (String name : ((BioChemicalReaction) ent).getPathwayList()
						.keySet()) {
					pathwayName += name + " ";
				}

			} catch (Exception e) {

			}
			data[i] = new Object[] { ent.getName(), pathwayName,
					Vars.round(map.get(ent)[0]), Vars.round(map.get(ent)[1]) };

			i++;
		}

		JTable table = new JTable(0, 2);

		DefaultTableModel model = new MyTableModel(data, columnNames);
		table.setModel(model);

		table.getColumnModel().getColumn(3).setPreferredWidth(15);
		table.getColumnModel().getColumn(2).setPreferredWidth(15);

		TableRowSorter<TableModel> mySorter = new TableRowSorter<TableModel>(
				table.getModel());
		table.setRowSorter(mySorter);

		JPanel northPanel = new JPanel();
		northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.PAGE_AXIS));

		JPanel centerPanel = new JPanel();
		centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.PAGE_AXIS));

		JPanel searchPanel = new JPanel(new FlowLayout());
		searchPanel.add(new JLabel("Search for an entity : "));

		JScrollPane tableScrollPane = new JScrollPane(table);

		tableScrollPane.setPreferredSize(new Dimension(400, Math.min(
				table.getRowHeight() * table.getRowCount() + 25, 200)));

		centerPanel.add(tableScrollPane);

		panel.add(northPanel);
		panel.add(centerPanel);

		return panel;
	}

}
