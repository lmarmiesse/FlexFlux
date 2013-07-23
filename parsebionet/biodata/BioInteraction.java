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
 * An entity that defines a single biochemical interaction between two or more
 * entities. An interaction cannot be defined without the entities it relates.
 * Since it is a highly abstract class in the ontology, instances of the
 * interaction class should be created rarely.
 */

public class BioInteraction extends BioEntity {
	
	private HashMap<String, BioPhysicalEntityParticipant> participantList = new HashMap<String, BioPhysicalEntityParticipant>();
	
	private HashMap<String, BioPathway> pathwayList = new HashMap<String, BioPathway>();
	
	
	public BioInteraction(String id) {
		super(id);
	}
	
	public BioInteraction(String id, String name) {
		super(id, name);
	}
	
	
	public BioInteraction() {
		super();
	}
	
	public BioInteraction(BioInteraction in) {
		super(in);
		this.copyParticipantList(in.getParticipantList());
		this.copyPathwayList(in.getPathwayList());
	}
	
	
	/**
	 * Get the participants list
	 * @return Returns the participantList.
	 */
	public HashMap<String, BioPhysicalEntityParticipant> getParticipantList() {
		return participantList;
	}
	
	public void setPathwayList(HashMap<String, BioPathway> pathways) {
		
		this.pathwayList = pathways;
		
	}
	
	/**
	 * Add a participant in the list
	 * @param a BioPhysicalEntityParticipant
	 */
	public void addParticipant(BioPhysicalEntityParticipant o) {
		this.participantList.put(o.getId(), o);
	}

	/**
	 * @param pathway : the pathway to add
	 */
	public void addPathway(BioPathway pathway) {
		pathwayList.put(pathway.getId(), pathway);
		pathway.getListOfInteractions().put(this.getId(), this);
	}

	/**
	 * @return Returns the pathwayList.
	 */
	public HashMap<String, BioPathway> getPathwayList() {
		return pathwayList;
	}


	/**
	 * @param pathwayList The pathwayList to set.
	 */
	public void copyPathwayList(HashMap<String, BioPathway> pathwayList) {
		
		this.pathwayList = new HashMap<String, BioPathway>();
		
		for(BioPathway p : pathwayList.values()) {
			BioPathway newBioPathway = new BioPathway(p);
			this.addPathway(newBioPathway);
		}
		
	}
	
	/**
	 * 
	 * @param participantList
	 */
	public void copyParticipantList(
			HashMap<String, BioPhysicalEntityParticipant> participantList) {
		
		this.setParticipantList(new HashMap<String, BioPhysicalEntityParticipant>());
		
		for(BioPhysicalEntityParticipant bpe : participantList.values()) {
			BioPhysicalEntityParticipant newBpe = new BioPhysicalEntityParticipant(bpe);
			this.addParticipant(newBpe);
		}
	}
	
	public void setParticipantList(
			HashMap<String, BioPhysicalEntityParticipant> participantList) {
		
		this.participantList = participantList;
		
	}
	
	
}
