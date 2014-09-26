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

	@Option(name = "-mdr", usage = "[OPTIONAL] Reaction metadata file used for heatmap analysis", metaVar = "File", required = false)
	public String metaReactionDataFile = "";

	@Option(name = "-mdg", usage = "[OPTIONAL] Gene metadata file used for heatmap analysis", metaVar = "File", required = false)
	public String metaGeneDataFile = "";
	
	@Option(name = "-mdreg", usage = "[OPTIONAL] Regulator metadata file used for heatmap analysis", metaVar = "File", required = false)
	public String metaRegulatorDataFile = "";

	@Option(name = "-mdSep", usage = "[Default=,] Separator for the columns in the metaData file", metaVar = "String", required = false)
	public String mdSep = ",";

	@Option(name = "-cond", usage = "File containing several conditions", metaVar = "File", required = true)
	public String conditionFile = "";

	@Option(name = "-cons", usage = "[OPTIONAL] File containing the constraints applied on the metabolic network", metaVar = "File", required = false)
	public String constraintFile = "";

	@Option(name = "-o", usage = "File containing the objective functions", metaVar = "File", required = true)
	public String objectiveFile = "";

	@Option(name = "-int", usage = "[OPTIONAL] Interaction file path", metaVar = "File")
	public String intFile = "";

	@Option(name = "-type", usage = "[OPTIONAL, default=DOUBLE] Type of the condition states", metaVar = "[BINARY,INTEGER,DOUBLE]")
	public String type = "DOUBLE";

	@Option(name = "-plot", usage = "[OPTIONAL, default = false] Plots the results")
	public Boolean plot = false;

	@Option(name = "-out", usage = "[OPTIONAL] Output file name", metaVar = "File")
	public String outName = "";

	@Option(name = "-n", usage = "[OPTIONAL, default = number of available processors] Number of threads", metaVar = "Integer")
	public int nThreads = Runtime.getRuntime().availableProcessors();

	@Option(name = "-lib", usage = "[OPTIONAL, default = 0] Percentage of non optimality for new constraints", metaVar = "Double")
	public double liberty = 0;

	@Option(name = "-pre", usage = "[OPTIONAL, default = 6] Number of decimals of precision for calculations and results", metaVar = "Integer")
	public int precision = 6;

	@Option(name = "-ext", usage = "[OPTIONAL, default = false] Uses the extended SBML format")
	public Boolean extended = false;

	@Option(name = "-minFlux", usage = "[OPTIONAL, default = false] Minimize the flux when performing the fva")
	public Boolean minFlux = false;

	@Option(name = "-sol", usage = "Solver name", metaVar = "Solver")
	public String solver = "GLPK";

	@Option(name = "-inchlibPath", usage = "[default=/usr/local/inchlib_clust/inchlib_clust.py]", metaVar = "String")
	public String inchlibPath = "/usr/local/inchlib_clust/inchlib_clust.py";

	@Option(name = "-noReactionAnalysis", usage = "Don't perform reaction essentiality analysis")
	public Boolean noReactionAnalysis = false;

	@Option(name = "-noGeneAnalysis", usage = "Don't perform gene essentiality analysis")
	public Boolean noGeneAnalysis = false;

	@Option(name = "-noRegulatorAnalysis", usage = "Don't perform regulator essentiality analysis")
	public Boolean noRegulatorAnalysis = false;

	@Option(name = "-verbose", usage = "[default=false] Activates the verbose mode")
	public Boolean verbose = false;

	
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

		ConstraintType c;
		if (f.type == "BINARY") {
			c = ConstraintType.BINARY;
		} else if (f.type == "INTEGER") {
			c = ConstraintType.INTEGER;
		} else {
			c = ConstraintType.DOUBLE;
		}

		ConditionComparisonAnalysis a = new ConditionComparisonAnalysis(null,
				f.sbmlFile, f.intFile, f.conditionFile, f.constraintFile,
				f.objectiveFile, c, f.extended, f.solver,
				f.metaReactionDataFile, f.metaGeneDataFile, f.metaRegulatorDataFile, f.mdSep,
				f.inchlibPath, f.minFlux, f.noReactionAnalysis,
				f.noGeneAnalysis, f.noRegulatorAnalysis, f.liberty, f.precision);

		if(f.verbose) 
		{
			Vars.verbose = true;
		}
		
		AnalysisResult r = a.runAnalysis();

		if (f.plot) {
			r.plot();
		}
		if (!f.outName.equals("")) {
			r.writeToFile(f.outName);
		}

	}

}
