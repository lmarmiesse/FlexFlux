package flexflux.analyses.result;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import flexflux.io.Utils;
import flexflux.utils.plot.Plot;
import flexflux.utils.run.Runner;
import flexflux.utils.web.JsonUtils;

public class ERAResult extends AnalysisResult {

	/**
	 * array indicating for each objective function the number of simulations
	 * where it is activated
	 */
	protected HashMap<String, Integer> objSimCount;

	/**
	 * array containing the number of simulations where each input is activated
	 */
	protected HashMap<String, Integer> inputOccurences;

	/**
	 * matrix whose the rows are the inputs in InputRandomParameters and the
	 * columns the objective functions in listOfObjectiveFunctions. The values
	 * are the numbers of times that an objective value is fired when the entity
	 * is activated
	 */
	protected HashMap<String, HashMap<String, Integer>> objInputMatrix;

	/**
	 * Array indicated the number of activated inputs per simulation
	 */
	protected ArrayList<Integer> numberOfActivatedInputs;
	
	protected Set<Set<String>> activatedInputSets;
	

	/**
	 * Directory used for web files
	 */
	private String webPath;
	private String jsPath;

	/**
	 * Path to the inchlib python program used to build web heatmap
	 */
	public String inchlibPath;

	/**
	 * Constructor
	 */
	public ERAResult(Set<String> inputs, Set<String> objectiveNames) {

		objSimCount = new HashMap<String, Integer>();
		inputOccurences = new HashMap<String, Integer>();
		objInputMatrix = new HashMap<String, HashMap<String, Integer>>();

		activatedInputSets = new HashSet<Set<String>>();
		
		for (String obj : objectiveNames) {
			objSimCount.put(obj, 0);
		}

		for (String input : inputs) {

			objInputMatrix.put(input, new HashMap<String, Integer>());
			for (String obj : objectiveNames) {

				objInputMatrix.get(input).put(obj, 0);
			}
		}

		for (String input : inputs) {
			inputOccurences.put(input, 0);
		}

		this.numberOfActivatedInputs = new ArrayList<Integer>();

	}

	/**
	 * Increment the number of simulations that activate a given objective
	 * 
	 * @param objName
	 */
	public synchronized void incrementObjSimCount(String objName) {
		Integer prev = objSimCount.get(objName);
		objSimCount.put(objName, prev + 1);
	}
	
	/**
	 * 
	 * @param set
	 */
	public synchronized void addActivatedInputSet(Set<String> set) {
		activatedInputSets.add(set);
	}
	

	/**
	 * Increment the number of simulations where a objective is active when an
	 * input is active
	 * 
	 * @param objName
	 * @param inputId
	 */
	public synchronized void incrementObjInputMatrix(String objName,
			String inputId) {
		Integer prev = objInputMatrix.get(inputId).get(objName);
		objInputMatrix.get(inputId).put(objName, prev + 1);
	}

	/**
	 * 
	 * @param inputId
	 */
	public synchronized void incrementInputOccurences(String inputId) {

		Integer prev = inputOccurences.get(inputId);
		inputOccurences.put(inputId, prev + 1);

	}

	/**
	 * 
	 * @param nb
	 */
	public synchronized void addNumberOfActivatedInputs(int nb) {
		this.numberOfActivatedInputs.add(nb);
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

		this.writeInputOccurences(path);
		this.writeActivatedInputs(path);
		this.writeActivatedObjectives(path);

		this.createWebDirectories(path);
		this.writeWebMediaSizeHistogram();
		this.writeWebObjInputHeatMap();
		this.writeWebObjSimCountHistogram();

	}

	@Override
	public void plot() {

		this.plotHistogramInputOccurences();
		this.plotHistogramActivatedObjectives();
		this.plotHistogramActivatedInputs();

	}

	/**
	 * Plot the histogram of the number of each activated input occurences in
	 * the simulation
	 */
	public void plotHistogramInputOccurences() {

		Plot.plotHistogram(
				inputOccurences.values(),
				"Distribution of the number of simulations that activated the input",
				"Number of simulations","Number of inputs",  50);

	}

	/**
	 * Plot the histogram of the number of simulations where each objective
	 * function is activated
	 */
	public void plotHistogramActivatedObjectives() {

		Plot.plotHistogram(
				objSimCount.values(),
				"Distribution of the number of simulations where each objective is activated",
				"Number of simulations where the objective is activated",
				"Number of objectives",
				50);

	}

	/**
	 * Plot the histogram of the number of activated inputs in each simulation
	 */
	public void plotHistogramActivatedInputs() {

		Plot.plotHistogram(
				numberOfActivatedInputs,
				"Distribution of the number of activated inputs in each simulation",
				"Number of inputs","Number of simulations", 50);

	}

