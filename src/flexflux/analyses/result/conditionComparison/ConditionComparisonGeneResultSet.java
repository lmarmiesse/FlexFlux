package flexflux.analyses.result.conditionComparison;

import java.util.HashMap;

public class ConditionComparisonGeneResultSet {

	HashMap<String, HashMap<String, ConditionComparisonGeneResult>> results = null;
	
	/**
	 * Constructor
	 */
	public ConditionComparisonGeneResultSet() {
		results = new HashMap<String, HashMap<String, ConditionComparisonGeneResult>>();
	}
	
	/**
	 * Add a result in the set
	 * @param result
	 */
	public void add(ConditionComparisonGeneResult result) {
		String conditionId = result.condition.code;
		String objId = result.objective.getName();
		
		if(! results.containsKey(conditionId)) {
			results.put(conditionId, new HashMap<String, ConditionComparisonGeneResult>());
		}
		
		results.get(conditionId).put(objId, result);
	}
	
	
	/**
	 * Returns the results corresponding to a condition
	 * @param code : the condition code
	 * @return {@link HashMap} of {@link String} and {@link ConditionComparisonGeneResult}
	 */
	public HashMap<String, ConditionComparisonGeneResult> get(String code) {
		
		return results.get(code);
		
	}
	
}
