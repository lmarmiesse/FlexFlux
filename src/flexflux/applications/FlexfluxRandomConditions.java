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
package flexflux.applications;

import java.io.File;

import org.kohsuke.args4j.Option;

import flexflux.analyses.randomConditions.ListOfInputRandomParameters;
import flexflux.analyses.randomConditions.RandomConditions;
import flexflux.analyses.result.RandomConditionsResult;
import flexflux.general.ConstraintType;
import flexflux.general.Vars;

/**
 * @author lcottret
 * 
 */
public class FlexfluxRandomConditions extends FFApplication {

	// order for the graphical version
	public static int order = 12;

	public static String applicationName = FlexfluxRandomConditions.class
			.getSimpleName();

	public static String message = applicationName + " [options...]\n"
			+ "Generates a file with randomized conditions";

	@Option(name = "-i", usage = "Input random parameter file"
			+ "Each line must contain a entity id, a value of inhibition, a value of activation and a weight for the simulation."
			+ "During the choice of the inputs to activate during a simulation, the inputs are duplicated n times, with n=weight", metaVar = "File", required = true)
	public String inputRandomParameterFile = "";

	@Option(name = "-nbSim", usage = "[100] Number of simulations to perform", metaVar = "Integer")
	public int nbSim = 100;

	@Option(name = "-mean", usage = "[10] Mean of the gaussian distribution used for selecting "
			+ "the number of inputs activated in each simulation", metaVar = "Double")
	public Double meanGaussian = 10.0;

	@Option(name = "-std", usage = "[5] Standard deviation of the gaussian distribution used for selecting "
			+ "the number of inputs activated in each simulation", metaVar = "Double")
	public Double stdGaussian = 5.0;

	@Option(name = "-min", usage = "[1] minimum number of activated inputs at each step of the simulation", metaVar = "Integer")
	public Integer minInputs = 1;

	@Option(name = "-max", usage = "[1] maximum number of activated inputs at each step of the simulation", metaVar = "Integer")
	public Integer maxInputs = 50;

	@Option(name = "-plot", usage = "[OPTIONAL, default = false] Plots the results")
	public Boolean plot = false;

	@Option(name = "-out", usage = "[OPTIONAL] Output file directory", metaVar = "File")
	public String outName = "";

	@Option(name = "-n", usage = "[OPTIONAL, default = number of available processors] Number of threads", metaVar = "Integer")
	public int nThreads = Runtime.getRuntime().availableProcessors();

	@Override
	public String getMessage() {
		return message;
	}

	@Override
	public String getExample() {
		return "";
	}

	/**
	 * MAIN
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		FlexfluxRandomConditions f = new FlexfluxRandomConditions();

		f.parseArguments(args);

		if (f.verbose) {
			Vars.verbose = true;
		}

		if (f.nThreads > 0) {
			Vars.maxThread = f.nThreads;
		} else {
			System.err.println("The number of threads must be at least 1");
			System.exit(0);
		}

		if (!new File(f.inputRandomParameterFile).isFile()) {
			System.err.println("Error : input random parameter file "
					+ f.inputRandomParameterFile + " not found");
			System.exit(0);
		}

		ListOfInputRandomParameters inputRandomParameters = new ListOfInputRandomParameters();
		inputRandomParameters
				.loadInputRandomParameterFile(f.inputRandomParameterFile);

		RandomConditions a = new RandomConditions(f.nbSim,
				inputRandomParameters, f.meanGaussian, f.stdGaussian,
				f.minInputs, f.maxInputs, ConstraintType.DOUBLE);

		RandomConditionsResult r = a.runAnalysis();
		
		if (!f.outName.equals("")) {
			r.writeToFile(f.outName);
		}

		if (f.plot) {
			r.plot();
		}
	}
}
