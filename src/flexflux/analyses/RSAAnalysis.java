package flexflux.analyses;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JOptionPane;

import parsebionet.biodata.BioEntity;
import parsebionet.biodata.BioPhysicalEntity;
import flexflux.analyses.result.RSAAnalysisResult;
import flexflux.general.Bind;
import flexflux.general.Constraint;
import flexflux.interaction.Interaction;
import flexflux.interaction.InteractionNetwork;

public class RSAAnalysis extends Analysis {

	private InteractionNetwork intNet;

	private List<Map<BioEntity, Integer>> statesList = new ArrayList<Map<BioEntity, Integer>>();

	private List<Map<BioEntity, Integer>> atractorStatesList = new ArrayList<Map<BioEntity, Integer>>();

	private List<Constraint> finalConstraints = new ArrayList<Constraint>();

	private Map<BioEntity, Constraint> simpleConstraints = new HashMap<BioEntity, Constraint>();

	/**
	 * Maximal number of iterations to find a steady state in the regulatory
	 * network.
	 */
	private int steadyStatesIterations = 100;

	public RSAAnalysis(Bind b, InteractionNetwork intNetwork,
			Map<BioEntity, Constraint> simpleConstraints) {
		super(b);
		this.intNet = intNetwork;
		this.simpleConstraints = simpleConstraints;

	}

