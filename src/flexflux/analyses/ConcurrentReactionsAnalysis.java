/*******************************************************************************
 * Copyright INRA
 * 
 *  Contact: ludovic.cottret@toulouse.inra.fr
 * 
 * 
 * This software is governed by the CeCILL license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 * 
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 *  In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 *  The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL license and that you accept its terms.
 ******************************************************************************/
/**
 * 15 mai 2015
 */
package flexflux.analyses;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import parsebionet.biodata.BioChemicalReaction;
import parsebionet.biodata.BioEntity;
import flexflux.analyses.result.ConcurrentReactionsResult;
import flexflux.analyses.result.FVAResult;
import flexflux.general.Bind;
import flexflux.general.Constraint;
import flexflux.general.Vars;
import flexflux.thread.ResolveThread;
import flexflux.thread.ThreadConcurrentReactions;

/**
 * 
 * Class to identify concurrentReactions. Developed only fo
 * ClassificationAnalysis
 * 
 * @author lmarmiesse 15 mai 2015
 * 
 */
public class ConcurrentReactionsAnalysis extends Analysis {

	HashMap<String, BioEntity> startingReactions;
	FVAResult previousFVA;
	
	/**
	 * List containing the threads that will maximize and minimize each entity.
	 * 
	 */
	protected List<ResolveThread> threads = new ArrayList<ResolveThread>();

	/**
	 * Constructor
	 * 
	 * @param b
	 * @param startingReactions
	 * @param previousFVA
	 * @param concurrentReactions
	 * @param otherReactions
	 */
	public ConcurrentReactionsAnalysis(Bind b,
			HashMap<String, BioEntity> startingReactions, FVAResult previousFVA) {
		super(b);

		this.startingReactions = startingReactions;
		this.previousFVA = previousFVA;

	}

	public ConcurrentReactionsResult runAnalysis() {

		double startTime = System.currentTimeMillis();
		
		ConcurrentReactionsResult result = new ConcurrentReactionsResult();
		
		// Change all the internal reaction bounds to extreme values
		for (BioEntity e : b.getSimpleConstraints().keySet()) {

			String id = e.getId();

			if (b.getBioNetwork().getBiochemicalReactionList().containsKey(id)) {

				BioChemicalReaction reaction = b.getBioNetwork()
						.getBiochemicalReactionList().get(id);

				// We limit the exchange reaction identification to one to
				// one
				// reactions
				if (!(reaction.isExchangeReaction()
						&& reaction.getLeftParticipantList().size() == 1 && reaction
						.getRightParticipantList().size() == 1)) {
					// if the reaction is not an exchange reaction we change
					// its
					// lower and upper bound to FVA max values.
					Constraint constraint = b.getSimpleConstraints().get(e);
					constraint.setUb(Vars.maxUpperBound);

					if (reaction.isReversible()) {
						constraint.setLb(Vars.minLowerBound);
					} else {
						constraint.setLb(0);
					}
				}
			}
		}
		
		Queue<BioEntity> entQueue = new LinkedBlockingQueue<BioEntity>();
		
		for(BioEntity e : this.startingReactions.values()) {
			entQueue.add(e);
		}
		
		for (int j = 0; j < Vars.maxThread; j++) {
			
			ThreadConcurrentReactions thread = new ThreadConcurrentReactions(b, entQueue,
					previousFVA, result);
			
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
			System.err.println("ConcurrentReactionAnalysis over "
					+ ((System.currentTimeMillis() - startTime) / 1000) + "s "
					+ Vars.maxThread + " threads");
		}
		
		
		return result;
		

	}

}
