package flexflux.applications;

import java.io.File;
import java.util.HashMap;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import parsebionet.biodata.BioEntity;
import flexflux.analyses.SteadyStateAnalysis;
import flexflux.analyses.result.SteadyStateAnalysisResult;
import flexflux.general.Constraint;
import flexflux.general.Vars;
import flexflux.input.InteractionFileReader;
import flexflux.input.SBMLQualReader;
import flexflux.interaction.InteractionNetwork;
import flexflux.interaction.RelationFactory;

/**
 * 
 * Find an attractor or the steady state of a given interaction network with
 * initial values.
 * 
 * 
 * @author lmarmiesse 29 septembre 2014
 * 
 */

public class FlexfluxSteadyState extends FFApplication {

	public static boolean requiresSolver = false;

	public static String message = "FlexfluxSteadyState [options...]\n"
			+ "Find an attractor or the steady state of a given interaction network with initial values.";

	public String example = "Example : FlexfluxSteadyState -int int.sbml -plot -out out.txt";

	@Option(name = "-int", usage = "Interaction file path", metaVar = "File", required = true)
	public String intFile = "";

	@Option(name = "-plot", usage = "[OPTIONAL, default = false]Plots the results")
	public boolean plot = false;

	@Option(name = "-out", usage = "[OPTIONAL]Output file name", metaVar = "File")
	public String outName = "";

	@Option(name = "-pre", usage = "[OPTIONAL, default = 6]Number of decimals of precision for calculations and results", metaVar = "Integer")
	public int precision = 6;

	@Option(name = "-h", usage = "Prints this help")
	public boolean h = false;

	public static void main(String[] args) {

		FlexfluxSteadyState f = new FlexfluxSteadyState();

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

		Vars.decimalPrecision = f.precision;

		if (!new File(f.intFile).isFile()) {
			System.err.println("Error : file " + f.intFile + " not found");
			System.exit(0);
		}

		InteractionNetwork intNet = null;

		intNet = SBMLQualReader.loadSbmlQual(f.intFile,
				new InteractionNetwork(), new RelationFactory());

		SteadyStateAnalysis analysis = new SteadyStateAnalysis(null, intNet,
				new HashMap<BioEntity, Constraint>());

		SteadyStateAnalysisResult res = analysis.runAnalysis();

		if (f.plot) {
			res.plot();
		}

		if (!f.outName.equals("")) {
			res.writeToFile(f.outName);
		}

	}
}
