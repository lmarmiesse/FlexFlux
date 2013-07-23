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
 * 26 mars 2013 
 */
package parsebionet.utils.flexconflux.interaction;

import parsebionet.biodata.BioEntity;
import parsebionet.utils.flexconflux.Bind;
import parsebionet.utils.flexconflux.operation.OperationFactory;

/**
 * 
 * Class representing a Unique with a probability.
 * 
 * 
 * These relations must be transformed : there must be set to a percentage of
 * the upper bound of the Entity concerned.
 * 
 * @author lmarmiesse 26 mars 2013
 * 
 */
public class UniqueProbability extends Unique {

	private And transformation;

	public UniqueProbability(BioEntity entity, double probability) {
		super(entity);
		this.value = probability;
	}

	/**
	 * 
	 * Transforms the relation into an AND interaction with two constraints.
	 * 
	 * 
	 * @param min
	 *            Lower bound.
	 * @param max
	 *            Upper bound.
	 * @param factory
	 *            Relation factory.
	 * @param opFactory
	 *            Operation factory.
	 */
	public void transform(double min, double max, RelationFactory factory,
			OperationFactory opFactory) {

		transformation = factory.makeAnd();
		transformation.addRelation(factory.makeUnique(entity,
				opFactory.makeGe(), min * value));

		transformation.addRelation(factory.makeUnique(entity,
				opFactory.makeLe(), max * value));

	}

	public Object runThrough(Bind b) {

		return transformation.runThrough(b);

	}

	protected void makeConstraints() {

		constraints = transformation.createConstraints();

	}

	public And getTransformation() {
		return transformation;
	}

	public String toString() {
		String s = "";
		s += entity.getId();

		s += "*";

		s += value;
		return s;
	}

}
