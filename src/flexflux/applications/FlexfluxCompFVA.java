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

import flexflux.analyses.CompFVAAnalysis;
import flexflux.analyses.FVAAnalysis;
import flexflux.analyses.result.CompFVAResult;
import flexflux.analyses.result.FVAResult;
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
 * Compares the results of two FVA analyses given a metabolic network, an
 * objective function and two different set of constraints.
 * </p>
 * An FVA analysis consists in getting the optimal value for the objective
 * function, setting this value as a constraint and, given a list of entities,
 * minimize and maximize their values. If no entity is specified in argument -e,
 * the FVA analysis is performed on all reactions.
 * 
 * @author lmarmiesse 6 mars 2013
 * 
 */
public class FlexfluxCompFVA extends FFApplication{
	
	// order for the graphical version
	public static int order = 5;
	
	public static String message =  "Compares the results of two FVA analyses given a metabolic network, an objective function and two different set of constraints.\n"
			+ "An FVA analysis consists in getting the optimal value for the objective function, setting this value as a\n"
			+ "constraint and, given a list of entities, minimize and maximize their values.\n"
			+ "If no entity is specified in argument -e, the FVA analysis is performed on all reactions.";

	public String example = "Example 1 : FlexfluxCompFVA -s network.xml -cond cond.txt -cond2 cond2.txt -int int.txt -plot -out out.txt\n"
			+ "Example 2 : FlexfluxCompFVA -s network.xml -cond cond.txt -cond2 cond2.txt -int int.txt -plot -out out.txt -e \"R1 R2 G1 G2\"\n";

	@Option(name = "-s", usage = "Metabolic network file path (SBML format)", metaVar = "File - in", required = true)
	public String sbmlFile = "";

	@Option(name = "-cons", usage = "First constraint file path", metaVar = "File - in", required = true)
	public String consFile = "";

	@Option(name = "-cons2", usage = "Second constraint file path", metaVar = "File - in", required = true)
	public String consFile2 = "";

	@Option(name = "-reg", usage = "[OPTIONAL]Regulation file path", metaVar = "File - in")
	public String regFile = "";

	@Option(name = "-sol", usage = "Solver name", metaVar = "Solver")
	public String solver = "GLPK";

	@Option(name = "-plot", usage = "[OPTIONAL, default = false]Plots the results")
	public boolean plot = false;

	@Option(name = "-e", usage = "[OPTIONAL]Biological entities to perform the FVA analysis on (Space-separated list of entities, example : \"R1 R2 G1 G2\"). If empty, FVA is done on all reactions", metaVar = "String")
	public String entities = "";

	@Option(name = "-out", usage = "[OPTIONAL]Output file name", metaVar = "File - out")
	public String outName = "";

	@Option(name = "-n", usage = "[OPTIONAL, default = number of available processors]Number of threads", metaVar = "Integer")
	public int nThreads = Runtime.getRuntime().availableProcessors();

	@Option(name = "-lib", usage = "[OPTIONAL, default = 0]Percentage of non optimality for new constraints", metaVar = "Double")
	public double liberty = 0;

	@Option(name = "-pre", usage = "[OPTIONAL, default = 6]Number of decimals of precision for calculations and results", metaVar = "Integer")
	public int precision = 6;

	@Option(name = "-ext", usage = "[OPTIONAL, default = false]Uses the extended SBML format")
	public boolean extended = false;

	
	public static void main(String[] args) {

		FlexfluxCompFVA f = new FlexfluxCompFVA();

		f.parseArguments(args);

		Vars.libertyPercentage = f.liberty;
		Vars.decimalPrecision = f.precision;

		if (f.nThreads > 0) {
			Vars.maxThread = f.nThreads;
		} else {
			System.err.println("The number of threads must be at least 1");
			System.exit(0);
		}

		Bind bind = null;
		Bind bind2 = null;

		if (!new File(f.sbmlFile).isFile()) {
			System.err.println("Error : file " + f.sbmlFile + " not found");
			System.exit(0);
		}
		if (!new File(f.consFile).isFile()) {
			System.err.println("Error : file " + f.consFile + " not found");
			System.exit(0);
		}
		if (!new File(f.consFile2).isFile()) {
			System.err.println("Error : file " + f.consFile2 + " not found");
			System.exit(0);
		}
		try {
			if (f.solver.equals("CPLEX")) {
				bind = new CplexBind();
				bind2 = new CplexBind();
			} else if (f.solver.equals("GLPK")) {
				Vars.maxThread = 1;
				bind = new GLPKBind();
				bind2 = new GLPKBind();
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
		bind2.loadSbmlNetwork(f.sbmlFile, f.extended);

		bind.loadConstraintsFile(f.consFile);
		bind2.loadConstraintsFile(f.consFile2);
		if (f.regFile != "") {
			bind.loadRegulationFile(f.regFile);
			bind2.loadRegulationFile(f.regFile);
		}
		bind.prepareSolver();
		bind2.prepareSolver();

		Map<String, BioEntity> entitiesMap = new HashMap<String, BioEntity>();

		if (!f.entities.equals("")) {
			String[] entitiesArray = f.entities.split("\\s+");
			for (int i = 0; i < entitiesArray.length; i++) {

				BioEntity b = bind.getInteractionNetwork().getEntity(
						entitiesArray[i]);
				if (b == null) {
					System.err.println("Unknown entity " + entitiesArray[i]);
					f.parser.printUsage(System.err);
					System.exit(0);
				}

				entitiesMap.put(b.getId(), b);
			}
		}

		FVAAnalysis analysis;
		FVAAnalysis analysis2;
		if (entitiesMap.size() > 0) {
			analysis = new FVAAnalysis(bind, entitiesMap, null);
			analysis2 = new FVAAnalysis(bind2, entitiesMap, null);
		} else {
			analysis = new FVAAnalysis(bind, null, null);
			analysis2 = new FVAAnalysis(bind2, null, null);
		}
		if(Vars.verbose){
			System.out.println("\nFirst FVA:");
		}
		FVAResult result = analysis.runAnalysis();
		if(Vars.verbose){
			System.out.println("\nSecond FVA:");
		}
		FVAResult result2 = analysis2.runAnalysis();

		CompFVAAnalysis compAnalysis = new CompFVAAnalysis(null, result,
				result2);
		CompFVAResult compAnalysisResult = compAnalysis.runAnalysis();

		if (f.plot) {
			compAnalysisResult.plot();
		}
		if (!f.outName.equals("")) {
			compAnalysisResult.writeToFile(f.outName);
		}
		if (f.web) {
			compAnalysisResult.writeHTML(f.outName+".html");
		}
		

		bind.end();
	}
	
	public String getMessage() {
		return message;
	}
}
