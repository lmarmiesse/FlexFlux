package flexflux.analyses.result.conditionComparison;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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

import flexflux.analyses.result.AnalysisResult;
import flexflux.analyses.result.FVAResult;
import flexflux.analyses.result.MyTableModel;
import flexflux.condition.Condition;
import flexflux.general.Objective;

public class ConditionComparisonResult extends AnalysisResult {

	ConditionComparisonFbaResultSet fbaResults = null;
	ConditionComparisonFvaResultSet fvaResults = null;

	ArrayList<Condition> conditions = null;
	HashMap<String, String> objectives = null;

	/**
	 * Table with all results.
	 */
	private JTable resultFbaTable = null;
	/**
	 * Table sorter
	 */
	private TableRowSorter<TableModel> fbaTableSorter;

	/**
	 * Field to search for entities in the plot.
	 */
	private JTextField fbaTableSearchField;

	/**
	 * Constructor
	 * 
	 * @param conditions
	 *            : list of conditions
	 * @param objectives
	 *            : list of objectives
	 */
	public ConditionComparisonResult(ArrayList<Condition> conditions,
			HashMap<String, String> objectives) {
		fbaResults = new ConditionComparisonFbaResultSet();
		fvaResults = new ConditionComparisonFvaResultSet();
		this.conditions = conditions;
		this.objectives = objectives;

	}

	/**
	 * Adds a FBA result for a pair obj/condition
	 * 
	 * @param obj
	 * @param condition
	 * @param value
	 */
	public void addFbaResult(Objective obj, Condition condition, Double value) {

		ConditionComparisonFbaResult result = new ConditionComparisonFbaResult(
				obj, condition, value);

		fbaResults.add(result);

		return;
	}

	/**
	 * 
	 * @param obj
	 * @param condition
	 * @param fvaResult
	 */
	public void addFvaResult(Objective obj, Condition condition,
			FVAResult fvaResult) {

		ConditionComparisonFvaResult result = new ConditionComparisonFvaResult(
				obj, condition, fvaResult);

		fvaResults.add(result);

		return;
	}

	@Override
	public void writeToFile(String path) {


		writeFbaResultsToFile(path);

		writeFvaResultsToFiles(path);
		
	}

