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
import flexflux.analyses.KOAnalysis;
import flexflux.analyses.result.AnalysisResult;
import flexflux.general.Bind;
import flexflux.general.CplexBind;
import flexflux.general.GLPKBind;
import flexflux.general.Vars;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import parsebionet.biodata.BioEntity;

/**
 * 
 * <p>Computes an KO analysis given a metabolic network, an objective function and
 * constraints.</p> A KO analysis consists in setting network entities values to 0,
 * and observe the effect on the objective function.
 * <p>There are 3 ways to perform this KO analysis :</p>
 * <ul>
 * <li> Mode 0: the KO analysis is performed on reactions. 
 * <li> Mode 1: the KO analysis is performed on genes. 
 * <li> If a list of biological entities is specified (with argument -e) the KO analysis is performed on
 * it.
 * </ul>
 * 
 * @author lmarmiesse 6 mars 2013
 * 
 */
public class FlexfluxKO extends FFApplication{

	public static String message = "FlexfluxKO\n"

			+ "Computes an KO analysis given a metabolic network, an objective function and constraints.\n"
			+ "A KO analysis consists in setting network entities values to 0, and observe the effect on the objective function.\n"
			+ "There are 3 ways to perform this KO analysis : \n"
			+ "- Mode 0: the KO analysis is performed on reactions.\n"
			+ "- Mode 1: the KO analysis is performed on genes.\n"
			+ "- If a list of biological entities is specified (with argument -e) the KO analysis is performed on it.";

	public String example = "Example 1 : FlexfluxKO -s network.xml -cond cond.txt -int int.txt -plot -out out.txt -mode 1\n"
			+ "Example 2 : FlexfluxKO -s network.xml -cond cond.txt -int int.txt -plot -out out.txt -e \"R1 R2 G1 G2\"\n";

	@Option(name = "-s", usage = "Sbml file path", metaVar = "File", required = true)
	public String sbmlFile = "";

	@Option(name = "-cond", usage = "Condition file path", metaVar = "File", required = true)
	public String condFile = "";

	@Option(name = "-int", usage = "[OPTIONAL]Interaction file path", metaVar = "File")
	public String intFile = "";

	@Option(name = "-sol", usage = "Solver name", metaVar = "Solver")
	public String solver = "GLPK";

	@Option(name = "-plot", usage = "[OPTIONAL, default = false]Plots the results")
	public boolean plot = false;

	@Option(name = "-e", usage = "[Optional]Biological entities to perfrom the KO analysis on (Space-separated list of entities, example : \"R1 R2 G1 G2\"). If empty, KO is done on all reactions (mode 0) or all genes (mode 1)", metaVar = "String")
	public String entities = "";

	@Option(name = "-mode", usage = "[OPTIONAL, default = 0]KO mode : \n- Mode 0: the KO analysis is performed on reactions.\n"
			+ "- Mode 1: the KO analysis is performed on genes.\n", metaVar = "[0,1]")
	public int mode = 0;

	@Option(name = "-out", usage = "[OPTIONAL]Output file name", metaVar = "File")
	public String outName = "";

	@Option(name = "-n", usage = "[OPTIONAL, default = number of available processors]Number of threads", metaVar = "Integer")
	public int nThreads = Runtime.getRuntime().availableProcessors();

	@Option(name = "-lib", usage = "[OPTIONAL, default = 0]Percentage of non optimality for new constraints", metaVar = "Double")
	public double liberty = 0;

	@Option(name = "-pre", usage = "[OPTIONAL, default = 6]Number of decimals of precision for calculations  and results", metaVar = "Integer")
	public int precision = 6;

	@Option(name = "-ext", usage = "[OPTIONAL, default = false]Uses the extended SBML format")
	public boolean extended = false;

	@Option(name = "-h", usage = "Prints this help")
	public boolean h = false;

	public static void main(String[] args) {

		FlexfluxKO f = new FlexfluxKO();

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
			parser.printUsage(System.err);
			System.exit(1);
		}

		Vars.libertyPercentage = f.liberty;
		Vars.decimalPrecision = f.precision;

		if (f.mode > 1 || f.mode<0) {
			System.err.println("-mode must be 0 or 1");
			System.err.println(f.message);
			parser.printUsage(System.err);
			System.exit(0);
		}


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
		if (!new File(f.condFile).isFile()) {
			System.err.println("Error : file " + f.condFile + " not found");
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

		bind.loadSbmlNetwork(f.sbmlFile, f.extended);
		if (f.condFile != "") {
			bind.loadConditionsFile(f.condFile);
		}
		if (f.intFile != "") {
			bind.loadInteractionsFile(f.intFile);
		}
		bind.prepareSolver();

		Map<String, BioEntity> entitiesMap = new HashMap<String, BioEntity>();

		if (!f.entities.equals("")) {
			String[] entitiesArray = f.entities.split("\\s+");
			for (int i = 0; i < entitiesArray.length; i++) {

				BioEntity b = bind.getInteractionNetwork().getEntity(
						entitiesArray[i]);
				if (b == null) {
					System.err.println("Unknown entity " + entitiesArray[i]);
					parser.printUsage(System.err);
					System.exit(0);
				}

				entitiesMap.put(b.getId(), b);
			}
		}

		Analysis analysis;
		if (entitiesMap.size() > 0) {
			analysis = new KOAnalysis(bind, f.mode, entitiesMap);
		} else {
			analysis = new KOAnalysis(bind, f.mode, null);
		}
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
