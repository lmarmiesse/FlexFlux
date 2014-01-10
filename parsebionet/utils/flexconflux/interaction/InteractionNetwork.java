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
package parsebionet.utils.flexconflux.interaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import parsebionet.biodata.BioEntity;

/**
 * 
 * Class containing all entities and all interactions of the problem.
 * 
 * @author lmarmiesse 6 mars 2013
 * 
 */
public class InteractionNetwork {

	/**
	 * List of entities with real values.
	 */
	private Map<String, BioEntity> numEntities = new HashMap<String, BioEntity>();
	/**
	 * List of entities with interger values.
	 */
	private Map<String, BioEntity> intEntities = new HashMap<String, BioEntity>();
	/**
	 * List of entities with binary values.
	 */
	private Map<String, BioEntity> binaryEntities = new HashMap<String, BioEntity>();

	/**
	 * List of GPR interactions.
	 */
	private List<Interaction> GPRInteractions = new ArrayList<Interaction>();

	/**
	 * List of interation added in the interaction file.
	 */
	private List<Interaction> addedInteractions = new ArrayList<Interaction>();

	private List<Unique> notTransformedProbaUnique = new ArrayList<Unique>();

	// map containing the interactions to check if the bioentity is set to 0
	protected Map<BioEntity, List<Interaction>> interactionsToCheck = new HashMap<BioEntity, List<Interaction>>();

	public Map<BioEntity, List<Interaction>> getInteractionToCheck() {
		return interactionsToCheck;
	}

	public List<BioEntity> getEntities() {

		List<BioEntity> entities = new ArrayList<BioEntity>();

		for (String s : numEntities.keySet()) {
			entities.add(numEntities.get(s));
		}
		for (String s : intEntities.keySet()) {
			entities.add(intEntities.get(s));
		}
		for (String s : binaryEntities.keySet()) {
			entities.add(binaryEntities.get(s));
		}

		return entities;
	}

	public void addInteractionToCheck(Interaction i) {

		List<BioEntity> entities = i.getCondition().getInvolvedEntities();

		for (BioEntity ent : entities) {
			if (interactionsToCheck.containsKey(ent)) {
				interactionsToCheck.get(ent).add(i);
			} else {
				List<Interaction> toAdd = new ArrayList<Interaction>();
				toAdd.add(i);
				interactionsToCheck.put(ent, toAdd);
			}

		}

	}

	public void addGPRIntercation(Interaction i) {
		addInteractionToCheck(i);
		GPRInteractions.add(i);
	}

	public void addAddedIntercation(Interaction i) {
		addInteractionToCheck(i);
		addedInteractions.add(i);
	}

	public void addNumEntity(BioEntity e) {
		if (numEntities.containsKey(e.getId())
				|| intEntities.containsKey(e.getId())
				|| binaryEntities.containsKey(e.getId())) {
			System.err.println("Warning: two entites have the same name : "
					+ e.getId() + ", second one not added");

			return;
		}
		numEntities.put(e.getId(), e);
	}

	public Set<BioEntity> getNumEntities() {

		Set<BioEntity> ents = new HashSet<BioEntity>();
		for (String s : numEntities.keySet()) {
			ents.add(numEntities.get(s));
		}
		return ents;
	}

	public void addIntEntity(BioEntity e) {
		if (numEntities.containsKey(e.getId())
				|| intEntities.containsKey(e.getId())
				|| binaryEntities.containsKey(e.getId())) {
			System.err.println("Warning: two entites have the same name : "
					+ e.getId() + ", second one not added");
			return;
		}
		intEntities.put(e.getId(), e);
	}

	public Set<BioEntity> getIntEntities() {

		Set<BioEntity> ents = new HashSet<BioEntity>();
		for (String s : intEntities.keySet()) {
			ents.add(intEntities.get(s));
		}
		return ents;
	}

	public void addBinaryEntity(BioEntity e) {
		if (numEntities.containsKey(e.getId())
				|| intEntities.containsKey(e.getId())
				|| binaryEntities.containsKey(e.getId())) {
			System.err.println("Warning: two entites have the same name : "
					+ e.getId() + ", second one not added");
			return;
		}
		binaryEntities.put(e.getId(), e);
	}

	public Set<BioEntity> getBinaryEntities() {

		Set<BioEntity> ents = new HashSet<BioEntity>();
		for (String s : binaryEntities.keySet()) {
			ents.add(binaryEntities.get(s));
		}
		return ents;
	}

	public BioEntity getEntity(String name) {

		if (binaryEntities.containsKey(name)) {
			return binaryEntities.get(name);
		} else if (numEntities.containsKey(name)) {
			return numEntities.get(name);
		} else if (intEntities.containsKey(name)) {
			return intEntities.get(name);
		}
		return null;
	}

	public List<Interaction> getGPRInteractions() {
		return GPRInteractions;
	}

	public List<Interaction> getAddedInteractions() {
		return addedInteractions;
	}

	public void addProbaUnique(Unique unique) {
		notTransformedProbaUnique.add(unique);
	}

	public List<Unique> getNotTransformedUniques() {
		return notTransformedProbaUnique;
	}

	public Unique removeNotTransformedUnique() {
		return notTransformedProbaUnique.remove(0);
	}

	/**
	 * Clears all lists.
	 */
	public void clear() {
		numEntities.clear();
		intEntities.clear();
		binaryEntities.clear();
		GPRInteractions.clear();
		addedInteractions.clear();
		interactionsToCheck.clear();
	}

	/**
	 * 
	 * @return True if there are not transformed unique probabilities.
	 */
	public boolean isNotTransformedUnique() {
		return notTransformedProbaUnique.size() > 0;
	}

	public void removeNumEntity(BioEntity entity) {
		numEntities.remove(entity.getId());
	}

}