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
import parsebionet.biodata.BioEntity;
import parsebionet.biodata.BioNetwork;
import parsebionet.biodata.BioPathway;
import flexflux.analyses.result.AnalysisResult;
import flexflux.analyses.result.KOResult;
import flexflux.analyses.result.MyTableModel;
import flexflux.analyses.result.PFBAResult;
import flexflux.condition.Condition;
import flexflux.condition.ListOfConditions;
import flexflux.general.Vars;
import flexflux.io.Utils;
import flexflux.objective.ListOfObjectives;
import flexflux.objective.Objective;
import flexflux.utils.run.Runner;
import flexflux.utils.web.JsonUtils;

public class ConditionComparisonResult extends AnalysisResult {

	HashMap<String, HashMap<String, PFBAResult>> pfbaAllResults = null;
	HashMap<String, HashMap<String, KOResult>> koAllResults = null;

	public ConditionComparisonFbaResultSet fbaAllResults = null;

	ListOfConditions conditions = null;
	ListOfObjectives objectives = null;

	public HashMap<String, HashMap<String, String>> reactionMetaData = null;
	public HashMap<String, HashMap<String, String>> geneMetaData = null;
	public HashMap<String, HashMap<String, String>> regulatorMetaData = null;

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
	private String regulatorPath;
	private String pathwayPath;

	private String inchlibPath;

	public Boolean launchReactionAnalysis;
	public Boolean launchGeneAnalysis;
	public Boolean launchRegulatorAnalysis;

	public HashMap<String, BioEntity> regulators;
	private HashMap<String, BioChemicalReaction> deadReactions;

	private HashMap<String, BioChemicalReaction> allReactions;

	/**
	 * Constructor
	 * 
	 * @param conditions
	 *            : list of conditions
	 * @param objectives
	 *            : list of objectives
	 */
	public ConditionComparisonResult(ListOfConditions conditions,
			ListOfObjectives objectives, BioNetwork network,
			String inchlibPath, Boolean launchReactionAnalysis,
			Boolean launchGeneAnalysis, Boolean launchRegulatorAnalysis) {

		pfbaAllResults = new HashMap<String, HashMap<String, PFBAResult>>();
		koAllResults = new HashMap<String, HashMap<String, KOResult>>();

		fbaAllResults = new ConditionComparisonFbaResultSet();

		this.inchlibPath = inchlibPath;

		this.conditions = conditions;
		this.objectives = objectives;

		this.network = network;

		// Sets the interaction targets
		this.interactionTargets = new HashSet<String>();

		this.launchGeneAnalysis = launchGeneAnalysis;
		this.launchReactionAnalysis = launchReactionAnalysis;
		this.launchRegulatorAnalysis = launchRegulatorAnalysis;

		this.allReactions = new HashMap<String, BioChemicalReaction>();
		this.allReactions.putAll(network.getBiochemicalReactionList());

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

		if (!pfbaAllResults.containsKey(conditionId)) {
			pfbaAllResults.put(conditionId, new HashMap<String, PFBAResult>());
		}

		pfbaAllResults.get(conditionId).put(objId, res);
	}

