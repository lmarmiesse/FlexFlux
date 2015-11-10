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
 * 6 mars 2013 
 */
package flexflux.general;
import java.util.HashMap;
import java.util.Map;

import parsebionet.biodata.BioEntity;

/**
 * 
 * This class represents a constraints for the problem. A constraint represents
 * a linear equation. It is composed of entities, their coefficients, a lower
 * bound and an upper bound.
 * 
 * @author lmarmiesse 6 mars 2013
 * 
 */
public class Constraint {

	/**
	 * map containing entities and their coefficients.
	 */

	protected Map<BioEntity, Double> entities;
	/**
	 * Lower bound.
	 */
	protected Double lb;
	/**
	 * Upper bound.
	 */
	protected Double ub;

	/**
	 * If it is true, the map must not be equal to lb.
	 */
	protected boolean not = false;

	/**
	 * 
	 * Creates a constraint;
	 * 
	 * @param entities
	 *            Entities with their coefficients.
	 * @param lb
	 *            Lower bound.
	 * @param ub
	 *            Upper bound.
	 */
	public Constraint(Map<BioEntity, Double> entities, Double lb, Double ub) {

		if (lb <= ub) {
			this.entities = entities;
			this.lb = lb;
			this.ub = ub;
		} else {
			this.entities = entities;
			this.lb = lb;
			this.ub = lb;
		}

	}
	
	public Constraint(BioEntity ent, Double lb, Double ub) {

		Map<BioEntity, Double> entitiesMap  = new HashMap<BioEntity, Double>();
		entitiesMap.put(ent, 1.0);
		
		if (lb <= ub) {
			this.entities = entitiesMap;
			this.lb = lb;
			this.ub = ub;
		} else {
			this.entities = entitiesMap;
			this.lb = lb;
			this.ub = lb;
		}

	}

	public boolean getNot() {
		return not;
	}
//
//	public void setOverWritesBounds(boolean b) {
//		this.overWritesBounds = b;
//	}
//
//	public boolean getOverWritesBounds() {
//		return overWritesBounds;
//	}

	/**
	 * Creates an inequality constraint.
	 * 
	 * @param entities
	 *            Entities and their coefficients.
	 * @param lb
	 *            Lower and Upper bound.
	 * @param not
	 *            Determines if the contraint in an inequality.
	 */
	public Constraint(Map<BioEntity, Double> entities, Double lb, boolean not) {

		this.entities = entities;
		this.lb = lb;
		this.ub = lb;
		this.not = not;

	}

	/**
	 * 
	 * @return Entities and their coefficients.
	 */
	public Map<BioEntity, Double> getEntities() {
		return entities;

	}

	/**
	 * 
	 * @return Entities's names and their coefficients.
	 */
	public Map<String, Double> getEntityNames() {
		Map<String, Double> names = new HashMap<String, Double>();

		for (BioEntity ent : entities.keySet()) {

			names.put(ent.getId(), entities.get(ent));
		}

		return names;

	}

	public double getLb() {
		return lb;
	}

	public double getUb() {
		return ub;
	}

	public void setLb(double lb) {
		this.lb = lb;
	}

	public void setUb(double ub) {
		this.ub = ub;
	}

	public String toString() {

		String result = "";

		if (not) {
			result += "NOT : ";
		}

		result += lb + " <= ";

		for (BioEntity entity : entities.keySet()) {
			result += entities.get(entity) + " " + entity.getId() + " + ";
		}

		return result.subSequence(0, result.length() - 3) + " <= " + ub;

	}
	
}
