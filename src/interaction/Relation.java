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
package src.interaction;

import java.util.List;
import java.util.Map;

import parsebionet.biodata.BioEntity;
import src.Bind;
import src.Constraint;

/**
 * 
 * Base class for all relations.
 * 
 * Relations can be composed of other relations (And, OR), or be the end of a
 * branch (Unique).
 * 
 * @author lmarmiesse 7 mars 2013
 * 
 */
public abstract class Relation {

	/**
	 * Used when interactions are in the solver.
	 * 
	 * @param b
	 *            The bind to get the data from.
	 * @return A solver constraint corresponding to the relation.
	 */
	public abstract Object runThrough(Bind b);

	public abstract String toString();

	protected boolean probabilityRelation = false;

	/**
	 * 
	 * @param simpleConstraints
	 *            Constraints to add before checking the relation.
	 * @return Whether or not the relation is true.
	 */
	public abstract boolean isTrue(Map<BioEntity, Constraint> simpleConstraints);

	/**
	 * Constraints corresponding to the relation.
	 * 
	 * <p>
	 * Used when interaction are not in the solver
	 * 
	 */
	List<Constraint> constraints;

	/**
	 * 
	 * Used when interaction are not in the solver
	 * 
	 * @return The constraints corresponding to the relation.
	 */
	public List<Constraint> createConstraints() {

		if (constraints == null) {
			makeConstraints();
		}
		return constraints;

	}

	/**
	 * Creates the constraints corresponding to the relation.
	 */
	protected abstract void makeConstraints();

	public boolean isProbabilityRelation() {
		return probabilityRelation;
	}

	public void setProbabilityRelation(boolean proba) {
		probabilityRelation = proba;
	}

	/**
	 * 
	 * @return A list of entities concerned by this relation.
	 */
	public abstract List<BioEntity> getInvolvedEntities();

}
