package flexflux.analyses;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

import flexflux.analyses.result.ROBAResult;
import flexflux.condition.Condition;
import flexflux.condition.ListOfConditions;
import flexflux.general.Bind;
import flexflux.general.Vars;
import flexflux.objective.ListOfObjectives;
import flexflux.thread.ResolveThread;
import flexflux.thread.ThreadROBA;

public class ROBAAnalysis extends Analysis {


	/**
	 * List of conditions : computed by simulations or asserted from a file
	 */
	public ListOfConditions conditions;
	
	/**
	 * List of inputs in inputRandomParameters
	 */
	Set<String> inputs;

	/**
	 * Map of objectives, key : name, value : expression
	 */
	public ListOfObjectives objectives;

	/**
	 * List containing the threads that will solve each step of simulation
	 */
	protected List<ResolveThread> threads = new ArrayList<ResolveThread>();


	/**
	 * Constructor
	 * 
	 * @param b
	 * @param numberSimulations
	 */
	public ROBAAnalysis(Bind b, ListOfObjectives objectives, ListOfConditions conditions) {
		super(b);
		this.objectives = objectives;
		this.conditions = conditions;
	}
	

	@Override
	public ROBAResult runAnalysis() {

		double startTime = System.currentTimeMillis();

		ROBAResult result = new ROBAResult(new HashSet<String>(this.conditions.entities), objectives.objectives.keySet());

		Queue<Condition> tasks = new LinkedBlockingQueue<Condition>();
		
		for (int i = 0; i < conditions.size(); i++) {
			tasks.add(conditions.conditions.get(i));
		}

		for (int i = 0; i < Vars.maxThread; i++) {

			Bind newBind = null;

			try {
				newBind = b.copy();
			} catch (ClassNotFoundException | NoSuchMethodException
					| SecurityException | InstantiationException
					| IllegalAccessException | IllegalArgumentException
					| InvocationTargetException e) {
				e.printStackTrace();
				System.exit(1);
			}

			ThreadROBA threadEra = new ThreadROBA(newBind, tasks, objectives, result);

			threads.add(threadEra);

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

		for (ResolveThread thread : threads) {
			thread.start();
		}

		for (ResolveThread thread : threads) {
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
			System.err.println("ROBA analysis over "
					+ ((System.currentTimeMillis() - startTime) / 1000) + "s "
					+ Vars.maxThread + " threads");
		}
		return result;

	}
	
	
	
}
