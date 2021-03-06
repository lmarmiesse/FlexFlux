package flexflux.applications;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.kohsuke.args4j.Option;

import parsebionet.biodata.BioEntity;
import flexflux.analyses.RSAAnalysis;
import flexflux.analyses.result.RSAAnalysisResult;
import flexflux.general.Constraint;
import flexflux.input.ConstraintsFileReader;
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

public class FlexfluxRSA extends FFApplication {
	
	//order for the graphical version
	public static int order = 1;

	public static boolean requiresSolver = false;

	public static String message = "Finds an attractor or the steady state of a given regulatory network with initial values.";

	public String example = "Example : FlexfluxRSA -reg int.sbml -plot -out out.txt";

	@Option(name = "-reg", usage = "Regulation file path", metaVar = "File - in", required = true)
	public String regFile = "";

	@Option(name = "-cons", usage = "[OPTIONAL]Constraints file path", metaVar = "File - in", required = false)
	public String consFile = "";

	@Option(name = "-plot", usage = "[OPTIONAL, default = false]Plots the results")
	public boolean plot = false;

	@Option(name = "-out", usage = "[OPTIONAL]Output file name", metaVar = "File - out")
	public String outName = "";

	public static void main(String[] args) {
		FlexfluxRSA f = new FlexfluxRSA();

		f.parseArguments(args);

		if (!new File(f.regFile).isFile()) {
			System.err.println("Error : file " + f.regFile + " not found");
			System.exit(0);
		}

		InteractionNetwork intNet = SBMLQualReader.loadSbmlQual(f.regFile,
				new InteractionNetwork(), new RelationFactory());


		Map<BioEntity, Constraint> simpleConstraints = new HashMap<BioEntity, Constraint>();

		if (f.consFile != "") {

			ConstraintsFileReader r = new ConstraintsFileReader(f.consFile,
					intNet);
			r.readConstraintsFile();

			simpleConstraints = r.simpleConstraints;

		}

		RSAAnalysis analysis = new RSAAnalysis(intNet, simpleConstraints);


		RSAAnalysisResult res = analysis.runAnalysis();

		if (f.plot) {
			res.plot();
		}

		if (!f.outName.equals("")) {
			res.writeToFile(f.outName);
		}
		
		if (f.web) {
			res.writeHTML(f.outName+".html");
		}

	}
	
	public String getMessage() {
		return message;
	}
}
