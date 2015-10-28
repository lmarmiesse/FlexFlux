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

import flexflux.analyses.MultiRSA;
import flexflux.analyses.result.MultiRSAResult;
import flexflux.condition.ListOfConditions;
import flexflux.general.Bind;
import flexflux.general.ConstraintType;
import flexflux.general.CplexBind;
import flexflux.general.GLPKBind;
import flexflux.general.Vars;
import flexflux.input.SBMLQualReader;
import flexflux.interaction.InteractionNetwork;
import flexflux.interaction.RelationFactory;

/**
 * @author lcottret
 * 
 */
public class FlexfluxMultiRSA extends FFApplication {

	// order for the graphical version
	public static int order = 2;
	
	public static boolean requiresSolver = false;

	public static String applicationName = FlexfluxMultiRSA.class
			.getSimpleName();

	public static String message = applicationName + " [options...]\n"
			+ "Performs a Steady State analysis for multiple conditions";

	@Option(name = "-reg", usage = "Regulation file path", metaVar = "File - in", required = true)
	public String regFile = "";

	@Option(name = "-cond", usage = "[OPTIONAL] " + ListOfConditions.fileFormat, metaVar = "File - in", required = false)
	public String conditionFile = "";

	@Option(name = "-fixConditions", usage = "[OPTIONAL] If true, the conditions set in the condition file are fixed and can not be updated by the regulation network.")
	public Boolean fixConditions = false;

	@Option(name = "-n", usage = "[OPTIONAL, default = number of available processors] Number of threads", metaVar = "Integer")
	public int nThreads = Runtime.getRuntime().availableProcessors();

	@Option(name = "-out", usage = "Output file", metaVar = "File - out")
	public String outName = "ssa.tab";

	/**
	 * Main
	 * 
	 * @param args
	 */
	public static void main(String[] args) {

		FlexfluxMultiRSA f = new FlexfluxMultiRSA();

		f.parseArguments(args);

		Boolean flag = f.run();

		if (flag == false) {
			System.err.println("Error in the execution");
			System.exit(0);
		}

	}

	/**
	 *
	 */
	public Boolean run() {

		if (this.verbose) {
			Vars.verbose = true;
		}

		if (this.nThreads > 0) {
			Vars.maxThread = this.nThreads;
		} else {
			System.err.println("The number of threads must be at least 1");
			return false;
		}

		/**
		 * Parsing the regulatory file
		 */
		if (!new File(this.regFile).isFile()) {
			System.err.println("Error : file " + this.regFile + " not found");
			return false;
		}

		InteractionNetwork intNet = SBMLQualReader.loadSbmlQual(this.regFile,
				new InteractionNetwork(), new RelationFactory());

		if (intNet == null) {
			return false;
		}

		/**
		 * Parsing the condition file
		 */
		if (!new File(this.conditionFile).isFile()) {
			System.err.println("Error : file " + this.conditionFile
					+ " not found");
			return false;
		}

		ListOfConditions conditions = new ListOfConditions();

		Boolean flag = conditions.loadConditionFile(this.conditionFile,
				ConstraintType.DOUBLE);

		if (flag == false) {
			System.err.println("Error in reading the condition file "
					+ this.conditionFile);
			return false;
		}

		/**
		 * Creation of the analysis
		 */

		MultiRSA analysis = new MultiRSA(intNet, conditions);

		MultiRSAResult res = analysis.runAnalysis();

		res.writeToFile(this.outName);

		return true;

	}

	@Override
	public String getMessage() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getExample() {
		// TODO Auto-generated method stub
		return null;
	}

}
