/**
 * 
 */
package flexflux.analyses.result;

import flexflux.condition.Condition;
import flexflux.condition.ListOfConditions;

/**
 * @author lcottret
 *
 */
public class MultiRSAResult extends AnalysisResult {
	
	/**
	 * conditions generated
	 */
	protected ListOfConditions conditions;
	
	/**
	 * To lock conditions
	 */
	private final Object lock = new Object();
	
	
	/**
	 * Constructor
	 */
	public MultiRSAResult() {
		
		this.conditions = new ListOfConditions();
		
	}
	
	/**
	 * adds a condition
	 * @param condition
	 */
	public void addCondition(Condition condition) {
		synchronized (lock) {
			this.getConditions().add(condition);
		}
	}
	
	/**
	 * 
	 * @return the conditions
	 */
	public ListOfConditions getConditions() {
		return conditions;
	}

	@Override
	public void writeToFile(String path) {
		
		Boolean flag = this.conditions.writeConditionFile(path);
		
		if(! flag)
		{
			System.err.println("Error while writing the new condition file");
		}
		
	}

	@Override
	public void plot() {
		System.err.println("Function not implemented");
		
	}
	
	
	
	
	
	
	

}