	/**
	 * 
	 * @param path
	 */
	public void writeFbaResultsToFile(String path) {

		PrintWriter out = null;

		ArrayList<String> objectiveNames = new ArrayList<String>(
				objectives.keySet());

		try {
			out = new PrintWriter(new File(path+ "fba_results"));

			// Prints the header
			out.print("ConditionCode");

			for (String objName : objectiveNames) {
				out.print("\t" + objName);
			}
			out.print("\n");

			// prints the lines corresponding to the conditions. Each cell
			// corresponds to a fba result given a condition
			// and an objective

			for (Condition c : conditions) {
				out.print(c.code);
				HashMap<String, ConditionComparisonFbaResult> results = fbaResults
						.get(c.code);

				for (String objName : objectiveNames) {
					ConditionComparisonFbaResult result = results.get(objName);
					out.print("\t" + result.value);
				}

				out.print("\n");
			}
		} catch (IOException e) {
			System.err.println("Error while writing the fba results");
			e.printStackTrace();
		}

		finally {
			if (out != null) {
				out.close();
			}
		}
	}

	
	/**
	 * 
	 * @param path
	 */
	public void writeFvaResultsToFiles(String path) {
		
		
		PrintWriter outEssential = null;
		PrintWriter outUsed = null;
		PrintWriter outDead = null;

		ArrayList<String> objectiveNames = new ArrayList<String>(
				objectives.keySet());

		try {
			outEssential = new PrintWriter(new File(path+ "essential_reactions"));
			outUsed = new PrintWriter(new File(path+ "used_reactions"));
			outDead = new PrintWriter(new File(path+ "dead_reactions"));

			// Prints the header
			outEssential.print("ConditionCode");
			outUsed.print("ConditionCode");
			outDead.print("ConditionCode");

			for (String objName : objectiveNames) {
				outEssential.print("\t" + objName);
				outUsed.print("\t" + objName);
				outDead.print("\t" + objName);
			}
			outEssential.print("\n");
			outUsed.print("\n");
			outDead.print("\n");

			// prints the lines corresponding to the conditions. Each cell
			// corresponds to a fba result given a condition
			// and an objective

			for (Condition c : conditions) {
				outEssential.print(c.code);
				outUsed.print(c.code);
				outDead.print(c.code);
				HashMap<String, ConditionComparisonFvaResult> results = fvaResults
						.get(c.code);

				for (String objName : objectiveNames) {
					
					outEssential.write("\t");
					outUsed.write("\t");
					outDead.write("\t");
					
					ConditionComparisonFvaResult result = results.get(objName);
					
					ArrayList<String> essentialReactionIds = new ArrayList<String>(result.essentialReactions.keySet());
					Collections.sort(essentialReactionIds);
					
					ArrayList<String> usedReactionIds = new ArrayList<String>(result.usedReactions.keySet());
					Collections.sort(usedReactionIds);
					
					ArrayList<String> deadReactionIds = new ArrayList<String>(result.deadReactions.keySet());
					Collections.sort(deadReactionIds);
					
					for(int i=0; i<essentialReactionIds.size();i++)
					{
						if(i>0) {
							outEssential.write(",");
						}
							
						outEssential.write(essentialReactionIds.get(i));
					}
					
					for(int i=0; i<usedReactionIds.size();i++)
					{
						if(i>0) {
							outUsed.write(",");
						}
							
						outUsed.write(usedReactionIds.get(i));
					}
					
					for(int i=0; i<deadReactionIds.size();i++)
					{
						if(i>0) {
							outDead.write(",");
						}
							
						outDead.write(deadReactionIds.get(i));
					}
					
					
				}

				outEssential.print("\n");
				outUsed.print("\n");
				outDead.print("\n");
			}
		} catch (IOException e) {
			System.err.println("Error while writing the fva results");
			e.printStackTrace();
		}

		finally {
			if (outEssential != null) {
				outEssential.close();
			}
			if (outUsed != null) {
				outUsed.close();
			}
			if (outDead != null) {
				outDead.close();
			}
		}
	}
	
	
	@Override
	public void plot() {

		ArrayList<String> columnNames = new ArrayList<String>();
		columnNames.add("conditionCode");

		ArrayList<String> objectiveNames = new ArrayList<String>(
				objectives.keySet());

		for (String objName : objectiveNames) {
			columnNames.add(objName);
		}

		Object[][] data = new Object[conditions.size()][columnNames.size()];

		int i = 0;
		for (Condition c : conditions) {
			ArrayList<String> line = new ArrayList<String>();
			line.add(c.code);

			HashMap<String, ConditionComparisonFbaResult> results = fbaResults
					.get(c.code);

			for (String objName : objectiveNames) {
				ConditionComparisonFbaResult result = results.get(objName);
				line.add(result.value.toString());
			}

			data[i] = line.toArray();

			i++;
		}

		DefaultTableModel model = new MyTableModel(data, columnNames.toArray());

		resultFbaTable = new JTable();

		resultFbaTable.setModel(model);

		fbaTableSorter = new TableRowSorter<TableModel>(
				resultFbaTable.getModel());
		resultFbaTable.setRowSorter(fbaTableSorter);

		JPanel northPanel = new JPanel();
		northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.PAGE_AXIS));

		JPanel centerPanel = new JPanel();
		centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.PAGE_AXIS));

		JPanel searchPanel = new JPanel(new FlowLayout());
		searchPanel.add(new JLabel("Search for an entity : "));

		MyDocumentListener mdl = new MyDocumentListener();
		// name search
		fbaTableSearchField = new JTextField(10);
		fbaTableSearchField.getDocument().addDocumentListener(mdl);
		searchPanel.add(fbaTableSearchField);

		northPanel.add(searchPanel);

		centerPanel.add(new JScrollPane(resultFbaTable));

		JFrame frame = new JFrame("Objectives/conditions FBA analysis");

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
		String text = fbaTableSearchField.getText();

		if (sorter.getModelRowCount() != 0) {

			List<RowFilter<TableModel, Object>> filters = new ArrayList<RowFilter<TableModel, Object>>();

			RowFilter<TableModel, Object> firstFilter = null;

			// case insensitive
			if (text.length() != 0) {
				firstFilter = RowFilter.regexFilter(
						"(?i)" + Pattern.quote(text), 0);
			}

			if (firstFilter != null) {
				filters.add(firstFilter);
			}
			sorter.setRowFilter(RowFilter.andFilter(filters));

		}

	}

	/**
	 * 
	 * @author lcottret
	 * 
	 */
	class MyDocumentListener implements DocumentListener {
		String newline = "\n";

		public void changedUpdate(DocumentEvent arg0) {
			updateTable(fbaTableSorter, arg0);
		}

		public void insertUpdate(DocumentEvent arg0) {
			updateTable(fbaTableSorter, arg0);
		}

		public void removeUpdate(DocumentEvent arg0) {
			updateTable(fbaTableSorter, arg0);
		}
	}

}
