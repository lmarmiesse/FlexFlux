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
	 * mean attractor generated
	 */
	protected ListOfConditions meanAttractors;
	
	/**
	 * To lock conditions
	 */
	private final Object lock = new Object();
	
	
	/**
	 * Constructor
	 */
	public MultiRSAResult() {
		
		this.meanAttractors = new ListOfConditions();
		
	}
	
	/**
	 * adds a condition
	 * @param condition
	 */
	public void addCondition(Condition condition) {
		synchronized (lock) {
			this.getMeanAttractors().add(condition);
		}
	}
	
	/**
	 * 
	 * @return the conditions
	 */
	public ListOfConditions getMeanAttractors() {
		return meanAttractors;
	}

	@Override
	public void writeToFile(String path) {
		
		Boolean flag = this.meanAttractors.writeConditionFile(path);
		
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
