package flexflux.analyses.result.conditionComparison;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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

import parsebionet.biodata.BioChemicalReaction;
import parsebionet.biodata.BioNetwork;
import parsebionet.biodata.BioPathway;
import flexflux.analyses.result.AnalysisResult;
import flexflux.analyses.result.MyTableModel;
import flexflux.analyses.result.PFBAResult;
import flexflux.condition.Condition;
import flexflux.general.Objective;
import flexflux.io.Utils;

public class ConditionComparisonResult extends AnalysisResult {

	HashMap<String, HashMap<String, PFBAResult>> results = null;

	public ConditionComparisonFbaResultSet fbaResults = null;

	ArrayList<Condition> conditions = null;
	HashMap<String, String> objectives = null;

	public HashMap<String, HashMap<String, String>> reactionMetaData = null;
	public HashMap<String, HashMap<String, String>> geneMetaData = null;

	public Set<String> interactionTargets;

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

	private String directoryPath = ".";
	private String webPath;
	private String summaryPath;
	private String cssPath;
	private String reactionPath;
	private String jsPath;
	private String genePath;
	private String pathwayPath;

	private String inchlibPath;

	public Boolean launchReactionAnalysis;
	public Boolean launchGeneAnalysis;

	/**
	 * Constructor
	 * 
	 * @param conditions
	 *            : list of conditions
	 * @param objectives
	 *            : list of objectives
	 */
	public ConditionComparisonResult(ArrayList<Condition> conditions,
			HashMap<String, String> objectives, BioNetwork network,
			String inchlibPath, Boolean launchReactionAnalysis,
			Boolean launchGeneAnalysis) {

		results = new HashMap<String, HashMap<String, PFBAResult>>();

		fbaResults = new ConditionComparisonFbaResultSet();

		this.inchlibPath = inchlibPath;

		this.conditions = conditions;
		this.objectives = objectives;

		this.network = network;

		// Sets the interaction targets
		this.interactionTargets = new HashSet<String>();

		this.launchGeneAnalysis = launchGeneAnalysis;
		this.launchReactionAnalysis = launchReactionAnalysis;

	}

