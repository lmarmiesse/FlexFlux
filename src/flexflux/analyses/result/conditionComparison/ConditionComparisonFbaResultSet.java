package flexflux.analyses.result.conditionComparison;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class ConditionComparisonFbaResultSet {
	
	HashMap<String, HashMap<String, ConditionComparisonFbaResult>> results = null;
	
	public ConditionComparisonFbaResultSet() {
		results = new HashMap<String, HashMap<String, ConditionComparisonFbaResult>>();
	}
	
	public void add(ConditionComparisonFbaResult result) {
		
		String conditionId = result.condition.code;
		String objId = result.objective.getName();
		
		if(! results.containsKey(conditionId)) {
			results.put(conditionId, new HashMap<String, ConditionComparisonFbaResult>());
		}
		
		results.get(conditionId).put(objId, result);
		
	}
	
	/**
	 * Returns the results corresponding to a condition
	 * @param code : the condition code
	 * @return {@link HashMap} of {@link String} and {@link ConditionComparisonFbaResult}
	 */
	public HashMap<String, ConditionComparisonFbaResult> get(String code) {
		
		return results.get(code);
		
	}
	
	

}
