/**
 * Performs multiple RSA from a list of Conditions
 */
package flexflux.analyses;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import flexflux.analyses.result.MultiRSAResult;
import flexflux.condition.Condition;
import flexflux.condition.ListOfConditions;
import flexflux.general.Vars;
import flexflux.interaction.InteractionNetwork;
import flexflux.thread.ThreadMultiRSA;

/**
 * @author lcottret
 *
 */
public class MultiRSA extends Analysis {
	
	/**
	 * list of threads
	 */
	protected List<ThreadMultiRSA> threads;
	
	/**
	 * List of conditions
	 */
	public ListOfConditions listOfConditions;
	
	/**
	 * Interaction network
	 */
	public InteractionNetwork interactionNetwork = null;
	
	/**
	 * Constructor
	 * @param b
	 * @param listOfConditions
	 * @param fixConditions
	 */
	public MultiRSA(InteractionNetwork intNet, ListOfConditions listOfConditions) {
		
		super(null);
		
		this.listOfConditions = listOfConditions;
		this.interactionNetwork = intNet;
		
	}

	@Override
	public MultiRSAResult runAnalysis() {
		
		double startTime = System.currentTimeMillis();
		
		MultiRSAResult result = new MultiRSAResult();
		
		Queue<Condition> tasks = new LinkedBlockingQueue<Condition>();
		
		for(Condition c : this.listOfConditions.conditions)
		{
			tasks.add(c);
		}
		
		for (int i = 0; i < Vars.maxThread; i++) {
			
			
			ThreadMultiRSA thread = new ThreadMultiRSA(this.interactionNetwork, tasks, result);
			
			threads.add(thread);
			
		}
		
		if (Vars.verbose) {
			System.err.println("Progress : ");

			System.err.print("[");
			for (int i = 0; i < 50; i++) {
				System.err.print(" ");
			}
			System.err.print("]\n");
			System.err.print("[");
		}
		
		for (Thread thread : threads) {
			thread.start();
		}

		for (Thread thread : threads) {
			// permits to wait for the threads to end
			try {
				thread.join();
			} catch (InterruptedException e) {
				// e.printStackTrace();
			}
		}

		if (Vars.verbose) {
			System.err.print("]\n");
		}

		// we remove the threads to permit another analysis
		while (threads.size() > 0) {
			threads.remove(0);
		}

		if (Vars.verbose) {
			System.err.println("MultiRSA analysis over "
					+ ((System.currentTimeMillis() - startTime) / 1000) + "s "
					+ Vars.maxThread + " threads");
		}
		return result;
		
		
		
	}
	

}
