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
 * 13 mars 2013 
 */
package thread;

import general.Bind;
import general.Constraint;
import general.Objective;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import parsebionet.biodata.BioEntity;
import analyses.result.FVAResult;

/**
 * 
 * Thread to perform an FVA analysis.
 * 
 * @author lmarmiesse 13 mars 2013
 * 
 */
public class ThreadFVA extends ResolveThread {

	/**
	 * Number of entities to treat.
	 */
	private int todo;

	/**
	 * Contains all entities to treat.
	 */
	private Queue<BioEntity> entities;
	private Queue<BioEntity> entitiesCopy = new LinkedBlockingQueue<BioEntity>();

	/**
	 * The FVA result.
	 */
	private FVAResult result;

	/**
	 * 
	 * Percentage of the FVA that is completed.
	 * 
	 */
	private static int percentage = 0;

	public ThreadFVA(Bind b, Queue<BioEntity> ents, Queue<BioEntity> entsCopy,
			FVAResult result) {
		super(b);
		this.todo = ents.size();
		this.entities = ents;
		this.entitiesCopy = entsCopy;
		this.result = result;
		percentage = 0;
	}

	/**
	 * Starts the thread.
	 */
	public void run() {
		bind.setObjective(new Objective());
		bind.makeSolverObjective();
		
		
		// we do all the minimize
		bind.setObjSense(false);
		double size;
		while ((size = entities.size()) > 0) {

			BioEntity entity = entities.poll();
			
			bind.changeObjVarValue(entity, 1.0);
			result.setMin(entity, bind.FBA(false, false).result);
			bind.changeObjVarValue(entity, 0.0);

			int percent = (int) Math.round((todo - size) / todo * 50);
			if (percent > percentage) {

				percentage = percent;
				if (percent % 2 == 0) {
					System.out.print("*");
				}
			}

		}
		// and all the maximize
		bind.setObjSense(true);
		while ((size = entitiesCopy.size()) > 0) {
			BioEntity reaction = entitiesCopy.poll();

			bind.changeObjVarValue(reaction, 1.0);
			result.setMax(reaction, bind.FBA(false, false).result);
			bind.changeObjVarValue(reaction, 0.0);

			int percent = (int) Math.round((todo - size) / todo * 50) + 50;
			if (percent > percentage) {
				percentage = percent;
				if (percent % 2 == 0 && percentage != 0) {
					System.out.print("*");
				}
			}
		}

		bind.end();
	}

}
