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
 * 4 avr. 2013 
 */

/**
 * 
 * Class containing global variables and parameters used by FlexFlux.
 * 
 * @author lmarmiesse 4 avr. 2013
 * 
 */
package flexflux.general;
public class Vars {

	
	public static Boolean verbose=false;
	
	/**
	 * Maximum lower bound and upper bounds
	 */
	
	public static double minLowerBound = -999999;
	public static double maxUpperBound = +999999;
	
	/**
	 * Maximum number of threads created.
	 */
	public static int maxThread = Runtime.getRuntime().availableProcessors()/2;

	/**
	 * Determines if FlexFlux uses epsilon.
	 */
	public static boolean cheat = true;
	
	
	/**
	 * Value used to approximate inequalities.
	 */
	public static double epsilon = 1e-10;

	/**
	 * Keyword for the sum of all fluxes.
	 */
	public static String FluxSumKeyWord = "FluxSum";

	public static String Irrev1 = "FlexFluxIrrev1";
	public static String Irrev2 = "FlexFluxIrrev2";

	/**
	 * Percentage of liberty for constraints created by objective functions.
	 */
	public static double libertyPercentage = 0;

	/**
	 * Number of decimals of precision of the calculations.
	 */
	public static int decimalPrecision = 6;
	
	/**
	 * Maximal number of iterations to find a steady state in the interaction network.
	 */
	public static int steadyStatesIterations = 100;
	
	
	/**
	 * Whether or not the calculated interaction network steady states must be saved to a file;
	 */
	public static boolean writeInteractionNetworkStates = false;

	/**
	 * 
	 * Rounds number to the decimal precision.
	 * 
	 * @param value
	 *            Initial value.
	 * @return The rounded value.
	 */
	static public double round(double value) {
		double r = (Math.round(value * Math.pow(10, decimalPrecision)))
				/ (Math.pow(10, decimalPrecision));
		return r;
	}

}
