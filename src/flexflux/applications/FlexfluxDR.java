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

import flexflux.analyses.DRAnalysis;
import flexflux.analyses.result.DRResult;
import flexflux.general.Bind;
import flexflux.general.Constraint;
import flexflux.general.CplexBind;
import flexflux.general.GLPKBind;
import flexflux.general.Vars;

import java.io.File;
import java.util.Collection;

import org.kohsuke.args4j.Option;

import parsebionet.biodata.BioChemicalReaction;
import parsebionet.biodata.BioNetwork;
import parsebionet.io.Sbml2Bionetwork;

/**
 * 
 * <p>
 * Finds dead reactions in a given metabolic network.
 * </p>
 * <p>
 * Dead reactions are those unable to carry a steady state flux.
 * </p>
 * 
 * @author lmarmiesse 6 mars 2013
 * 
 */
public class FlexfluxDR extends FFApplication{
	
	// order for the graphical version
	public static int order = 11;	

	public static String message = "FlexfluxDR\n"

	+ "Finds dead reactions in a given metabolic network.\n"
			+ "Dead reactions are those unable to carry a steady state flux.";

	public String example = "Example 1 : FlexfluxDR -s network.xml -plot -out out.txt\n"
			+ "Example 2 : FlexfluxDR -s network.xml -cond cond.txt -int int.txt -plot -out out.txt -mode 1 -d 0.1\n";

	@Option(name = "-s", usage = "Sbml file path", metaVar = "File - in", required = true)
	public String sbmlFile = "";

	@Option(name = "-cons", usage = "[OPTIONAL]Constraints file path", metaVar = "File - in")
	public String condFile = "";

	@Option(name = "-reg", usage = "[OPTIONAL]Regulation file path", metaVar = "File - in")
	public String regFile = "";

	@Option(name = "-sol", usage = "Solver name", metaVar = "Solver")
	public String solver = "GLPK";

	@Option(name = "-plot", usage = "[OPTIONAL, default = false]Plots the results")
	public boolean plot = false;

	@Option(name = "-out", usage = "[OPTIONAL]Output file name", metaVar = "File - out")
	public String outName = "";

	@Option(name = "-n", usage = "[OPTIONAL, default = number of available processors]Number of threads", metaVar = "Integer")
	public int nThreads = Runtime.getRuntime().availableProcessors();

	@Option(name = "-mode", usage = "[OPTIONAL, default = 0]Dead reactions mode : \n- Mode 0: Reactions fluxes are not changed.\n"
			+ "- Mode 1: All reactions fluxes are set to a maximum value.\n", metaVar = "Integer")
	public int mode = 0;

	@Option(name = "-d", usage = "[OPTIONAL, default = 0.000001]Maximal distance of the reaction flux from 0 to be considered as dead", metaVar = "Double")
	public double d = 0.000001;

	@Option(name = "-lib", usage = "[OPTIONAL, default = 0]Percentage of non optimality for new constraints", metaVar = "Double")
	public double liberty = 0;

	@Option(name = "-pre", usage = "[OPTIONAL, default = 6]Number of decimals of precision for calculations and results", metaVar = "Integer")
	public int precision = 6;

	@Option(name = "-ext", usage = "[OPTIONAL, default = false]Uses the extended SBML format")
	public boolean extended = false;

	public static void main(String[] args) {

		FlexfluxDR f = new FlexfluxDR();

		f.parseArguments(args);
		
		Vars.libertyPercentage = f.liberty;
		Vars.decimalPrecision = f.precision;

		if (f.mode > 1) {
			System.err.println("-mode must be 0 or 1");
			System.err.println(f.getMessage());
			f.parser.printUsage(System.err);
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
		if (f.condFile != "") {
			if (!new File(f.condFile).isFile()) {
				System.err.println("Error : file " + f.condFile + " not found");
				System.exit(0);
			}
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

		Sbml2Bionetwork sbmlParser = new Sbml2Bionetwork(f.sbmlFile, f.extended);
		BioNetwork network = sbmlParser.getBioNetwork();
		Collection<BioChemicalReaction> trimed = network.trim();

		bind.loadSbmlNetwork(f.sbmlFile, f.extended);

		if (f.mode == 1) {
			for (String reac : bind.getBioNetwork()
					.getBiochemicalReactionList().keySet()) {

				BioChemicalReaction reaction = (BioChemicalReaction) bind
						.getInteractionNetwork().getEntity(reac);

				if (bind.getSimpleConstraints().containsKey(reaction)) {

					Constraint c = bind.getSimpleConstraints().get(reaction);
					c.setUb(999999);
					if (reaction.isReversible()) {
						c.setLb(-999999);
					} else {
						c.setLb(0);
					}

				}

			}

		}

		if (f.condFile != "") {
			bind.loadConstraintsFile(f.condFile);
		}
		if (f.regFile != "") {
			bind.loadRegulationFile(f.regFile);
		}
		bind.prepareSolver();

		DRAnalysis analysis = new DRAnalysis(bind, f.d);
		DRResult result = analysis.runAnalysis();

		for (BioChemicalReaction trimedReac : trimed) {

			result.addLine(trimedReac, new double[] { 0.0, 0.0 });

		}

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
