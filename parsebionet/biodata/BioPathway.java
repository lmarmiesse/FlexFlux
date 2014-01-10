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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import baobab.hypercyc.connection.JavacycPlus;


/**
 * A set or series of interactions, often forming a network, which biologists
 * have found useful to group together for organizational, historic, biophysical
 * or other reasons.
 */

public class BioPathway extends BioEntity {

//	private HashMap<String, BioPathwayStep> pathwayComponents = new HashMap<String, BioPathwayStep>();
	
	private HashMap<String, BioPathway> superPathways = new HashMap<String, BioPathway> ();
	private HashMap<String, BioPathway>  subPathways = new HashMap<String, BioPathway> ();
	
	private ArrayList<String> primaryCompounds = new ArrayList<String>();
	
	private HashMap<String, BioReactionLayout> reactionLayouts = new HashMap<String, BioReactionLayout>();
	
	private HashMap<String, BioInteraction> listOfInteractions = new HashMap<String, BioInteraction>();
	
	private HashMap<String, BioChemicalReaction> reactions = new HashMap<String, BioChemicalReaction>();
	
	public BioPathway(String id) {
		super(id);
	}
	
	public BioPathway(String id, String name) {
		super(id, name);
	}
	
	public BioPathway(BioPathway in) {
		super(in);
//		this.copyPathwayComponents(in.getPathwayComponents());
		this.copySuperPathways(in.getSuperPathways());
		this.copySubPathways(in.getSubPathways());
		this.setPrimaryCompounds(new ArrayList<String>());
		this.getPrimaryCompounds().addAll(in.getPrimaryCompounds());
		this.copyReactionLayouts(in.getReactionLayouts());
	}
	
	/**
	 * @return Returns the pathwayComponents.
	 */
//	public HashMap<String, BioPathwayStep> getPathwayComponents() {
//		return pathwayComponents;
//	}
	
	/**
	 * @param the pathway step to add
	 */
//	public void addPathwayComponent(BioPathwayStep o) {
//		this.pathwayComponents.put(o.getId(), o);
//	}
	
	/**
	 * @param the interaction to add
	 */
//	public void addPathwayComponent(BioInteraction o) {
//		BioPathwayStep pathwayStep = new BioPathwayStep(o);
//		this.addPathwayComponent(pathwayStep);
//	}
	
	/**
	 * Returns the list of all interactions present in all pathwayStep
	 */
//	public HashMap<String, BioInteraction> getListOfInteractions () {
//		
//		HashMap<String, BioInteraction> totalListOfInteractions = new HashMap<String, BioInteraction>();
//		
//		for(BioPathwayStep step : this.getPathwayComponents().values()) {
//			HashMap<String, BioInteraction> listOfInteractions = step.getStepInteractions();
//			totalListOfInteractions.putAll(listOfInteractions);
//		}
//		
//		return totalListOfInteractions;
//		
//	}
	

	/**
	 * @return Returns the subPathways.
	 */
	public HashMap<String, BioPathway> getSubPathways() {
		return subPathways;
	}

	/**
	 * @param subPathways The subPathways to set.
	 */
	public void setSubPathways(HashMap<String, BioPathway>  subPathways) {
		this.subPathways = subPathways;
	}
	
	public void copySubPathways(HashMap<String, BioPathway> subPathways) {
		
		this.setSubPathways(new HashMap<String, BioPathway>());
		
		for(BioPathway subPathway : subPathways.values()) {
			BioPathway newSubPathway = new BioPathway(subPathway);
			this.addSubPathway(newSubPathway);
		}
		
	}
	
	
	/**
	 * Adds a sub Pathway in the list
	 */
	public void addSubPathway(BioPathway pathway) {
		this.subPathways.put(pathway.getId(), pathway);
	}

	/**
	 * @return Returns the superPathways.
	 */
	public HashMap<String, BioPathway> getSuperPathways() {
		return superPathways;
	}

	/**
	 * @param superPathways The superPathways to set.
	 */
	public void setSuperPathways(HashMap<String, BioPathway>  superPathways) {
		this.superPathways = superPathways;
	}
	
	public void copySuperPathways(HashMap<String, BioPathway> superPathways) {
		
		this.setSuperPathways(new HashMap<String, BioPathway>());
		
		for(BioPathway superPathway : superPathways.values()) {
			BioPathway newSuperPathway = new BioPathway(superPathway);
			this.addSuperPathway(newSuperPathway);
		}
		
	}
	
	
	/**
	 * Adds a super Pathway in the list
	 */
	public void addSuperPathway(BioPathway pathway) {
		this.superPathways.put(pathway.getId(), pathway);
	}

	/**
	 * @return Returns the primaryCompounds.
	 */
	public ArrayList<String> getPrimaryCompounds() {
		return primaryCompounds;
	}

	/**
	 * @param primaryCompounds The primaryCompounds to set.
	 */
	public void setPrimaryCompounds(ArrayList<String> primaryCompounds) {
		this.primaryCompounds = primaryCompounds;
	}
	
