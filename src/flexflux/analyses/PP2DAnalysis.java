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
import flexflux.analyses.result.PP2DResult;
import flexflux.general.Bind;
import flexflux.general.Constraint;
import flexflux.general.Vars;
import flexflux.thread.ResolveThread;
import flexflux.thread.ThreadPP2D;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import parsebionet.biodata.BioEntity;

/**
 * 
 * Class to run an analysis with one varying flux value.
 * 
 * @author lmarmiesse 18 avr. 2013
 * 
 */
public class PP2DAnalysis extends Analysis {

	/**
	 * Name of the varying entity.
	 */
	String name;

	/**
	 * Initial value
	 */
	double init;
	/**
	 * Final value
	 */
	double end;
	/**
	 * Difference between each step.
	 */
	double deltaF;

	protected List<ThreadPP2D> threads = new ArrayList<ThreadPP2D>();

	/**
	 * Varying entity with it's coefficient (1) to make a constraint.
	 * 
	 */
	private Map<BioEntity, Double> entities = new HashMap<BioEntity, Double>();

	/**
	 * Minimal group size to consider it is a phenotype phase.
	 */
	int minGrpsSize = 10;

	/**
	 * Determines if FVA's must be performed on each phenotype phase.
	 */
	private boolean fva;

	public PP2DAnalysis(Bind bind, Map<BioEntity, Double> entities,
			String name, double init, double end, double deltaF, boolean fva) {
		super(bind);
		this.name = name;
		this.init = init;
		this.end = end;
		this.deltaF = deltaF;
		this.fva = fva;
		this.entities = entities;

	}

	public PP2DResult runAnalysis() {
		double startTime = System.currentTimeMillis();

		Queue<Double> fluxesQueue = new LinkedBlockingQueue<Double>();

		for (double value = init; value <= end; value += deltaF) {
			fluxesQueue.add(value);

		}

		Map<Double, Double> resultValues = new HashMap<Double, Double>();
		List<Double> fluxValues = new ArrayList<Double>();

		PP2DResult result = new PP2DResult(b.getObjective()
				.getName(), name, fluxValues, resultValues, minGrpsSize, init,
				end, deltaF);

		for (int j = 0; j < Vars.maxThread; j++) {
			
			Bind newBind = null;
			
			try {
				newBind = b.copy();
			} catch (ClassNotFoundException | NoSuchMethodException
					| SecurityException | InstantiationException
					| IllegalAccessException | IllegalArgumentException
					| InvocationTargetException e) {
				e.printStackTrace();
				System.exit(1);
			}
			
			ThreadPP2D threadReac = new ThreadPP2D(newBind, fluxesQueue, entities, result, b.getObjective());
			
			threads.add(threadReac);

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

		for (ResolveThread thread : threads) {
			thread.start();
		}

		for (ResolveThread thread : threads) {
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

		if (fva) {
			threads.get(0).setShadowPriceGroups();

			// we remove the threads to permit another analysis
			while (threads.size() > 0) {
				threads.remove(0);
			}
			// now we calculate the shadow price groups
			Map<Double, List<Double>> shadowPriceGroups = result
					.getShadowPriceGroups();

			Map<Double, Integer> groupIndex = result.getGroupIndex();

			// we make one FVA by group
			// group index => fvaresult
			Map<Integer, FVAResult> fvaResults = new HashMap<Integer, FVAResult>();

			if (Vars.verbose) {
				System.err.println("Starting an FVA analysis for each of the "
						+ groupIndex.size() + " phenotypic phases found");
			}

			for (double group : groupIndex.keySet()) {

				int test = 5;
				double value = shadowPriceGroups.get(group).get(test);

				List<Constraint> constraintsToAdd = new ArrayList<Constraint>();
				constraintsToAdd.add(new Constraint(entities, value, value));

				FVAAnalysis analysis = new FVAAnalysis(b, null,
						constraintsToAdd);
				fvaResults.put(groupIndex.get(group), analysis.runAnalysis());

			}

			PhenotypicPhaseComparator comparator = new PhenotypicPhaseComparator(
					fvaResults);
			result.setComparator(comparator);

		}

		if (Vars.verbose) {
			System.err.println("Reac analysis over "
					+ ((System.currentTimeMillis() - startTime) / 1000) + "s "
					+ Vars.maxThread + " threads");
		}

		return result;

	}

}
