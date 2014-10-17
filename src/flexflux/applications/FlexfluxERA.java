package flexflux.applications;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import parsebionet.biodata.BioEntity;
import flexflux.analyses.era.ERAAnalysis;
import flexflux.analyses.era.InputRandomParameters;
import flexflux.analyses.result.ERAResult;
import flexflux.general.Bind;
import flexflux.general.CplexBind;
import flexflux.general.GLPKBind;
import flexflux.general.Vars;

public class FlexfluxERA extends FFApplication {

	private String applicationName = FlexfluxConditionComparison.class
			.getSimpleName();

	private String message = applicationName
			+ "[options...]\n"
			+ "Computes Robustness of the network among a set of objective functions "
			+ "in front of random environmental perturbations.";

	@Option(name = "-s", usage = "Sbml file path", metaVar = "File", required = true)
	public String sbmlFile = "";

	@Option(name = "-cons", usage = "[OPTIONAL] File containing the constraints applied on the metabolic network", metaVar = "File", required = false)
	public String constraintFile = "";

	@Option(name = "-o", usage = "File containing the objective functions", metaVar = "File", required = true)
	public String objectiveFile = "";

	@Option(name = "-i", usage = "Input random parameter file"
			+ "Each line must contain a entity id, a value of inhibition, a value of activation and a weight for the simulation."
			+ "During the choice of the inputs to activate during a simulation, the inputs are duplicated n times, with n=weight", metaVar = "File", required = true)
	public String inputRandomParameterFile = "";

	@Option(name = "-nbSim", usage = "[100] Number of simulations to perform")
	public int nbSim = 100;

	@Option(name = "-mean", usage = "[10] Mean of the gaussian distribution used for selecting "
			+ "the number of inputs activated in each simulation")
	public Double meanGaussian = 10.0;

	@Option(name = "-std", usage = "[5] Standard deviation of the gaussian distribution used for selecting "
			+ "the number of inputs activated in each simulation")
	public Double stdGaussian = 5.0;

	@Option(name = "-min", usage = "[1] minimum number of activated inputs at each step of the simulation")
	public Integer minInputs = 1;

	@Option(name = "-max", usage = "[1] maximum number of activated inputs at each step of the simulation")
	public Integer maxInputs = 50;

	@Option(name = "-int", usage = "[OPTIONAL] Interaction file path", metaVar = "File")
	public String intFile = "";

	@Option(name = "-plot", usage = "[OPTIONAL, default = false] Plots the results")
	public Boolean plot = false;

	@Option(name = "-out", usage = "[OPTIONAL] Output file directory", metaVar = "File")
	public String outName = "";

	@Option(name = "-n", usage = "[OPTIONAL, default = number of available processors] Number of threads", metaVar = "Integer")
	public int nThreads = Runtime.getRuntime().availableProcessors();

	@Option(name = "-pre", usage = "[OPTIONAL, default = 6] Number of decimals of precision for calculations and results", metaVar = "Integer")
	public int precision = 6;

	@Option(name = "-ext", usage = "[OPTIONAL, default = false] Uses the extended SBML format")
	public Boolean extended = false;

	@Option(name = "-sol", usage = "Solver name", metaVar = "Solver")
	public String solver = "GLPK";

	@Option(name = "-inchlibPath", usage = "[default=/usr/local/inchlib_clust/inchlib_clust.py]", metaVar = "String")
	public String inchlibPath = "/usr/local/inchlib_clust/inchlib_clust.py";

	@Option(name = "-verbose", usage = "[default=false] Activates the verbose mode")
	public Boolean verbose = false;

	@Option(name = "-h", usage = "Prints this help")
	public Boolean h = false;