	/**
	 * 
	 * @param path
	 */
	public void writeInputOccurences(String path) {

		PrintWriter out = null;

		try {
			out = new PrintWriter(new File(path + "/inputOccurences.tsv"));

			out.print("# Number of simulations where each input has been activated\n");
			out.print("inputId\tnbOccurences\n");

			for (String inputId : inputOccurences.keySet()) {
				Integer val = inputOccurences.get(inputId);
				out.print(inputId + "\t" + val + "\n");
			}
		} catch (IOException e) {
			System.err
					.println("Error while writing the input occurences results");
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
	public void writeActivatedInputs(String path) {

		PrintWriter out = null;

		try {
			out = new PrintWriter(new File(path
					+ "/numberOfActivatedInputs.tsv"));

			out.print("# Number of activated inputs for each simulations\n");

			for (Integer val : numberOfActivatedInputs) {
				out.print(val + "\n");
			}

		} catch (IOException e) {
			System.err
					.println("Error while writing the number of activated input results");
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
	public void writeActivatedObjectives(String path) {

		PrintWriter out = null;

		try {
			out = new PrintWriter(new File(path + "/activatedObjectives.tsv"));

			out.print("# number of simulations where each objective is activated\n");
			out.print("objectiveId\tnbSimulations\n");

			for (String objId : this.objSimCount.keySet()) {
				Integer val = this.objSimCount.get(objId);
				out.print(objId + "\t" + val + "\n");
			}

		} catch (IOException e) {
			System.err
					.println("Error while writing the number of activated input results");
			e.printStackTrace();
		}

		finally {
			if (out != null) {
				out.close();
			}
		}

	}

	/**
	 * Create required web directories and Files
	 * 
	 * @param path
	 */
	private void createWebDirectories(String path) {

		// Create web directories
		this.webPath = path + "/" + "web";
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

		// copy summary.html in index.html
		try {
			Utils.copyProjectResource(
					"flexflux/data/web/templates/era/summary.html", webPath,
					"index.html");
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Error while copying web files");
			return;
		}

		// Copy d3.v3.js files
		try {
			Utils.copyProjectResource("flexflux/data/web/js/d3.v3.js", jsPath,
					"d3.v3.js");
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Error while copying web files");
			return;
		}

		// copy inchlib js files
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
	 * Create the d3 plot of the histogram of the number of simulations where
	 * each objective function is activated
	 */
	public void writeWebObjSimCountHistogram() {

		// Create new directory
		String path = this.webPath + "/robustnessHistogram";

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

		// copy template html
		try {
			Utils.copyProjectResource(
					"flexflux/data/web/templates/histogram/histogram.html",
					path, "histogram.html");
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Error while copying histogram web template");
			return;
		}

		// Creates values.js file
		PrintWriter out = null;

		try {
			out = new PrintWriter(new File(path + "/values.js"));

			out.write("var values = [");

			int i = 0;
			for (Integer val : objSimCount.values()) {
				if (i == 0) {
					out.write(val.toString());
				} else {
					out.write("," + val.toString());
				}
				i++;
			}
			out.write("];");

		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.err.println("Error while creating histogram web data");
			return;
		} finally {
			if (out != null) {
				out.close();
			}
		}
	}

	/**
	 * Create the d3 plot of the histogram of the number of activated inputs per
	 * simulations
	 */
	public void writeWebMediaSizeHistogram() {

		// Create new directory
		String path = this.webPath + "/mediaSizeHistogram";

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

		// copy template html
		try {
			Utils.copyProjectResource(
					"flexflux/data/web/templates/histogram/histogram.html",
					path, "histogram.html");
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Error while copying histogram web template");
			return;
		}

		// Creates values.js file
		PrintWriter out = null;

		try {
			out = new PrintWriter(new File(path + "/values.js"));

			out.write("var values = [");

			int i = 0;
			for (Integer val : numberOfActivatedInputs) {
				if (i == 0) {
					out.write(val.toString());
				} else {
					out.write("," + val.toString());
				}
				i++;
			}
			out.write("];");

		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.err.println("Error while creating histogram web data");
			return;
		} finally {
			if (out != null) {
				out.close();
			}
		}
	}

	/**
	 * Creates the inchlib heatmap whose the columns are the inputs and the
	 * columns the objective functions
	 */
	public void writeWebObjInputHeatMap() {

		PrintWriter outData = null;

		String path = this.webPath + "/heatmap";

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

		try {
			Utils.copyProjectResource(
					"flexflux/data/web/templates/heatmap/heatmap.html", path,
					"heatmap.html");
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Error while copying heatmap.html");
			return;
		}

		try {
			outData = new PrintWriter(new File(path + "/heatMapData.csv"));

			// Prints the header
			outData.write("inputId");

			// set the objectives in an array
			ArrayList<String> objNames = new ArrayList<String>(
					this.objSimCount.keySet());

			for (String objName : objNames) {
				outData.write("," + objName);
			}
			outData.write("\n");

			for (String inputId : inputOccurences.keySet()) {
				outData.write(inputId);
				for (String objName : objNames) {
					Double val = objInputMatrix.get(inputId).get(objName) + 0.0;
					outData.write("," + val);
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
		}

		// Build inchlib cmd
		if (inchlibPath != null && inchlibPath != "") {
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

			String jsonFile = path + "/heatmap_data.json";
			String jsFile = path + "/heatmap_data.js";

			String cmd = "python " + inchlibPath + " " + path
					+ "/heatMapData.csv" + " -dh -a both -o " + jsonFile;

			try {
				Runner.runExternalCommand(cmd);
				JsonUtils.jsonToJs(jsonFile, jsFile);
			} catch (IOException e) {
				e.printStackTrace();
				System.err.println("Problem while running inchlib");
			}
		}
	}

	public HashMap<String, Integer> getObjSimCount() {
		return objSimCount;
	}

	public HashMap<String, Integer> getInputOccurences() {
		return inputOccurences;
	}

	public HashMap<String, HashMap<String, Integer>> getObjInputMatrix() {
		return objInputMatrix;
	}

	public ArrayList<Integer> getNumberOfActivatedInputs() {
		return numberOfActivatedInputs;
	}

	public Set<Set<String>> getActivatedInputSets() {
		return activatedInputSets;
	}
	
	
	
}