	/**
	 * 
	 */
	public void addPrimaryCompound(String primaryCompound) {
		if(! primaryCompounds.contains(primaryCompound)) {
			primaryCompounds.add(primaryCompound);
		}
	}
	
	/**
	 * Gets the primary compounds of a pathway
	 */
	
	public void setPrimaryCompounds(JavacycPlus cyc) {
		
		ArrayList reactionLayouts = cyc.getSlotValues(this.getId(), "REACTION-LAYOUT");
		
		Collection superPathwayList = this.getSuperPathways().values();
		
		for(int i=0; i < reactionLayouts.size(); i++) {
//			 ex : (PGLUCISOM-RXN( :LEFT-PRIMARIES GLC-6-P)( :DIRECTION :L2R)( :RIGHT-PRIMARIES FRUCTOSE-6P))
			
			String reactionLayout = (String)reactionLayouts.get(i);
			
			String REGEX = "LEFT-PRIMARIES\\s([^)]*)\\)";
			
			Pattern pattern = Pattern.compile(REGEX);
	        Matcher matcher = pattern.matcher(reactionLayout);
	        
	        while(matcher.find()) {
	        	String compounds = matcher.group(1);
	        	
	        	String[] tab = compounds.split("\\s");
	        	for (int j=0; j < tab.length; j++) {
	        		String cpdId = tab[j];
	        		
	        		if(! primaryCompounds.contains(cpdId)) {
	        			this.addPrimaryCompound(cpdId);
	        			Iterator k = superPathwayList.iterator(); 
	        			
	        			while (k.hasNext()){
	        				BioPathway superPathway = (BioPathway) k.next();
	        				superPathway.addPrimaryCompound(cpdId);
	        			}
	        		}
	        	}
	        }
	        
	        REGEX = "RIGHT-PRIMARIES\\s([^)]*)\\)";
	        pattern = Pattern.compile(REGEX);
	        matcher = pattern.matcher(reactionLayout);
	        
	        while(matcher.find()) {
	        	String compounds = matcher.group(1);
	        	String[] tab = compounds.split("\\s");
	        	for (int j=0; j < tab.length; j++) {
	        		String cpdId = tab[j];
	        		
	        		if(! primaryCompounds.contains(cpdId)) {
	        			this.addPrimaryCompound(cpdId);
	        			Iterator k = superPathwayList.iterator(); 
	        			
	        			while (k.hasNext()){
	        				BioPathway superPathway = (BioPathway) k.next();
	        				superPathway.addPrimaryCompound(cpdId);
	        			}
	        		}
	        		
	        	}
	        }
		}
	}

	public void setReactionLayouts(JavacycPlus cyc) {
		
		ArrayList reactionLayouts = cyc.getSlotValues(this.getId(), "REACTION-LAYOUT");
		
		for(int i=0; i < reactionLayouts.size(); i++) {
			String reactionLayoutString = (String)reactionLayouts.get(i);
			
			BioReactionLayout layout = new BioReactionLayout(reactionLayoutString, cyc);
			
			if(layout.getReactionId() != null) {
				this.addReactionLayout(layout);
			}
		}
		
	}

	/**
	 * @return Returns the reactionLayouts.
	 */
	public HashMap<String, BioReactionLayout> getReactionLayouts() {
		return reactionLayouts;
	}
	
	public void addReactionLayout(BioReactionLayout x) {
		reactionLayouts.put(x.getReactionId(), x);
		
		Collection superPathways = this.getSuperPathways().values();
		Iterator k = superPathways.iterator();
		while (k.hasNext()){
			BioPathway superPathway = (BioPathway) k.next();
			superPathway.addReactionLayout(x);
		}
		
	}
	
	public void copyReactionLayouts(HashMap<String, BioReactionLayout> layouts) {
		
		this.reactionLayouts = new HashMap<String, BioReactionLayout>();
		
		for(BioReactionLayout l : layouts.values()){
			BioReactionLayout newL = new BioReactionLayout(l);
			this.addReactionLayout(newL);
		}
	}

	public HashMap<String, BioInteraction> getListOfInteractions() {
		return listOfInteractions;
	}

	public void setListOfInteractions(
			HashMap<String, BioInteraction> listOfInteractions) {
		this.listOfInteractions = listOfInteractions;
	}

	public HashMap<String, BioChemicalReaction> getReactions() {
		return reactions;
	}

	public void setReactions(HashMap<String, BioChemicalReaction> reactions) {
		this.reactions = reactions;
	}
	
	public void addReaction(BioChemicalReaction reaction) {
		
		this.reactions.put(reaction.getId(), reaction);
		
	}

//	public void copyPathwayComponents(
//			HashMap<String, BioPathwayStep> pathwayComponents) {
//		
//		this.pathwayComponents = new HashMap<String, BioPathwayStep>();
//		
//		for(BioPathwayStep bps : pathwayComponents.values()) {
//			BioPathwayStep newBps = new BioPathwayStep(bps);
//			this.addPathwayComponent(newBps);
//		}
//	}
	
	


}