	/**
	 * Main
	 * 
	 * @param args
	 */
	public static void main(String[] args) {

		FlexfluxERA f = new FlexfluxERA();

		CmdLineParser parser = new CmdLineParser(f);

		try {
			parser.parseArgument(args);
		} catch (CmdLineException e) {
			System.err.println(e.getMessage());
			System.err.println(f.message);
			parser.printUsage(System.err);
			System.exit(0);
		}

		if (f.h) {
			System.err.println(f.message);
			parser.printUsage(System.out);
			System.exit(1);
		}

		if(f.verbose) {
			Vars.verbose = true;
		}
			
		
		Vars.decimalPrecision = f.precision;

		if (f.nThreads > 0) {
			Vars.maxThread = f.nThreads;
		} else {
			System.err.println("The number of threads must be at least 1");
			System.exit(0);
		}

		if (!new File(f.sbmlFile).isFile()) {
			System.err.println("Error : file " + f.sbmlFile + " not found");
			System.exit(0);
		}

		if (!new File(f.constraintFile).isFile()) {
			System.err.println("Error : constraint file " + f.constraintFile
					+ " not found");
			System.exit(0);
		}

		if (!new File(f.objectiveFile).isFile()) {
			System.err.println("Error : constraint file " + f.objectiveFile
					+ " not found");
			System.exit(0);
		}

		if (!new File(f.inputRandomParameterFile).isFile()) {
			System.err.println("Error : input random parameter file "
					+ f.inputRandomParameterFile + " not found");
			System.exit(0);
		}

		HashMap<String, String> objectives = f
				.loadObjectiveFile(f.objectiveFile);

		if (objectives == null) {
			System.err.println("Error in reading the objective file "
					+ f.objectiveFile);
			System.exit(0);
		}

		ArrayList<InputRandomParameters> inputRandomParameterList = f
				.loadInputRandomParameterFile(f.inputRandomParameterFile);
		if (inputRandomParameterList == null) {
			System.err.println("Error in reading the input file "
					+ f.inputRandomParameterFile);
			System.exit(0);
		}

		Bind bind = null;

		try {
			if (f.solver.equals("CPLEX")) {
				bind = new CplexBind();
			} else if (f.solver.equals("GLPK")) {
				Vars.maxThread = 1;
				bind = new GLPKBind();
			} else {
				System.err.println("Unknown solver name");
				parser.printUsage(System.err);
				System.exit(0);
			}
		} catch (UnsatisfiedLinkError e) {
			System.err
					.println("Error, the solver "
							+ f.solver
							+ " cannot be found. Check your solver installation and the configuration file, or choose a different solver (-sol).");
			System.exit(0);
		} catch (NoClassDefFoundError e) {
			System.err
					.println("Error, the solver "
							+ f.solver
							+ " cannot be found. There seems to be a problem with the .jar file of "
							+ f.solver + ".");
			System.exit(0);
		}

		bind.setLoadObjective(false);
		
		bind.loadSbmlNetwork(f.sbmlFile, f.extended);
		if (f.constraintFile != "") {
			bind.loadConditionsFile(f.constraintFile);
		}

		if (f.intFile != "") {
			bind.loadInteractionsFile(f.intFile);
		}

		bind.prepareSolver();

		ERAAnalysis a = new ERAAnalysis(bind, f.nbSim, objectives,
				inputRandomParameterList, f.meanGaussian, f.stdGaussian,
				f.minInputs, f.maxInputs);

		ERAResult r = a.runAnalysis();

		r.inchlibPath = f.inchlibPath;

		r.writeToFile(f.outName);

		if (f.plot) {
			r.plot();
		}
	}

	/**
	 * Reads the file containing for each input its value of inhibition, of
	 * activation and its weight during the selection of the activated inputs
	 * during the simulation
	 * 
	 * @param inputFile
	 * @return a list of {@link InputRandomParameters} or null if problem
	 */
	public ArrayList<InputRandomParameters> loadInputRandomParameterFile(
			String inputFile) {

		ArrayList<InputRandomParameters> list = new ArrayList<InputRandomParameters>();

		BufferedReader in = null;

		try {
			in = new BufferedReader(new FileReader(inputFile));

			String line;

			int nbLine = 0;

			while ((line = in.readLine()) != null) {
				if (line.startsWith("#") || line.equals("")) {
					nbLine++;
					continue;
				}
				
				nbLine++;
				
				String tab[] = line.split("\t");

				if (tab.length != 4) {
					System.err.println("Error in the input file :  line "
							+ nbLine + " does not contain four columns");
					return null;
				}

				String inputId = tab[0];
				String inhibitionValueStr = tab[1];
				String activationValueStr = tab[2];
				String weightStr = tab[3];

				double inhibitionValue;
				double activationValue;
				int weight;

				try {
					inhibitionValue = Double.parseDouble(inhibitionValueStr);
				} catch (NumberFormatException e) {
					System.err.println("Inhibition value badly formatted line "
							+ nbLine);
					return null;
				}

				try {
					activationValue = Double.parseDouble(activationValueStr);
				} catch (NumberFormatException e) {
					System.err.println("Activation value badly formatted line "
							+ nbLine);
					return null;
				}

				try {
					weight = Integer.parseInt(weightStr);
				} catch (NumberFormatException e) {
					System.err.println("Weight value badly formatted line "
							+ nbLine);
					return null;
				}

				InputRandomParameters inputRandomParameters = new InputRandomParameters(
						inputId, inhibitionValue, activationValue, weight);

				list.add(inputRandomParameters);
				

			}
		} catch (FileNotFoundException e) {
			System.err.println(objectiveFile + " not found");
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			System.err.println("Error while reading " + objectiveFile);
			e.printStackTrace();
		}

		finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					System.err.println("Error while closing the input file");
					e.printStackTrace();
				}
			}
		}

		return list;

	}

}
