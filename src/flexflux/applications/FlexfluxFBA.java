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

import java.io.File;

import org.kohsuke.args4j.Option;

import flexflux.analyses.Analysis;
import flexflux.analyses.FBAAnalysis;
import flexflux.analyses.result.FBAResult;
import flexflux.general.Bind;
import flexflux.general.CplexBind;
import flexflux.general.GLPKBind;
import flexflux.general.Vars;

/**
 * 
 * Computes an FBA analysis given a metabolic network, an objective function and
 * constraints.
 * 
 * @author lmarmiesse 6 mars 2013
 * 
 */
public class FlexfluxFBA extends FFApplication {

	// order for the graphical version
	public static int order = 3;

	public static String message = "FlexfluxFBA [options...]\n"
			+ "Computes an FBA given a metabolic network, an objective function and constraints.\n"
			+ "Constraints can be obtained with calculated steady-states of a given regulatory network.";

	public String example = "Example : FlexfluxFBA -s network.xml -cond cond.txt -int int.txt -plot -out out.txt -states res.tab";

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

	@Option(name = "-out", usage = "[OPTIONAL]Output file name", metaVar = "File")
	public String outName = "";

	@Option(name = "-states", usage = "[OPTIONAL]The states of the regulatory network are saved in the indicated file name", metaVar = "File")
	public String stateFile = "";

	@Option(name = "-lib", usage = "[OPTIONAL, default = 0]Percentage of non optimality for new constraints", metaVar = "Double")
	public double liberty = 0;

	@Option(name = "-pre", usage = "[OPTIONAL, default = 6]Number of decimals of precision for calculations and results", metaVar = "Integer")
	public int precision = 6;

	@Option(name = "-ext", usage = extParameterDescription)
	public boolean extended = false;

	@Option(name = "-senFile", usage = "[OPTIONAL] A sensitivity analysis is performed and saved in the indicated file name", metaVar = "File")
	public String senFile = "";

	public static void main(String[] args) {

		FlexfluxFBA f = new FlexfluxFBA();

		f.parseArguments(args);

		Vars.libertyPercentage = f.liberty;
		Vars.decimalPrecision = f.precision;

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

		if (f.stateFile != "") {
			Vars.writeInteractionNetworkStates = true;
			bind.statesFileName = f.stateFile;
		}

		bind.loadSbmlNetwork(f.sbmlFile, f.extended);
		if (f.consFile != "") {
			bind.loadConstraintsFile(f.consFile);
		}
		if (f.regFile != "") {
			bind.loadRegulationFile(f.regFile);
		}
		bind.prepareSolver();

		Analysis analysis = new FBAAnalysis(bind);
		FBAResult result = (FBAResult) analysis.runAnalysis();

		if (f.plot) {
			result.plot();
		}
		if (!f.outName.equals("")) {
			result.writeToFile(f.outName);
		}

		if (f.senFile != "") {
			result.sensitivityAnalysis(f.senFile);
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
