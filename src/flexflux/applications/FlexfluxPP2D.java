/*******************************************************************************
 * Copyright INRA
 * 
 *  Contact: ludovic.cottret@toulouse.inra.fr
 * 
 * 
 * This software is governed by the CeCILL license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 * 
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 *  In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 *  The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL license and that you accept its terms.
 ******************************************************************************/
/**
 * 6 mars 2013 
 */
package flexflux.applications;

import flexflux.analyses.Analysis;
import flexflux.analyses.PP2DAnalysis;
import flexflux.analyses.result.AnalysisResult;
import flexflux.general.Bind;
import flexflux.general.CplexBind;
import flexflux.general.GLPKBind;
import flexflux.general.Vars;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.kohsuke.args4j.Option;

import parsebionet.biodata.BioEntity;

/**
 * 
 * <p>
 * Computes different FBA analysis given a metabolic network, an objective
 * function and constraints, by making a reaction flux change.
 * </p>
 * 
 * @author lmarmiesse 6 mars 2013
 * 
 */
public class FlexfluxPP2D extends FFApplication {

	public static String message = "FlexfluxPP2D\n"

			+ "Computes different FBA analysis given a metabolic network, an objective function and constraints, "
			+ "by making a reaction flux change.";

	public String example = "Example : FlexfluxReac -s network.xml -cond cond.txt -int int.txt -r R_EX_o2_e_ -init -10 -end 0 -plot -out out.txt";

	@Option(name = "-s", usage = "Sbml file path", metaVar = "File", required = true)
	public String sbmlFile = "";

	@Option(name = "-cons", usage = "Constraints file path", metaVar = "File", required = true)
	public String consFile = "";

	@Option(name = "-reg", usage = "[OPTIONAL]Regulation file path", metaVar = "File")
	public String regFile = "";

	@Option(name = "-sol", usage = "Solver name", metaVar = "Solver")
	public String solver = "GLPK";

	@Option(name = "-plot", usage = "[OPTIONAL, default = false]Plots the results")
	public boolean plot = false;

	@Option(name = "-r", usage = "Name of the reaction to test", metaVar = "String", required = true)
	public String reac = "";

	@Option(name = "-out", usage = "[OPTIONAL]Output file name", metaVar = "File")
	public String outName = "";

	@Option(name = "-init", usage = "Initial flux value of the reaction to test", metaVar = "Double", required = true)
	public double init = 0;

	@Option(name = "-end", usage = "Final flux value of the reaction to test", metaVar = "Double", required = true)
	public double end = 0;

	@Option(name = "-f", usage = "[OPTIONAL, default = 0.1]Difference between each FBA for the reaction flux", metaVar = "Double")
	public double deltaF = 0.1;
	
	@Option(name = "-n", usage = "[OPTIONAL, default = number of available processors]Number of threads", metaVar = "Integer")
	public int nThreads = Runtime.getRuntime().availableProcessors();

	@Option(name = "-lib", usage = "[OPTIONAL, default = 0]Percentage of non optimality for new constraints", metaVar = "Double")
	public double liberty = 0;

	@Option(name = "-pre", usage = "[OPTIONAL, default = 6]Number of decimals of precision for calculations and results", metaVar = "Integer")
	public int precision = 6;

	@Option(name = "-ext", usage = "[OPTIONAL, default = false]Uses the extended SBML format")
	public boolean extended = false;

	public static void main(String[] args) {

		FlexfluxPP2D f = new FlexfluxPP2D();

		f.parseArguments(args);

		if (f.end < f.init) {
			System.err
					.println("The initial value should be less than the final value");
			f.parser.printUsage(System.err);
			System.exit(0);
		}

		Vars.libertyPercentage = f.liberty;
		Vars.decimalPrecision = f.precision;
		
		
		if (f.nThreads > 0) {
			Vars.maxThread = f.nThreads;
		} else {
			System.err.println("The number of threads must be at least 1");
			System.exit(0);
		}

		Bind bind = null;

		if (!new File(f.sbmlFile).isFile()) {
			System.err.println("Error : file " + f.sbmlFile + " not found");
			System.exit(0);
		}
		if (!new File(f.consFile).isFile()) {
			System.err.println("Error : file " + f.consFile + " not found");
			System.exit(0);
		}

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

		bind.loadSbmlNetwork(f.sbmlFile, f.extended);
		if (f.consFile != "") {
			bind.loadConstraintsFile(f.consFile);
		}
		if (f.regFile != "") {
			bind.loadRegulationFile(f.regFile);
		}
		bind.prepareSolver();

		Map<BioEntity, Double> map = new HashMap<BioEntity, Double>();

		if (bind.getInteractionNetwork().getEntity(f.reac) == null) {

			System.err.println("Error " + f.reac + " is unknown");
			System.exit(0);
		}

		map.put(bind.getInteractionNetwork().getEntity(f.reac), 1.0);

		Analysis analysis = new PP2DAnalysis(bind, map, f.reac, f.init, f.end,
				f.deltaF, true);
		AnalysisResult result = analysis.runAnalysis();

		if (f.plot) {
			result.plot();
		}
		if (!f.outName.equals("")) {
			result.writeToFile(f.outName);
		}

		bind.end();
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
