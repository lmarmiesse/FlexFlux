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
package interaction;

import general.Bind;
import general.Constraint;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import operation.Operation;
import parsebionet.biodata.BioEntity;

/**
 * 
 * This class represents a Unique relation.
 * 
 * 
 * <p>
 * This type of relation doesn't contain any other relation and is the end of a
 * branch.
 * </p>
 * 
 * 
 * <p>
 * It contains an entity, a value and an operation.
 * </p>
 * 
 * @author lmarmiesse 7 mars 2013
 * 
 */
public class Unique extends Relation {

	/**
	 * Entity concerned by the relation.
	 */
	protected BioEntity entity;

	/**
	 * Value of the relation.
	 */
	protected double value;
	// probability is used to set the max value to the entity to it's upper
	// bound / probability

	/**
	 * Operation of the relation.
	 */
	protected Operation operation;

	public Unique(BioEntity entity, Operation op, double value) {
		this.operation = op;
		this.entity = entity;
		this.value = value;
	}

	/**
	 * By default, operation is Greater or equal and the value is 0.
	 * 
	 * @param entity
	 *            Entity concerned by this relation.
	 */
	public Unique(BioEntity entity) {
		this.entity = entity;
		this.value = 0.0;
	}

	public BioEntity getEntity() {
		return entity;
	}

	public String toString() {
		String s = "";
		s += entity.getId();

		s += operation;

		s += value;
		return s;
	}

	public boolean isTrue(Map<BioEntity, Constraint> simpleConstraints) {

		if (!simpleConstraints.containsKey(entity)) {

			// System.err.println("unknown value for "+entity.getId()+", interaction ignored");
			return false;

		} else {

			Constraint cons = simpleConstraints.get(entity);

			return operation.isTrue(cons, value);
		}

	}

	protected void makeConstraints() {

		constraints = operation.makeConstraint(entity, value);

	}

	public List<BioEntity> getInvolvedEntities() {

		List<BioEntity> entities = new ArrayList<BioEntity>();
		entities.add(entity);
		return entities;
	}

	/**
	 * Only used when interaction are in the solver.
	 * 
	 * <p>
	 * in this case, this class must be extended.
	 * 
	 */
	public Object runThrough(Bind b) {
		// TODO Auto-generated method stub
		return null;
	}

}
