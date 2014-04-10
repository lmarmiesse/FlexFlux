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
 * 15 mars 2013 
 */
package thread;

import general.Bind;
import general.Constraint;
import general.DoubleResult;
import general.Objective;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import parsebionet.biodata.BioEntity;
import analyses.result.KOResult;

/**
 * Thread to perform an FVA analysis.
 * 
 * @author lmarmiesse 15 mars 2013
 * 
 */
public class ThreadKO extends ResolveThread {

	/**
	 * Number of entities to treat.
	 */
	private double todo;

	/**
	 * Contains all entities to treat.
	 */
	private Queue<BioEntity> entities;

	/**
	 * The KO result.
	 */
	private KOResult result;

	/**
	 * 
	 * Percentage of the KO that is completed.
	 * 
	 */
	private static int percentage = 0;

	public ThreadKO(Bind b, Queue<BioEntity> entities, KOResult result,
			Objective obj) {
		super(b, obj);
		this.todo = entities.size();
		this.entities = entities;
		this.result = result;
	}

	public void run() {
		double size;
		while ((size = entities.size()) > 0) {

			BioEntity entity = entities.poll();

			Map<BioEntity, Double> entityMap = new HashMap<BioEntity, Double>();
			entityMap.put(entity, 1.0);

			List<Constraint> constraintsToAdd = new ArrayList<Constraint>();

			constraintsToAdd.add(new Constraint(entityMap, 0.0, 0.0));

			DoubleResult value = bind.FBA(constraintsToAdd, false, true);

			result.addLine(entity, value.result);

			int percent = (int) Math.round((todo - size) / todo * 100);
			if (percent > percentage) {
				percentage = percent;
				if (percent % 2 == 0) {
					System.out.print("*");
				}
			}

		}

		bind.end();
	}
}
