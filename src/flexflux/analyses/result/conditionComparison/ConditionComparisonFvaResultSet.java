package flexflux.analyses.result.conditionComparison;

import java.util.HashMap;

public class ConditionComparisonFvaResultSet {

HashMap<String, HashMap<String, ConditionComparisonFvaResult>> results = null;
	
	public ConditionComparisonFvaResultSet() {
		results = new HashMap<String, HashMap<String, ConditionComparisonFvaResult>>();
	}
	
	public void add(ConditionComparisonFvaResult result) {
		
		String conditionId = result.condition.code;
		String objId = result.objective.getName();
		
		if(! results.containsKey(conditionId)) {
			results.put(conditionId, new HashMap<String, ConditionComparisonFvaResult>());
		}
		
		results.get(conditionId).put(objId, result);
		
	}
	
	/**
	 * Returns the results corresponding to a condition
	 * @param code : the condition code
	 * @return {@link HashMap} of {@link String} and {@link ConditionComparisonFvaResult}
	 */
	public HashMap<String, ConditionComparisonFvaResult> get(String code) {
		
		return results.get(code);
		
	}
	
	
}
