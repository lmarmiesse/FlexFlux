package flexflux.analyses.result.conditionComparison;

import java.util.HashMap;
import java.util.List;

import parsebionet.biodata.BioChemicalReaction;
import parsebionet.biodata.BioEntity;
import parsebionet.biodata.BioNetwork;
import parsebionet.biodata.BioPhysicalEntity;
import flexflux.analyses.result.FVAResult;
import flexflux.condition.Condition;
import flexflux.general.Objective;

public class ConditionComparisonFvaResult {

	public Objective objective;

	public Condition condition;

	public FVAResult fvaResult;

	HashMap<String, BioEntity> essentialReactions;
	/**
	 * Potentially used but note essential
	 */
	HashMap<String, BioEntity> dispensableReactions;
	HashMap<String, BioEntity> potentiallyUsedReactions;
	
	HashMap<String, BioEntity> deadReactions;

	public ConditionComparisonFvaResult(Objective o, Condition c,
			FVAResult result) {
		this.objective = o;
		this.condition = c;
		this.fvaResult = result;

		/**
		 * Get essential reactions
		 */
		essentialReactions = new HashMap<String, BioEntity>();
		List<BioEntity> essentialReactionsList = result.getEssentialReactions();
		for (BioEntity e : essentialReactionsList) {
			this.essentialReactions.put(e.getId(), e);
		}

		/**
		 * Get used, dispensable and dead reactions
		 */
		dispensableReactions = new HashMap<String, BioEntity>();
		potentiallyUsedReactions = new HashMap<String, BioEntity>();
		
		deadReactions = new HashMap<String, BioEntity>();
		for (BioEntity e : result.getMap().keySet()) {
			double values[] = result.getMap().get(e);

			double min = values[0];
			double max = values[1];

			if (min != 0 || max != 0) {
				potentiallyUsedReactions.put(e.getId(), e);
				if(! essentialReactions.containsKey(e.getId())) {
					dispensableReactions.put(e.getId(), e);
				}
			} else if (min == 0 && max == 0) {
				deadReactions.put(e.getId(), e);
			}
		}
	}
}
