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
package parsebionet.io;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Iterator;

import parsebionet.biodata.BioChemicalReaction;
import parsebionet.biodata.BioNetwork;
import parsebionet.biodata.BioPhysicalEntity;
import parsebionet.biodata.BioPhysicalEntityParticipant;




public class BioNetwork2File {
	private BioNetwork bioNetwork;
	private Boolean onlyPrimaries;
	private Boolean keepHolderClassCpd;
	private OutputStreamWriter writer;
	
	private HashMap<String, BioPhysicalEntity> listOfSpecies;
	private HashMap<String, BioChemicalReaction> listOfReactions;
	
	public BioNetwork2File(BioNetwork bn, Boolean onlyPrimaries, Boolean keepHolderClassCompounds, String fileName) {
		this.setBioNetwork(bn);
		this.setOnlyPrimaries(onlyPrimaries);
		this.setKeepHolderClassCpd(keepHolderClassCompounds);
		try {
			setWriter(new OutputStreamWriter(new FileOutputStream(fileName), "ascii"));
		} catch (IOException e) {
			System.err.println("Error while opening the file "+fileName);
			e.printStackTrace();
		}
				
		if(bn.getType().compareToIgnoreCase("cyc")==0)
			this.filterReactionsAndCpds();
		else {
			this.setListOfSpecies(bn.getPhysicalEntityList());
			this.setListOfReactions(bn.getBiochemicalReactionList());
		}
		
	}
	
	public BioNetwork2File(BioNetwork bn,  String fileName) {
		this.setBioNetwork(bn);
		this.setOnlyPrimaries(false);
		this.setKeepHolderClassCpd(true);
		try {
			setWriter(new OutputStreamWriter(new FileOutputStream(fileName), "ascii"));
		} catch (IOException e) {
			System.err.println("Erreur ouverture fichier");
			e.printStackTrace();
		}
				
		if(bn.getType().compareToIgnoreCase("cyc")==0)
			this.filterReactionsAndCpds();
		else {
			this.setListOfSpecies(bn.getPhysicalEntityList());
			this.setListOfReactions(bn.getBiochemicalReactionList());
		}
		
	}
	
	
	public void filterReactionsAndCpds() {
		
		this.setListOfReactions(new HashMap<String, BioChemicalReaction>());
		this.setListOfSpecies(new HashMap<String, BioPhysicalEntity>());
		
		HashMap<String, BioChemicalReaction> totalListOfReactions =  this.getBioNetwork().getBiochemicalReactionList();
		
		for(Iterator iterReaction = totalListOfReactions.keySet().iterator(); iterReaction.hasNext(); ) {
			
			HashMap<String, BioPhysicalEntityParticipant> left;
			HashMap<String, BioPhysicalEntityParticipant> right;
			
			BioChemicalReaction reaction = totalListOfReactions.get(iterReaction.next());
			
			if(reaction.testReaction(this.getOnlyPrimaries(), this.getKeepHolderClassCpd()) == true)  {
				
				this.addReaction(reaction);
				if(this.getOnlyPrimaries() == true) {
					left  = reaction.getPrimaryLeftParticipantList();
					right  = reaction.getPrimaryRightParticipantList();
				}
				else {
					left  = reaction.getLeftParticipantList();
					right  = reaction.getRightParticipantList();
				}
				
				for(Iterator iter = left.keySet().iterator(); iter.hasNext(); ) {
					BioPhysicalEntity cpd = left.get(iter.next()).getPhysicalEntity();
					if(! this.getListOfSpecies().containsKey(cpd.getId())) {
						this.addSpecie(cpd.getId(), cpd);
					}
				}
				
				for(Iterator iter = right.keySet().iterator(); iter.hasNext(); ) {
					BioPhysicalEntity cpd = right.get(iter.next()).getPhysicalEntity();
					if(! this.getListOfSpecies().containsKey(cpd.getId())) {
						this.addSpecie(cpd.getId(), cpd);
					}
				}
				
			}
		}
		
		
	}	
	
	public void setWriter(OutputStreamWriter fw) {
		this.writer = fw;
	}
	
	
	/**
	 * @return Returns the bioNetwork.
	 */
	public BioNetwork getBioNetwork() {
		return this.bioNetwork;
	}

	/**
	 * @param bioNetwork The bioNetwork to set.
	 */
	public void setBioNetwork(BioNetwork bioNetwork) {
		this.bioNetwork = bioNetwork;
	}

	/**
	 * @return Returns the keepHolderClass.
	 */
	public Boolean getKeepHolderClassCpd() {
		return this.keepHolderClassCpd;
	}

	/**
	 * @param keepHolderClass The keepHolderClass to set.
	 */
	public void setKeepHolderClassCpd(Boolean keepHolderClass) {
		this.keepHolderClassCpd = keepHolderClass;
	}

	/**
	 * @return Returns the onlyPrimaries.
	 */
	public Boolean getOnlyPrimaries() {
		return this.onlyPrimaries;
	}

	/**
	 * @param onlyPrimaries The onlyPrimaries to set.
	 */
	public void setOnlyPrimaries(Boolean onlyPrimaries) {
		this.onlyPrimaries = onlyPrimaries;
	}


	/**
	 * @param o
	 * @return
	 * @see java.util.ArrayList#add(java.lang.Object)
	 */
	public void addReaction(BioChemicalReaction o) {
		this.listOfReactions.put(o.getId(), o);
	}


	/**
	 * @param key
	 * @param value
	 * @return
	 * @see java.util.HashMap#put(java.lang.Object, java.lang.Object)
	 */
	public void addSpecie(String key, BioPhysicalEntity value) {
		this.listOfSpecies.put(key, value);
	}


	/**
	 * @return the listOfReactions
	 */
	public HashMap<String, BioChemicalReaction> getListOfReactions() {
		return this.listOfReactions;
	}


	/**
	 * @param listOfReactions the listOfReactions to set
	 */
	public void setListOfReactions(
			HashMap<String, BioChemicalReaction> listOfReactions) {
		this.listOfReactions = listOfReactions;
	}


	/**
	 * @return the listOfSpecies
	 */
	public HashMap<String, BioPhysicalEntity> getListOfSpecies() {
		return this.listOfSpecies;
	}


	/**
	 * @param listOfSpecies the listOfSpecies to set
	 */
	public void setListOfSpecies(HashMap<String, BioPhysicalEntity> listOfSpecies) {
		this.listOfSpecies = listOfSpecies;
	}


	/**
	 * @return the writer
	 */
	public OutputStreamWriter getWriter() {
		return this.writer;
	}
	

	
}
