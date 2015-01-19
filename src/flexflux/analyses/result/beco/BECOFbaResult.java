package flexflux.analyses.result.beco;

import flexflux.condition.Condition;
import flexflux.objective.Objective;

public class BECOFbaResult {
	
	public Objective objective;
	
	public Condition condition;
	
	public Double value;
	
	public BECOFbaResult(Objective o, Condition c, Double v) {
		this.objective = o;
		this.condition = c;
		this.value = v;
	}
	
	

}
