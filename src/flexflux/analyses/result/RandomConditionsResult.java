package flexflux.analyses.result;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import flexflux.condition.Condition;
import flexflux.condition.ListOfConditions;
import flexflux.io.Utils;
import flexflux.utils.plot.Plot;

public class RandomConditionsResult extends AnalysisResult {
	
	/**
	 * array containing the number of simulations where each input is activated
	 */
	protected HashMap<String, Integer> inputOccurences;
	
	/**
	 * Array indicated the number of activated inputs per simulation
	 */
	protected ArrayList<Integer> numberOfActivatedInputs;
	
	protected Set<Set<String>> activatedInputSets;
	
	/**
	 * conditions generated
	 */
	protected ListOfConditions conditions;
	
	
	/**
	 * Directory used for web files
	 */
	private String webPath;
	private String jsPath;
	
	/**
	 * To lock conditions
	 */
	private final Object lock = new Object();

	
	/**
	 * Constructor
	 * @param inputs
	 */
	public RandomConditionsResult(Set<String> inputs) {
		inputOccurences = new HashMap<String, Integer>();
		activatedInputSets = new HashSet<Set<String>>();
		
		for (String input : inputs) {
			inputOccurences.put(input, 0);
		}

		this.numberOfActivatedInputs = new ArrayList<Integer>();
		
		this.conditions = new ListOfConditions();
		
	}
	
	/**
	 * 
	 * @param set
	 */
	public synchronized void addActivatedInputSet(Set<String> set) {
		activatedInputSets.add(set);
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
	
	/**
	 * adds a condition
	 * @param condition
	 */
	public void addCondition(Condition condition) {
		synchronized (lock) {
			this.getConditions().add(condition);
		}
	}
	
	
	
	@Override
	public void plot() {

		this.plotHistogramInputOccurences();
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
		this.conditions.writeConditionFile(path+"/conditions.tab");

		
		this.createWebDirectories(path);
		this.writeWebMediaSizeHistogram();

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

		// Copy d3.v3.js files
		try {
			Utils.copyProjectResource("flexflux/data/web/js/d3.v3.js", jsPath,
					"d3.v3.js");
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Error while copying web files");
			return;
		}


	}
	
	public HashMap<String, Integer> getInputOccurences() {
		return inputOccurences;
	}
	
	public ArrayList<Integer> getNumberOfActivatedInputs() {
		return numberOfActivatedInputs;
	}

	public Set<Set<String>> getActivatedInputSets() {
		return activatedInputSets;
	}

	public ListOfConditions getConditions() {
		return conditions;
	}

	public void setConditions(ListOfConditions conditions) {
		this.conditions = conditions;
	}
	
	/**
	 * Test if a similar condition is already present
	 * @param c
	 * @return
	 */
	public Boolean containsCondition(Condition c) {
		synchronized (lock) {
			return this.getConditions().contains(c);
		}
		
		
	}
	
	/**
	 * Writes the conditions in a file
	 * @return
	 */
	public Boolean writeConditionFile(String fileName) {
		
		
		this.conditions.writeConditionFile(fileName);
		
		return true;
		
	}

	@Override
	public void writeHTML(String path) {
		// TODO Auto-generated method stub
		
	}
	
	

}
