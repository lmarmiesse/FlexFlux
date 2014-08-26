package flexflux.applications;

import java.io.File;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import flexflux.analyses.ConditionComparisonAnalysis;
import flexflux.analyses.result.AnalysisResult;
import flexflux.analyses.result.conditionComparison.ConditionComparisonResult;
import flexflux.general.Bind;
import flexflux.general.ConstraintType;
import flexflux.general.CplexBind;
import flexflux.general.GLPKBind;
import flexflux.general.Vars;

public class FlexfluxConditionComparison {

	public String applicationName = FlexfluxConditionComparison.class
			.getSimpleName();

	public String message = applicationName
			+ "[options...]\n"
			+ "Compares the list of essential/usable genes/reactions in several conditions";

	public String example = applicationName
			+ " -s network.xml -c conditionFile.tab -o objectives.txt -int int.txt  -plot -out out.tab";

	@Option(name = "-s", usage = "Sbml file path", metaVar = "File", required = true)
	public String sbmlFile = "";

	@Option(name = "-c", usage = "File containing several conditions", metaVar = "File", required = true)
	public String conditionFile = "";

	@Option(name = "-o", usage = "File containing the objectiver functions", metaVar = "File", required = true)
	public String objectiveFile = "";

	@Option(name = "-int", usage = "[OPTIONAL]Interaction file path", metaVar = "File")
	public String intFile = "";

	@Option(name = "-type", usage = "[OPTIONAL] Type of the condition states", metaVar = "[BINARY,INTEGER,DOUBLE]")
	public String type = "BINARY";

	@Option(name = "-plot", usage = "[OPTIONAL, default = false]Plots the results")
	public Boolean plot = false;

	@Option(name = "-out", usage = "[OPTIONAL]Output file name", metaVar = "File")
	public String outName = "";

	@Option(name = "-n", usage = "[OPTIONAL, default = number of available processors]Number of threads", metaVar = "Integer")
	public int nThreads = Runtime.getRuntime().availableProcessors();

	@Option(name = "-lib", usage = "[OPTIONAL, default = 0]Percentage of non optimality for new constraints", metaVar = "Double")
	public double liberty = 0;

	@Option(name = "-pre", usage = "[OPTIONAL, default = 6]Number of decimals of precision for calculations and results", metaVar = "Integer")
	public int precision = 6;

	@Option(name = "-ext", usage = "[OPTIONAL, default = false]Uses the extended SBML format")
	public Boolean extended = false;

	@Option(name = "-sol", usage = "Solver name", metaVar = "Solver")
	public String solver = "GLPK";

	@Option(name = "-h", usage = "Prints this help")
	public Boolean h = false;

	public static void main(String[] args) {

		FlexfluxConditionComparison f = new FlexfluxConditionComparison();

		CmdLineParser parser = new CmdLineParser(f);

		try {
			parser.parseArgument(args);
		} catch (CmdLineException e) {
			System.err.println(e.getMessage());
			System.err.println(f.message);
			parser.printUsage(System.err);
			System.err.println(f.example);
			System.exit(0);
		}

		if (f.h) {
			System.err.println(f.message);
			parser.printUsage(System.out);
			System.exit(1);
		}

		Vars.libertyPercentage = f.liberty;
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

		if (!new File(f.conditionFile).isFile()) {
			System.err
					.println("Error : file " + f.conditionFile + " not found");
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

		/**
		 * Loads the metabolic network
		 */
		bind.loadSbmlNetwork(f.sbmlFile, f.extended);

		ConstraintType c;
		if (f.type == "BINARY") {
			c = ConstraintType.BINARY;
		} else if (f.type == "INTEGER") {
			c = ConstraintType.INTEGER;
		} else {
			c = ConstraintType.DOUBLE;
		}

		ConditionComparisonAnalysis a = new ConditionComparisonAnalysis(null,
				f.sbmlFile, f.intFile, f.conditionFile, f.objectiveFile, c, f.extended, 
				f.solver);
		
		AnalysisResult r = a.runAnalysis();

		if (f.plot) {
			r.plot();
		}
		if (!f.outName.equals("")) {
			r.writeToFile(f.outName);
		}

		bind.end();

	}

}
