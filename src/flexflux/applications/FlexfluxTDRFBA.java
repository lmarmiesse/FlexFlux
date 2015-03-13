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
import flexflux.analyses.TDRFBAAnalysis;
import flexflux.analyses.result.AnalysisResult;
import flexflux.general.Bind;
import flexflux.general.CplexBind;
import flexflux.general.GLPKBind;
import flexflux.general.Vars;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.kohsuke.args4j.Option;

import parsebionet.biodata.BioEntity;

/**
 * 
 * 
 * <p>
 * Computes a time dependent analysis given a metabolic network, an objective
 * function and constraints.
 * </p>
 * 
 * <p>
 * This analysis is based on external metabolic concentrations and cell density.
 * </p>
 * 
 * <p>
 * Given initial metabolite concentrations, cell density, a time step and a
 * number of iterations, this analysis returns the value of each metabolite and
 * cell density for each time
 * </p>
 * 
 * @author lmarmiesse 6 mars 2013
 * 
 */
public class FlexfluxTDRFBA extends FFApplication{

	public static String message = "FlexfluxTDRFBA\n"

			+ "Computes a time dependent analysis given a metabolic network, an objective function and constraints.\n"
			+ "This analysis is based on external metabolic concentrations and cell density.\n"
			+ "Given initial metabolite concentrations, cell density, a time step and a number of iterations, this "
			+ "\nanalysis returns the value of each metabolite and cell density for each time";

	public String example = "Example 1 : FlexfluxRFBA -s network.xml -cond cond.txt -int int.txt -bio R_Biomass -x 0.01 -plot -out out.txt\n"
			+ "Example 2 : FlexfluxRFBA -s network.xml -cond cond.txt -int int.txt -bio R_Biomass -plot -out out.txt -x 0.01 -t 0.02 -it 400 -e \"R1 R2 G1 G2\"\n";

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

	@Option(name = "-e", usage = "[OPTIONAL]Biological entities included in the results.\nIf empty, only concerned metabolites and cell density will be included", metaVar = "String")
	public String entities = "";

	@Option(name = "-out", usage = "[OPTIONAL]Output file name", metaVar = "File")
	public String outName = "";

	@Option(name = "-x", usage = "Cell density initial value in g/L", metaVar = "Double", required = true)
	public double X = 0;

	@Option(name = "-bio", usage = "Name of the biomass reaction", metaVar = "String", required = true)
	public String biomassReac = "";

	@Option(name = "-t", usage = "[OPTIONAL, default = 0.1]Time between each iteration in hour", metaVar = "Double")
	public double deltaT = 0.1;

	@Option(name = "-it", usage = "[OPTIONAL, default = 150]Number of iterations", metaVar = "Integer")
	public int iterations = 150;

	@Option(name = "-lib", usage = "[OPTIONAL, default = 0]Percentage of non optimality for new constraints", metaVar = "Double")
	public double liberty = 0;

	@Option(name = "-pre", usage = "[OPTIONAL, default = 6]Number of decimals of precision for calculations and results", metaVar = "Integer")
	public int precision = 6;

	@Option(name = "-ext", usage = "[OPTIONAL, default = false]Uses the extended SBML format")
	public boolean extended = false;

	public static void main(String[] args) {

		FlexfluxTDRFBA f = new FlexfluxTDRFBA();

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

		// we check the biomass reaction
		if (bind.getInteractionNetwork().getEntity(f.biomassReac) == null) {

			System.err.println("Error : biomass reaction : " + f.biomassReac
					+ " unknown");
			System.exit(0);
		}

		List<String> toDisplay = new ArrayList<String>();
		if (!f.entities.equals("")) {
			String[] entitiesArray = f.entities.split("\\s+");
			for (int i = 0; i < entitiesArray.length; i++) {

				BioEntity b = bind.getInteractionNetwork().getEntity(
						entitiesArray[i]);
				if (b == null) {
					System.err.println("Unknown entity " + entitiesArray[i]
							+ " in the arguments");
					f.parser.printUsage(System.err);
					System.exit(0);
				}

				toDisplay.add(b.getId());
			}
		}

		Analysis analysis = new TDRFBAAnalysis(bind, f.biomassReac, f.X,
				f.deltaT, f.iterations, toDisplay);
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
