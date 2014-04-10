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
 * 18 avr. 2013 
 */
package flexflux.analyses;

import flexflux.analyses.result.FVAResult;
import flexflux.analyses.result.TwoReacsAnalysisResult;
import flexflux.general.Bind;
import flexflux.general.Constraint;
import flexflux.general.Vars;
import flexflux.thread.ResolveThread;
import flexflux.thread.ThreadTwoReacs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import parsebionet.biodata.BioEntity;

/**
 * 
 * Class to run an analysis with two varying fluxes.
 * 
 * @author lmarmiesse 18 avr. 2013
 * 
 */
public class TwoReacsAnalysis extends Analysis {

	/**
	 * Names of the varying variables.
	 */
	String reacName, reacName2;

	/**
	 * Initial values.
	 */
	double init, init2;
	/**
	 * Final values
	 */
	double end, end2;
	/**
	 * Differences between each step.
	 */
	double deltaF, deltaF2;

	protected List<ThreadTwoReacs> threads = new ArrayList<ThreadTwoReacs>();

	/**
	 * phenotype phases sorted by shadow-price value.
	 */
	private Map<Double, List<double[]>> shadowPriceGroups;

	/**
	 * Minimal group size to consider it is a phenotype phase.
	 */
	int minGrpsSize = 10;
	/**
	 * Determines if FVA's must be performed on each phenotype phase.
	 */
	private boolean fva;

	/**
	 * First varying entity with it's coefficient (1) to make a constraint.
	 * 
	 */
	private Map<BioEntity, Double> entities1 = new HashMap<BioEntity, Double>();
	/**
	 * Second varying entity with it's coefficient (1) to make a constraint.
	 * 
	 */
	private Map<BioEntity, Double> entities2 = new HashMap<BioEntity, Double>();

	public TwoReacsAnalysis(Bind bind, String reac,
			Map<BioEntity, Double> entities1, Map<BioEntity, Double> entities2,
			double init, double end, double deltaF, String reac2, double init2,
			double end2, double deltaF2, boolean fva) {

		super(bind);

		this.reacName = reac;
		this.init = init;
		this.end = end;
		this.deltaF = deltaF;

		this.reacName2 = reac2;
		this.init2 = init2;
		this.end2 = end2;
		this.deltaF2 = deltaF2;
		this.fva = fva;
		this.entities1 = entities1;
		this.entities2 = entities2;

	}

	public TwoReacsAnalysisResult runAnalysis() {

		double startTime = System.currentTimeMillis();

		Queue<double[]> fluxesQueue = new LinkedBlockingQueue<double[]>();

		for (double value = init; value <= end; value += deltaF) {

			for (double value2 = init2; value2 <= end2; value2 += deltaF2) {

				fluxesQueue.add(new double[] { value, value2 });
			}
		}

		TwoReacsAnalysisResult result = new TwoReacsAnalysisResult(b
				.getObjective().getName(), reacName, reacName2, minGrpsSize,
				init, end, deltaF, init2, end2, deltaF2);

		for (int j = 0; j < Vars.maxThread; j++) {
			threads.add((ThreadTwoReacs) b.getThreadFactory()
					.makeTwoReacsThread(fluxesQueue,
							result, entities1, entities2, b.getObjective()));

		}

		System.out.println("Progress : ");
		System.out.print("[");
		for (int i = 0; i < 50; i++) {
			System.out.print(" ");
		}
		System.out.print("]\n");
		System.out.print("[");

		for (ResolveThread thread : threads) {
			thread.start();
		}

		for (ResolveThread thread : threads) {
			// permits to wait for the threads to end
			try {
				thread.join();
			} catch (InterruptedException e) {
//				e.printStackTrace();
			}
		}

		System.out.print("]\n");

		if (fva) {
			threads.get(0).setShadowPriceGroups();

			// we remove the threads to permit another analysis
			while (threads.size() > 0) {
				threads.remove(0);
			}

			// now we calculate the shadow price groups
			shadowPriceGroups = result.getShadowPriceGroups();

			Map<Double, Integer> groupIndex = result.getGroupIndex();

			// we make one FVA by group
			// group index => fvaresult
			Map<Integer, FVAResult> fvaResults = new HashMap<Integer, FVAResult>();

			System.out.println("Starting an FVA analysis for each of the "
					+ groupIndex.size() + " phenotypic phases found");

			for (double group : groupIndex.keySet()) {

				int test = 0;
				double value = shadowPriceGroups.get(group).get(test)[0];
				double value2 = shadowPriceGroups.get(group).get(test)[1];

				List<Constraint> constraintsToAdd = new ArrayList<Constraint>();
				constraintsToAdd.add(new Constraint(entities1, value, value));
				constraintsToAdd.add(new Constraint(entities2, value2, value2));

				FVAAnalysis analysis = new FVAAnalysis(b, null,
						constraintsToAdd);
				fvaResults.put(groupIndex.get(group), analysis.runAnalysis());

			}

			PhenotypicPhaseComparator comparator = new PhenotypicPhaseComparator(
					fvaResults);
			result.setComparator(comparator);

		}
		System.out.println("Two reactions analysis over "
				+ ((System.currentTimeMillis() - startTime) / 1000) + "s "
				+ Vars.maxThread + " threads");

		return result;
	}

}
