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
package flexflux.analyses.randomConditions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.math3.exception.MathArithmeticException;
import org.apache.commons.math3.util.ArithmeticUtils;

import flexflux.analyses.Analysis;
import flexflux.analyses.result.RandomConditionsResult;
import flexflux.general.ConstraintType;
import flexflux.general.Vars;
import flexflux.thread.ThreadRandomConditions;

public class RandomConditions extends Analysis {

	private int numberSimulations;
	private ListOfInputRandomParameters inputRandomParameters;
	private double gaussianMean;
	private double gaussianStd;
	private int minInputs;
	private int maxInputs;
	private long numberOfCombinations;
	private ConstraintType type;

	Set<String> inputs;

	/**
	 * List containing the threads that will solve each step of simulation
	 */
	protected List<ThreadRandomConditions> threads;

	public RandomConditions(int numberSimulations,
			ListOfInputRandomParameters inputRandomParameters,
			double gaussianMean, double gaussianStd, int minInputs,
			int maxInputs, ConstraintType type) {

		super(null);

		this.numberSimulations = numberSimulations;
		this.inputRandomParameters = inputRandomParameters;
		this.gaussianMean = gaussianMean;
		this.gaussianStd = gaussianStd;
		this.type = type;

		if (minInputs > inputRandomParameters.size()) {
			minInputs = inputRandomParameters.size();
		}

		if (maxInputs > inputRandomParameters.size()) {
			maxInputs = inputRandomParameters.size();
		}

		this.minInputs = minInputs;
		this.maxInputs = maxInputs;

		// Calcul du nombre de combinaisons
		for (int i = minInputs; i <= maxInputs; i++) {
			try {
				numberOfCombinations += ArithmeticUtils.binomialCoefficient(
						inputRandomParameters.size(), i);
			} catch (MathArithmeticException e) {
				// TODO Auto-generated catch block
				numberOfCombinations = Long.MAX_VALUE;
				break;
			}
		}

		System.err.println("Number of combinations : " + numberOfCombinations);

		if (numberOfCombinations < numberSimulations) {
			System.err
					.println("Number of combinations ("
							+ numberSimulations
							+ ")< number of simulations : we can't have non redundant simulations");
			inputs = null;
			return;
		}

		inputs = new HashSet<String>();

		for (InputRandomParameters irp : inputRandomParameters) {
			inputs.add(irp.getId());
		}

		threads = new ArrayList<ThreadRandomConditions>();

	}

	@Override
	public RandomConditionsResult runAnalysis() {

		if (inputs == null) {
			return null;
		}

		double startTime = System.currentTimeMillis();

		RandomConditionsResult result = new RandomConditionsResult(inputs);

		Queue<Integer> tasks = new LinkedBlockingQueue<Integer>();

		for (int i = 0; i < numberSimulations; i++) {
			tasks.add(i);
		}

		for (int i = 0; i < Vars.maxThread; i++) {

			ThreadRandomConditions thread = new ThreadRandomConditions(tasks,
					gaussianMean, gaussianStd, minInputs, maxInputs,
					inputRandomParameters, type, result);
			threads.add(thread);
		}

		if (Vars.verbose) {
			System.err.println("Progress : ");

			System.err.print("[");
			for (int i = 0; i < 50; i++) {
				System.err.print(" ");
			}
			System.err.print("]\n");
			System.err.print("[");
		}

		for (ThreadRandomConditions thread : threads) {
			thread.start();
		}

		for (ThreadRandomConditions thread : threads) {
			// permits to wait for the threads to end
			try {
				thread.join();
			} catch (InterruptedException e) {
				// e.printStackTrace();
			}
		}

		if (Vars.verbose) {
			System.err.print("]\n");
		}

		// we remove the threads to permit another analysis
		while (threads.size() > 0) {
			threads.remove(0);
		}

		if (Vars.verbose) {
			System.err.println("Random conditions over "
					+ ((System.currentTimeMillis() - startTime) / 1000) + "s "
					+ Vars.maxThread + " threads");
		}
		return result;
	}
	
}
