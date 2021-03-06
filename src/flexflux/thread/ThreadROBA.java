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
package flexflux.thread;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import parsebionet.biodata.BioEntity;
import flexflux.analyses.result.ROBAResult;
import flexflux.condition.Condition;
import flexflux.general.Bind;
import flexflux.general.Constraint;
import flexflux.general.DoubleResult;
import flexflux.general.Vars;
import flexflux.objective.ListOfObjectives;
import flexflux.objective.Objective;

public class ThreadROBA extends ResolveThread {

	/**
	 * Number of simulations to treat.
	 */
	private double todo;

	/**
	 * Contains all the conditions to treat
	 */
	private Queue<Condition> conditions;

	/**
	 * ERA result
	 */
	private ROBAResult result;

	/**
	 * Map [name objective<->expression objective]
	 */
	private ListOfObjectives objectives;

	/**
	 * 
	 * For the output Thread safe integer
	 * 
	 */
	private static AtomicInteger nbPrintedStars = new AtomicInteger(0);

	/**
	 * If true, the conditions set in the condition file are fixed and can not
	 * be updated by the regulation network
	 */
	Boolean fixConditions = false;

	public ThreadROBA(Bind b, Queue<Condition> conditions, ListOfObjectives objectives, ROBAResult result,
			Boolean fixConditions) {

		super(b);
		this.todo = conditions.size();
		this.conditions = conditions;
		this.result = result;
		this.objectives = objectives;
		this.fixConditions = fixConditions;
		nbPrintedStars = new AtomicInteger(0);

	}

	/**
	 * Starts the thread.
	 */
	public void run() {

		Condition condition;
		while ((condition = conditions.poll()) != null) {

			Set<String> inputsWithPositiveValue = new HashSet<String>();

			// condition.addListOfConstraintsToBind(bind, fixConditions);

			List<Constraint> constraints = condition.getConstraints(bind, fixConditions);

			for (String entityId : condition.constraints.keySet()) {
				Double value = condition.getConstraint(entityId).getValue();

				if (value > 0) {
					inputsWithPositiveValue.add(entityId);
				}
			}
			// With prepare solver, the constraints are still there in the next
			// conditions
			// bind.prepareSolver();

			// the value in ObjSimCount for an objective function and the value
			// for the activated inputs
			// in ObjInputMatrix are incremented if the new set of conditions
			// activates it

			for (String objName : objectives.objectives.keySet()) {

				this.setObjective(objName);

				List<Constraint> constraintsToAdd = new ArrayList<Constraint>(constraints);

				DoubleResult res = bind.FBA(constraintsToAdd, false, true);

				double value = Vars.round(res.result);

				Objective o = bind.getObjective();
				Boolean maximize = o.getMaximize();

				if ((maximize && value > 0) || (!maximize && value < 0)) {

					result.incrementObjCondCount(objName);
					for (String inputId : inputsWithPositiveValue) {
						result.incrementObjInputMatrix(objName, inputId);
					}
				}
			}

			if (Vars.verbose) {
				int percent = (int) Math.round(((todo - conditions.size()) / todo) * 100);

				while (nbPrintedStars.intValue() < (percent / 2)) {
					System.err.print("*");
					nbPrintedStars.incrementAndGet();
				}
			}
		}

		bind.end();
		
	}

	/**
	 * 
	 * Set a new objective
	 */
	private void setObjective(String objName) {
		String expr = objectives.get(objName);
		String objString = (String) expr.subSequence(expr.indexOf("(") + 1, expr.indexOf(")"));

		Boolean maximize = false;

		if (expr.contains("MIN(")) {
			maximize = false;
		} else if (expr.contains("MAX(")) {
			maximize = true;
		}

		Objective obj = bind.makeObjectiveFromString(objString, maximize, objName);
		bind.setObjective(obj);
	}

}
