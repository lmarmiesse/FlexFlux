package flexflux.applications;

import java.io.File;

import org.kohsuke.args4j.Option;

import flexflux.analyses.ROBAAnalysis;
import flexflux.analyses.result.ROBAResult;
import flexflux.condition.ListOfConditions;
import flexflux.general.Bind;
import flexflux.general.ConstraintType;
import flexflux.general.CplexBind;
import flexflux.general.GLPKBind;
import flexflux.general.Vars;
import flexflux.objective.ListOfObjectives;

public class FlexfluxROBA extends FFApplication {

	public Boolean graphicalVersion = false;
	
	public static String applicationName = FlexfluxROBA.class
			.getSimpleName();

	public static String message = applicationName
			+ "[options...]\n"
			+ "Computes Robustness of the network among a set of objective functions "
			+ "in front of random perturbations.";

	@Option(name = "-s", usage = "Sbml file path", metaVar = "File - in", required = true)
	public String sbmlFile = "";

	@Option(name = "-cond", usage = "[OPTIONAL] " + ListOfConditions.fileFormat, metaVar = "File - in", required = false)
	public String conditionFile = "";

	@Option(name = "-cons", usage = "[OPTIONAL] File containing the constraints applied on the metabolic network", metaVar = "File - in", required = false)
	public String constraintFile = "";

	@Option(name = "-o", usage = "File containing the objective functions", metaVar = "File - in", required = true)
	public String objectiveFile = "";

	@Option(name = "-reg", usage = "[OPTIONAL] Regulation file path", metaVar = "File - in")
	public String regFile = "";

	@Option(name = "-plot", usage = "[OPTIONAL, default = false] Plots the results")
	public Boolean plot = false;

	@Option(name = "-out", usage = "[OPTIONAL] Output file directory", metaVar = "File - out")
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

	@Option(name = "-fixConditions", usage = "[OPTIONAL] If true, the conditions set in the condition file are fixed and can not be updated by the regulation network.")
	public Boolean fixConditions = false;
	
	/**
	 * Main
	 * 
	 * @param args
	 */
	public static void main(String[] args) {

		FlexfluxROBA f = new FlexfluxROBA();

		f.parseArguments(args);

		if (f.verbose) {
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
			System.err.println("Error : objective file " + f.objectiveFile
					+ " not found");
			System.exit(0);
		}

		if (!new File(f.conditionFile).isFile()) {
			System.err.println("Error : condition file " + f.conditionFile
					+ " not found");
			System.exit(0);
		}

		ListOfObjectives objectives = new ListOfObjectives();

		Boolean flag = objectives.loadObjectiveFile(f.objectiveFile);

		if (flag == false) {
			System.err.println("Error in reading the objective file "
					+ f.objectiveFile);
			System.exit(0);
		}
		
		/**
		 * Load the condition file
		 */
		ListOfConditions conditions = new ListOfConditions();
		
		flag = conditions.loadConditionFile(f.conditionFile, ConstraintType.DOUBLE);
		
		if (flag == false) {
			System.err.println("Error in reading the condition file "
					+ f.conditionFile);
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
				f.parser.printUsage(System.err);
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
			bind.loadConstraintsFile(f.constraintFile);
		}

		if (f.regFile != "") {
			bind.loadRegulationFile(f.regFile);
		}

		bind.prepareSolver();

		ROBAAnalysis a = new ROBAAnalysis(bind, objectives, conditions, f.fixConditions);

		ROBAResult r = a.runAnalysis();

		r.inchlibPath = f.inchlibPath;

		r.writeToFile(f.outName);

		if (f.plot) {
			r.plot();
		}
	}

	@Override
	public String getMessage() {
		return message;
	}

	@Override
	public String getExample() {
		return "";
	}

}
