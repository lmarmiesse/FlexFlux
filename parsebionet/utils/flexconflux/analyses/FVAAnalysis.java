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
 * 5 avr. 2013 
 */
package parsebionet.utils.flexconflux.analyses;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import parsebionet.biodata.BioEntity;
import parsebionet.utils.flexconflux.Bind;
import parsebionet.utils.flexconflux.Constraint;
import parsebionet.utils.flexconflux.DoubleResult;
import parsebionet.utils.flexconflux.Vars;
import parsebionet.utils.flexconflux.analyses.result.FVAResult;
import parsebionet.utils.flexconflux.thread.ResolveThread;

/**
 * 
 * Class to run an FVA analysis.
 * 
 * @author lmarmiesse 5 avr. 2013
 * 
 */
public class FVAAnalysis extends Analysis {

	/**
	 * List of contraints to add before the FVA.
	 */
	private List<Constraint> constraints = new ArrayList<Constraint>();

	/**
	 * 
	 * Entities to run the FVA analysis on.
	 * 
	 */
	Map<String, BioEntity> mapEntities;

	/**
	 * List containing the threads that will maximize and minimize each entity.
	 * 
	 */
	protected List<ResolveThread> threads = new ArrayList<ResolveThread>();

	public FVAAnalysis(Bind b, Map<String, BioEntity> mapEntities,
			List<Constraint> constraints) {
		super(b);
		this.mapEntities = mapEntities;

		if (constraints != null) {
			this.constraints = constraints;
		}
	}

	public FVAResult runAnalysis() {
		double startTime = System.currentTimeMillis();

		DoubleResult result = b.FBAWithConstraints(constraints, false, true);

		if (result.flag != 0) {

			System.out.println("Unfeasible");
			System.exit(0);

		}

		List<Constraint> constraintsToAdd = new ArrayList<Constraint>();
		constraintsToAdd.addAll(constraints);

		// we add the constraints corresponding to the interactions
		if (!b.isInteractionInSolver()) {

			for (Constraint c : b.checkInteractions().keySet()) {
				constraintsToAdd.add(c);
			}
		}
		b.getConstraints().addAll(constraintsToAdd);

		FVAResult fvaResult = new FVAResult(result.result);

		Map<BioEntity, Double> constraintMap = new HashMap<BioEntity, Double>();

		BioEntity[] entities = b.getObjective().getEntities();
		double[] coeffs = b.getObjective().getCoeffs();

		for (int i = 0; i < entities.length; i++) {
			constraintMap.put(entities[i], coeffs[i]);
		}

		double lb = result.result;
		double ub = result.result;
		double delta = Math.abs(result.result) * Vars.libertyPercentage / 100;

		Constraint c = new Constraint(constraintMap, lb - delta, ub + delta);

		System.out.println(Vars.libertyPercentage + "% of non optimality");
		System.out.println("FVA initial constraint : \n" + c);

		b.getConstraints().add(c);

		Map<String, BioEntity> FVAMap = new HashMap<String, BioEntity>();

		if (mapEntities == null) {
			for (String reactionName : b.getBioNetwork()
					.getBiochemicalReactionList().keySet()) {
				FVAMap.put(reactionName, b.getBioNetwork()
						.getBiochemicalReactionList().get(reactionName));
			}
		} else {
			FVAMap = mapEntities;
		}

		// one queue to minimize and the other to maximize
		Queue<BioEntity> entQueue = new LinkedBlockingQueue<BioEntity>();
		Queue<BioEntity> entQueueCopy = new LinkedBlockingQueue<BioEntity>();

		for (String entName : FVAMap.keySet()) {
			entQueue.add(FVAMap.get(entName));
			entQueueCopy.add(FVAMap.get(entName));
		}

		for (int j = 0; j < Vars.maxThread; j++) {
			threads.add(b.getThreadFactory().makeFVAThread(
					b.isInteractionInSolver(), entQueue, entQueueCopy,
					fvaResult));
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

		// we remove the threads to permit another analysis
		while (threads.size() > 0) {
			threads.remove(0);
		}

		// we remove the constraints that sets the objective and interactions
		// to permit other analysis
		b.getConstraints().remove(c);
		b.getConstraints().removeAll(constraintsToAdd);

		System.out.println("FVA over "
				+ ((System.currentTimeMillis() - startTime) / 1000) + "s "
				+ Vars.maxThread + " threads");
		return fvaResult;

	}

}
