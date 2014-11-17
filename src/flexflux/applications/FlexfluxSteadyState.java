package flexflux.applications;

import java.io.File;
import java.util.HashMap;

import org.kohsuke.args4j.Option;

import parsebionet.biodata.BioEntity;
import flexflux.analyses.SteadyStateAnalysis;
import flexflux.analyses.result.SteadyStateAnalysisResult;
import flexflux.general.Constraint;
import flexflux.general.Vars;
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

	public static void main(String[] args) {
		FlexfluxSteadyState f = new FlexfluxSteadyState();

		f.parseArguments(args);

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

	@Override
	public String getMessage() {
		return message;
	}

	@Override
	public String getExample() {
		return example;
	}
}
