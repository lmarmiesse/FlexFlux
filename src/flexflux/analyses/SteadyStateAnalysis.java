package flexflux.analyses;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import parsebionet.biodata.BioEntity;
import parsebionet.biodata.BioPhysicalEntity;
import flexflux.analyses.result.SteadyStateAnalysisResult;
import flexflux.general.Bind;
import flexflux.general.Constraint;
import flexflux.general.Vars;
import flexflux.interaction.Interaction;
import flexflux.interaction.InteractionNetwork;

public class SteadyStateAnalysis extends Analysis {

	private InteractionNetwork intNet;

	private List<Map<BioEntity, Constraint>> statesList = new ArrayList<Map<BioEntity, Constraint>>();

	private List<Map<BioEntity, Constraint>> atractorStatesList = new ArrayList<Map<BioEntity, Constraint>>();

	private List<Constraint> finalConstraints = new ArrayList<Constraint>();

	private Map<BioEntity, Constraint> simpleConstraints = new HashMap<BioEntity, Constraint>();

	/**
	 * Maximal number of iterations to find a steady state in the interaction
	 * network.
	 */
	private int steadyStatesIterations = 100;

	public SteadyStateAnalysis(Bind b, InteractionNetwork intNetwork,
			Map<BioEntity, Constraint> simpleConstraints) {
		super(b);
		this.intNet = intNetwork;
		this.simpleConstraints = simpleConstraints;

	}

	@Override
	public SteadyStateAnalysisResult runAnalysis() {

		SteadyStateAnalysisResult res = new SteadyStateAnalysisResult();

		if (intNet.getTargetToInteractions().isEmpty()
				&& intNet.getInitialConstraints().isEmpty()) {
			return res;
		}

		List<BioEntity> entitiesToCheck = new ArrayList<BioEntity>();
		entitiesToCheck.addAll(intNet.getTargetToInteractions().keySet());
		
		for (BioEntity ent : entitiesToCheck){
			res.addResultEntity(ent);
		}

		// we set the values for the variables in the first state
		Map<BioEntity, Constraint> thisState = new HashMap<BioEntity, Constraint>();

		for (BioEntity b : simpleConstraints.keySet()) {

			thisState.put(b, simpleConstraints.get(b));

			// if the entity is already set by a constraint, we remove te
			// interactions
			// that have this entity as a target
			if (simpleConstraints.get(b).getLb() == simpleConstraints.get(b)
					.getUb()) {

				if (intNet.getTargetToInteractions().containsKey(b)) {
					entitiesToCheck.remove(b);
				}
			}
		}
		//
		
		for (BioEntity b : intNet.getInitialConstraints().keySet()) {

			if (!thisState.containsKey(b)) {
				thisState.put(b, intNet.getInitialConstraints().get(b));
			}
			// if this entity had a simple constraint, but not fix (ub!=lb) we
			// overwrite it
			else {
				if (simpleConstraints.get(b).getLb() != simpleConstraints
						.get(b).getUb()) {
					thisState.put(b, intNet.getInitialConstraints().get(b));
				}
			}
		}

		int attractorSize = 0;

		for (int it = 1; it < steadyStatesIterations; it++) {
			statesList.add(thisState);

			// we copy the previous state
			Map<BioEntity, Constraint> newState = new HashMap<BioEntity, Constraint>();
			for (BioEntity b : thisState.keySet()) {
				newState.put(b, thisState.get(b));
			}

			Map<Constraint, double[]> newtStepConstraints = goToNextInteractionNetworkState(
					thisState, entitiesToCheck);

			// we update the values
			for (Constraint c : newtStepConstraints.keySet()) {
				newState.put((BioEntity) c.getEntities().keySet().toArray()[0],
						c);
			}

			thisState = newState;

			// /////We check that this step has not already been achieved
			boolean areTheSame = false;
			for (Map<BioEntity, Constraint> previousStep : statesList) {
				areTheSame = true;
				// compare "thisStepSimpleConstraints" with "previousStep"
				// They have to be exactly the same

				// the same size
				if (thisState.size() == previousStep.size()) {

					for (BioEntity b : thisState.keySet()) {
						if (previousStep.containsKey(b)) {
							Constraint c1 = thisState.get(b);
							Constraint c2 = previousStep.get(b);

							if (c1.getLb() != c2.getLb()
									|| c1.getUb() != c2.getUb()) {
								areTheSame = false;
							}
						} else {
							areTheSame = false;
						}
					}
				} else {
					areTheSame = false;
				}

				if (areTheSame) {
					attractorSize = it - statesList.indexOf(previousStep);

					for (int index = statesList.indexOf(previousStep); index < it; index++) {
						atractorStatesList.add(statesList.get(index));
					}

					break;
				}

			}

			if (areTheSame) {
//				if (Vars.verbose) {
//					System.err.println("Steady state found in " + (it)
//							+ " iterations.");
//					System.err.println("Attractor size : " + attractorSize);
//				}
				break;
			}

		}

		statesList.add(thisState);

		if (atractorStatesList.size() != 0) {

			for (BioEntity b : atractorStatesList.get(0).keySet()) {

				// If it is an external metab, we set a constraint
				boolean isExtMetab = false;

				if (b.getClass().getSimpleName().equals("BioPhysicalEntity")) {
					BioPhysicalEntity metab = (BioPhysicalEntity) b;
					// If it is external
					if (metab.getBoundaryCondition()) {
						isExtMetab = true;
					}
				}

				if (intNet.getTargetToInteractions().containsKey(b)
						|| isExtMetab) {

					// We make the average of the values of all states of the
					// attractor
					double lb = 0;
					double ub = 0;
					for (int nb = 0; nb < atractorStatesList.size(); nb++) {
						lb += atractorStatesList.get(nb).get(b).getLb();
						ub += atractorStatesList.get(nb).get(b).getUb();
					}

					lb = lb / atractorStatesList.size();
					ub = ub / atractorStatesList.size();

					Map<BioEntity, Double> constMap = new HashMap<BioEntity, Double>();
					constMap.put(b, 1.0);
					finalConstraints.add(new Constraint(constMap, lb, ub));
				}

				else {

				}
			}

		}
		res.setStatesList(statesList);
		res.setSteadyStateConstraints(finalConstraints);

		return res;

	}

