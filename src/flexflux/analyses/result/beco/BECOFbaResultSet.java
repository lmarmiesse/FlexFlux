package flexflux.analyses.result.beco;

import java.util.HashMap;

public class BECOFbaResultSet {
	
	HashMap<String, HashMap<String, BECOFbaResult>> results = null;
	
	public BECOFbaResultSet() {
		results = new HashMap<String, HashMap<String, BECOFbaResult>>();
	}
	
	public void add(BECOFbaResult result) {
		
		String conditionId = result.condition.code;
		String objId = result.objective.getName();
		
		if(! results.containsKey(conditionId)) {
			results.put(conditionId, new HashMap<String, BECOFbaResult>());
		}
		
		results.get(conditionId).put(objId, result);
		
	}
	
	/**
	 * Returns the results corresponding to a condition
	 * @param code : the condition code
	 * @return {@link HashMap} of {@link String} and {@link BECOFbaResult}
	 */
	public HashMap<String, BECOFbaResult> get(String code) {
		
		return results.get(code);
		
	}
	
	

}
