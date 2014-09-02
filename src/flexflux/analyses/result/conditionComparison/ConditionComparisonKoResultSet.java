package flexflux.analyses.result.conditionComparison;

import java.util.HashMap;

public class ConditionComparisonKoResultSet {

	
	HashMap<String, HashMap<String, ConditionComparisonKoResult>> results = null;

	public ConditionComparisonKoResultSet() {
		results = new HashMap<String, HashMap<String, ConditionComparisonKoResult>>();
	}
	
	public void add(ConditionComparisonKoResult result) {
		String conditionId = result.condition.code;
		String objId = result.objective.getName();
		
		if(! results.containsKey(conditionId)) {
			results.put(conditionId, new HashMap<String, ConditionComparisonKoResult>());
		}
		
		results.get(conditionId).put(objId, result);
	}
	
	/**
	 * Returns the results corresponding to a condition
	 * @param code : the condition code
	 * @return {@link HashMap} of {@link String} and {@link ConditionComparisonKoResult}
	 */
	public HashMap<String, ConditionComparisonKoResult> get(String code) {
		
		return results.get(code);
		
	}
	
	
}
