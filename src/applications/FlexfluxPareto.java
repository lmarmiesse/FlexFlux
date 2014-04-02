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
package applications;

import general.Bind;
import general.CplexBind;
import general.GLPKBind;
import general.Vars;

import java.io.File;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import analyses.Analysis;
import analyses.ParetoAnalysis;
import analyses.result.AnalysisResult;

/**
 * 
 * <p>
 * The goal of this analysis is to determine what objective (or set of
 * objectives) an organism is optimizing.
 * </p>
 * 
 * <p>
 * It take as an argument a file containing the objective functions to test and
 * experimental values, and finds out (in one, two and three dimensions) what
 * objective optimization is closest to the experimental values.
 * </p>
 * 
 * <p>
 * The objectives that are optimized are those for witch the experimental values
 * are the closest to the pareto surface.
 * </p>
 * 
 * 
 * <p>
 * The pareto surface is the surface in which all points are pareto optimal.
 * This means that to increase the value of an objective, you must decrease
 * another.
 * </p>
 * 
 * <p>
 * In this analysis, the objective function of the condition file is optional
 * and ignored if present.
 * </p>
 * 
 * 
 * @author lmarmiesse 6 mars 2013
 * 
 */
public class FlexfluxPareto {

	public String message = "FlexfluxPareto [options...]\n"
			+ "The goal of this analysis is to determine what objective (or set of objectives) an organism is optimizing.\n"

			+ "It take as an argument a file containing the objective functions to test and"
			+ " experimental values, and finds out\n (in one, two and three dimensions) what"
			+ " objective optimization is closest to the experimental values.";
	public String example = "Example : FlexfluxPareto -s network.xml -cond cond.txt -int int.txt -plot -e expFile";

	@Option(name = "-s", usage = "Sbml file path", metaVar = "File", required = true)
	public String sbmlFile = "";

	@Option(name = "-cond", usage = "Condition file path", metaVar = "File")
	public String condFile = "";

	@Option(name = "-int", usage = "[OPTIONAL]Interaction file path", metaVar = "File")
	public String intFile = "";

	@Option(name = "-exp", usage = "Path of file containing objective functions and experimental values", metaVar = "File", required = true)
	public String expFile = "";

	@Option(name = "-sol", usage = "Solver name", metaVar = "Solver")
	public String solver = "GLPK";

	@Option(name = "-plot", usage = "[OPTIONAL, default = false]Plots the results")
	public boolean plot = false;

	@Option(name = "-out", usage = "[OPTIONAL]Output folder name", metaVar = "File")
	public String outName = "";

	@Option(name = "-lib", usage = "[OPTIONAL, default = 0]Percentage of non optimality for new constraints", metaVar = "Double")
	public double liberty = 0;

	@Option(name = "-pre", usage = "[OPTIONAL, default = 6]Number of decimals of precision for calculations and results", metaVar = "Integer")
	public int precision = 6;

	@Option(name = "-ext", usage = "[OPTIONAL, default = false]Uses the extended SBML format")
	public boolean extended = false;

	@Option(name = "-all", usage = "[OPTIONAL, default = false]Plots all results. If false, plots 1D results and only the best result for 2D and 3D results")
	public boolean plotAll = false;

	@Option(name = "-h", usage = "Prints this help")
	public boolean h = false;

	public static void main(String[] args) {

		FlexfluxPareto f = new FlexfluxPareto();

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
			System.out.println(f.message);
			parser.printUsage(System.out);
			System.exit(1);
		}

		Vars.libertyPercentage = f.liberty;
		Vars.decimalPrecision = f.precision;


		if (!new File(f.expFile).isFile()) {
			System.err.println(f.expFile + " is not a valid file path");
			System.exit(1);
		}

		Bind bind = null;

		if (!new File(f.sbmlFile).isFile()) {
			System.err.println("Error : file " + f.sbmlFile + " not found");
			System.exit(0);
		}
		if (!new File(f.condFile).isFile() && !f.condFile.equals("")) {
			System.err.println("Error : file " + f.condFile + " not found");
			System.exit(0);
		}

		try {
			if (f.solver.equals("CPLEX")) {
				bind = new CplexBind();
			} else if (f.solver.equals("GLPK")) {
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

		// it ignores the original objective function
		bind.setLoadObjective(false);
		bind.loadSbmlNetwork(f.sbmlFile, f.extended);
		if (f.condFile != "") {
			bind.loadConditionsFile(f.condFile);
		}
		if (f.intFile != "") {
			bind.loadInteractionsFile(f.intFile);
		}
		bind.prepareSolver();

		Analysis analysis = new ParetoAnalysis(bind, f.expFile, f.plotAll);

		AnalysisResult result = analysis.runAnalysis();

		if (f.plot) {
			result.plot();
		}
		if (!f.outName.equals("")) {
			result.writeToFile(f.outName);
		}
		bind.end();
	}

}
