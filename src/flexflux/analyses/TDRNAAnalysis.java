package flexflux.analyses;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import parsebionet.biodata.BioEntity;
import flexflux.analyses.result.TDRNAAnalysisResult;
import flexflux.general.Constraint;
import flexflux.interaction.Interaction;
import flexflux.interaction.InteractionNetwork;

public class TDRNAAnalysis extends Analysis {

	private InteractionNetwork intNet;

	private List<Map<BioEntity, Integer>> statesList = new ArrayList<Map<BioEntity, Integer>>();

	private Set<BioEntity> entitiesToCheck;

	public double deltaT;

	public int iterations;

	public TDRNAAnalysis(InteractionNetwork intNetwork, int iter, double deltaT) {
		super(null);
		this.intNet = intNetwork;
		this.deltaT = deltaT;
		this.iterations = iter;
		
		
//		System.out.println(intNet);

	}

	public TDRNAAnalysisResult runAnalysis() {

		entitiesToCheck = intNet.getInitialStates().keySet();

		TDRNAAnalysisResult res = new TDRNAAnalysisResult(deltaT,
				entitiesToCheck);

		// // check regulatory network not empty
		if (intNet.getTargetToInteractions().isEmpty()
				&& intNet.getInitialConstraints().isEmpty()
				&& intNet.getInitialStates().isEmpty()) {
			return res;
		}
		// //

		// we set the values for the variables in the first state
		Map<BioEntity, Integer> thisState = intNet.getInitialStates();

		Map<Integer, Map<BioEntity, Integer>> iterationToStates = new HashMap<Integer, Map<BioEntity, Integer>>();

		for (int it = 1; it <= iterations+1; it++) {

			statesList.add(thisState);

			List<Interaction> trueInteractions = getLogicalUpdates(thisState);

			for (Interaction i : trueInteractions) {

				double begins = i.getTimeInfos()[0];
				double lasts = i.getTimeInfos()[1];
				double ends = begins + lasts;

				begins = begins + deltaT * it;
				ends = ends + deltaT * it;

				for (double t = begins; t <= ends; t += deltaT) {

					int iteration = (int) Math.round(t / deltaT);

					if (iterationToStates.containsKey(iteration)) {

						// ////////// Need check that the entity doesn't already
						// have a rule

//						if (iterationToStates.get(iteration).containsKey(
//								i.getConsequence().getEntity())) {
//
//							System.out.println("PROBLEM");
//							System.out.println("Iteration " + iteration + " "
//									+ i.getConsequence().getEntity().getId());
//							System.out.println(iterationToStates.get(iteration)
//									.get(i.getConsequence().getEntity()));
//							System.out.println((int) i.getConsequence()
//									.getValue());
//							
//							
//							
//							
//							
//						} else {

							iterationToStates.get(iteration).put(
									i.getConsequence().getEntity(),
									(int) i.getConsequence().getValue());
//						}

					} else {

						Map<BioEntity, Integer> map = new HashMap<BioEntity, Integer>();
						map.put(i.getConsequence().getEntity(), (int) i
								.getConsequence().getValue());

						iterationToStates.put(iteration, map);
					}

				}
			}

			// we copy the previous state
			Map<BioEntity, Integer> newState = new HashMap<BioEntity, Integer>();
			for (BioEntity b : thisState.keySet()) {
				newState.put(b, thisState.get(b));
			}

			if (iterationToStates.containsKey(it)) {

				for (BioEntity b : iterationToStates.get(it).keySet()) {
					newState.put(b, iterationToStates.get(it).get(b));
				}

			}

			thisState = newState;

		}

		res.setStatesList(statesList);

		return res;
	}

	public List<Interaction> getLogicalUpdates(
			Map<BioEntity, Integer> networkState) {

		// list of interactions that are true (contain the time infos)
		List<Interaction> trueInteractions = new ArrayList<Interaction>();

		Map<BioEntity, Constraint> netConstraints = new HashMap<BioEntity, Constraint>();
		for (BioEntity ent : networkState.keySet()) {

			netConstraints.put(ent,
					new Constraint(ent, (double) networkState.get(ent),
							(double) networkState.get(ent)));
		}

		Set<BioEntity> setEntities = new HashSet<BioEntity>();

		for (BioEntity entity : entitiesToCheck) {
			for (Interaction i : intNet.getTargetToInteractions().get(entity)
					.getConditionalInteractions()) {
				if (i.getCondition().isTrue(netConstraints)) {
					trueInteractions.add(i);
					setEntities.add(entity);
					break;
				}
			}
		}

		for (BioEntity entity : entitiesToCheck) {
			if (!setEntities.contains(entity)) {
				Interaction defaultInt = intNet.getTargetToInteractions()
						.get(entity).getdefaultInteraction();
				trueInteractions.add(defaultInt);
			}
		}

		return trueInteractions;
	}

}
