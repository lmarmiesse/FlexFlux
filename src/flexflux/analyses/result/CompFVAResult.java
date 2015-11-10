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

import flexflux.general.Vars;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
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
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.jfree.ui.RefineryUtilities;



/**
 * 
 * Class representing the result of a comparison between two FVA's.
 * 
 * @author lmarmiesse 18 avr. 2013
 * 
 */
public class CompFVAResult extends AnalysisResult {

	/**
	 * Map containing each entity and the results ofr both FVA's.
	 */
	private Map<String, double[]> map;

	/**
	 * First initial objective value.
	 */
	private double obj1;
	/**
	 * 
	 * Second initial objective value.
	 */
	private double obj2;

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
	 * Table containing all results.
	 */
	private JTable resultTable;

	public CompFVAResult(Map<String, double[]> map, double obj1, double obj2) {
		this.map = map;
		this.obj1 = obj1;
		this.obj2 = obj2;
	}

	public void writeToFile(String path) {

		try {
			PrintWriter out = new PrintWriter(new File(path));
			out.println("FVA comparison result\n");

			out.println("obj1 : " + Vars.round(obj1) + "(+-"
					+ Vars.libertyPercentage + "%)");
			out.println("obj2 : " + Vars.round(obj2) + "(+-"
					+ Vars.libertyPercentage + "%)");

			out.println("Name\tmin1\tmax1\tmin2\tmax2");

			for (String entityName : map.keySet()) {

				out.println(entityName + "\t"
						+ Vars.round(map.get(entityName)[0]) + "\t"
						+ Vars.round(map.get(entityName)[1]) + "\t"
						+ Vars.round(map.get(entityName)[2]) + "\t"
						+ Vars.round(map.get(entityName)[3]));
			}
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void plot() {
		
		resultTable = new JTable(0, 2);

		String[] columnNames = { "Entity name", "Min first FVA",
				"Max first FVA", "Min second FVA", "Max second FVA",
				"Min difference", "Max difference" };
		Object[][] data = new Object[map.size()][columnNames.length];

		int i = 0;
		for (String entName : map.keySet()) {

			double min1 = Vars.round(map.get(entName)[0]);
			double max1 = Vars.round(map.get(entName)[1]);
			double min2 = Vars.round(map.get(entName)[2]);
			double max2 = Vars.round(map.get(entName)[3]);

			DecimalFormat df = new DecimalFormat("######.####");
			String str = df.format(min1);
			min1 = Double.parseDouble(str.replace(',', '.'));

			df = new DecimalFormat("######.####");
			str = df.format(max1);
			max1 = Double.parseDouble(str.replace(',', '.'));

			df = new DecimalFormat("######.####");
			str = df.format(min2);
			min2 = Double.parseDouble(str.replace(',', '.'));

			df = new DecimalFormat("######.####");
			str = df.format(max2);
			max2 = Double.parseDouble(str.replace(',', '.'));

			data[i] = new Object[] { entName, min1, max1, min2, max2,
					Vars.round(min2 - min1), Vars.round(max2 - max1) };

			i++;
		}

		DefaultTableModel model = new MyTableModel(data, columnNames);
		resultTable.setModel(model);
		resultTable.setDefaultRenderer(Double.class, new MyTableCellRenderer());

		sorter = new TableRowSorter<TableModel>(resultTable.getModel());
		resultTable.setRowSorter(sorter);

		JPanel northPanel = new JPanel();
		northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.PAGE_AXIS));
		northPanel.add(new JLabel("obj first FVA : " + Vars.round(obj1) + "(±"
				+ Vars.libertyPercentage + "%)"));
		northPanel.add(new JLabel("obj second FVA : " + Vars.round(obj2) + "(±"
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

		// northPanel.add(searchPanel2);
		centerPanel.add(new JScrollPane(resultTable));

		JFrame frame = new JFrame("FBA results");

		frame.add(northPanel, BorderLayout.PAGE_START);
		frame.add(centerPanel, BorderLayout.CENTER);
		frame.pack();
		RefineryUtilities.centerFrameOnScreen(frame);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);

	}

	/**
	 * Updates the table when a search is made in the plot.
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
					RowFilter.ComparisonType.BEFORE, max, 2, 4);

			thirdFilter = RowFilter.numberFilter(
					RowFilter.ComparisonType.AFTER, min, 1, 3);

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
	 * 
	 * Class to set the right colors foe the two last columns of the FVA
	 * comparison plot.
	 * 
	 * @author lmarmiesse 11 juin 2013
	 */
	class MyTableCellRenderer extends DefaultTableCellRenderer {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row,
				int column) {
			Component c = super.getTableCellRendererComponent(table, value,
					isSelected, hasFocus, row, column);

			// Only for specific cell
			if (column == 5 || column == 6) {

				if ((Double) value > 0) {
					c.setForeground(new Color(0, 255, 0));
				}

				if ((Double) value < 0) {
					c.setForeground(new Color(255, 0, 0));
				}
			} else {
				c.setForeground(new Color(0, 0, 0));
			}
			return c;
		}
	}

}