	/**
	 * Adds a ko result
	 * 
	 * @param o
	 * @param condition
	 * @param res
	 */
	public void addKoResult(Objective o, Condition condition, KOResult res) {
		String conditionId = condition.code;
		String objId = o.getName();

		if (!koAllResults.containsKey(conditionId)) {
			koAllResults.put(conditionId, new HashMap<String, KOResult>());
		}

		koAllResults.get(conditionId).put(objId, res);
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

		fbaAllResults.add(result);

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

		this.createWebDirectories();

		if (launchGeneAnalysis || launchReactionAnalysis
				|| launchRegulatorAnalysis) {

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

		if (launchRegulatorAnalysis) {

			this.regulatorPath = webPath + "/" + "regulators";
			File file = new File(regulatorPath);

			if (!file.exists()) {
				try {
					file.mkdir();
				} catch (SecurityException se) {
					se.printStackTrace();
					System.err.println("Security Exception during creation of "
							+ regulatorPath);
				}
			}

		}

		writeFbaResultsToFile();
		writeFbaResultHeatMap();

		if (launchGeneAnalysis) {
			writePfbaSummaryFile(false);
			writePfbaClassificationToFiles(false);
			writePfbaBarplot(false);
			writeFilesForPfbaHeatMap(false);
		}

		if (launchReactionAnalysis) {
			writePfbaClassificationToFiles(true);
			writePfbaSummaryFile(true);
			writePfbaBarplot(true);
			writeFilesForPfbaHeatMap(true);
			writePathwayHeatMap();
		}

		if (launchRegulatorAnalysis) {
			writeKoAnalysisToFiles();
			writeKoSummaryFile();
			writeKoAnalysisBarplot();
			writeFilesForKoHeatMap();
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
				objectives.objectives.keySet());

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
				HashMap<String, ConditionComparisonFbaResult> results = fbaAllResults
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
	public void writePfbaClassificationToFiles(Boolean isReaction) {

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
		PrintWriter outRedundant = null;
		PrintWriter outDead = null;

		HashMap<String, PrintWriter> writers = new HashMap<String, PrintWriter>();

		HashMap<String, ArrayList<String>> classification = new HashMap<String, ArrayList<String>>();

		ArrayList<String> objectiveNames = new ArrayList<String>(
				objectives.objectives.keySet());

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
			outDead = new PrintWriter(new File(path + "/dead" + objectName
					+ ".tsv"));

			if (objectName.compareTo("Genes") == 0) {
				outRedundant = new PrintWriter(new File(path
						+ "/redundantGenesForEssentialReactions" + objectName
						+ ".tsv"));
			}

			writers.put("ess", outEssential);
			writers.put("zf", outZeroFlux);
			writers.put("mle", outMle);
			writers.put("ele", outEle);
			writers.put("conc", outConcurrent);
			writers.put("ind", outIndependent);
			writers.put("opt", outOptima);
			writers.put("dead", outDead);

			if (objectName.compareTo("Genes") == 0) {
				writers.put("red", outRedundant);
			}

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

				HashMap<String, PFBAResult> pfbaResults = pfbaAllResults
						.get(c.code);

				for (String objName : objectiveNames) {

					for (PrintWriter out : writers.values()) {
						out.print("\t");
					}

					PFBAResult result = pfbaResults.get(objName);

					if (result != null) {

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

						ArrayList<String> optimaIds = new ArrayList<String>(
								result.get("optima" + objectName).keySet());
						Collections.sort(optimaIds);
						classification.put("opt", optimaIds);

						ArrayList<String> deadIds = new ArrayList<String>(
								result.get("dead" + objectName).keySet());
						Collections.sort(deadIds);
						classification.put("dead", deadIds);

						if (objectName.compareTo("Genes") == 0) {
							ArrayList<String> redundantIds = new ArrayList<String>(
									result.redundantGenesForEssentialReactions
											.keySet());
							Collections.sort(redundantIds);
							classification.put("red", redundantIds);
						}
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
	 * 
	 */
	public void writeKoAnalysisToFiles() {

		if (Vars.verbose) {
			System.err
					.println("\n***********\nwriteKoAnalysisToFiles\n************");
		}

		String path = this.directoryPath;

		PrintWriter outEssential = null;
		PrintWriter outOptimaEssential = null;
		PrintWriter outNeutral = null;

		HashMap<String, PrintWriter> writers = new HashMap<String, PrintWriter>();

		HashMap<String, ArrayList<String>> classification = new HashMap<String, ArrayList<String>>();

		ArrayList<String> objectiveNames = new ArrayList<String>(
				objectives.objectives.keySet());

		try {
			outEssential = new PrintWriter(new File(path
					+ "/essentialRegulators.tsv"));
			outOptimaEssential = new PrintWriter(new File(path
					+ "/optimaRegulators.tsv"));
			outNeutral = new PrintWriter(new File(path
					+ "/neutralRegulators.tsv"));

			writers.put("ess", outEssential);
			writers.put("opt", outOptimaEssential);
			writers.put("neu", outNeutral);

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

				if (Vars.verbose) {
					System.err.println("Condition : " + c.code);
				}

				HashMap<String, KOResult> koResults = koAllResults.get(c.code);
				HashMap<String, ConditionComparisonFbaResult> fbaResults = fbaAllResults
						.get(c.code);

				for (String objName : objectiveNames) {

					if (Vars.verbose) {
						System.err.println("Obj : " + objName);
					}

					for (PrintWriter out : writers.values()) {
						out.print("\t");
					}

					KOResult result = koResults.get(objName);

					if (result != null) {

						ConditionComparisonFbaResult fbaResult = fbaResults
								.get(objName);
						Double optValue = fbaResult.value;

						if (Vars.verbose) {
							System.err.println("Opt Value : " + optValue);
						}

						ArrayList<String> essentialIds = new ArrayList<String>(
								result.getEssentialEntities().keySet());
						Collections.sort(essentialIds);
						classification.put("ess", essentialIds);

						if (Vars.verbose) {
							System.err.println("Essential :" + essentialIds);
						}

						ArrayList<String> optimaEssentialIds = new ArrayList<String>(
								result.getOptimaEntities(optValue).keySet());
						Collections.sort(optimaEssentialIds);
						classification.put("opt", optimaEssentialIds);

						if (Vars.verbose) {
							System.err.println("Optima :" + optimaEssentialIds);
						}

						ArrayList<String> neutralIds = new ArrayList<String>(
								result.getNeutralEntities(optValue).keySet());
						Collections.sort(neutralIds);
						classification.put("neu", neutralIds);

						if (Vars.verbose) {
							System.err.println("Neutral :" + neutralIds);
						}

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
				}

				for (PrintWriter out : writers.values()) {
					out.print("\n");
				}
			}
		} catch (IOException e) {
			System.err
					.println("Error while writing the regulation classification results");
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

		if (inchlibPath != "") {
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
				Runner.runExternalCommand(cmd);
				JsonUtils.jsonToJs(jsonFile, jsFile);
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

	public void writePfbaSummaryFile(Boolean isReaction) {
		String objectName = "Reactions";
		if (isReaction == false) {
			objectName = "Genes";
		}

		String path = this.directoryPath;

		PrintWriter out = null;

		ArrayList<String> objectiveNames = new ArrayList<String>(
				objectives.objectives.keySet());

		try {
			out = new PrintWriter(new File(path + "/summary" + objectName
					+ ".txt"));

			if (isReaction) {
				out.write("name,essential,zeroFlux,mle,ele,conc,ind,opt,dead\n");
			} else {
				out.write("name,essential,red,zeroFlux,mle,ele,conc,ind,opt,dead\n");
			}

			for (Condition c : conditions) {
				HashMap<String, PFBAResult> pfbaResults = pfbaAllResults
						.get(c.code);

				for (String objName : objectiveNames) {

					PFBAResult result = pfbaResults.get(objName);

					int nbEssential = 0;
					int nbZeroFlux = 0;
					int nbMle = 0;
					int nbEle = 0;
					int nbConc = 0;
					int nbInd = 0;
					int nbOpt = 0;
					int nbRed = 0;
					int nbDead = 0;

					if (result != null) {

						nbEssential = result.get("essential" + objectName)
								.size();
						nbZeroFlux = result.get("zeroFlux" + objectName).size();
						nbMle = result.get("mle" + objectName).size();
						nbEle = result.get("ele" + objectName).size();
						nbConc = result.get("concurrent" + objectName).size();
						nbInd = result.get("objectiveIndependent" + objectName)
								.size();
						nbOpt = result.get("optima" + objectName).size();
						nbDead = result.get("dead" + objectName).size();

						if (!isReaction) {
							nbRed = result.get(
									"redundantGenesForEssentialReactions")
									.size();
						}

					}
					if (isReaction) {
						out.write(c.code + "__" + objName + "," + nbEssential
								+ "," + nbZeroFlux + "," + nbMle + "," + nbEle
								+ "," + nbConc + "," + nbInd + "," + nbOpt
								+ "," + nbDead + "\n");
					} else {
						out.write(c.code + "__" + objName + "," + nbEssential
								+ "," + nbRed + "," + nbZeroFlux + "," + nbMle
								+ "," + nbEle + "," + nbConc + "," + nbInd
								+ "," + nbOpt + "," + nbDead + "\n");
					}

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
	 * Write a tabulated file with the number of regulators by type
	 */

	public void writeKoSummaryFile() {

		String path = this.directoryPath;

		PrintWriter out = null;

		ArrayList<String> objectiveNames = new ArrayList<String>(
				objectives.objectives.keySet());

		try {
			out = new PrintWriter(new File(path + "/summaryRegulators.txt"));

			out.write("name,essential,opt,neu\n");

			for (Condition c : conditions) {
				HashMap<String, KOResult> koResults = koAllResults.get(c.code);
				HashMap<String, ConditionComparisonFbaResult> fbaResults = fbaAllResults
						.get(c.code);

				for (String objName : objectiveNames) {

					KOResult result = koResults.get(objName);

					int nbEssential = 0;
					int nbOptimal = 0;
					int nbNeutral = 0;

					if (result != null) {

						ConditionComparisonFbaResult fbaResult = fbaResults
								.get(objName);
						Double optimalValue = fbaResult.value;

						nbEssential = result.getEssentialEntities().size();
						nbOptimal = result.getOptimaEntities(optimalValue)
								.size();
						nbNeutral = result.getNeutralEntities(optimalValue)
								.size();
					}

					out.write(c.code + "__" + objName + "," + nbEssential + ","
							+ nbOptimal + "," + nbNeutral + "\n");

				}

			}

		} catch (IOException e) {
			System.err
					.println("Error while writing the regulator summary results");
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
	public void writePfbaBarplot(Boolean isReaction) {

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
			if (isReaction) {
				Utils.copyProjectResource(
						"flexflux/data/web/templates/multiBar/multiBarReactions.js",
						outPath, "multiBar.js");
			} else {
				Utils.copyProjectResource(
						"flexflux/data/web/templates/multiBar/multiBarGenes.js",
						outPath, "multiBar.js");
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Error while copying web files");
			return;
		}

		// Create js files with data inside
		ArrayList<String> objectiveNames = new ArrayList<String>(
				objectives.objectives.keySet());
		PrintWriter out = null;
		try {
			out = new PrintWriter(new File(outPath + "/multiBar_data.js"));
			if (isReaction) {
				out.write("var str = \"name,essential,zeroFlux,mle,ele,conc,ind,opt,dead\\n");
			} else {
				out.write("var str = \"name,essential,red,zeroFlux,mle,ele,conc,ind,opt,dead\\n");
			}

			for (Condition c : conditions) {

				HashMap<String, PFBAResult> pfbaResults = pfbaAllResults
						.get(c.code);

				for (String objName : objectiveNames) {

					PFBAResult result = pfbaResults.get(objName);

					int nbEssential = 0;
					int nbZeroFlux = 0;
					int nbMle = 0;
					int nbEle = 0;
					int nbConc = 0;
					int nbInd = 0;
					int nbOpt = 0;
					int nbRed = 0;
					int nbDead = 0;

					if (result != null) {

						nbEssential = result.get("essential" + objectName)
								.size();
						nbZeroFlux = result.get("zeroFlux" + objectName).size();
						nbMle = result.get("mle" + objectName).size();
						nbEle = result.get("ele" + objectName).size();
						nbConc = result.get("concurrent" + objectName).size();
						nbInd = result.get("objectiveIndependent" + objectName)
								.size();
						nbOpt = result.get("optima" + objectName).size();
						nbDead = result.get("dead" + objectName).size();
						if (!isReaction) {
							nbRed = result.get(
									"redundantGenesForEssentialReactions")
									.size();
						}

					}
					if (isReaction) {
						out.write(c.code + "__" + objName + "," + nbEssential
								+ "," + nbZeroFlux + "," + nbMle + "," + nbEle
								+ "," + nbConc + "," + nbInd + "," + nbOpt
								+ "," + nbDead + "\\n");
					} else {
						out.write(c.code + "__" + objName + "," + nbEssential
								+ "," + nbRed + "," + nbZeroFlux + "," + nbMle
								+ "," + nbEle + "," + nbConc + "," + nbInd
								+ "," + nbOpt + "," + nbDead + "\\n");
					}
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
	 * Create web architecture to display barplots with the D3 library
	 * 
	 * @throws IOException
	 */
	public void writeKoAnalysisBarplot() {

		String outPath = regulatorPath;

		// Copy required files
		try {
			Utils.copyProjectResource(
					"flexflux/data/web/templates/multiBar/multiBar.html",
					outPath, "summary.html");
			Utils.copyProjectResource(
					"flexflux/data/web/templates/multiBar/multiBarKo.js",
					outPath, "multiBar.js");
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Error while copying web files");
			return;
		}

		// Create js files with data inside
		ArrayList<String> objectiveNames = new ArrayList<String>(
				objectives.objectives.keySet());
		PrintWriter out = null;
		try {
			out = new PrintWriter(new File(outPath + "/multiBar_data.js"));
			out.write("var str = \"name,essential,opt,neu\\n");

			for (Condition c : conditions) {

				HashMap<String, KOResult> koResults = koAllResults.get(c.code);
				HashMap<String, ConditionComparisonFbaResult> fbaResults = fbaAllResults
						.get(c.code);

				for (String objName : objectiveNames) {

					KOResult result = koResults.get(objName);
					int nbEssential = 0;
					int nbOptimal = 0;
					int nbNeutral = 0;

					if (result != null) {

						ConditionComparisonFbaResult fbaResult = fbaResults
								.get(objName);
						Double optimalValue = fbaResult.value;

						nbEssential = result.getEssentialEntities().size();
						nbOptimal = result.getOptimaEntities(optimalValue)
								.size();
						nbNeutral = result.getNeutralEntities(optimalValue)
								.size();
					}

					out.write(c.code + "__" + objName + "," + nbEssential + ","
							+ nbOptimal + "," + nbNeutral + "\\n");
				}

			}

			out.write("\"\n");

		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.err.println("Error while creating regulator web data");
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
	public void writeFilesForPfbaHeatMap(Boolean isReaction) {

		String objectName = "Reactions";

		if (!isReaction) {
			objectName = "Genes";
		}

		PrintWriter outData = null;
		PrintWriter outMetaData = null;
		ArrayList<String> objectiveNames = new ArrayList<String>(
				objectives.objectives.keySet());

		String outPath = genePath;
		HashMap<String, HashMap<String, String>> metaData = geneMetaData;
		Set<String> ids = network.getGeneList().keySet();

		Set<String> chokes = new HashSet<String>();

		if (isReaction) {
			outPath = reactionPath;
			metaData = reactionMetaData;
			ids = allReactions.keySet();
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
					BioChemicalReaction reaction = allReactions.get(id);
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

					HashMap<String, PFBAResult> pfbaResults = pfbaAllResults
							.get(c.code);

					for (String objName : objectiveNames) {
						PFBAResult result = pfbaResults.get(objName);

						int value = 0;

						if (result != null) {

							if (result.get("essential" + objectName)
									.containsKey(id)) {
								value = 8;
							} else if (!isReaction
									&& result.redundantGenesForEssentialReactions
											.containsKey(id)) {
								value = 7;
							} else if (result.get("optima" + objectName)
									.containsKey(id)) {
								value = 6;
							} else if (result.get("ele" + objectName)
									.containsKey(id)) {
								value = 5;
							} else if (result.get("mle" + objectName)
									.containsKey(id)) {
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
				Runner.runExternalCommand(cmd);
				JsonUtils.jsonToJs(jsonFile, jsFile);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.err.println("Problem while running inchlib");
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
	public void writeFilesForKoHeatMap() {

		PrintWriter outData = null;
		PrintWriter outMetaData = null;
		ArrayList<String> objectiveNames = new ArrayList<String>(
				objectives.objectives.keySet());

		String outPath = regulatorPath;
		HashMap<String, HashMap<String, String>> metaData = regulatorMetaData;

		Set<String> ids = regulators.keySet();

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

			// Prints the header
			outData.write("id");

			for (Condition c : conditions) {
				for (String objName : objectiveNames) {
					outData.write("," + c.code + "__" + objName);
				}
			}
			outData.write("\n");

			ArrayList<String> additionalMetaDataColumns = new ArrayList<String>();

			if (metaData != null && metaData.size() != 0) {

				outMetaData = new PrintWriter(new File(outPath
						+ "/heatMapMetaData.csv"));

				/**
				 * Header for metadata file
				 */
				outMetaData.write("id");

				additionalMetaDataColumns = new ArrayList<String>(
						metaData.keySet());

				for (String col : additionalMetaDataColumns) {
					outMetaData.write("," + col);
				}

				outMetaData.write("\n");

			}

			for (String id : ids) {

				if (metaData != null && metaData.size() != 0) {
					outMetaData.write(id);
					for (String col : additionalMetaDataColumns) {
						String value = "NA";
						if (metaData.get(col).containsKey(id)) {
							value = metaData.get(col).get(id);
						}
						outMetaData.write("," + value);
					}
					outMetaData.write("\n");
				}

				/**
				 * Prints the values in the data file
				 */
				outData.write(id);
				for (Condition c : conditions) {

					HashMap<String, KOResult> koResults = koAllResults
							.get(c.code);
					HashMap<String, ConditionComparisonFbaResult> fbaResults = fbaAllResults
							.get(c.code);

					for (String objName : objectiveNames) {
						KOResult result = koResults.get(objName);
						ConditionComparisonFbaResult fbaResult = fbaResults
								.get(objName);
						Double optimalValue = fbaResult.value;

						int value = 0;

						if (result != null) {

							if (result.getEssentialEntities().containsKey(id)) {
								value = 3;
							} else if (result.getOptimaEntities(optimalValue)
									.containsKey(id)) {
								value = 2;
							} else if (result.getNeutralEntities(optimalValue)
									.containsKey(id)) {
								value = 1;
							}

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
					.println("Error while creating the data files for regulator clustering");
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

			String cmd = "";

			if (metaData != null && metaData.size() > 0) {
				cmd = "python " + inchlibPath + " " + outPath
						+ "/heatMapData.csv -m " + outPath
						+ "/heatMapMetaData.csv" + " -dh -mh -a both -o "
						+ jsonFile;
			} else {
				cmd = "python " + inchlibPath + " " + outPath
						+ "/heatMapData.csv -dh -a both -o " + jsonFile;
			}
			try {
				Runner.runExternalCommand(cmd);
				JsonUtils.jsonToJs(jsonFile, jsFile);
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
				objectives.objectives.keySet());

		for (String objName : objectiveNames) {
			columnNames.add(objName);
		}

		Object[][] data = new Object[conditions.size()][columnNames.size()];

		int i = 0;
		for (Condition c : conditions) {
			ArrayList<String> line = new ArrayList<String>();
			line.add(c.code);

			HashMap<String, ConditionComparisonFbaResult> results = fbaAllResults
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
	 * Create required web directories and Files
	 * 
	 * @param path
	 */
	private void createWebDirectories() {

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
					"flexflux/data/web/templates/conditionComparison/summary.html",
					webPath, "index.html");
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
	 * Counts the proportion of dead, dispensable and essential reactions in the
	 * pathways
	 */
	public void writePathwayHeatMap() {
		PrintWriter outData = null;
		PrintWriter outMetaData = null;
		ArrayList<String> objectiveNames = new ArrayList<String>(
				objectives.objectives.keySet());

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

					HashMap<String, PFBAResult> pfbaResults = pfbaAllResults
							.get(c.code);
					for (String objName : objectiveNames) {
						PFBAResult result = pfbaResults.get(objName);

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
				Runner.runExternalCommand(cmd);
				JsonUtils.jsonToJs(jsonFile, jsFile);
			} catch (IOException e) {
				e.printStackTrace();
				System.err.println("Problem while running inchlib");
			}
		}
	}

	/**
	 * Adds dead reactions in the analysis fill in also the allReactions array
	 * 
	 * @param deadReactions
	 */
	public void setDeadReactions(
			HashMap<String, BioChemicalReaction> deadReactions) {
		this.deadReactions = new HashMap<String, BioChemicalReaction>();
		this.deadReactions.putAll(deadReactions);
		this.allReactions.putAll(deadReactions);
	}

	/**
	 * 
	 * @return the dead reactions
	 */
	public HashMap<String, BioChemicalReaction> getDeadReactions() {
		return this.deadReactions;
	}

}
