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
			+ "Find an attractor or the steady state of a given regulatory network with initial values.";

	public String example = "Example : FlexfluxSteadyState -int int.sbml -plot -out out.txt";

	@Option(name = "-reg", usage = "Regulation file path", metaVar = "File", required = true)
	public String regFile = "";

	@Option(name = "-plot", usage = "[OPTIONAL, default = false]Plots the results")
	public boolean plot = false;

	@Option(name = "-out", usage = "[OPTIONAL]Output file name", metaVar = "File")
	public String outName = "";

	public static void main(String[] args) {
		FlexfluxSteadyState f = new FlexfluxSteadyState();

		f.parseArguments(args);


		if (!new File(f.regFile).isFile()) {
			System.err.println("Error : file " + f.regFile + " not found");
			System.exit(0);
		}

		InteractionNetwork intNet = null;

		intNet = SBMLQualReader.loadSbmlQual(f.regFile,
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
