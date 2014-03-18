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
package analyses;

import general.Bind;
import general.Constraint;
import general.DoubleResult;
import general.Vars;
import interaction.Interaction;
import interaction.Relation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import parsebionet.biodata.BioChemicalReaction;
import parsebionet.biodata.BioEntity;
import analyses.result.RFBAResult;

/**
 * 
 * Class to run a time dependent FBA.
 * 
 * @author lmarmiesse 5 avr. 2013
 * 
 */
public class RFBAAnalysis extends Analysis {

	/**
	 * Cell density.
	 */
	protected double X;
	protected double initX;

	/**
	 * Time between each step.
	 */
	protected double deltaT;

	/**
	 * Number of iterations.
	 */
	protected int iterations;

	/**
	 * List of entities names to plot.
	 */
	protected List<String> toPlot;

	/**
	 * Name of the biomass reaction of the network.
	 */
	String biomassReac;

	public RFBAAnalysis(Bind b, String biomassReac, double X, double deltaT,
			int iterations, List<String> toPlot) {
		super(b);
		this.X = X;
		this.initX = X;
		this.deltaT = deltaT;
		this.iterations = iterations;
		this.toPlot = toPlot;
		this.biomassReac = biomassReac;
	}

	public RFBAResult runAnalysis() {

		// warning list to tell when the model was unfeasible
		List<Integer> unfeasibleSteps = new ArrayList<Integer>();

		double startTime = System.currentTimeMillis();

		Map<BioEntity, Constraint> simpleConstraints = b.getSimpleConstraints();
		Map<BioChemicalReaction, Map<BioEntity, Double>> exchangeInteractions = b
				.getExchangeInteractions();

		Map<Integer, Set<Constraint>> timeConstraintMap = new HashMap<Integer, Set<Constraint>>();

		for (int i = 0; i < iterations; i++) {
			timeConstraintMap.put(i, new HashSet<Constraint>());
		}

		RFBAResult rFBAResult = new RFBAResult();

		int noGrowthCount = 0;

		for (int i = 0; i < iterations; i++) {

			// we save the results
			Map<String, Double> valuesMap = new HashMap<String, Double>();
			valuesMap.put("X", X);

			List<Constraint> constraintsToAdd = new ArrayList<Constraint>();

			
			//we save simple constraints
			Map<BioEntity, Constraint> oldSimpleConstraints = new HashMap<BioEntity, Constraint>();

			//we add the constraints of this step to the simple constraints
			for (Constraint c : timeConstraintMap.get(i)) {
				if (c.getEntities().size() == 1) {
					for (BioEntity ent : c.getEntities().keySet()) {
						if (c.getEntities().get(ent) == 1) {
							oldSimpleConstraints.put(ent,
									simpleConstraints.get(ent));
							simpleConstraints.put(ent, c);
						}
					}
				}

			}

			
			
//			b.goToNextInteractionNetworkState();
			for (Constraint c :  b.goToNextInteractionNetworkState()) {
				constraintsToAdd.add(c);
			}
			//we check all interactions
//			for (Constraint c :  b.findInteractionNetworkSteadyState()) {
//				constraintsToAdd.add(c);
//			}
			
			//we go back to the old simple constraints
			for (BioEntity ent : oldSimpleConstraints.keySet()) {
				simpleConstraints.put(ent, oldSimpleConstraints.get(ent));
			}
			
			
			// we add the constraints for the current iteration
			for (Constraint c : timeConstraintMap.get(i)) {

				c.setOverWritesBounds(false);
				constraintsToAdd.add(c);

			}

			// we create the constraints
			Map<BioEntity, Constraint> metabConstraints = new HashMap<BioEntity, Constraint>();

			for (BioChemicalReaction reac : exchangeInteractions.keySet()) {

				// we add one condition per external metabolite
				// => the max flux of the exchange reaction is the
				// amount of metabolite available : concentration/(X*deltaT)

				for (BioEntity metab : exchangeInteractions.get(reac).keySet()) {

					if (simpleConstraints.containsKey(metab)) {
						valuesMap.put(metab.getId(),
								simpleConstraints.get(metab).getUb());

						double coeff = exchangeInteractions.get(reac)
								.get(metab);

						double availableSubstrate = (simpleConstraints.get(
								metab).getUb() / (X * deltaT))
								/ coeff;

						if (coeff > 0) {
							// we add one condition per reaction
							Map<BioEntity, Double> constraintMap = new HashMap<BioEntity, Double>();
							constraintMap.put(reac, 1.0);
							Constraint c = new Constraint(constraintMap,
									simpleConstraints.get(reac).getLb(),
									availableSubstrate);
							metabConstraints.put(metab, c);
						} else {
							Map<BioEntity, Double> constraintMap = new HashMap<BioEntity, Double>();
							constraintMap.put(reac, 1.0);
							Constraint c = new Constraint(constraintMap,
									availableSubstrate, simpleConstraints.get(
											reac).getUb());
							metabConstraints.put(metab, c);
						}

					}
				}

			}

			for (BioEntity metab : metabConstraints.keySet()) {

				Constraint c = metabConstraints.get(metab);
				c.setOverWritesBounds(false);

				constraintsToAdd.add(c);

				Map<BioEntity, Double> map = new HashMap<BioEntity, Double>();
				map.put(metab, 1.0);

				Constraint c2 = new Constraint(map, simpleConstraints
						.get(metab).getLb(), simpleConstraints.get(metab)
						.getUb());
				constraintsToAdd.add(c2);

			}

			double fbaResult = 0;
			double mu = 0;
			DoubleResult result;

			try {
				result = b.FBAWithConstraints(constraintsToAdd, true, false);

				fbaResult = result.result;

			} catch (Exception e) {
				System.err.println("rFBA stopped");
				System.out.println("RFBA over "
						+ ((System.currentTimeMillis() - startTime) / 1000)
						+ "s");
				return rFBAResult;
			}

			mu = b.getSolvedValue(b.getInteractionNetwork().getEntity(
					biomassReac));

			// if it's unfeasbile : no growth
			if (result.flag != 0) {
				mu = 0;
			}

			// we add the results for this iteration
			for (String s : toPlot) {
				valuesMap.put(s, b.getSolvedValue(b.getInteractionNetwork()
						.getEntity(s)));
			}

			rFBAResult.addValues(deltaT * i, valuesMap);

			if (mu == 0) {

				// we check the activators
				for (BioEntity ent : b.getInteractionsEntities().keySet()) {
					Relation cause = b.getInteractionsEntitiesCause().get(ent);

					if (cause.isTrue(simpleConstraints)) {

						for (Relation r : b.getInteractionsEntities().get(ent)
								.keySet()) {

							double begins = b.getInteractionsEntities()
									.get(ent).get(r)[0];
							double lasts = b.getInteractionsEntities().get(ent)
									.get(r)[1];

							int iterBegin = (int) (i + begins / deltaT);

							int iterEnd = iterBegin + (int) (lasts / deltaT);

							for (int iter = iterBegin; iter <= iterEnd; iter++) {
								if (iter < iterations) {

									timeConstraintMap.get(iter).addAll(
											r.createConstraints());

								}
							}
						}

					}

				}

				if (result.flag == 0) {
					System.out.println(timeToString(i * deltaT) + " X = "
							+ Vars.round(X) + " no growth");
				} else {
					unfeasibleSteps.add(i);
					System.out.println(timeToString(i * deltaT) + " X = "
							+ Vars.round(X) + " no growth : unfeasible");
					continue;
				}
				// noGrowthCount++;
				// if (noGrowthCount > 1 / deltaT) {
				// System.out
				// .println("No feasible solution - nutrients exhausted");
				//
				// System.out.println("RFBA over "
				// + ((System.currentTimeMillis() - startTime) / 1000)
				// + "s");
				//
				// if (!unfeasibleSteps.isEmpty()) {
				//
				// String steps = "";
				// for (int step : unfeasibleSteps) {
				// steps += String.valueOf(step) + " ";
				// }
				//
				// System.err.println("Warning : unfeasible steps : "
				// + steps);
				//
				// }
				// return rFBAResult;
				// }
				// continue;

			}

			// we check the activators
			for (BioEntity ent : b.getInteractionsEntities().keySet()) {

				for (Relation r : b.getInteractionsEntities().get(ent).keySet()) {

					if (b.getSolvedValue(ent) > 0) {

						double begins = b.getInteractionsEntities().get(ent)
								.get(r)[0];
						double lasts = b.getInteractionsEntities().get(ent)
								.get(r)[1];

						int iterBegin = (int) (i + begins / deltaT);

						int iterEnd = iterBegin + (int) (lasts / deltaT);

						for (int iter = iterBegin; iter <= iterEnd; iter++) {
							if (iter < iterations) {

								timeConstraintMap.get(iter).addAll(
										r.createConstraints());

							}
						}
					}
				}
			}

			// System.out.println("mu = " + mu);
			// System.out.println(i);

			// we set the new concentrations for the metabolites according to
			// the uptake
			for (BioEntity metab : metabConstraints.keySet()) {

				double ub = simpleConstraints.get(metab).getUb();

				double uptake = 0;

				Constraint c = metabConstraints.get(metab);

				uptake = 0;

				for (BioEntity reac : c.getEntities().keySet()) {

					// if the metab is a reactant, we invert the flux to get he
					// uptake
					// ( if a ==> b and the flux of the reaction is 20, then a
					// uptake is -20)
					uptake += b.getSolvedValue(reac)
							* exchangeInteractions.get(reac).get(metab) * -1;

				}

				// System.out.println(metab.getId() + " " + uptake);
				// System.out.println(simpleConstraints.get(metab).getUb());

				// uptake is >0 when the metabolite goes out of the cell
				// <0 when it comes in

				// explanation
				// the concentration S = f(x)
				// f'(x) = uptake*g(x)
				// f(x) = uptake*G(x) + cst
				// G(x) : prim of g(x)

				if (mu != 0) {
					// if mu !=0 : G(x) = g(0)/mu e^mu * x + cst(see the
					// calculation
					// of X)
					// so f(x) = uptake * g(0)/mu e^mu*x + cst
					// we consider that the old concentration value is f(0)
					// f(0) = uptake * g(0)/mu e^mu*0 + cst
					// f(0) = uptake*g(0)/mu + cst
					// cst = f(0) - uptake*g(0)/mu
					// so f(x) = uptake*g(0)/mu e^mu * x + f(0) - uptake*g(0)/mu
					// we factorise by uptake*g(0)/mu
					// => f(x) = uptake*g(0)/mu (e^mu*x -1) + f(0)
					// f(x) = f(0) + uptake*g(0)/mu (e^mu*x -1)
					// f(0) = previous ub
					// g(0) = previous X
					// x = deltat
					// new ub = ub + (uptake/mu)*X (exp(mu*deltat-1)

					ub += ((uptake / mu) * X * (Math.exp(mu * deltaT) - 1));

				} else {
					// if mu ==0 : G(x) = g(0)x + cst (see the calculation
					// of X)
					// so f(x) = uptake * g(0)x + cst
					// we consider that the old concentration value is f(0)
					// f(0) = uptake * g(0)*0 +cst
					// cst = f(0)
					// f(x) = uptake*g(0)*x + f(0)
					// f(0) = previous ub
					// g(0) = previous X
					// x = deltat
					// new ub = ub + uptake*X*deltat

					ub += uptake * X * deltaT;
				}

				if (Double.isNaN(ub)) {
					simpleConstraints.get(metab).setUb(0);
					simpleConstraints.get(metab).setLb(0);
				} else {
					// we set it to 0 it the concentration is negative
					// System.out.println(metab.getId() + " : "
					simpleConstraints.get(metab).setUb(Math.max(ub, 0));
					simpleConstraints.get(metab).setLb(Math.max(ub, 0));
				}

			}

			// explanation
			// X = g(x)
			// g'(x) = mu g(x)
			// g(x) = g(0) e^mu * x
			// we consider a new function at each step
			// and we recaculatr g(deltaT) considering that g(0) = the value of
			// the previous step.

			X *= Math.exp(mu * deltaT);

			if (mu != 0) {
				System.out.println(timeToString(i * deltaT) + " X = "
						+ Vars.round(X));
			}
			// System.out.println("X = " + X);

		}
		System.out.println("RFBA over "
				+ ((System.currentTimeMillis() - startTime) / 1000) + "s");

		if (!unfeasibleSteps.isEmpty()) {

			String steps = "";
			for (int step : unfeasibleSteps) {
				steps += String.valueOf(step) + " ";
			}

			System.err.println("Warning : unfeasible steps : " + steps);

		}

		return rFBAResult;

	}

	// transforms a time in hour into a string
	public String timeToString(double time) {

		int nbHour = (int) Math.floor(time);

		int nbMin = (int) Math.floor((time - nbHour) * 60);

		String add = "";

		if (nbMin < 10) {
			add = "0";
		}

		return String.valueOf(nbHour) + "h" + add + String.valueOf(nbMin)
				+ "min";

	}

}