	public Map<Constraint, double[]> goToNextInteractionNetworkState(
			Map<BioEntity, Constraint> networkState,
			List<BioEntity> entitiesToCheck) {

		Map<Constraint, double[]> contToTimeInfos = new HashMap<Constraint, double[]>();

		Map<BioEntity, Constraint> nextStepState = new HashMap<BioEntity, Constraint>();

		Set<BioEntity> setEntities = new HashSet<BioEntity>();

		for (BioEntity entity : entitiesToCheck) {

			for (Interaction i : intNet.getTargetToInteractions().get(entity)
					.getConditionalInteractions()) {

				if (i.getCondition().isTrue(networkState)) {

					// we go through all the consequences (there should be only
					// one)
					if (intNet.getInteractionToConstraints().containsKey(i)) {
						for (Constraint consequence : this.intNet
								.getInteractionToConstraints().get(i)) {

							// we check it's a simple constraint
							if (consequence.getEntities().size() == 1) {
								for (BioEntity ent : consequence.getEntities()
										.keySet()) {
									if (consequence.getEntities().get(ent) == 1.0) {

										contToTimeInfos.put(consequence,
												i.getTimeInfos());
										nextStepState.put(ent, consequence);

										setEntities.add(ent);
									}
								}
							}
						}
					}
					break;
				}
			}
		}

		for (BioEntity entity : entitiesToCheck) {
			if (!setEntities.contains(entity)) {

				Interaction defaultInt = intNet.getTargetToInteractions()
						.get(entity).getdefaultInteraction();

				// we go through all the consequences (there should be only
				// one)
				for (Constraint consequence : defaultInt.getConsequence()
						.createConstraints()) {

					for (BioEntity ent : consequence.getEntities().keySet()) {
						if (consequence.getEntities().get(ent) == 1.0) {
							contToTimeInfos.put(consequence,
									defaultInt.getTimeInfos());
							nextStepState.put(ent, consequence);

							setEntities.add(ent);
						}
					}

				}

			}
		}

		Map<Constraint, double[]> steadyStateConstraints = new HashMap<Constraint, double[]>();

		for (BioEntity ent : nextStepState.keySet()) {

			if (intNet.getTargetToInteractions().containsKey(ent)) {
				steadyStateConstraints.put(nextStepState.get(ent),
						contToTimeInfos.get(nextStepState.get(ent)));
			}
		}

		return steadyStateConstraints;
	}

}
