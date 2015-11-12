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
package flexflux.thread;

import flexflux.analyses.result.PP3DResult;
import flexflux.general.Bind;
import flexflux.general.Constraint;
import flexflux.general.DoubleResult;
import flexflux.objective.Objective;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import parsebionet.biodata.BioEntity;

/**
 * 
 * Thread to perform an analysis with two varying fluxes.
 * 
 * @author lmarmiesse 18 avr. 2013
 * 
 */
public class ThreadPP3D extends ResolveThread {

	/**
	 * Percentage of the analysis that is completed.
	 */
	private static int percentage = 0;
	/**
	 * Number of fluxes already treated.
	 */
	private static int done = 0;

	private static Lock lock = new ReentrantLock();

	/**
	 * Contains all fluxes to treat.
	 */
	private Queue<double[]> fluxesQueue = new LinkedBlockingQueue<double[]>();

	/**
	 * The result.
	 */
	private PP3DResult result;

	/**
	 * Groups of phenotype phases.
	 */
	private static Map<Double, List<double[]>> shadowPriceGroups = new ConcurrentHashMap<Double, List<double[]>>();

	/**
	 * The entities to make vary.
	 */
	private Map<BioEntity, Double> entities1 = new HashMap<BioEntity, Double>();
	private Map<BioEntity, Double> entities2 = new HashMap<BioEntity, Double>();

	public ThreadPP3D(Bind b, Queue<double[]> fluxesQueue,
			PP3DResult result, Map<BioEntity, Double> entities1,
			Map<BioEntity, Double> entities2, Objective obj) {
		super(b, obj);

		this.result = result;
		this.fluxesQueue = fluxesQueue;
		this.entities1 = entities1;
		this.entities2 = entities2;
		percentage = 0;
	}

	public void run() {
		int todo = fluxesQueue.size();

		double[] values;

		while ((values = fluxesQueue.poll()) != null) {

			double value = values[0];
			double value2 = values[1];

			List<Constraint> constraintsToAdd = new ArrayList<Constraint>();
			constraintsToAdd.add(new Constraint(entities1, value, value));
			constraintsToAdd.add(new Constraint(entities2, value2, value2));

			DoubleResult res = bind.FBA(constraintsToAdd, false, true);

			if (res.flag != 0) {
				done++;

				int percent = (int) Math.round((double) done / (double) todo
						* 100);
				if (percent > percentage) {

					percentage = percent;
					if (percent % 2 == 0) {
						System.out.print("*");
					}
				}

				continue;
			}

			// calculation of shadowPrice
			double shadowPrice = 0;

			constraintsToAdd.clear();
			constraintsToAdd.add(new Constraint(entities1, value + 0.1,
					value + 0.1));
			constraintsToAdd.add(new Constraint(entities2, value2 + 0.1,
					value2 + 0.1));

			DoubleResult resShadowPrice = bind.FBA(constraintsToAdd, false,
					true);

			shadowPrice = resShadowPrice.result - res.result;
			boolean add = true;

			lock.lock();
			for (Double val : shadowPriceGroups.keySet()) {

				if (Math.abs(shadowPrice - val) < 0.0000001) {
					add = false;
					shadowPriceGroups.get(val).add(values);
					break;
				}
			}

			if (add) {
				List<double[]> list = new ArrayList<double[]>();
				list.add(values);

				shadowPriceGroups.put(shadowPrice, list);
			}

			lock.unlock();

			result.addValue(new double[] { value, value2, res.result });

			done++;

			int percent = (int) Math.round((double) done / (double) todo * 100);
			if (percent > percentage) {

				percentage = percent;
				if (percent % 2 == 0) {
					System.out.print("*");
				}
			}
		}
	}

	public void setShadowPriceGroups() {
		result.setShadowPriceGroups(shadowPriceGroups);
	}

}