	/**
	 * Add a result
	 * 
	 * @param o
	 *            objective
	 * @param condition
	 * @param res
	 *            a pfba result
	 */
	public void addPFBAResult(Objective o, Condition condition, PFBAResult res) {
		String conditionId = condition.code;
		String objId = o.getName();

		if (!results.containsKey(conditionId)) {
			results.put(conditionId, new HashMap<String, PFBAResult>());
		}

		results.get(conditionId).put(objId, res);
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

	@Override
	public void writeToFile(String path) {

		this.directoryPath = path;

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

		this.createWebDirectories(path);

		if (launchGeneAnalysis || launchReactionAnalysis) {

			// Copy nvd3 required files
			// Copy required files
			try {
				Utils.copyProjectResource("flexflux/data/web/js/d3.v3.js",
						jsPath, "d3.v3.js");
				Utils.copyProjectResource("flexflux/data/web/js/nv.d3.js",
						jsPath, "nv.d3.js");
				Utils.copyProjectResource("flexflux/data/web/css/nv.d3.css",
						cssPath, "nv.d3.css");
			} catch (Exception e) {
				e.printStackTrace();
				System.err.println("Error while copying web files");
				return;
			}
		}

		if (launchReactionAnalysis) {
			this.reactionPath = webPath + "/" + "reactions";
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

			this.pathwayPath = webPath + "/" + "pathways";
			File pathwayfile = new File(pathwayPath);

			if (!pathwayfile.exists()) {
				try {
					pathwayfile.mkdir();
				} catch (SecurityException se) {
					se.printStackTrace();
					System.err.println("Security Exception during creation of "
							+ pathwayPath);
				}
			}

		}

		if (launchGeneAnalysis) {

			this.genePath = webPath + "/" + "genes";
			File geneFile = new File(genePath);

			if (!geneFile.exists()) {
				try {
					geneFile.mkdir();
				} catch (SecurityException se) {
					se.printStackTrace();
					System.err.println("Security Exception during creation of "
							+ genePath);
				}
			}

		}

		writeFbaResultsToFile();
		writeFbaResultHeatMap();

		if (launchGeneAnalysis) {
			writeSummaryFile(false);
			writeClassificationToFiles(false);
			writeBarplot(false);
			writeFilesForHeatMap(false);
		}

		if (launchReactionAnalysis) {
			writeClassificationToFiles(true);
			writeSummaryFile(true);
			writeBarplot(true);
			writeFilesForHeatMap(true);
			writePathwayHeatMap();
		}

		this.writeFbaResultHeatMap();

	}

	/**
	 * 
	 * @param path
	 */
	public void writeFbaResultsToFile() {

		String path = this.directoryPath;

		PrintWriter out = null;

		ArrayList<String> objectiveNames = new ArrayList<String>(
				objectives.keySet());

		try {
			out = new PrintWriter(new File(path + "/fba_results.csv"));

			// Prints the header
			out.print("ConditionCode");

			for (String objName : objectiveNames) {
				out.print("," + objName);
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
					out.print("," + result.value);
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
	 * @param isReaction
	 *            : if true print the reaction classification, if false prints
	 *            the gene classification
	 */
	public void writeClassificationToFiles(Boolean isReaction) {

		String objectName = "Reactions";
		if (isReaction == false) {
			objectName = "Genes";
		}

		String path = this.directoryPath;

		PrintWriter outEssential = null;
		PrintWriter outZeroFlux = null;
		PrintWriter outMle = null;
		PrintWriter outEle = null;
		PrintWriter outConcurrent = null;
		PrintWriter outIndependent = null;
		PrintWriter outOptima = null;

		HashMap<String, PrintWriter> writers = new HashMap<String, PrintWriter>();

		HashMap<String, ArrayList<String>> classification = new HashMap<String, ArrayList<String>>();

		ArrayList<String> objectiveNames = new ArrayList<String>(
				objectives.keySet());

		try {
			outEssential = new PrintWriter(new File(path + "/essential"
					+ objectName + ".tsv"));
			outZeroFlux = new PrintWriter(new File(path + "/zeroFlux"
					+ objectName + ".tsv"));
			outMle = new PrintWriter(new File(path + "/mle" + objectName
					+ ".tsv"));
			outEle = new PrintWriter(new File(path + "/ele" + objectName
					+ ".tsv"));
			outConcurrent = new PrintWriter(new File(path + "/concurrent"
					+ objectName + ".tsv"));
			outIndependent = new PrintWriter(new File(path + "/independent"
					+ objectName + ".tsv"));
			outOptima = new PrintWriter(new File(path + "/optima" + objectName
					+ ".tsv"));

			writers.put("ess", outEssential);
			writers.put("zf", outZeroFlux);
			writers.put("mle", outMle);
			writers.put("ele", outEle);
			writers.put("conc", outConcurrent);
			writers.put("ind", outIndependent);
			writers.put("opt", outOptima);

			// Prints the headers
			for (PrintWriter out : writers.values()) {
				out.print("ConditionCode");
				for (String objName : objectiveNames) {
					out.print("\t" + objName);
				}
				out.print("\n");
			}

			// prints the lines corresponding to the conditions. Each cell
			// corresponds to a fba result given a condition
			// and an objective

			for (Condition c : conditions) {
				for (PrintWriter out : writers.values()) {
					out.print(c.code);
				}

				HashMap<String, PFBAResult> pfbaResults = results.get(c.code);

				for (String objName : objectiveNames) {

					for (PrintWriter out : writers.values()) {
						out.print("\t");
					}

					PFBAResult result = pfbaResults.get(objName);

					ArrayList<String> essentialIds = new ArrayList<String>(
							result.get("essential" + objectName).keySet());
					Collections.sort(essentialIds);
					classification.put("ess", essentialIds);

					ArrayList<String> zeroFluxIds = new ArrayList<String>(
							result.get("zeroFlux" + objectName).keySet());
					Collections.sort(zeroFluxIds);
					classification.put("zf", zeroFluxIds);

					ArrayList<String> mleIds = new ArrayList<String>(result
							.get("mle" + objectName).keySet());
					Collections.sort(mleIds);
					classification.put("mle", mleIds);

					ArrayList<String> concurrentIds = new ArrayList<String>(
							result.get("concurrent" + objectName).keySet());
					Collections.sort(concurrentIds);
					classification.put("conc", concurrentIds);

					ArrayList<String> eleIds = new ArrayList<String>(result
							.get("ele" + objectName).keySet());
					Collections.sort(eleIds);
					classification.put("ele", eleIds);

					ArrayList<String> objectiveIndependentIds = new ArrayList<String>(
							result.get("objectiveIndependent" + objectName)
									.keySet());
					Collections.sort(objectiveIndependentIds);
					classification.put("ind", objectiveIndependentIds);

					ArrayList<String> optimaIds = new ArrayList<String>(result
							.get("optima" + objectName).keySet());
					Collections.sort(optimaIds);
					classification.put("opt", optimaIds);

					for (String key : writers.keySet()) {
						PrintWriter out = writers.get(key);
						ArrayList<String> ids = classification.get(key);
						for (int i = 0; i < ids.size(); i++) {
							if (i > 0) {
								out.write(",");
							}
							out.write(ids.get(i));
						}
					}
				}

				for (PrintWriter out : writers.values()) {
					out.print("\n");
				}
			}
		} catch (IOException e) {
			System.err
					.println("Error while writing the classification results");
			e.printStackTrace();
		}

		finally {
			for (PrintWriter out : writers.values()) {
				if (out != null) {
					out.close();
				}
			}
		}
	}

	/**
	 * Write a tabulated file with the number of reactions by type
	 */
	public void writeFbaResultHeatMap() {

		try {
			Utils.copyProjectResource(
					"flexflux/data/web/templates/heatmap/heatmap_independentColumns.html",
					summaryPath, "fba_results.html");
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Error while copying fba_results.html");
			return;
		}

		// copy html template
		// Build inchlib cmd
		if (inchlibPath != "") {
			if (inchlibPath.contains(" ") || !inchlibPath.contains("inchlib")
					|| !inchlibPath.endsWith(".py")) {
				System.err.println("Inchlib command not valid");
				return;
			}
			File f = new File(inchlibPath);
			if (!f.exists() || f.isDirectory()) {
				System.err.println("The python file " + inchlibPath
						+ " does not exist");
				return;
			}

			String jsonFile = this.summaryPath + "/fba_results.json";
			String jsFile = this.summaryPath + "/heatmap_data.js";

			String cmd = "python " + inchlibPath + " " + this.directoryPath
					+ "/fba_results.csv" + " -dh -mh -a both -o " + jsonFile;

			try {
				this.runInchlib(cmd);
				this.jsonToJs(jsonFile, jsFile);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.err.println("Problem while running inchlib");
			}

		}

	}

	/**
	 * Write a tabulated file with the number of genes by type
	 */

	public void writeSummaryFile(Boolean isReaction) {
		String objectName = "Reactions";
		if (isReaction == false) {
			objectName = "Genes";
		}

		String path = this.directoryPath;

		PrintWriter out = null;

		ArrayList<String> objectiveNames = new ArrayList<String>(
				objectives.keySet());

		try {
			out = new PrintWriter(new File(path + "/summary" + objectName));

			out.write("name,essential,zeroFlux,mle,ele,conc,ind,opt\n");

			for (Condition c : conditions) {
				HashMap<String, PFBAResult> pfbaResults = results.get(c.code);

				for (String objName : objectiveNames) {

					PFBAResult result = pfbaResults.get(objName);

					int nbEssential = result.get("essential" + objectName)
							.size();
					int nbZeroFlux = result.get("zeroFlux" + objectName).size();
					int nbMle = result.get("mle" + objectName).size();
					int nbEle = result.get("ele" + objectName).size();
					int nbConc = result.get("concurrent" + objectName).size();
					int nbInd = result.get("objectiveIndependent" + objectName)
							.size();
					int nbOpt = result.get("optima" + objectName).size();

					out.write(c.code + "__" + objName + "," + nbEssential + ","
							+ nbZeroFlux + "," + nbMle + "," + nbEle + ","
							+ nbConc + "," + nbInd + "," + nbOpt + "\n");

				}

			}

		} catch (IOException e) {
			System.err.println("Error while writing the summary results");
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
	public void writeBarplot(Boolean isReaction) {

		String outPath = genePath;

		String objectName = "Genes";

		if (isReaction) {
			outPath = reactionPath;
			objectName = "Reactions";
		}

		// Copy required files
		try {
			Utils.copyProjectResource(
					"flexflux/data/web/templates/multiBar/multiBar.html",
					outPath, "summary.html");
			Utils.copyProjectResource(
					"flexflux/data/web/templates/multiBar/multiBar.js",
					outPath, "multiBar.js");
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
			out = new PrintWriter(new File(outPath + "/multiBar_data.js"));
			out.write("var str = \"name,essential,zeroFlux,mle,ele,conc,ind,opt\\n");

			for (Condition c : conditions) {

				HashMap<String, PFBAResult> pfbaResults = results.get(c.code);

				for (String objName : objectiveNames) {

					PFBAResult result = pfbaResults.get(objName);

					int nbEssential = result.get("essential" + objectName)
							.size();
					int nbZeroFlux = result.get("zeroFlux" + objectName).size();
					int nbMle = result.get("mle" + objectName).size();
					int nbEle = result.get("ele" + objectName).size();
					int nbConc = result.get("concurrent" + objectName).size();
					int nbInd = result.get("objectiveIndependent" + objectName)
							.size();
					int nbOpt = result.get("optima" + objectName).size();

					out.write(c.code + "__" + objName + "," + nbEssential + ","
							+ nbZeroFlux + "," + nbMle + "," + nbEle + ","
							+ nbConc + "," + nbInd + "," + nbOpt + "\\n");
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

	/**
	 * Write the files that will be used to generate heatmap with inCHlib
	 * 
	 * Launch this command to create the html page called inchlib.html that
	 * contains the heatmap python inchlib_clust.py reactionsVsConditions.csv -m
	 * reactionsMetaData.csv -dh -mh -a both -html htmlPath
	 * 
	 * Be careful, to have the same color code in the whole heatmap, the
	 * inchlib_clust.py template has been transformed to integrate the parameter
	 * independent_columns: false
	 * 
	 * @param directoryPath
	 *            : global result path
	 * @param isReaction
	 *            : if true, builds heatmap for reactions, otherwise for genes
	 */
	public void writeFilesForHeatMap(Boolean isReaction) {

		String objectName = "Reactions";

		if (!isReaction) {
			objectName = "Genes";
		}

		PrintWriter outData = null;
		PrintWriter outMetaData = null;
		ArrayList<String> objectiveNames = new ArrayList<String>(
				objectives.keySet());

		String outPath = genePath;
		HashMap<String, HashMap<String, String>> metaData = geneMetaData;
		Set<String> ids = network.getGeneList().keySet();

		Set<String> chokes = new HashSet<String>();

		if (isReaction) {
			outPath = reactionPath;
			metaData = reactionMetaData;
			ids = network.getBiochemicalReactionList().keySet();
			chokes = network.getChokeReactions();
		}

		try {
			Utils.copyProjectResource(
					"flexflux/data/web/templates/heatmap/heatmap.html",
					outPath, "heatmap.html");
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Error while copying heatmap.html");
			return;
		}

		// Create the data and metadata files
		try {
			outData = new PrintWriter(new File(outPath + "/heatMapData.csv"));
			outMetaData = new PrintWriter(new File(outPath
					+ "/heatMapMetaData.csv"));

			// Prints the header
			outData.write("id");

			for (Condition c : conditions) {
				for (String objName : objectiveNames) {
					outData.write("," + c.code + "__" + objName);
				}
			}
			outData.write("\n");

			/**
			 * Header for metadata file
			 */
			if (isReaction) {
				outMetaData.write("id,pathway,nbEnzymes,choke");
			} else {
				outMetaData.write("id,nbReactions,interactionTarget");
			}

			ArrayList<String> additionalMetaDataColumns = new ArrayList<String>();

			if (metaData != null && metaData.size() != 0) {
				additionalMetaDataColumns = new ArrayList<String>(
						metaData.keySet());

				for (String col : additionalMetaDataColumns) {
					outMetaData.write("," + col);
				}

			}

			outMetaData.write("\n");

			for (String id : ids) {

				outMetaData.write(id);

				if (isReaction) {
					BioChemicalReaction reaction = network
							.getBiochemicalReactionList().get(id);
					HashMap<String, BioPathway> pathways = reaction
							.getPathwayList();
					// Build pathway string
					ArrayList<String> pathwayArray = new ArrayList<String>(
							pathways.keySet());
					Collections.sort(pathwayArray);
					String pathwayStr = "";

					if (reaction.isExchangeReaction()) {
						pathwayStr = "Exchange";
					} else {
						for (int i = 0; i < pathwayArray.size(); i++) {
							if (i != 0) {
								pathwayStr += "__";
							} else {
								pathwayStr += pathwayArray.get(i);
							}
						}
					}

					outMetaData.write("," + pathwayStr + ",");

					// Prints the number of enzymes
					outMetaData.write(Integer.toString(reaction.getEnzList()
							.size()));

					// Checks if the reaction is a choke reaction
					String choke = "-";
					if (chokes.contains(id)) {
						choke = "+";
					}
					outMetaData.write("," + choke);

				} else {
					// It's a gene

					/**
					 * number of reactions in which the gene is involved
					 */
					int nbReactions = network.getReactionsFromGene(id).size();
					outMetaData.write("," + Integer.toString(nbReactions));

					/**
					 * Checks if the gene is a target of the interaction network
					 */
					String target = "-";

					if (this.interactionTargets.contains(id)) {
						target = "+";
					}

					outMetaData.write("," + target);

				}

				for (String col : additionalMetaDataColumns) {
					String value = "NA";
					if (metaData.get(col).containsKey(id)) {
						value = metaData.get(col).get(id);
					}
					outMetaData.write("," + value);
				}

				outMetaData.write("\n");

				/**
				 * Prints the values in the data file
				 */
				outData.write(id);
				for (Condition c : conditions) {

					HashMap<String, PFBAResult> pfbaResults = results
							.get(c.code);

					for (String objName : objectiveNames) {
						PFBAResult result = pfbaResults.get(objName);

						int value = 0;

						if (result.get("essential" + objectName)
								.containsKey(id)) {
							value = 7;
						} else if (result.get("optima" + objectName)
								.containsKey(id)) {
							value = 6;
						} else if (result.get("ele" + objectName).containsKey(
								id)) {
							value = 5;
						} else if (result.get("mle" + objectName).containsKey(
								id)) {
							value = 4;
						} else if (result.get("concurrent" + objectName)
								.containsKey(id)) {
							value = 3;
						} else if (result.get(
								"objectiveIndependent" + objectName)
								.containsKey(id)) {
							value = 2;
						} else if (result.get("zeroFlux" + objectName)
								.containsKey(id)) {
							value = 1;
						}
						outData.write("," + value);
					}
				}
				outData.write("\n");
			}

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.err
					.println("Error while creating the data files for clustering");
			return;
		}

		finally {
			if (outData != null) {
				outData.close();
			}
			if (outMetaData != null) {
				outMetaData.close();
			}
		}

		// Build inchlib cmd
		if (inchlibPath != "") {
			if (inchlibPath.contains(" ") || !inchlibPath.contains("inchlib")
					|| !inchlibPath.endsWith(".py")) {
				System.err.println("Inchlib command not valid");
				return;
			}
			File f = new File(inchlibPath);
			if (!f.exists() || f.isDirectory()) {
				System.err.println("The python file " + inchlibPath
						+ " does not exist");
				return;
			}

			String jsonFile = outPath + "/heatmap_data.json";
			String jsFile = outPath + "/heatmap_data.js";

			String cmd = "python " + inchlibPath + " " + outPath
					+ "/heatMapData.csv -m " + outPath + "/heatMapMetaData.csv"
					+ " -dh -mh -a both -o " + jsonFile;

			try {
				this.runInchlib(cmd);
				this.jsonToJs(jsonFile, jsFile);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.err.println("Problem while running inchlib");
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

	/**
	 * Run the inchlib command
	 * 
	 * @return
	 * @throws IOException
	 */
	public Boolean runInchlib(String inchlibCmd) throws IOException {

		Process p = null;
		try {
			p = Runtime.getRuntime().exec(inchlibCmd);
			String lineError;
			BufferedReader bre = new BufferedReader(new InputStreamReader(
					p.getErrorStream()));
			while ((lineError = bre.readLine()) != null) {
				System.err.println(lineError);
			}
			p.waitFor();
		} catch (IOException e) {
			System.err.println("Error in launching the command " + inchlibCmd);
			e.printStackTrace();
			return false;
		} catch (InterruptedException e) {
			System.err.println("Interruption of the command " + inchlibCmd);
			e.printStackTrace();
			return false;
		} finally {
			if (p != null) {
				if (p.getOutputStream() != null)
					p.getOutputStream().close();
				if (p.getInputStream() != null)
					p.getInputStream().close();
				if (p.getErrorStream() != null)
					p.getErrorStream().close();
			}
		}

		return true;

	}

	/**
	 * Create required web directories and Files
	 * 
	 * @param path
	 */
	private void createWebDirectories(String path) {

		// Create web directories
		this.webPath = directoryPath + "/" + "web";
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

		this.summaryPath = this.webPath + "/summary";

		File summaryDir = new File(this.summaryPath);

		if (!summaryDir.exists()) {
			try {
				summaryDir.mkdir();
			} catch (SecurityException se) {
				se.printStackTrace();
				System.err.println("Security Exception during creation of "
						+ this.summaryPath);
			}
		}

		this.jsPath = webPath + "/" + "js";
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

		this.cssPath = webPath + "/" + "css";
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

		// copy summary.html in index.html
		try {
			Utils.copyProjectResource(
					"flexflux/data/web/templates/summary.html", webPath,
					"index.html");
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Error while copying web files");
			return;
		}

		// copy heatmap javascript files.
		try {
			Utils.copyProjectResource(
					"flexflux/data/web/js/inchlib-1.0.1.min.js", jsPath,
					"inchlib-1.0.1.min.js");
			Utils.copyProjectResource(
					"flexflux/data/web/js/jquery-2.0.3.min.js", jsPath,
					"jquery-2.0.3.min.js");
			Utils.copyProjectResource(
					"flexflux/data/web/js/kinetic-v5.0.0.min.js", jsPath,
					"kinetic-v5.0.0.min.js");
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Error while copying heatmap js files");
			return;
		}

	}

	/**
	 * Transform a json file to a js file to enable direct loadin of data
	 * 
	 * @param jsonFile
	 * @param jsFile
	 * @return
	 */
	private Boolean jsonToJs(String jsonFile, String jsFile) {

		Boolean flag = true;

		BufferedReader in = null;
		PrintWriter out = null;

		try {
			in = new BufferedReader(new FileReader(jsonFile));
			out = new PrintWriter(new File(jsFile));

			out.write("var data = ");

			String line = "";

			while ((line = in.readLine()) != null) {

				line = line.replaceAll("\"", "'");

				out.write(line);
				out.write("\n");
			}

			out.write(";");

		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.err.println("Json file " + jsonFile + " not found");
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Error reading Json file " + jsonFile);
		}

		finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
					System.err.println("Error while closing " + jsonFile);
				}
			}
			if (out != null) {
				out.close();
			}
		}

		return flag;

	}

	/**
	 * Counts the proportion of dead, dispensable and essential reactions in the
	 * pathways
	 */
	public void writePathwayHeatMap() {
		PrintWriter outData = null;
		PrintWriter outMetaData = null;
		ArrayList<String> objectiveNames = new ArrayList<String>(
				objectives.keySet());

		String outPath = pathwayPath;
		Set<String> ids = network.getPathwayList().keySet();

		try {
			Utils.copyProjectResource(
					"flexflux/data/web/templates/heatmap/heatmap.html",
					outPath, "heatmap.html");
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Error while copying heatmap.html");
			return;
		}

		// Create the data and metadata files
		try {
			outData = new PrintWriter(new File(outPath + "/heatMapData.csv"));
			outMetaData = new PrintWriter(new File(outPath
					+ "/heatMapMetaData.csv"));

			// Prints the header
			outData.write("id");

			for (Condition c : conditions) {
				for (String objName : objectiveNames) {
					outData.write("," + c.code + "__" + objName);
				}
			}
			outData.write("\n");

			/**
			 * Header for metadata file
			 */
			outMetaData.write("id,nbReactions\n");

			for (String id : ids) {

				BioPathway pathway = network.getPathwayList().get(id);

				HashMap<String, BioChemicalReaction> reactions = pathway
						.getReactions();

				outMetaData.write(id + "," + reactions.size() + "\n");

				outData.write(id);

				for (Condition c : conditions) {

					HashMap<String, PFBAResult> pfbaResults = results
							.get(c.code);
					for (String objName : objectiveNames) {
						PFBAResult result = pfbaResults
								.get(objName);

						int nbEssential = 0;

						if (result != null) {

							for (String idReaction : reactions.keySet()) {
								if (result.essentialReactions
										.containsKey(idReaction)) {
									nbEssential++;
								}
							}
						}

						double prop = nbEssential / reactions.size();
						prop = (Math.round(prop * 100)) / 100;

						outData.write("," + prop);

					}
				}

				outData.write("\n");

			}
		} catch (FileNotFoundException e) {
			System.err
					.println("Error while writing pathway heatmap data files");
		}

		finally {
			if (outData != null) {
				outData.close();
			}
			if (outMetaData != null) {
				outMetaData.close();
			}
		}

		// Build inchlib cmd
		if (inchlibPath != "") {
			if (inchlibPath.contains(" ") || !inchlibPath.contains("inchlib")
					|| !inchlibPath.endsWith(".py")) {
				System.err.println("Inchlib command not valid");
				return;
			}
			File f = new File(inchlibPath);
			if (!f.exists() || f.isDirectory()) {
				System.err.println("The python file " + inchlibPath
						+ " does not exist");
				return;
			}

			String jsonFile = outPath + "/heatmap_data.json";
			String jsFile = outPath + "/heatmap_data.js";

			String cmd = "python " + inchlibPath + " " + outPath
					+ "/heatMapData.csv -m " + outPath + "/heatMapMetaData.csv"
					+ " -dh -mh -a both -o " + jsonFile;

			try {
				this.runInchlib(cmd);
				this.jsonToJs(jsonFile, jsFile);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.err.println("Problem while running inchlib");
			}
		}

	}

}
