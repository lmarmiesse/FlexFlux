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
 * 7 mai 2013 
 */
package parsebionet.utils.flexconflux.interaction;

import java.util.List;
import java.util.Map;

import parsebionet.biodata.BioEntity;
import parsebionet.utils.flexconflux.Bind;
import parsebionet.utils.flexconflux.Constraint;

/**
 * 
 * Class representing an inversed Relation.
 * 
 * @author lmarmiesse 7 mai 2013
 * 
 */
public class InversedRelation extends Relation {

	/**
	 * Relation that is the opposite.
	 */
	protected Relation rel;

	public InversedRelation(Relation rel) {
		this.rel = rel;
	}

	/**
	 * Return the opposite of the isTrue function of rel.
	 */
	public boolean isTrue(Map<BioEntity, Constraint> simpleConstraints) {
		return (!rel.isTrue(simpleConstraints));
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

	public String toString() {
		return "opposite of " + rel;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * parsebionet.utils.flexconflux.interaction.Relation#makeConstraints(boolean
	 * )
	 */
	@Override
	protected void makeConstraints() {
		// TODO Auto-generated method stub
	}

	public List<BioEntity> getInvolvedEntities() {
		return rel.getInvolvedEntities();
	}

}
