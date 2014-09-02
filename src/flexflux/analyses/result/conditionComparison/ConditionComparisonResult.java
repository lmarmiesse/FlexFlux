package flexflux.analyses.result.conditionComparison;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.File;
import java.io.FileNotFoundException;
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

import parsebionet.biodata.BioNetwork;
import flexflux.analyses.result.AnalysisResult;
import flexflux.analyses.result.FVAResult;
import flexflux.analyses.result.KOResult;
import flexflux.analyses.result.MyTableModel;
import flexflux.condition.Condition;
import flexflux.general.Objective;
import flexflux.io.Utils;

public class ConditionComparisonResult extends AnalysisResult {

	public ConditionComparisonFbaResultSet fbaResults = null;
	public ConditionComparisonFvaResultSet fvaResults = null;
	public ConditionComparisonKoResultSet koResults = null;
	public ConditionComparisonGeneResultSet geneResults = null;

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

	private BioNetwork network;

	/**
	 * Constructor
	 * 
	 * @param conditions
	 *            : list of conditions
	 * @param objectives
	 *            : list of objectives
	 */
	public ConditionComparisonResult(ArrayList<Condition> conditions,
			HashMap<String, String> objectives, BioNetwork network) {
		fbaResults = new ConditionComparisonFbaResultSet();
		fvaResults = new ConditionComparisonFvaResultSet();
		koResults = new ConditionComparisonKoResultSet();
		geneResults = new ConditionComparisonGeneResultSet();

		this.conditions = conditions;
		this.objectives = objectives;

		this.network = network;

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

	/**
	 * 
	 * @param obj
	 * @param condition
	 * @param fvaResult
	 */
	public void addKoResult(Objective obj, Condition condition,
			KOResult koResult) {

		ConditionComparisonKoResult result = new ConditionComparisonKoResult(
				obj, condition, koResult);

		koResults.add(result);

		return;
	}

	/**
	 * 
	 * @param obj
	 * @param condition
	 * @param koResult
	 * @param fvaResult
	 */
	public void addGeneResult(Objective obj, Condition condition,
			ConditionComparisonKoResult koResult,
			ConditionComparisonFvaResult fvaResult) {

		ConditionComparisonGeneResult result = new ConditionComparisonGeneResult(
				obj, condition, koResult, fvaResult, network);

		geneResults.add(result);

		return;

	}

	@Override
	public void writeToFile(String path) {

		// First create the directory if it does not exist
		File theDir = new File(path);

		if (!theDir.exists()) {
			try {
				theDir.mkdir();
			} catch (SecurityException se) {
				se.printStackTrace();
				System.err.println("Security Exception during creation of "
						+ path);
			}
		}

		writeFbaResultsToFile(path);

		writeFvaResultsToFiles(path);

		 writeGeneResultsToFiles(path);

		writeSummaryReactionFile(path);

		writeD3Files(path);

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
			out = new PrintWriter(new File(path + "/fba_results"));

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
		PrintWriter outDispensable = null;
		PrintWriter outDead = null;

		ArrayList<String> objectiveNames = new ArrayList<String>(
				objectives.keySet());

		try {
			outEssential = new PrintWriter(new File(path
					+ "/essential_reactions"));
			outDispensable = new PrintWriter(new File(path
					+ "/dispensable_reactions"));
			outDead = new PrintWriter(new File(path + "/dead_reactions"));

			// Prints the header
			outEssential.print("ConditionCode");
			outDispensable.print("ConditionCode");
			outDead.print("ConditionCode");

			for (String objName : objectiveNames) {
				outEssential.print("\t" + objName);
				outDispensable.print("\t" + objName);
				outDead.print("\t" + objName);
			}
			outEssential.print("\n");
			outDispensable.print("\n");
			outDead.print("\n");

			// prints the lines corresponding to the conditions. Each cell
			// corresponds to a fba result given a condition
			// and an objective

			for (Condition c : conditions) {
				outEssential.print(c.code);
				outDispensable.print(c.code);
				outDead.print(c.code);
				HashMap<String, ConditionComparisonFvaResult> results = fvaResults
						.get(c.code);

				for (String objName : objectiveNames) {

					outEssential.write("\t");
					outDispensable.write("\t");
					outDead.write("\t");

					ConditionComparisonFvaResult result = results.get(objName);

					ArrayList<String> essentialReactionIds = new ArrayList<String>(
							result.essentialReactions.keySet());
					Collections.sort(essentialReactionIds);

					ArrayList<String> usedReactionIds = new ArrayList<String>(
							result.dispensableReactions.keySet());
					Collections.sort(usedReactionIds);

					ArrayList<String> deadReactionIds = new ArrayList<String>(
							result.deadReactions.keySet());
					Collections.sort(deadReactionIds);

					for (int i = 0; i < essentialReactionIds.size(); i++) {
						if (i > 0) {
							outEssential.write(",");
						}

						outEssential.write(essentialReactionIds.get(i));
					}

					for (int i = 0; i < usedReactionIds.size(); i++) {
						if (i > 0) {
							outDispensable.write(",");
						}

						outDispensable.write(usedReactionIds.get(i));
					}

					for (int i = 0; i < deadReactionIds.size(); i++) {
						if (i > 0) {
							outDead.write(",");
						}

						outDead.write(deadReactionIds.get(i));
					}

				}

				outEssential.print("\n");
				outDispensable.print("\n");
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
			if (outDispensable != null) {
				outDispensable.close();
			}
			if (outDead != null) {
				outDead.close();
			}
		}
	}

	/**
	 * 
	 * @param path
	 */
	public void writeGeneResultsToFiles(String path) {

		PrintWriter outEssential = null;
		PrintWriter outDispensable = null;
		PrintWriter outDead = null;

		ArrayList<String> objectiveNames = new ArrayList<String>(
				objectives.keySet());

		try {
			outEssential = new PrintWriter(new File(path + "/essential_genes"));
			outDispensable = new PrintWriter(new File(path
					+ "/dispensable_genes"));
			outDead = new PrintWriter(new File(path + "/dead_genes"));

			// Prints the header
			outEssential.print("ConditionCode");
			outDispensable.print("ConditionCode");
			outDead.print("ConditionCode");

			for (String objName : objectiveNames) {
				outEssential.print("\t" + objName);
				outDispensable.print("\t" + objName);
				outDead.print("\t" + objName);
			}
			outEssential.print("\n");
			outDispensable.print("\n");
			outDead.print("\n");

			// prints the lines corresponding to the conditions. Each cell
			// corresponds to a fba result given a condition
			// and an objective

			for (Condition c : conditions) {
				outEssential.print(c.code);
				outDispensable.print(c.code);
				outDead.print(c.code);
				HashMap<String, ConditionComparisonGeneResult> results = geneResults
						.get(c.code);

				for (String objName : objectiveNames) {

					outEssential.write("\t");
					outDispensable.write("\t");
					outDead.write("\t");

					ConditionComparisonGeneResult result = results.get(objName);

					ArrayList<String> essentialGeneIds = new ArrayList<String>(
							result.essentialGenes.keySet());
					Collections.sort(essentialGeneIds);

					ArrayList<String> dispensableGeneIds = new ArrayList<String>(
							result.dispensableGenes.keySet());
					Collections.sort(dispensableGeneIds);

					ArrayList<String> deadGeneIds = new ArrayList<String>(
							result.deadGenes.keySet());
					Collections.sort(deadGeneIds);

					for (int i = 0; i < essentialGeneIds.size(); i++) {
						if (i > 0) {
							outEssential.write(",");
						}

						outEssential.write(essentialGeneIds.get(i));
					}

					for (int i = 0; i < dispensableGeneIds.size(); i++) {
						if (i > 0) {
							outDispensable.write(",");
						}

						outDispensable.write(dispensableGeneIds.get(i));
					}

					for (int i = 0; i < deadGeneIds.size(); i++) {
						if (i > 0) {
							outDead.write(",");
						}

						outDead.write(deadGeneIds.get(i));
					}

				}

				outEssential.print("\n");
				outDispensable.print("\n");
				outDead.print("\n");
			}
		} catch (IOException e) {
			System.err.println("Error while writing the gene results");
			e.printStackTrace();
		}

		finally {
			if (outEssential != null) {
				outEssential.close();
			}
			if (outDispensable != null) {
				outDispensable.close();
			}
			if (outDead != null) {
				outDead.close();
			}
		}
	}

	/**
	 * Write a tabulated file with the number of reactions by type
	 */
	public void writeSummaryReactionFile(String path) {
		PrintWriter out = null;

		ArrayList<String> objectiveNames = new ArrayList<String>(
				objectives.keySet());

		try {
			out = new PrintWriter(new File(path + "/summary_reactions"));

			out.write("name,essential,dispensable,dead\n");

			for (Condition c : conditions) {
				HashMap<String, ConditionComparisonFvaResult> results = fvaResults
						.get(c.code);
				for (String objName : objectiveNames) {

					ConditionComparisonFvaResult result = results.get(objName);

					int nbEssential = result.essentialReactions.size();
					int nbDispensable = result.dispensableReactions.size();
					int nbDead = result.deadReactions.size();

					out.write(c.code + "__" + objName + "," + nbEssential + ","
							+ nbDispensable + "," + nbDead + "\n");

				}

			}

		} catch (IOException e) {
			System.err
					.println("Error while writing the reaction summary results");
			e.printStackTrace();
		}

		finally {
			if (out != null) {
				out.close();
			}
		}

	}

	/**
	 * Create web architecture to display barplots with the D3 library
	 * 
	 * @throws IOException
	 */
	public void writeD3Files(String directoryPath) {

		// Create directories
		String webPath = directoryPath + "/" + "web";
		File webFile = new File(webPath);

		if (!webFile.exists()) {
			try {
				webFile.mkdir();
			} catch (SecurityException se) {
				se.printStackTrace();
				System.err.println("Security Exception during creation of "
						+ webPath);
			}
		}
		String reactionPath = webPath + "/" + "reactions";
		File reactionFile = new File(reactionPath);

		if (!reactionFile.exists()) {
			try {
				reactionFile.mkdir();
			} catch (SecurityException se) {
				se.printStackTrace();
				System.err.println("Security Exception during creation of "
						+ reactionPath);
			}
		}
		String jsPath = webPath + "/" + "js";
		File jsFile = new File(jsPath);

		if (!jsFile.exists()) {
			try {
				jsFile.mkdir();
			} catch (SecurityException se) {
				se.printStackTrace();
				System.err.println("Security Exception during creation of "
						+ jsPath);
			}
		}
		
		String cssPath = webPath + "/" + "css";
		File cssFile = new File(cssPath);
		
		if (!cssFile.exists()) {
			try {
				cssFile.mkdir();
			} catch (SecurityException se) {
				se.printStackTrace();
				System.err.println("Security Exception during creation of "
						+ cssPath);
			}
		}
		

		// Copy required files
		try {
			Utils.copyProjectResource(
					"flexflux/data/web/templates/multiBar/multiBar.html",
					reactionPath, "summaryReactions.html");
			Utils.copyProjectResource(
					"flexflux/data/web/templates/multiBar/multiBar.js",
					reactionPath, "multiBar.js");
			Utils.copyProjectResource("flexflux/data/web/js/d3.v3.js", jsPath,
					"d3.v3.js");
			Utils.copyProjectResource("flexflux/data/web/js/nv.d3.js", jsPath,
					"nv.d3.js");
			Utils.copyProjectResource("flexflux/data/web/css/nv.d3.css",
					cssPath, "nv.d3.css");
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Error while copying web files");
			return;
		}

		// Create js files with data inside
		ArrayList<String> objectiveNames = new ArrayList<String>(
				objectives.keySet());
		PrintWriter out = null;
		try {
			out = new PrintWriter(new File(reactionPath + "/data.js"));
			out.write("var str = \"name,essential,dispensable,dead\\n");

			for (Condition c : conditions) {
				HashMap<String, ConditionComparisonFvaResult> results = fvaResults
						.get(c.code);
				for (String objName : objectiveNames) {

					ConditionComparisonFvaResult result = results.get(objName);

					int nbEssential = result.essentialReactions.size();
					int nbDispensable = result.dispensableReactions.size();
					int nbDead = result.deadReactions.size();

					out.write(c.code + "__" + objName + "," + nbEssential + ","
							+ nbDispensable + "," + nbDead + "\\n");

				}
			}

			out.write("\"\n");

		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.err.println("Error while creating web data");
			return;
		}

		finally {
			if (out != null) {
				out.close();
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
