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
 * 15 mai 2015
 */
package flexflux.thread;

import java.lang.reflect.InvocationTargetException;
import java.util.Queue;

import flexflux.analyses.FBAAnalysis;
import flexflux.analyses.result.ConcurrentReactionsResult;
import flexflux.analyses.result.FBAResult;
import flexflux.analyses.result.FVAResult;
import flexflux.general.Bind;
import flexflux.general.Constraint;
import flexflux.general.Vars;
import parsebionet.biodata.BioChemicalReaction;
import parsebionet.biodata.BioEntity;

/**
 * @author lcottret
 * 
 */
public class ThreadConcurrentReactions extends ResolveThread {

	/**
	 * Number of entities to treat.
	 */
	private int todo;

	/**
	 * Contains all entities to treat.
	 */
	private Queue<BioEntity> entities;

	private ConcurrentReactionsResult result;

	private FVAResult previousFVA;

	/**
	 * percentage of the analysis completed
	 */
	private static int percentage = 0;

	public ThreadConcurrentReactions(Bind b, Queue<BioEntity> ents,
			FVAResult previousFVA, ConcurrentReactionsResult result) {
		
		
		super(b);
		this.todo = ents.size();
		this.entities = ents;
		this.result = result;
		this.previousFVA = previousFVA;
		percentage = 0;
	}

	/**
	 * Starts the thread.
	 */
	public void run() {

		BioEntity entity;

		while ((entity = entities.poll()) != null) {
			
			
			Bind newBind = null;

			try {
				newBind = bind.copy();
			} catch (ClassNotFoundException | NoSuchMethodException
					| SecurityException | InstantiationException
					| IllegalAccessException | IllegalArgumentException
					| InvocationTargetException e) {
				e.printStackTrace();
				System.exit(1);
			}
			

			BioChemicalReaction reaction = newBind.getBioNetwork()
					.getBiochemicalReactionList().get(entity.getId());

			Double min = 0.0;
			Double max = 0.0;

			Constraint constraint = bind.getSimpleConstraints().get(entity);

			double oldLb = constraint.getLb();
			double oldUb = constraint.getUb();

			max = previousFVA.getMap().get(entity)[1];
			min = previousFVA.getMap().get(entity)[0];

			Constraint newConstraint = new Constraint(entity, max, max);
			newBind.getSimpleConstraints().remove(entity);
			newBind.getConstraints().remove(constraint);
			
			newBind.getConstraints().add(newConstraint);
			newBind.addSimpleConstraint(entity, newConstraint);
			
			// We compute the optimal value of the main objective. If the
			// optimal value equals to 0, the reaction is concurrent
			newBind.prepareSolver();
			FBAAnalysis fbaAnalysis = new FBAAnalysis(newBind);
			FBAResult res = fbaAnalysis.runAnalysis();

			if (res.getObjValue().isNaN() || res.getObjValue() == 0) {
				result.addConcurrentReaction(entity.getId(), entity);
			} else {
				if (reaction.isReversible()) {
					newConstraint.setUb(min);
					newConstraint.setLb(min);

					newBind.prepareSolver();

					fbaAnalysis = new FBAAnalysis(newBind);
					res = fbaAnalysis.runAnalysis();

					if (res.getObjValue().isNaN() || res.getObjValue() == 0) {
						result.addConcurrentReaction(entity.getId(), entity);
					} else {
						result.addOtherReaction(entity.getId(), entity);
					}

				} else {
					result.addOtherReaction(entity.getId(), entity);
				}
			}
			
			int percent = (int) Math
					.round((todo - entities.size()) / todo * 50);
			if (percent > percentage) {

				percentage = percent;
				if (Vars.verbose && percent % 2 == 0) {
					System.err.print("*");
				}
			}

		}

	}

}
