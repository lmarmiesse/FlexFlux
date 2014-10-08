package flexflux.interaction;

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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import parsebionet.biodata.BioEntity;
import parsebionet.biodata.BioPhysicalEntity;
import flexflux.general.Constraint;
import flexflux.general.Vars;
import flexflux.interaction.Interaction;
import flexflux.interaction.Unique;

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
	 * List of initial values of the entities of the interaction network.
	 */
	protected Map<BioEntity, Constraint> initialConstraints = new ConcurrentHashMap<BioEntity, Constraint>();

	/**
	 * Links an interactions to a list of constraints. To avoid doing it
	 * multiple times
	 */

	protected Map<Interaction, List<Constraint>> interactionToConstraints = new HashMap<Interaction, List<Constraint>>();

	private Map<BioEntity, FFTransition> targetToInteractions = new HashMap<BioEntity, FFTransition>();

	public Map<Interaction, List<Constraint>> getInteractionToConstraints() {
		return interactionToConstraints;
	}

	public void setTargetToInteractions(BioEntity ent, FFTransition transition) {
		targetToInteractions.put(ent, transition);
	}

	public void addInteractionToConstraints(Interaction inter) {
		interactionToConstraints.put(inter, inter.getConsequence()
				.createConstraints());
	}

	public Constraint getInitialConstraint(BioEntity ent) {
		return initialConstraints.get(ent);
	}

	public Map<BioEntity, Constraint> getInitialConstraints() {
		return initialConstraints;
	}

	public void addInitialConstraint(BioEntity ent, Constraint constr) {
			initialConstraints.put(ent, constr);
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

	public void addTargetConditionalInteraction(BioEntity target,
			Interaction inter) {

		if (targetToInteractions.get(target) == null) {
			targetToInteractions.put(target, new FFTransition());
		}
		targetToInteractions.get(target).addConditionalInteraction(inter);

		interactionToConstraints.put(inter, inter.getConsequence()
				.createConstraints());

	}

	public void setTargetDefaultInteraction(BioEntity target,
			Interaction defaultInt) {

		if (targetToInteractions.get(target) == null) {
			targetToInteractions.put(target, new FFTransition());
		}
		targetToInteractions.get(target).setdefaultInteraction(defaultInt);
	}

	public Map<BioEntity, FFTransition> getTargetToInteractions() {
		return targetToInteractions;
	}

	public void addGPRIntercation(Interaction i) {
		GPRInteractions.add(i);
		interactionToConstraints.put(i, i.getConsequence().createConstraints());
	}

	public void addNumEntity(BioEntity e) {

		if (numEntities.containsKey(e.getId())
				|| intEntities.containsKey(e.getId())
				|| binaryEntities.containsKey(e.getId())) {

			// System.err.println("Warning: two entites have the same name : "
			// + e.getId() + ", second one not added");

			return;
		}
		numEntities.put(e.getId(), e);
	}

	public void addIntEntity(BioEntity e) {
		if (numEntities.containsKey(e.getId())
				|| intEntities.containsKey(e.getId())
				|| binaryEntities.containsKey(e.getId())) {
			// System.err.println("Warning: two entites have the same name : "
			// + e.getId() + ", second one not added");
			return;
		}
		intEntities.put(e.getId(), e);
	}

	public void addBinaryEntity(BioEntity e) {
		if (numEntities.containsKey(e.getId())
				|| intEntities.containsKey(e.getId())
				|| binaryEntities.containsKey(e.getId())) {
			// System.err.println("Warning: two entites have the same name : "
			// + e.getId() + ", second one not added");
			return;
		}
		binaryEntities.put(e.getId(), e);
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

	/**
	 * Clears all lists.
	 */
	public void clear() {
		numEntities.clear();
		intEntities.clear();
		binaryEntities.clear();
		GPRInteractions.clear();
		initialConstraints.clear();
		interactionToConstraints.clear();
	}

	public void removeNumEntity(BioEntity entity) {
		numEntities.remove(entity.getId());
	}
	
	/**
	 * Copy this interaction network without duplicating all the entities
	 * @return
	 */
	public InteractionNetwork copy() {
		InteractionNetwork newInteractionNetwork = new InteractionNetwork();
		
		newInteractionNetwork.setBinaryEntities(this.getBinaryEntities());
		newInteractionNetwork.setGPRInteractions(this.getGPRInteractions());
		newInteractionNetwork.setInitialConstraints(this.getInitialConstraints());
		newInteractionNetwork.setIntEntities(this.getIntEntities());
		newInteractionNetwork.setInteractionToConstraints(this.getInteractionToConstraints());
		newInteractionNetwork.setNumEntities(this.getNumEntities());
		newInteractionNetwork.setTargetToInteractions(this.getTargetToInteractions());
		
		return newInteractionNetwork;
	}

	public void setNumEntities(Map<String, BioEntity> numEntities) {
		this.numEntities = numEntities;
	}

	public void setIntEntities(Map<String, BioEntity> intEntities) {
		this.intEntities = intEntities;
	}

	public void setBinaryEntities(Map<String, BioEntity> binaryEntities) {
		this.binaryEntities = binaryEntities;
	}

	public void setGPRInteractions(List<Interaction> gPRInteractions) {
		GPRInteractions = gPRInteractions;
	}

	public void setInitialConstraints(Map<BioEntity, Constraint> initialConstraints) {
		this.initialConstraints = initialConstraints;
	}

	public void setInteractionToConstraints(
			Map<Interaction, List<Constraint>> interactionToConstraints) {
		this.interactionToConstraints = interactionToConstraints;
	}

	public void setTargetToInteractions(
			Map<BioEntity, FFTransition> targetToInteractions) {
		this.targetToInteractions = targetToInteractions;
	}

	public Map<String, BioEntity> getBinaryEntities() {
		return binaryEntities;
	}

	public Map<String, BioEntity> getIntEntities() {
		return intEntities;
	}

	public Map<String, BioEntity> getNumEntities() {
		return numEntities;
	}
	
	
	

}
