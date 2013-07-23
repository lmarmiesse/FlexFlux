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
package parsebionet.biodata;

import java.util.HashMap;


/**
 * A control interaction in which a physical entity (a catalyst) increases the
 * rate of a conversion interaction by lowering its activation energy. Instances
 * of this class describe a pairing between a catalyzing entity and a catalyzed
 * conversion. A separate catalysis instance should be created for each
 * different conversion that a physical entity may catalyze and for each
 * different physical entity that may catalyze a conversion. For example, a
 * bifunctional enzyme that catalyzes two different biochemical reactionNodes would
 * be linked to each of those biochemical reactionNodes by two separate instances of
 * the catalysis class. Typically, each step in a metabolic pathway is either an
 * instance of the catalysis class or a spontaneous conversion, which occurs
 * under biological conditions without the aid of a catalyzing entity. Synonyms
 * for this class include 'facilitation' and 'acceleration'. Examples of this
 * class include the catalysis of a biochemical reaction by an enzyme, the
 * enabling of a transport interaction by a membrane pore complex, and the
 * facilitation of a complex assembly by a scaffold protein.
 */

public class BioCatalysis extends BioControl {
	
	private HashMap<String, BioPhysicalEntityParticipant> cofactorList = new HashMap<String, BioPhysicalEntityParticipant>();
	private String direction = null;
	// Specifies the reaction direction of the interaction catalyzed 
	// by this instance of the catalysis class. Possible values of 
	// this property are: REVERSIBLE: Interaction occurs in both directions in physiological settings. 
	// PHYSIOL-LEFT-TO-RIGHT PHYSIOL-RIGHT-TO-LEFT The interaction occurs in the specified 
	// direction in physiological settings, because of several possible factors including the energetics 
	// of the reaction, local concentrations of reactants and products, and the regulation of the enzyme or its expression. 
	// IRREVERSIBLE-LEFT-TO-RIGHT IRREVERSIBLE-RIGHT-TO-LEFT For all practical purposes, 
	// the interactions occurs only in the specified direction in physiological settings, 
	// because of chemical properties of the reaction. (This definition from EcoCyc)
	
	private HashMap<String, BioChemicalReaction> reactionList = new HashMap<String, BioChemicalReaction>();
	
	public BioCatalysis(String id) {
		super(id);
	}
	
	public BioCatalysis(BioCatalysis in) {
		super(in);
		this.copyCofactorList(in.getCofactorList());
		this.setDirection(in.getDirection());
	}
	
	
	/**
	 * @return Returns the cofactor.
	 */
	public HashMap<String, BioPhysicalEntityParticipant> getCofactorList() {
		return cofactorList;
	}
	
	/**
	 * Add a cofactor in the list
	 * @param o the object to add
	 */
	public void addCofactor(BioPhysicalEntityParticipant o) {
		this.cofactorList.put(o.getId(), o);
	}
	
	public void setCofactorList(HashMap<String, BioPhysicalEntityParticipant> list) {
		this.cofactorList = list;
	}
	
	
	public void copyCofactorList(HashMap<String, BioPhysicalEntityParticipant> list) {
		
		this.setCofactorList(new HashMap<String, BioPhysicalEntityParticipant>());
		
		for(BioPhysicalEntityParticipant cof : list.values()) {
			BioPhysicalEntityParticipant newCof = new BioPhysicalEntityParticipant(cof);
			this.addCofactor(newCof);
		}
		
	}
	
	/**
	 * @return Returns the direction.
	 */
	public String getDirection() {
		return direction;
	}
	/**
	 * @param direction The direction to set.
	 */
	public void setDirection(String direction) {
		this.direction = direction;
	}
	
	
	/**
	 * @return the list of the reactionNodes catalysed by this instance
	 */
	public HashMap getReactionList() {
		return reactionList;
	}
	
	
	/**
	 * @param reactionList
	 */
	public void setReactionList(HashMap<String, BioChemicalReaction> reactionList) {
		this.reactionList = reactionList;
	}
	
	/**
	 * @param BioChemicalReaction reaction
	 */
	public void addReaction(BioChemicalReaction reaction) {
			reactionList.put(reaction.getId(), reaction);
	}

	

}
