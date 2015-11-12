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
 * 7 mars 2013 
 */
package flexflux.operation;

import flexflux.general.Constraint;

import java.util.List;

import parsebionet.biodata.BioEntity;

/**
 * 
 * Superclass of all operations.
 * 
 * @author lmarmiesse 7 mars 2013
 * 
 */
public abstract class Operation {

	public abstract String toString();

	/**
	 * 
	 * Make constraint from an entity and a value.
	 * 
	 * <p>
	 * Only used when interactions are not in the solver.
	 * 
	 * @param entity
	 *            The entity concerned.
	 * @param value
	 *            The value to use.
	 * @return A list of constraints.
	 */
	public abstract List<Constraint> makeConstraint(BioEntity entity,
			double value);

	/**
	 * 
	 * Checks if the combination of the constraint, the operation and the value
	 * is true or not.
	 * 
	 * <p>
	 * Only used when interactions are not in the solver.
	 * 
	 * @param cons
	 *            The constraint to check.
	 * @param value
	 *            The value to check.
	 * @return If the combination is true.
	 */
	public abstract boolean isTrue(Constraint cons, double value);

	public abstract boolean isInverseTrue(Constraint cons, double value);

	public abstract String toFormula();

}
