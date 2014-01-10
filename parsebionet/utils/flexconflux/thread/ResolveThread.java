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
 * 8 mars 2013 
 */
package parsebionet.utils.flexconflux.thread;

import java.util.HashMap;
import java.util.Map;

import parsebionet.utils.flexconflux.Bind;
import parsebionet.utils.flexconflux.Constraint;
import parsebionet.utils.flexconflux.Objective;

/**
 * 
 * 
 * Superclass of all FlexFlux threads.
 * 
 * 
 * To avoid problems, all threads copy the a bind object.
 * 
 * @author lmarmiesse 8 mars 2013
 * 
 */
public abstract class ResolveThread extends Thread {

	/**
	 * 
	 * The bind to copy.
	 * 
	 */
	protected Bind bind;

	/**
	 * Constraints to add.
	 */
	protected Map<Constraint, Boolean> constraintsToAdd = new HashMap<Constraint, Boolean>();

	/**
	 * Creates a thread with an objective function.
	 * 
	 * @param b
	 *            The bind to copy.
	 * @param obj
	 *            The objective function.
	 */
	public ResolveThread(Bind b, Objective obj) {

		this.bind = b;
		bind.setObjective(obj);
		bind.prepareSolver();

	}

	/**
	 * Creates a thread without an objective function.
	 * 
	 * @param b
	 *            The bind to copy.
	 */
	public ResolveThread(Bind b) {

		this.bind = b;
		bind.prepareSolver();
	}

	public void setConstraintsToAdd(Map<Constraint, Boolean> c) {
		this.constraintsToAdd = c;
	}

}