	@Override
	public RSAAnalysisResult runAnalysis() {

		RSAAnalysisResult res = new RSAAnalysisResult();

		if (intNet.getTargetToInteractions().isEmpty()
				&& intNet.getInitialConstraints().isEmpty()
				&& intNet.getInitialStates().isEmpty()) {
			return res;
		}

		List<BioEntity> entitiesToCheck = new ArrayList<BioEntity>();
		entitiesToCheck.addAll(intNet.getTargetToInteractions().keySet());

		for (BioEntity ent : entitiesToCheck) {
			res.addResultEntity(ent);
		}

		// we set the values for the variables in the first state
		Map<BioEntity, Integer> thisState = intNet.getInitialStates();

		for (BioEntity b : intNet.getInitialConstraints().keySet()) {

			// If the entity is in the regulatory network
			if (intNet.getInteractionNetworkEntities().containsKey(b.getId())) {

				// TRANSLATION
				if (intNet.canTranslate(b)) {
					thisState.put(
							b,
							intNet.getStateFromValue(b, intNet
									.getInitialConstraints().get(b).getLb()));
				} else {

					int stateMin = intNet.getInteractionNetworkEntitiesStates()
							.get(b)[0];

					int stateMax = intNet.getInteractionNetworkEntitiesStates()
							.get(b)[1];

					double value = intNet.getInitialConstraints().get(b)
							.getLb();

					// If the value is an integer AND is between min and max
					// states
					if (value <= stateMax && value >= stateMin
							&& value == Math.floor(value)) {

						thisState.put(b, (int) value);

					} else {
						System.err
								.println("Error : no translation available for variable "
										+ b.getId() + " and value " + value);
						System.exit(0);
					}

				}

				// if this entity had a simple constraint, but not fix (ub!=lb)
				// we
				// overwrite it

				if (simpleConstraints.containsKey(b)) {
					if (simpleConstraints.get(b).getLb() != simpleConstraints
							.get(b).getUb()) {

						if (intNet.canTranslate(b)) {
							thisState.put(
									b,
									intNet.getStateFromValue(b, intNet
											.getInitialConstraints().get(b)
											.getLb()));
						} else {
							int stateMin = intNet
									.getInteractionNetworkEntitiesStates().get(
											b)[0];

							int stateMax = intNet
									.getInteractionNetworkEntitiesStates().get(
											b)[1];

							double value = simpleConstraints.get(b).getLb();

							// If the value is an integer AND is between min and
							// max
							// states
							if (value <= stateMax && value >= stateMin
									&& value == Math.floor(value)) {

								thisState.put(b, (int) value);

							} else {
								System.err
										.println("Error : no translation available for variable "
												+ b.getId()
												+ " and value "
												+ value);
								System.exit(0);
							}
						}
					}
				}
			}
		}

		for (BioEntity b : simpleConstraints.keySet()) {

			// If the entity is in the regulatory network
			if (intNet.getInteractionNetworkEntities().containsKey(b.getId())) {

				// if the entity is already set by a constraint, we remove te
				// interactions
				// that have this entity as a target
				if (simpleConstraints.get(b).getLb() == simpleConstraints
						.get(b).getUb()) {

					// TRANSLATION
					if (intNet.canTranslate(b)) {

						thisState.put(b, intNet.getStateFromValue(b,
								simpleConstraints.get(b).getLb()));
					} else {
						int stateMin = intNet
								.getInteractionNetworkEntitiesStates().get(b)[0];

						int stateMax = intNet
								.getInteractionNetworkEntitiesStates().get(b)[1];

						double value = simpleConstraints.get(b).getLb();

						// If the value is an integer AND is between min and
						// max
						// states
						if (value <= stateMax && value >= stateMin
								&& value == Math.floor(value)) {

							thisState.put(b, (int) value);

						} else {
							System.err
									.println("Error : no translation available for variable "
											+ b.getId() + " and value " + value);
							System.exit(0);
						}
					}

					if (intNet.getTargetToInteractions().containsKey(b)) {
						entitiesToCheck.remove(b);
					}
				}
			}
		}
		//

		int attractorSize = 0;

		for (int it = 1; it < steadyStatesIterations; it++) {
			statesList.add(thisState);

			// we copy the previous state
			Map<BioEntity, Integer> newState = new HashMap<BioEntity, Integer>();
			for (BioEntity b : thisState.keySet()) {
				newState.put(b, thisState.get(b));
			}

			Map<Constraint, double[]> newtStepConstraints = goToNextInteractionNetworkState(
					thisState, entitiesToCheck);

			// we update the values
			for (Constraint c : newtStepConstraints.keySet()) {
				newState.put((BioEntity) c.getEntities().keySet().toArray()[0],
						(int) Math.round(c.getLb()));
			}

			thisState = newState;

			// /////We check that this step has not already been achieved
			boolean areTheSame = false;
			for (Map<BioEntity, Integer> previousStep : statesList) {
				areTheSame = true;
				// compare "thisStepSimpleConstraints" with "previousStep"
				// They have to be exactly the same

				// the same size
				if (thisState.size() == previousStep.size()) {

					for (BioEntity b : thisState.keySet()) {
						if (previousStep.containsKey(b)) {

							if (thisState.get(b) != previousStep.get(b)) {
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
				// if (Vars.verbose) {
				// System.err.println("Steady state found in " + (it)
				// + " iterations.");
				// System.err.println("Attractor size : " + attractorSize);
				// }
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

				// if (intNet.getTargetToInteractions().containsKey(b)
				// || isExtMetab) {

				// We make the average of the values of all states of the
				// attractor
				double lb = 0;
				double ub = 0;
				for (int nb = 0; nb < atractorStatesList.size(); nb++) {
					if (intNet.canTranslate(b)) {
						lb += intNet.getConstraintFromState(b,
								atractorStatesList.get(nb).get(b)).getLb();
						ub += intNet.getConstraintFromState(b,
								atractorStatesList.get(nb).get(b)).getUb();
					} else {
						lb += atractorStatesList.get(nb).get(b);
						ub += atractorStatesList.get(nb).get(b);
					}

				}

				lb = lb / atractorStatesList.size();
				ub = ub / atractorStatesList.size();

				Map<BioEntity, Double> constMap = new HashMap<BioEntity, Double>();
				constMap.put(b, 1.0);
				finalConstraints.add(new Constraint(constMap, lb, ub));
				// }

			}

		} else {
			System.err
					.println("Warning, no regulatory network attractor was found in "
							+ steadyStatesIterations + " iterations.");

		}
		res.setStatesList(statesList);
		res.setatractorStatesList(atractorStatesList);

		// ////TRANSLATION

		res.setSteadyStateConstraints(finalConstraints);

		// System.out.println("Attractor size : "+attractorSize);

		return res;

	}

	public Map<Constraint, double[]> goToNextInteractionNetworkState(
			Map<BioEntity, Integer> networkState,
			List<BioEntity> entitiesToCheck) {
		Map<BioEntity, Constraint> netConstraints = new HashMap<BioEntity, Constraint>();
		for (BioEntity ent : networkState.keySet()) {

			netConstraints.put(ent,
					new Constraint(ent, (double) networkState.get(ent),
							(double) networkState.get(ent)));
		}

		Map<Constraint, double[]> contToTimeInfos = new HashMap<Constraint, double[]>();

		Map<BioEntity, Constraint> nextStepState = new HashMap<BioEntity, Constraint>();

		Set<BioEntity> setEntities = new HashSet<BioEntity>();

		for (BioEntity entity : entitiesToCheck) {

			for (Interaction i : intNet.getTargetToInteractions().get(entity)
					.getConditionalInteractions()) {

				if (i.getCondition().isTrue(netConstraints)) {

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
