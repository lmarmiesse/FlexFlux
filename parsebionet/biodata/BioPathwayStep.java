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
import java.util.List;

import org.jdom.Attribute;
import org.jdom.Element;

import parsebionet.sandbox.rdf.RdfConstants;
import parsebionet.sandbox.rdf.RdfQuery;



public class BioPathwayStep extends BioUtility {

//	private BioPathwayStep nextStep;
	private HashMap<String, BioInteraction> stepInteractions = new HashMap<String, BioInteraction>();
	
	
	public BioPathwayStep (BioInteraction bioInteraction) {
		super(bioInteraction.getId(), bioInteraction.getName());
		
		this.addStepInteraction(bioInteraction);
		
	}
	
	public BioPathwayStep(BioPathwayStep in) {
		super(in);
//		if(in.getNextStep() != null)
//			this.setNextStep(new BioPathwayStep(in.getNextStep()));
		this.copyStepInteractions(in.getStepInteractions());
	}
	
	/**
	 * @return Returns the nextStep.
	 */
//	public BioPathwayStep getNextStep() {
//		return nextStep;
//	}

	/**
	 * @param nextStep The nextStep to set.
	 */
//	public void setNextStep(BioPathwayStep ns) {
//		this.nextStep = ns;
//	}

	/**
	 * @return Returns the stepInteractions.
	 */
	public HashMap<String, BioInteraction> getStepInteractions() {
		return stepInteractions;
	}

	/**
	 * @param stepInteractions The stepInteractions to add
	 */
	public void addStepInteraction(BioInteraction o) {
		this.stepInteractions.put(o.getId(), o);
	}
	
	public void copyStepInteractions(HashMap<String, BioInteraction> stepInteractions) {
		
		this.stepInteractions = new HashMap<String, BioInteraction>();
		
		for(BioInteraction interaction : stepInteractions.values()) {
			BioInteraction newInteraction = new BioInteraction(interaction);
			this.addStepInteraction(newInteraction);
		}
	}
	
	

}
