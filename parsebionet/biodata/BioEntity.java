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
import java.util.HashSet;
import java.util.Set;


/**
 * @author ludo
 * The root class of ontology
 */

public class BioEntity {
	
	private String id;
	private String name;
	private String shortName;
	private ArrayList<String> synonyms = new ArrayList<String>();
	private String comment;
	/**
	 * Describes the availability of this data (e.g. a copyright statement).
	 */
	private String availability; 
	
	
	/**
	 * From BioCyc : for substrates whose names imply a braod specificity
	 * for a given enzymes. These nonsepcific substrates are often
	 * involved in multiple reactionNodes
	 */
	private Boolean isHolderClass; 	// From BioCyc : for substrates whose names imply a braod specificity
									// for a given enzymes. These nonsepcific substrates are often
									// involved in multiple reactionNodes
	
	/**
	 * Permet les parcours dans les donnees. Ex : backtrack
	 */
	private Boolean flag = false;
	


	private BioCompartment compartment;
	
	private String score = "NA";
	private String status = "NA";
	private HashSet<String> pmids;
	
	private Set<Comment> userComments = new HashSet<Comment>();
	
	
	public BioEntity(BioEntity in) {
		this.setAvailability(in.getAvailability());
		this.setId(in.getId());
		this.setName(in.getName());
		this.setShortName(in.getName());
		this.setSynonyms(new ArrayList<String>());
		this.getSynonyms().addAll(in.getSynonyms());
		this.setComment(in.getComment());
		this.setFlag(false);
		this.setIsHolderClass(in.getIsHolderClass());
		this.setCompartment(new BioCompartment(in.getCompartment()));
		
		this.setPmids(new HashSet<String>());
		
	}
	
	public BioEntity() {
		super();
		this.setIsHolderClass(false);
		this.setCompartment(new BioCompartment("NA", "NA"));
		this.setPmids(new HashSet<String>());
	}
	
	public BioEntity(String id, String name) {
		
		this.setId(id);
		this.setName(name);
		this.setIsHolderClass(false);
		this.setCompartment(new BioCompartment("NA", "NA"));
		this.setPmids(new HashSet<String>());
	}
	
	
	public BioEntity(String id) {
		this.setId(id);
		this.setName(id);
		this.setIsHolderClass(false);
		this.setCompartment(new BioCompartment("NA", "NA"));
		this.setPmids(new HashSet<String>());
	}
	


	/**
	 * Set the name of the entity
	 * @param n : String
	 */
	public void setName (String n) {
		this.name = n;
	}
	
	/**
	 * Get the name of the entity
	 * @return the name of the entity
	 */
	public String getName() {
		return this.name;
	}
	
	/**
	 * Get the short name of the entity
	 * @return the short name of the entity
	 */
	public String getShortName() {
		return this.shortName;
	}
	
	/**
	 * Set the short name of the entity
	 * @param n : String
	 */
	public void setShortName (String n) {
		this.shortName = n;
	}
	
	/**
	 * Get the list of the synonyms
	 * @return an ArrayList of the synonyms of the entity
	 */
	public ArrayList<String> getSynonyms() {
		return this.synonyms;
	}
	
	/**
	 * add a synonym in the list
	 * @param a String to add
	 */
	public void addSynonym(String s) {
		this.synonyms.add(s);
	}
	
	/**
	 * Set the comment on the entity
	 * @param c
	 */
	public void setComment (String c) {
		this.comment = c;
	}
	
	/**
	 * Get the comment on the entity
	 * @return the comment on the entity
	 */
	public String getComment() {
		return this.comment;
	}
	
	/**
	 * Set the availability of the entity
	 * @param a : A string
	 */
	public void setAvailability(String a) {
		this.availability = a;
	}
	
	/**
	 * Get the availability of the entity
	 * @return a string
	 */
	public String getAvailability() {
		return this.availability;
	}

	/**
	 * @return Returns the id.
	 */
	public String getId() {
		return id;
	}

	/**
	 * @param id The id to set.
	 */
	public void setId(String id) {
		this.id = id.replace("\"", "");
	}

	/**
	 * @return Returns the isHolderClass.
	 */
	public Boolean getIsHolderClass() {
		return isHolderClass;
	}

	/**
	 * @param isHolderClass The isHolderClass to set.
	 */
	public void setIsHolderClass(Boolean isHolderClass) {
		this.isHolderClass = isHolderClass;
	}

	public Boolean getFlag() {
		return flag;
	}

	public void setFlag(Boolean flag) {
		this.flag = flag;
	}

	public void setSynonyms(ArrayList<String> synonyms) {
		this.synonyms = synonyms;
	}

	public BioCompartment getCompartment() {
		return compartment;
	}

	public void setCompartment(BioCompartment compartment) {
		this.compartment = compartment;
	}
	
	/**
	 * @return the score
	 */
	public String getScore() {
		return score;
	}

	/**
	 * @param score the score to set
	 */
	public void setScore(String score) {
		this.score = score;
	}

	/**
	 * @return the status
	 */
	public String getStatus() {
		return status;
	}

	/**
	 * @param status the status to set
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * @param e
	 * @return
	 * @see java.util.Set#add(java.lang.Object)
	 */
	public boolean addComment(Comment e) {
		return userComments.add(e);
	}

	/**
	 * @return the userComments
	 */
	public Set<Comment> getUserComments() {
		return userComments;
	}

	/**
	 * @return the pmids
	 */
	public HashSet<String> getPmids() {
		return pmids;
	}

	/**
	 * @param pmids the pmids to set
	 */
	public void setPmids(HashSet<String> pmids) {
		this.pmids = pmids;
	}

	/**
	 * Adds a pmid
	 * @param pmid
	 */
	public void addPmid(String pmid) {
		this.pmids.add(pmid);
	}
	
	

}
