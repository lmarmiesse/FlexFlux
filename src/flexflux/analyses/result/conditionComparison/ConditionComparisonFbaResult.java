package flexflux.analyses.result.conditionComparison;

import flexflux.condition.Condition;
import flexflux.general.Objective;

public class ConditionComparisonFbaResult {
	
	public Objective objective;
	
	public Condition condition;
	
	public Double value;
	
	public ConditionComparisonFbaResult(Objective o, Condition c, Double v) {
		this.objective = o;
		this.condition = c;
		this.value = v;
	}
	
	

}
