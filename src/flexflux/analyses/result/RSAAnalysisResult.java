package flexflux.analyses.result;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.RowSorter.SortKey;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.jfree.ui.RefineryUtilities;

import parsebionet.biodata.BioEntity;
import flexflux.general.Constraint;

public class RSAAnalysisResult extends AnalysisResult {

	/**
	 * Field to search for entities in the plot.
	 */
	private JTextField searchField;

	private JTable resultTable;

	private List<Map<BioEntity, Integer>> statesList = new ArrayList<Map<BioEntity, Integer>>();

	private List<Constraint> finalConstraints = new ArrayList<Constraint>();

	private Set<BioEntity> resultEntities = new HashSet<BioEntity>();

	private List<Map<BioEntity, Integer>> attractorStatesList = new ArrayList<Map<BioEntity, Integer>>();

	/**
	 * Mean state values
	 */
	private Map<BioEntity, Double> meanAttractorStates;

	public void addResultEntity(BioEntity ent) {
		resultEntities.add(ent);
	}

	public void setAttractorStatesList(List<Map<BioEntity, Integer>> states) {
		attractorStatesList = states;
	}

	public void setStatesList(List<Map<BioEntity, Integer>> states) {
		statesList = states;
	}

	public void setSteadyStateConstraints(List<Constraint> consts) {

		finalConstraints = consts;

	}

	public List<Constraint> getSteadyStateConstraints() {

		return finalConstraints;

	}

	public List<Map<BioEntity, Integer>> getAttractorStatesList() {

		return attractorStatesList;

	}

	public List<Map<BioEntity, Integer>> getStatesList() {

		return statesList;

	}

