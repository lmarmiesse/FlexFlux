package flexflux.analyses;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.plaf.synth.SynthScrollBarUI;

import parsebionet.biodata.BioEntity;
import flexflux.analyses.result.TDRNAAnalysisResult;
import flexflux.general.Constraint;
import flexflux.interaction.Interaction;
import flexflux.interaction.InteractionNetwork;
import flexflux.interaction.Unique;

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

		// System.out.println(intNet);

	}

	public TDRNAAnalysisResult runAnalysis() {

		entitiesToCheck = intNet.getInitialStates().keySet();

		TDRNAAnalysisResult res = new TDRNAAnalysisResult(deltaT, entitiesToCheck);

		// // check regulatory network not empty
		if (intNet.getTargetToInteractions().isEmpty() && intNet.getInitialConstraints().isEmpty()
				&& intNet.getInitialStates().isEmpty()) {
			return res;
		}
		// //

		// we set the values for the variables in the first state
		Map<BioEntity, Integer> thisState = intNet.getInitialStates();

		Map<Integer, Map<BioEntity, Unique>> iterationToStates = new HashMap<Integer, Map<BioEntity, Unique>>();

		for (int it = 1; it <= iterations + 1; it++) {

			statesList.add(thisState);

			List<Interaction> trueInteractions = getLogicalUpdates(thisState);

			// System.out.println(it);

			for (Interaction i : trueInteractions) {

				// if(i.getConsequence().getEntity().getId().equals("MYB30_p")){
				// System.out.println(i);
				// }

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

						if (iterationToStates.get(iteration).containsKey(i.getConsequence().getEntity())) {

							if (iterationToStates.get(iteration).get(i.getConsequence().getEntity()).getValue() != i
									.getConsequence().getValue()) {

								// System.out.println(iteration);
								// System.out.println(i.getConsequence().getEntity().getId());
								// System.out.println(i.getConsequence().getValue());
								// System.out.println(iterationToStates.get(iteration).get(i.getConsequence().getEntity())
								// .getValue());
								//
								// System.out.println(i.getConsequence().getPriority());
								// System.out.println(iterationToStates.get(iteration).get(i.getConsequence().getEntity())
								// .getPriority());

								if (i.getConsequence().getPriority() >= iterationToStates.get(iteration)
										.get(i.getConsequence().getEntity()).getPriority()) {

									iterationToStates.get(iteration).put(i.getConsequence().getEntity(),
											i.getConsequence());

								} 
							}

						} else {

							iterationToStates.get(iteration).put(i.getConsequence().getEntity(), i.getConsequence());
						}

					} else {

						Map<BioEntity, Unique> map = new HashMap<BioEntity, Unique>();
						map.put(i.getConsequence().getEntity(), i.getConsequence());

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
					newState.put(b, (int) iterationToStates.get(it).get(b).getValue());
				}

			}

			thisState = newState;

		}

		res.setStatesList(statesList);

		return res;
	}

	public List<Interaction> getLogicalUpdates(Map<BioEntity, Integer> networkState) {

		// list of interactions that are true (contain the time infos)
		List<Interaction> trueInteractions = new ArrayList<Interaction>();

		Map<BioEntity, Constraint> netConstraints = new HashMap<BioEntity, Constraint>();
		for (BioEntity ent : networkState.keySet()) {

			netConstraints.put(ent,
					new Constraint(ent, (double) networkState.get(ent), (double) networkState.get(ent)));
		}

		Set<BioEntity> setEntities = new HashSet<BioEntity>();

		for (BioEntity entity : entitiesToCheck) {
			if (intNet.getTargetToInteractions().get(entity)==null){
				System.err.println("Error: no update rule for variable "+entity.getId());
				System.exit(0);
			}
			
			for (Interaction i : intNet.getTargetToInteractions().get(entity).getConditionalInteractions()) {
				if (i.getCondition().isTrue(netConstraints)) {
					trueInteractions.add(i);
					setEntities.add(entity);
					break;
				}
			}
		}

		for (BioEntity entity : entitiesToCheck) {
			if (!setEntities.contains(entity)) {
				Interaction defaultInt = intNet.getTargetToInteractions().get(entity).getdefaultInteraction();
				trueInteractions.add(defaultInt);
			}
		}

		return trueInteractions;
	}

}
