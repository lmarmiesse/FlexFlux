package flexflux.thread;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

import parsebionet.biodata.BioEntity;
import flexflux.analyses.RSAAnalysis;
import flexflux.analyses.result.MultiRSAResult;
import flexflux.analyses.result.RSAAnalysisResult;
import flexflux.condition.Condition;
import flexflux.general.Constraint;
import flexflux.general.ConstraintType;
import flexflux.general.Vars;
import flexflux.interaction.InteractionNetwork;

public class ThreadMultiRSA extends Thread {

	/**
	 * Number of conditions to treat.
	 */
	private int todo;

	/**
	 * Queue of conditions
	 */
	private Queue<Condition> conditions;

	/**
	 * Final result
	 */
	private MultiRSAResult result;

	/**
	 * 
	 * Percentage of the analysis that is completed.
	 * 
	 */
	private static long percentage = 0;

	/**
	 * Interaction network
	 */
	public InteractionNetwork interactionNetwork = null;

	/**
	 * Constructor
	 * 
	 * @param conditions
	 * @param result
	 */
	public ThreadMultiRSA(InteractionNetwork intNet,
			Queue<Condition> conditions, MultiRSAResult result) {

		this.conditions = conditions;
		this.todo = conditions.size();
		this.result = result;
		this.interactionNetwork = intNet;
		percentage = 0;
	}

	/**
	 * Starts the thread.
	 */
	public void run() {

		Condition condition;

		
		while ((condition = conditions.poll()) != null) {

			Condition newCondition = new Condition(condition.code,
					condition.name);
			
			condition
					.addInitialConstraintsToInteractionNetwork(this.interactionNetwork);

			RSAAnalysis rsa = new RSAAnalysis(this.interactionNetwork,
					new HashMap<BioEntity, Constraint>());

			RSAAnalysisResult res = rsa.runAnalysis();

			Map<BioEntity, Double> meanAttractorStates = res
					.getMeanAttractorStates();

			for (BioEntity ent : meanAttractorStates.keySet()) {
				Double value = meanAttractorStates.get(ent);
				newCondition.addConstraint(ent.getId(), Vars.round(value),
						ConstraintType.DOUBLE);
			}

			result.addCondition(newCondition);

			int percent = (int) Math.round(((double) todo - (double) conditions
					.size()) / (double) todo * 100);

			if (percent > percentage) {

				percentage = percent;
				if (percent % 2 == 0 && !Vars.verbose) {
					 System.err.print("*");
				}
			}
			

		}

	}

}
