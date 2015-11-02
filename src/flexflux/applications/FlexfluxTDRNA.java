package flexflux.applications;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.kohsuke.args4j.Option;

import parsebionet.biodata.BioEntity;
import flexflux.analyses.TDRNAAnalysis;
import flexflux.analyses.result.TDRNAAnalysisResult;
import flexflux.general.Constraint;
import flexflux.input.ConstraintsFileReader;
import flexflux.input.SBMLQualReader;
import flexflux.interaction.InteractionNetwork;
import flexflux.interaction.RelationFactory;

public class FlexfluxTDRNA extends FFApplication {
	
	public static boolean requiresSolver = false;

	public static String message = "Time-dependant updates of a regulatory network state.";

	public String example = "Example : FlexfluxTDRNA -int int.sbml -plot -out out.txt";
	
	@Option(name = "-reg", usage = "Regulation file path", metaVar = "File - in", required = true)
	public String regFile = "";
	
	@Option(name = "-plot", usage = "[OPTIONAL, default = false]Plots the results")
	public boolean plot = false;

	@Option(name = "-out", usage = "[OPTIONAL]Output file name", metaVar = "File - out")
	public String outName = "";
	
	@Option(name = "-t", usage = "[OPTIONAL, default = 0.1]Time between each iteration in hour", metaVar = "Double")
	public double deltaT = 0.1;

	@Option(name = "-it", usage = "[OPTIONAL, default = 150]Number of iterations", metaVar = "Integer")
	public int iterations = 150;
	
	
	

	public static void main(String[] args) {
		
		
		FlexfluxTDRNA f = new FlexfluxTDRNA();

		f.parseArguments(args);

		if (!new File(f.regFile).isFile()) {
			System.err.println("Error : file " + f.regFile + " not found");
			System.exit(0);
		}

		InteractionNetwork intNet = SBMLQualReader.loadSbmlQual(f.regFile,
				new InteractionNetwork(), new RelationFactory());

		
		TDRNAAnalysis analysis = new TDRNAAnalysis(intNet,f.iterations,f.deltaT);

		TDRNAAnalysisResult res = analysis.runAnalysis();

		if (f.plot) {
			res.plot();
		}

		if (!f.outName.equals("")) {
			res.writeToFile(f.outName);
		}
	}
	
	
	public String getMessage() {
		return message;
	}
	

}
