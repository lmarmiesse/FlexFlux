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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import parsebionet.biodata.BioEntity;
import flexflux.general.Constraint;

/**
 * 
 * Class containing all entities and all interactions of the problem.
 * 
 * @author lmarmiesse 6 mars 2013
 * 
 */
public class InteractionNetwork {

	/**
	 * List of entities that belong to the interaction network
	 */
	private Map<String, BioEntity> interactionNetworkEntities = new HashMap<String, BioEntity>();

	/**
	 * Minimum and maximum states of the entities that belong to the interaction
	 * network
	 */
	private Map<BioEntity, int[]> interactionNetworkEntitiesStates = new HashMap<BioEntity, int[]>();

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
	 * List of initial qualitative states of the entities of the interaction
	 * network.
	 */
	protected Map<BioEntity, Integer> initialStates = new HashMap<BioEntity, Integer>();

	/**
	 * Links an interactions to a list of constraints. To avoid doing it
	 * multiple times
	 */

	protected Map<Interaction, List<Constraint>> interactionToConstraints = new HashMap<Interaction, List<Constraint>>();

	private Map<BioEntity, FFTransition> targetToInteractions = new HashMap<BioEntity, FFTransition>();

	/**
	 * Map to get a constraint from an entity state and vice versa
	 */
	protected Map<BioEntity, Map<Integer, Constraint>> entityStateConstraintTranslation = new HashMap<BioEntity, Map<Integer, Constraint>>();

	public Map<BioEntity, Map<Integer, Constraint>> getEntityStateConstraintTranslation() {
		return entityStateConstraintTranslation;
	}

	public void addEntityStateConstraintTranslation(BioEntity ent,
			Integer state, Constraint c) {

		if (!entityStateConstraintTranslation.containsKey(ent)) {
			entityStateConstraintTranslation.put(ent,
					new HashMap<Integer, Constraint>());
		}

		entityStateConstraintTranslation.get(ent).put(state, c);

	}

	public Map<String, BioEntity> getInteractionNetworkEntities() {
		return interactionNetworkEntities;
	}

	public void addInteractionNetworkEntity(BioEntity ent) {
		interactionNetworkEntities.put(ent.getId(), ent);
	}

	public void setInteractionNetworkEntityState(BioEntity ent, int maxState) {

		interactionNetworkEntitiesStates.put(ent, new int[] { 0, maxState });

	}

	public void updateInteractionNetworkEntityState(BioEntity ent, int state) {

		if (state > interactionNetworkEntitiesStates.get(ent)[1]) {
			System.err.println("Error : value " + state + " for species "
					+ ent.getId() + " is greater than maximum state");
		}
		if (state < interactionNetworkEntitiesStates.get(ent)[0]) {
			interactionNetworkEntitiesStates.get(ent)[0] = state;
		}

	}

	public Map<BioEntity, int[]> getInteractionNetworkEntitiesStates() {
		return interactionNetworkEntitiesStates;
	}

	public void setInteractionNetworkEntities(Map<String, BioEntity> a) {
		interactionNetworkEntities = a;
	}

	public boolean canTranslate(BioEntity ent) {
		return entityStateConstraintTranslation.containsKey(ent);
	}

	public Map<BioEntity, Integer> getInitialStates() {
		return initialStates;
	}

	public void setInitialStates(Map<BioEntity, Integer> states) {
		initialStates = states;
	}

	public Constraint getConstraintFromState(BioEntity ent, Integer state) {

		// if ND
		if (entityStateConstraintTranslation.get(ent).get(state) == null) {
			return (new Constraint(ent, -Double.MAX_VALUE, Double.MAX_VALUE));
		}

		return entityStateConstraintTranslation.get(ent).get(state);

	}

	/**
	 * 
	 * Parse the translation table coming from SBML qual If a interval in the
	 * SBML qual corresponds to the given value, returns the corresponding
	 * qualitative state, -1 otherwise
	 * 
	 * 
	 * @param ent
	 * @param value
	 * @return
	 */
	public Integer getStateFromValue(BioEntity ent, double value) {

		int ndState = -1;

		for (Integer i : entityStateConstraintTranslation.get(ent).keySet()) {

			Constraint c = entityStateConstraintTranslation.get(ent).get(i);

			if (c == null) {
				ndState = i;
				continue;
			}

			if (value >= c.getLb() && value <= c.getUb()) {
				return i;
			}

		}

		if (ndState == -1) {
			System.err.println("Error : value " + value + " for variable "
					+ ent.getId()
					+ " does not correspond to any qualitative state.");
			System.exit(0);
		}

		return ndState;

	}

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

