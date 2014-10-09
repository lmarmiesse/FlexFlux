package flexflux.analyses.result;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
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

import parsebionet.biodata.BioEntity;
import flexflux.general.Constraint;

public class SteadyStateAnalysisResult extends AnalysisResult {

	/**
	 * Field to search for entities in the plot.
	 */
	private JTextField searchField;

	private JTable resultTable;

	private List<Map<BioEntity, Integer>> statesList = new ArrayList<Map<BioEntity, Integer>>();

	private List<Constraint> finalConstraints = new ArrayList<Constraint>();

	private Set<BioEntity> resultEntities = new HashSet<BioEntity>();

	public void addResultEntity(BioEntity ent) {
		resultEntities.add(ent);
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

	@Override
	public void writeToFile(String path) {

		try {
			PrintWriter out = new PrintWriter(new File(path));
			out.println("Steady state analysis result\n");

			for (BioEntity ent : resultEntities) {

				out.print(ent.getId());
				for (int j = 0; j < statesList.size(); j++) {

					if (statesList.get(j).containsKey(ent)) {

						out.print("\t" + statesList.get(j).get(ent));
					} else {
						out.print("\t" + "?");
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

		JFrame frame = new JFrame("Steady state analysis results");

		resultTable = new JTable(0, statesList.size() + 1);

		String[] columnNames = new String[statesList.size() + 1];

		columnNames[0] = "Entity name";

		for (int i = 1; i <= statesList.size(); i++) {
			columnNames[i] = String.valueOf(i);
		}

		Object[][] data = new Object[resultEntities.size()][columnNames.length];

		int i = 0;
		for (BioEntity ent : resultEntities) {

			Object[] entValues = new Object[statesList.size() + 1];

			entValues[0] = ent.getId();
			for (int j = 0; j < statesList.size(); j++) {

				if (statesList.get(j).containsKey(ent)) {
					entValues[j + 1] = statesList.get(j).get(ent);
				}else
				{
					entValues[j + 1] = "?";
				}

			}

			data[i] = entValues;
			i++;

		}

		DefaultTableModel model = new MyTableModel(data, columnNames);
		resultTable.setModel(model);

		final MyTableRowSorter<TableModel> sorter = new MyTableRowSorter<TableModel>(
				resultTable.getModel());

		resultTable.setRowSorter(sorter);

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
			sorter.setRowFilter(RowFilter.regexFilter(
					"(?i)" + Pattern.quote(text), 0));
		}

	}

}
