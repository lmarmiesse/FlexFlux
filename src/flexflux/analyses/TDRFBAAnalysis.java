package flexflux.analyses;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import parsebionet.biodata.BioChemicalReaction;
import parsebionet.biodata.BioEntity;
import flexflux.analyses.result.TDRFBAResult;
import flexflux.general.Bind;
import flexflux.general.Constraint;
import flexflux.general.DoubleResult;
import flexflux.general.Vars;
import flexflux.interaction.Interaction;

/**
 * 
 * Class to run a time dependent FBA.
 * 
 * @author lmarmiesse 5 avr. 2013
 * 
 */
public class TDRFBAAnalysis extends Analysis {

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

	public TDRFBAAnalysis(Bind b, String biomassReac, double X, double deltaT, int iterations, List<String> toPlot) {
		super(b);
		this.X = X;
		this.initX = X;
		this.deltaT = deltaT;
		this.iterations = iterations;
		this.toPlot = toPlot;
		this.biomassReac = biomassReac;
	}

	public TDRFBAResult runAnalysis() {
		Map<String, Double> lastSolve = new HashMap<String, Double>();

		// warning list to tell when the model was unfeasible
		List<Integer> unfeasibleSteps = new ArrayList<Integer>();

		double startTime = System.currentTimeMillis();

		Map<BioEntity, Constraint> simpleConstraints = b.getSimpleConstraints();

		for (BioEntity ent : b.getInteractionNetwork().getInitialStates().keySet()) {

			Integer state = b.getInteractionNetwork().getInitialStates().get(ent);

			if (b.getInteractionNetwork().canTranslate(ent)) {

				simpleConstraints.put(ent, b.getInteractionNetwork().getConstraintFromState(ent, state));

			} else {
				simpleConstraints.put(ent, new Constraint(ent, (double) state, (double) state));
			}
		}

		for (BioEntity ent : b.getInteractionNetwork().getInitialConstraints().keySet()) {

			simpleConstraints.put(ent, b.getInteractionNetwork().getInitialConstraints().get(ent));
		}

		Map<BioChemicalReaction, Map<BioEntity, Double>> exchangeInteractions = b.getExchangeInteractions();

		Map<Integer, Set<Constraint>> timeConstraintMap = new HashMap<Integer, Set<Constraint>>();

		for (int i = 0; i < iterations; i++) {
			timeConstraintMap.put(i, new HashSet<Constraint>());
		}

		TDRFBAResult rFBAResult = new TDRFBAResult();

		for (int i = 0; i < iterations; i++) {

			// we save the results
			Map<String, Double> valuesMap = new HashMap<String, Double>();
			valuesMap.put("X", X);

			Set<Constraint> constraintsToAdd = new HashSet<Constraint>();

			if (Vars.verbose) {
				System.err.println("-----");
				System.err.println("it number " + i);
			}

			// // translation
			Map<BioEntity, Integer> networkState = new HashMap<BioEntity, Integer>();

			for (BioEntity enti : simpleConstraints.keySet()) {
				if (b.getInteractionNetwork().canTranslate(enti)) {
					networkState.put(enti,
							b.getInteractionNetwork().getStateFromValue(enti, simpleConstraints.get(enti).getLb()));

				} else if (b.getInteractionNetwork().getInteractionNetworkEntities().keySet().contains(enti.getId())) {
					networkState.put(enti, (int) simpleConstraints.get(enti).getLb());
				}
			}

			if (i != 0) {

				RSAAnalysis ssa = new RSAAnalysis(b.getInteractionNetwork(), new HashMap<BioEntity, Constraint>());
				List<BioEntity> entitiesToCheck = new ArrayList<BioEntity>();
				entitiesToCheck.addAll(b.getInteractionNetwork().getTargetToInteractions().keySet());

				Map<Constraint, double[]> nextStepStates = ssa.goToNextInteractionNetworkState(networkState,
						entitiesToCheck);

				Map<Constraint, double[]> nextStepConsMap = new HashMap<Constraint, double[]>();

				// System.out.println("\n"+i+"\n");

				// translation
				for (Constraint c1 : nextStepStates.keySet()) {

					// System.out.println(c1);

					BioEntity enti = (BioEntity) c1.getEntities().keySet().toArray()[0];

					if (b.getInteractionNetwork().canTranslate(enti)) {
						nextStepConsMap.put(b.getInteractionNetwork().getConstraintFromState(enti, (int) c1.getLb()),
								nextStepStates.get(c1));

					} else {

						nextStepConsMap.put(c1, nextStepStates.get(c1));

					}
				}

				for (Constraint c : nextStepConsMap.keySet()) {

					double begins = nextStepConsMap.get(c)[0];

					double lasts = nextStepConsMap.get(c)[1];

					int iterBegin = (int) (i + begins / deltaT);

					int iterEnd = iterBegin + (int) (lasts / deltaT);

					for (int iter = iterBegin; iter <= iterEnd; iter++) {
						if (iter < iterations) {
							timeConstraintMap.get(iter).add(c);

						}
					}
				}
			}

			// to check if an entity has multiple simple constraints in the same
			// iteration
			Set<BioEntity> hasConstraint = new HashSet<BioEntity>();
			// we add the constraints for the current iteration
			for (Constraint c : timeConstraintMap.get(i)) {

				if (c.getEntities().size() == 1) {

					BioEntity ent = null;

					for (BioEntity e : c.getEntities().keySet()) {
						ent = e;
					}

					if (hasConstraint.contains(ent)) {

						Constraint c1 = c;
						Constraint c2 = simpleConstraints.get(ent);

						double newLb = Math.max(c1.getLb(), c2.getLb());
						double newUb = Math.min(c1.getUb(), c2.getUb());

						simpleConstraints.put(ent, new Constraint(ent, newLb, newUb));

					} else {

						simpleConstraints.put(ent, c);
						hasConstraint.add(ent);
					}
				}
			}
			for (BioEntity ent : b.getInteractionNetwork().getInteractionNetworkEntities().values()) {
				if (simpleConstraints.containsKey(ent)) {
					Constraint c = simpleConstraints.get(ent);

					constraintsToAdd.add(c);

				}
			}

			// we create the constraints
			Map<BioEntity, Constraint> metabConstraints = new HashMap<BioEntity, Constraint>();
			for (BioChemicalReaction reac : exchangeInteractions.keySet()) {

				// we add one condition per external metabolite
				// => the max flux of the exchange reaction is the
				// amount of metabolite available : concentration/(X*deltaT)

				for (BioEntity metab : exchangeInteractions.get(reac).keySet()) {

					if (simpleConstraints.containsKey(metab)) {

						valuesMap.put(metab.getId(), simpleConstraints.get(metab).getUb());

						double coeff = exchangeInteractions.get(reac).get(metab);

						double availableSubstrate = (simpleConstraints.get(metab).getUb() / (X * deltaT)) / coeff;

						if (coeff > 0) {
							// we add one condition per reaction
							Map<BioEntity, Double> constraintMap = new HashMap<BioEntity, Double>();
							constraintMap.put(reac, 1.0);
							Constraint c = new Constraint(constraintMap, simpleConstraints.get(reac).getLb(),
									availableSubstrate);

							metabConstraints.put(metab, c);

						} else {

							Map<BioEntity, Double> constraintMap = new HashMap<BioEntity, Double>();
							constraintMap.put(reac, 1.0);
							Constraint c = new Constraint(constraintMap, availableSubstrate,
									simpleConstraints.get(reac).getUb());

							metabConstraints.put(metab, c);
						}

					}
				}

			}

			for (BioEntity metab : metabConstraints.keySet()) {

				Constraint c = metabConstraints.get(metab);

				constraintsToAdd.add(c);

				Map<BioEntity, Double> map = new HashMap<BioEntity, Double>();
				map.put(metab, 1.0);

				Constraint c2 = new Constraint(map, simpleConstraints.get(metab).getLb(),
						simpleConstraints.get(metab).getUb());
				constraintsToAdd.add(c2);

			}

			double fbaResult = 0;
			double mu = 0;
			DoubleResult result;

			///////// We check the GPR constraints
			List<Constraint> GPRConstraints = new ArrayList<Constraint>();
			for (Interaction inter : b.getInteractionNetwork().getGPRInteractions()) {
				if (inter.getCondition().isTrue(simpleConstraints)) {
					GPRConstraints.addAll(b.getInteractionNetwork().getInteractionToConstraints().get(inter));
				}
			}
			constraintsToAdd.addAll(GPRConstraints);
			/////////

			for (Constraint c : constraintsToAdd) {

				BioEntity b = c.getEntities().keySet().iterator().next();

				if (b.getId().startsWith("R_")) {

					if (!b.getId().contains("_acc")) {

						if (c.getLb() > -1) {

							System.out.println(b.getId() + "\t" + c.getLb() + "\t" + c.getUb());
							// System.out.println(c);
						}
					}

				}

			}

			try {
				result = b.FBA(new ArrayList<Constraint>(constraintsToAdd), true, false);

				fbaResult = result.result;

			} catch (Exception e) {
				System.err.println("rFBA stopped");
				System.err.println("RFBA over " + ((System.currentTimeMillis() - startTime) / 1000) + "s");
				return rFBAResult;
			}

			// if it's unfeasbile : no growth
			if (result.flag != 0) {

				mu = 0;

			} else {
				lastSolve = b.getLastSolve();

				mu = b.getSolvedValue(b.getInteractionNetwork().getEntity(biomassReac));
			}

			System.out.println(mu);

			// we add the results for this iteration
			for (String s : toPlot) {
				valuesMap.put(s, lastSolve.get(s));
			}

			rFBAResult.addValues(deltaT * i, valuesMap);

			if (mu == 0) {

				if (result.flag == 0) {
					if (Vars.verbose) {
						System.err.println(timeToString(i * deltaT) + " X = " + Vars.round(X) + " no growth");
					}
				} else {
					unfeasibleSteps.add(i);

					if (Vars.verbose) {
						System.err.println(
								timeToString(i * deltaT) + " X = " + Vars.round(X) + " no growth : unfeasible");
					}

					b.resetLastSolve();
					continue;
				}

			}

			// System.err.println("mu = " + mu);
			// System.err.println(i);

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
					uptake += b.getSolvedValue(reac) * exchangeInteractions.get(reac).get(metab) * -1;

				}

				// System.err.println(metab.getId() + " " + uptake);
				// System.err.println(simpleConstraints.get(metab).getUb());

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
				if (Vars.verbose) {
					System.err.println(timeToString(i * deltaT) + " X = " + Vars.round(X));
				}
			}
			// System.out.println("X = " + X);

		}
		System.err.println("RFBA over " + ((System.currentTimeMillis() - startTime) / 1000) + "s");

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

		return String.valueOf(nbHour) + "h" + add + String.valueOf(nbMin) + "min";

	}

}