	public Integer getInitialState(BioEntity ent) {
		return initialStates.get(ent);
	}

	public Map<BioEntity, Constraint> getInitialConstraints() {
		return initialConstraints;
	}

	public void addInitialConstraint(BioEntity ent, Constraint constr) {
		initialConstraints.put(ent, constr);
	}

	public void addInitialState(BioEntity ent, Integer state) {
		initialStates.put(ent, state);
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
		initialStates.clear();
		interactionToConstraints.clear();
	}

	public void removeNumEntity(BioEntity entity) {
		numEntities.remove(entity.getId());
	}

	/**
	 * Copy this interaction network without duplicating all the entities
	 * 
	 * @return
	 */
	public InteractionNetwork copy() {
		InteractionNetwork newInteractionNetwork = new InteractionNetwork();

		newInteractionNetwork.binaryEntities = new HashMap<String, BioEntity>(this.getBinaryEntities());
		
		newInteractionNetwork.entityStateConstraintTranslation = new HashMap<BioEntity, Map<Integer, Constraint>>();
		for(BioEntity e : this.entityStateConstraintTranslation.keySet())
		{
			HashMap<Integer, Constraint> constraint = new HashMap<Integer, Constraint>(this.entityStateConstraintTranslation.get(e));
			newInteractionNetwork.entityStateConstraintTranslation.put(e, constraint);
		}
		
		newInteractionNetwork.GPRInteractions = new ArrayList<Interaction>(this.GPRInteractions);
		
		newInteractionNetwork.initialConstraints = new HashMap<BioEntity, Constraint>(this.initialConstraints);
		
		newInteractionNetwork.initialStates = new HashMap<BioEntity, Integer>(this.initialStates);
		
		newInteractionNetwork.intEntities = new HashMap<String, BioEntity>(this.intEntities);
		
		newInteractionNetwork.interactionNetworkEntities = new HashMap<String, BioEntity>(this.interactionNetworkEntities);
		
		newInteractionNetwork.interactionNetworkEntitiesStates = new HashMap<BioEntity, int[]>();
		for(BioEntity e : this.interactionNetworkEntitiesStates.keySet())
		{
			int newStates[] = this.interactionNetworkEntitiesStates.get(e).clone();
			newInteractionNetwork.interactionNetworkEntitiesStates.put(e, newStates);
		}
		
		newInteractionNetwork.interactionToConstraints = new HashMap<Interaction, List<Constraint>>();
		for(Interaction i : this.interactionToConstraints.keySet())
		{
			List<Constraint> constraints = new ArrayList<Constraint>(this.interactionToConstraints.get(i));
			newInteractionNetwork.interactionToConstraints.put(i, constraints);
		}
		
		newInteractionNetwork.numEntities = new HashMap<String, BioEntity> (this.numEntities);
		
		newInteractionNetwork.targetToInteractions = new HashMap<BioEntity, FFTransition>(this.targetToInteractions);
		
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

	public void setInitialConstraints(
			Map<BioEntity, Constraint> initialConstraints) {
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

	/**
	 * returns a string describing the network in a human readable format
	 */
	public String toString() {

		String str = "";

		for (BioEntity e : this.getEntities()) {

			str += "--------\n" + e.getId() + "\n";

			if (this.getInitialConstraints().containsKey(e)) {
				str += "Initial constraint : ["
						+ this.getInitialConstraint(e).getLb() + " , "
						+ this.getInitialConstraint(e).getUb() + "]\n";
				if (this.canTranslate(e)) {
					str += "Initial state : ["
							+ this.getStateFromValue(e, this
									.getInitialConstraint(e).getLb())
							+ " , "
							+ +this.getStateFromValue(e, this
									.getInitialConstraint(e).getUb()) + "]\n";
				}
			}

			if (this.getTargetToInteractions().containsKey(e)) {
				for (Interaction i : this.getTargetToInteractions().get(e)
						.getConditionalInteractions()) {
					str += i + "\n";
				}
				str += "ELSE : "
						+ this.getTargetToInteractions().get(e)
								.getdefaultInteraction().getConsequence()
						+ "\n";
			}

		}

		return str;

	}

}
