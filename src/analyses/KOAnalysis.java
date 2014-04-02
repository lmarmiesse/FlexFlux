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
 * 5 avr. 2013 
 */
package analyses;

import general.Bind;
import general.Constraint;
import general.Vars;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import parsebionet.biodata.BioChemicalReaction;
import parsebionet.biodata.BioEntity;
import parsebionet.biodata.BioGene;
import thread.ResolveThread;
import analyses.result.KOResult;

/**
 * 
 * Class to run a KO analysis.
 * 
 * @author lmarmiesse 5 avr. 2013
 * 
 */
public class KOAnalysis extends Analysis {

	/**
	 * 
	 * 0 : the KO is performed on reactions. 1 : the KO is performed on genes.
	 */
	protected int mode;

	/**
	 * 
	 * Entities to run the KO analysis on.
	 * 
	 */
	protected Map<String, BioEntity> entities;

	/**
	 * List containing the threads that will knock out each entity.
	 * 
	 */
	protected List<ResolveThread> threads = new ArrayList<ResolveThread>();

	public KOAnalysis(Bind b, int mode, Map<String, BioEntity> entities) {
		super(b);
		this.mode = mode;
		this.entities = entities;
	}

	public KOResult runAnalysis() {
		// Objective obj =bind.get

		List<Constraint> constraintsToAdd = new ArrayList<Constraint>();
		// we add the constraints corresponding to the interactions


			for (Constraint c : b.findInteractionNetworkSteadyState()) {
				constraintsToAdd.add(c);
			}
		

		b.getConstraints().addAll(constraintsToAdd);

		double startTime = System.currentTimeMillis();

		KOResult koResult = new KOResult();

		Map<String, BioEntity> reactionsMap = new HashMap<String, BioEntity>();

		if (entities == null) {

			if (mode == 0) {
				Map<String, BioChemicalReaction> networkEntities = b
						.getBioNetwork().getBiochemicalReactionList();
				for (String name : networkEntities.keySet()) {
					reactionsMap.put(name, networkEntities.get(name));
				}
			} else if (mode == 1) {
				Map<String, BioGene> networkEntities = b.getBioNetwork()
						.getGeneList();
				for (String name : networkEntities.keySet()) {
					reactionsMap.put(name, networkEntities.get(name));
				}
			}

		} else {
			reactionsMap = entities;
		}

		Queue<BioEntity> tasks = new LinkedBlockingQueue<BioEntity>();

		for (String entityName : reactionsMap.keySet()) {
			tasks.add(reactionsMap.get(entityName));
		}

		for (int j = 0; j < Vars.maxThread; j++) {
			threads.add(b.getThreadFactory().makeKOThread(tasks, koResult,
					b.getObjective()));
		}

		System.out.println("Progress : ");

		System.out.print("[");
		for (int i = 0; i < 50; i++) {
			System.out.print(" ");
		}
		System.out.print("]\n");
		System.out.print("[");

		for (ResolveThread thread : threads) {
			thread.start();
		}

		for (ResolveThread thread : threads) {

			// permits to wait for the threads to end
			try {
				thread.join();
			} catch (InterruptedException e) {
//				e.printStackTrace();
			}

		}
		System.out.print("]\n");

		while (threads.size() > 0) {
			threads.remove(0);
		}

		b.getConstraints().removeAll(constraintsToAdd);

		System.out.println("KO over "
				+ ((System.currentTimeMillis() - startTime) / 1000) + "s "
				+ Vars.maxThread + " threads");
		return koResult;
	}

}
