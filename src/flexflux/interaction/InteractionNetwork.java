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
			// System.err.println("Warning: two entites have the same name : "
			// + e.getId() + ", second one not added");
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
			// System.err.println("Warning: two entites have the same name : "
			// + e.getId() + ", second one not added");
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

//	/**
//	 * 
//	 * Find a steady state in the interaction network and return the constraints
//	 * corresponding to this steady state
//	 */
//
//	public List<Constraint> findSteadyState(
//			Map<BioEntity, Constraint> simpleConstraints) {
//
//		if (targetToInteractions.isEmpty() && initialConstraints.isEmpty()) {
//			return new ArrayList<Constraint>();
//		}
//
//		List<BioEntity> entitiesToCheck = new ArrayList<BioEntity>();
//		entitiesToCheck.addAll(targetToInteractions.keySet());
//
//		// copy simpleConstraints
//		Map<BioEntity, Constraint> thisStepSimpleConstraints = new HashMap<BioEntity, Constraint>();
//
//		for (BioEntity b : simpleConstraints.keySet()) {
//
//			thisStepSimpleConstraints.put(b, simpleConstraints.get(b));
//
//			// if the entity is already set by a constraint, we remove te
//			// interactions
//			// that have this entity as a target
//			if (simpleConstraints.get(b).getLb() == simpleConstraints.get(b)
//					.getUb()) {
//
//				if (targetToInteractions.containsKey(b)) {
////					for (Interaction i : targetToInteractions.get(b)
////							.getConditionalInteractions()) {
//						entitiesToCheck.remove(b);
////					}
//				}
//			}
//		}
//		//
//		for (BioEntity b : initialConstraints.keySet()) {
//
//			if (!thisStepSimpleConstraints.containsKey(b)) {
//				thisStepSimpleConstraints.put(b, initialConstraints.get(b));
//			}
//			// if this entity had a simple constraint, but not fix (ub!=lb) we
//			// overwrite it
//			else {
//				if (simpleConstraints.get(b).getLb() != simpleConstraints
//						.get(b).getUb()) {
//					thisStepSimpleConstraints.put(b, initialConstraints.get(b));
//				}
//			}
//		}
//
//		List<Map<BioEntity, Constraint>> allIterationsSimpleConstraints = new ArrayList<Map<BioEntity, Constraint>>();
//		List<Map<BioEntity, Constraint>> attractorSimpleConstraints = new ArrayList<Map<BioEntity, Constraint>>();
//
//		int attractorSize = 0;
//
//		// ////////////////////////////////////////WRITE TO FILE
//
//		Map<BioEntity, List<String>> toWrite = new HashMap<BioEntity, List<String>>();
//		if (Vars.writeInteractionNetworkStates) {
//
//			for (BioEntity ent : simpleConstraints.keySet()) {
//				toWrite.put(ent, new ArrayList<String>());
//			}
//
//			for (BioEntity ent : initialConstraints.keySet()) {
//
//				if (!toWrite.containsKey(ent)) {
//					toWrite.put(ent, new ArrayList<String>());
//
//				}
//			}
//
//			for (BioEntity ent : targetToInteractions.keySet()) {
//
//				if (!toWrite.containsKey(ent)) {
//					toWrite.put(ent, new ArrayList<String>());
//
//				}
//			}
//		}
//
//		// ////////////////////////////////////////
//
//		for (int it = 1; it < Vars.steadyStatesIterations; it++) {
//
//			// ////////////////////////////////////////WRITE TO FILE
//			if (Vars.writeInteractionNetworkStates) {
//				for (BioEntity ent : toWrite.keySet()) {
//
//					if (thisStepSimpleConstraints.get(ent) != null) {
//						double lb = thisStepSimpleConstraints.get(ent).getLb();
//						double ub = thisStepSimpleConstraints.get(ent).getUb();
//
//						if (lb == ub) {
//
//							toWrite.get(ent).add(String.valueOf(lb));
//						} else {
//
//							toWrite.get(ent).add(
//									String.valueOf(lb) + ";"
//											+ String.valueOf(ub));
//						}
//
//					} else {
//						toWrite.get(ent).add("?");
//					}
//				}
//
//			}
//
//			if (thisStepSimpleConstraints.size() == 0) {
//				System.err
//						.println("Warning : all values of the interaction network are undetermined.");
//				break;
//			}
//
//			// ////////////////////////////////////////
//
//			// /////We check that this step has not already been achieved
//
//			boolean areTheSame = false;
//			for (Map<BioEntity, Constraint> previousStep : allIterationsSimpleConstraints) {
//				areTheSame = true;
//				// compare "thisStepSimpleConstraints" with "previousStep"
//				// They have to be exactly the same
//
//				// the same size
//				if (thisStepSimpleConstraints.size() == previousStep.size()) {
//
//					for (BioEntity b : thisStepSimpleConstraints.keySet()) {
//						if (previousStep.containsKey(b)) {
//							Constraint c1 = thisStepSimpleConstraints.get(b);
//							Constraint c2 = previousStep.get(b);
//
//							if (c1.getLb() != c2.getLb()
//									|| c1.getUb() != c2.getUb()) {
//								areTheSame = false;
//							}
//						} else {
//							areTheSame = false;
//						}
//					}
//				} else {
//					areTheSame = false;
//				}
//
//				if (areTheSame) {
//					attractorSize = it
//							- allIterationsSimpleConstraints
//									.indexOf(previousStep) - 1;
//
//					for (int index = allIterationsSimpleConstraints
//							.indexOf(previousStep); index < it - 1; index++) {
//						attractorSimpleConstraints
//								.add(allIterationsSimpleConstraints.get(index));
//					}
//
//					break;
//				}
//
//			}
//
//			Set<BioEntity> setEntities = new HashSet<BioEntity>();
//			Set<BioEntity> checkedEntities = new HashSet<BioEntity>();
//
//			if (areTheSame) {
//				// System.err.println("Steady state found in " + (it - 1)
//				// + " iterations.");
//				// System.err.println("Attractor size : " + attractorSize);
//				break;
//			}
//
//			// /////
//			allIterationsSimpleConstraints.add(thisStepSimpleConstraints);
//
//			// we copy the previous state
//			Map<BioEntity, Constraint> nextStepSimpleConstraints = new HashMap<BioEntity, Constraint>();
//			for (BioEntity b : thisStepSimpleConstraints.keySet()) {
//				nextStepSimpleConstraints.put(b,
//						thisStepSimpleConstraints.get(b));
//			}
//
//			for (BioEntity entity : entitiesToCheck) {
//
//				for (Interaction i : targetToInteractions.get(entity)
//						.getConditionalInteractions()) {
//					if (i.getCondition().isTrue(thisStepSimpleConstraints)) {
//
//						// we go through all the consequences (there should be
//						// only
//						// one)
//						if (interactionToConstraints.containsKey(i)) {
//							for (Constraint consequence : this.interactionToConstraints
//									.get(i)) {
//
//								// we check it's a simple constraint
//								if (consequence.getEntities().size() == 1) {
//									for (BioEntity ent : consequence
//											.getEntities().keySet()) {
//
//										if (consequence.getEntities().get(ent) == 1.0) {
//
//											nextStepSimpleConstraints.put(ent,
//													consequence);
//											setEntities.add(ent);
//											checkedEntities.add(ent);
//
//										}
//									}
//								}
//							}
//						}
//						break;
//					}
//				}
//			}
//
//			// if it was not set, we put its default value
//			for (BioEntity ent : nextStepSimpleConstraints.keySet()) {
//
//				if (!setEntities.contains(ent)) {
//
//					if (entitiesToCheck.contains(ent)) {
//
//						Map<BioEntity, Double> constMap = new HashMap<BioEntity, Double>();
//						constMap.put(ent, 1.0);
//
//						nextStepSimpleConstraints.put(ent, targetToInteractions
//								.get(ent).getdefaultInteraction()
//								.getConsequence().createConstraints().get(0));
//						checkedEntities.add(ent);
//					}
//				}
//			}
//
//			thisStepSimpleConstraints = nextStepSimpleConstraints;
//
//		}
//
//		List<Constraint> steadyStateConstraints = new ArrayList<Constraint>();
//
//		if (attractorSimpleConstraints.size() != 0) {
//
//			for (BioEntity b : attractorSimpleConstraints.get(0).keySet()) {
//
//				// If it is an external metab, we set a constraint
//				boolean isExtMetab = false;
//
//				if (b.getClass().getSimpleName().equals("BioPhysicalEntity")) {
//					BioPhysicalEntity metab = (BioPhysicalEntity) b;
//					// If it is external
//					if (metab.getBoundaryCondition()) {
//						isExtMetab = true;
//					}
//				}
//
//				if (targetToInteractions.containsKey(b) || isExtMetab) {
//
//					// We make the average of the values of all states of the
//					// attractor
//					double lb = 0;
//					double ub = 0;
//					for (int nb = 0; nb < attractorSimpleConstraints.size(); nb++) {
//						lb += attractorSimpleConstraints.get(nb).get(b).getLb();
//						ub += attractorSimpleConstraints.get(nb).get(b).getUb();
//					}
//
//					lb = lb / attractorSimpleConstraints.size();
//					ub = ub / attractorSimpleConstraints.size();
//
//					Map<BioEntity, Double> constMap = new HashMap<BioEntity, Double>();
//					constMap.put(b, 1.0);
//					steadyStateConstraints
//							.add(new Constraint(constMap, lb, ub));
//				}
//
//				else {
//
//				}
//			}
//		}
//
//		if (Vars.writeInteractionNetworkStates) {
//
//			PrintWriter out = null;
//
//			try {
//				out = new PrintWriter(new File("files/states"));
//			} catch (FileNotFoundException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//
//			}
//
//			for (BioEntity ent : toWrite.keySet()) {
//
//				out.print(ent.getId());
//
//				for (String s : toWrite.get(ent)) {
//					out.print("\t");
//					out.print(s);
//				}
//				out.print("\n");
//
//			}
//
//			out.close();
//		}
//
//		return steadyStateConstraints;
//	}

}