	@Override
	public void writeToFile(String path) {

		if (resultEntities.size() == 0) {

			System.err.println(
					"No component has a steady state value. Check that some of your components have initial values.");
			// System.exit(0);
		}

		try {
			PrintWriter out = new PrintWriter(new File(path));
			out.println("Steady state analysis result\n");

			out.print("Entity name\t");
			for (int j = 0; j < statesList.size() - 1; j++) {

				int limit = statesList.size() - attractorStatesList.size();
				if (j + 1 >= limit) {
					out.print("State " + (j + 1) + "(Attractor)\t");
				} else {
					out.print("State " + (j + 1) + "\t");
				}
			}
			out.print("Final constraint lower bound\t");
			out.print("Final constraint upper bound\t");
			out.print("\n");

			for (BioEntity ent : resultEntities) {

				out.print(ent.getId());
				for (int j = 0; j < statesList.size() - 1; j++) {

					if (statesList.get(j).containsKey(ent)) {

						out.print("\t" + statesList.get(j).get(ent));
					} else {
						out.print("\t" + "?");
					}

				}

				out.print("\t");

				for (Constraint c : finalConstraints) {
					if (c.getEntities().keySet().contains(ent)) {
						out.print(c.getLb() + "\t");
						out.print(c.getUb() + "\t");
						break;
					}
				}

				out.print("\n");

			}
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	public void plot() {

		if (resultEntities.size() == 0) {

			System.err.println(
					"Error : no component has a steady state value. Check that some of your components have initial values.");
			System.exit(0);
		}

		JFrame frame = new JFrame("Steady state analysis results");

		resultTable = new JTable(0, statesList.size());

		resultTable.getTableHeader().setDefaultRenderer(new DefaultTableHeaderCellRenderer());

		String[] columnNames = new String[statesList.size() + 2];

		columnNames[0] = "<html><center>Entity<br/>name</center></html>";

		for (int i = 1; i <= statesList.size() - 1; i++) {
			columnNames[i] = String.valueOf(i);
		}

		columnNames[statesList.size()] = "<html><center>Final constraint<br/>lower bound</center></html>";
		columnNames[statesList.size() + 1] = "<html><center>Final constraint<br/>upper bound</center></html>";

		Object[][] data = new Object[resultEntities.size()][columnNames.length];

		int i = 0;
		for (BioEntity ent : resultEntities) {

			Object[] entValues = new Object[statesList.size() + 2];

			entValues[0] = ent.getId();
			for (int j = 0; j < statesList.size() - 1; j++) {

				if (statesList.get(j).containsKey(ent)) {
					entValues[j + 1] = statesList.get(j).get(ent);
				} else {
					entValues[j + 1] = "?";
				}

			}

			for (Constraint c : finalConstraints) {
				if (c.getEntities().keySet().contains(ent)) {
					entValues[statesList.size()] = c.getLb();
					entValues[statesList.size() + 1] = c.getUb();
					break;
				}
			}

			data[i] = entValues;
			i++;

		}

		DefaultTableModel model = new MyTableModel(data, columnNames);
		resultTable.setModel(model);

		resultTable.getColumnModel().getColumn(statesList.size()).setPreferredWidth(130);
		resultTable.getColumnModel().getColumn(statesList.size() + 1).setPreferredWidth(130);

		final MyTableRowSorter<TableModel> sorter = new MyTableRowSorter<TableModel>(resultTable.getModel());

		resultTable.setRowSorter(sorter);

		resultTable.setPreferredScrollableViewportSize(resultTable.getPreferredSize());

		JPanel searchPanel = new JPanel(new FlowLayout());

		searchPanel.add(new JLabel("Search for an entity : "));

		searchField = new JTextField(10);
		searchField.getDocument().addDocumentListener(new DocumentListener() {

			public void changedUpdate(DocumentEvent arg0) {
				updateTable(sorter);
			}

			public void insertUpdate(DocumentEvent arg0) {
				updateTable(sorter);
			}

			public void removeUpdate(DocumentEvent arg0) {
				updateTable(sorter);
			}

		});

		searchPanel.add(searchField);

		JPanel centerPanel = new JPanel();
		centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.PAGE_AXIS));
		centerPanel.add(searchPanel);
		centerPanel.add(new JLabel("Attractor of size " + attractorStatesList.size()));

		centerPanel.add(new JScrollPane(resultTable));

		frame.add(centerPanel, BorderLayout.CENTER);
		frame.pack();
		RefineryUtilities.centerFrameOnScreen(frame);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);

	}

	/**
	 * Updates the table when a search is made in the plot.
	 */
	private void updateTable(TableRowSorter<TableModel> sorter) {
		String text = searchField.getText();
		if (sorter.getModelRowCount() != 0) {

			// case insensitive
			sorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(text), 0));
		}

	}

	// http://www.camick.com/java/source/DefaultTableHeaderCellRenderer.java
	private class DefaultTableHeaderCellRenderer extends DefaultTableCellRenderer {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		/**
		 * Constructs a <code>DefaultTableHeaderCellRenderer</code>.
		 * <P>
		 * The horizontal alignment and text position are set as appropriate to
		 * a table header cell, and the opaque property is set to false.
		 */
		public DefaultTableHeaderCellRenderer() {
			setHorizontalAlignment(CENTER);
			setHorizontalTextPosition(LEFT);
			setVerticalAlignment(BOTTOM);
			// setOpaque(false);
		}

		/**
		 * Returns the default table header cell renderer.
		 * <P>
		 * If the column is sorted, the approapriate icon is retrieved from the
		 * current Look and Feel, and a border appropriate to a table header
		 * cell is applied.
		 * <P>
		 * Subclasses may overide this method to provide custom content or
		 * formatting.
		 *
		 * @param table
		 *            the <code>JTable</code>.
		 * @param value
		 *            the value to assign to the header cell
		 * @param isSelected
		 *            This parameter is ignored.
		 * @param hasFocus
		 *            This parameter is ignored.
		 * @param row
		 *            This parameter is ignored.
		 * @param column
		 *            the column of the header cell to render
		 * @return the default table header cell renderer
		 */
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			JTableHeader tableHeader = table.getTableHeader();
			if (tableHeader != null) {
				setForeground(tableHeader.getForeground());
			}
			setIcon(getIcon(table, column));
			setBorder(UIManager.getBorder("TableHeader.cellBorder"));

			int limit = statesList.size() - attractorStatesList.size();
			if (column >= limit && column < statesList.size()) {

				c.setBackground(Color.GREEN);
			} else {
				c.setBackground(new Color(230, 230, 230));
			}
			if (column >= statesList.size()) {
				c.setFont(new Font("Arial", Font.BOLD, 13));
			}
			return c;
		}

		/**
		 * Overloaded to return an icon suitable to the primary sorted column,
		 * or null if the column is not the primary sort key.
		 *
		 * @param table
		 *            the <code>JTable</code>.
		 * @param column
		 *            the column index.
		 * @return the sort icon, or null if the column is unsorted.
		 */
		protected Icon getIcon(JTable table, int column) {
			SortKey sortKey = getSortKey(table, column);
			if (sortKey != null && table.convertColumnIndexToView(sortKey.getColumn()) == column) {
				switch (sortKey.getSortOrder()) {
				case ASCENDING:
					return UIManager.getIcon("Table.ascendingSortIcon");
				case DESCENDING:
					return UIManager.getIcon("Table.descendingSortIcon");
				}
			}
			return null;
		}

		/**
		 * Returns the current sort key, or null if the column is unsorted.
		 *
		 * @param table
		 *            the table
		 * @param column
		 *            the column index
		 * @return the SortKey, or null if the column is unsorted
		 */
		protected SortKey getSortKey(JTable table, int column) {
			RowSorter rowSorter = table.getRowSorter();
			if (rowSorter == null) {
				return null;
			}

			List sortedColumns = rowSorter.getSortKeys();
			if (sortedColumns.size() > 0) {
				return (SortKey) sortedColumns.get(0);
			}
			return null;
		}
	}

	public Map<BioEntity, Double> getMeanAttractorStates() {
		return meanAttractorStates;
	}

	public void setMeanAttractorStates(Map<BioEntity, Double> meanAttractorStates) {
		this.meanAttractorStates = meanAttractorStates;
	}

	@Override
	public void writeHTML(String path) {
		
		try {
			PrintWriter out = new PrintWriter(new File(path));

			out.println("<table>");
			out.println("<tr>");
			
			out.print("<th>Entity name</th>");
			for (int j = 0; j < statesList.size() - 1; j++) {

				int limit = statesList.size() - attractorStatesList.size();
				if (j + 1 >= limit) {
					out.print("<th>State " + (j + 1) + "(Attractor)</th>");
				} else {
					out.print("<th>State " + (j + 1) + "</th>");
				}
			}
			
			out.print("<th>Final constraint lower bound</th>");
			out.print("<th>Final constraint upper bound</th>");
			out.println("</tr>");

			for (BioEntity ent : resultEntities) {

				out.println("<tr>");
				
				out.print("<td>"+ent.getId()+"</td>");
				for (int j = 0; j < statesList.size() - 1; j++) {

					if (statesList.get(j).containsKey(ent)) {

						out.print("<td>" + statesList.get(j).get(ent)+"</td>");
					} else {
						out.print("<td>" + "?</td>");
					}

				}

				for (Constraint c : finalConstraints) {
					if (c.getEntities().keySet().contains(ent)) {
						out.print("<td>"+c.getLb() + "</td>");
						out.print("<td>"+c.getUb() + "</td>");
						break;
					}
				}
				
				out.println("</tr>");

			}
			
			out.println("</table>");
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
}
