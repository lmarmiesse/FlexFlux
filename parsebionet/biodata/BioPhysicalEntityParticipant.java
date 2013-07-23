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
// Any additional special characteristics of a physical entity in the context of an interaction. 
// These currently include stoichiometric coefficient and cellular location, but 
// this list may be expanded in later levels.

package parsebionet.biodata;

public class BioPhysicalEntityParticipant extends BioUtility {

	private BioPhysicalEntity physicalEntity;
	private String stoichiometricCoefficient;
	private BioCompartment location;
	
	private Boolean isPrimaryCompound = true;
	private Boolean isCofactor = false;
	
	
	public Boolean getIsCofactor() {
		return isCofactor;
	}

	public void setIsCofactor(Boolean isCofactor) {
		this.isCofactor = isCofactor;
	}

	public BioPhysicalEntityParticipant(String id, BioPhysicalEntity physicalEntity, String stoichiometricCoefficient, BioCompartment location) {
		super(id);
		this.setPhysicalEntity(physicalEntity);
		this.setStoichiometricCoefficient(stoichiometricCoefficient);
		this.setLocation(location);
	}
	
	public BioPhysicalEntityParticipant(String id, BioPhysicalEntity physicalEntity) {
		super(id);
		this.setPhysicalEntity(physicalEntity);
		this.setStoichiometricCoefficient("1");
	}
	
	public BioPhysicalEntityParticipant(BioPhysicalEntity physicalEntity) {
		super(physicalEntity.getId());
		this.setPhysicalEntity(physicalEntity);
		this.setStoichiometricCoefficient("1");
	}
	
	public BioPhysicalEntityParticipant(BioPhysicalEntity physicalEntity, String sto) {
		super(physicalEntity.getId());
		this.setPhysicalEntity(physicalEntity);
		this.setStoichiometricCoefficient(sto);
	}
	
	public BioPhysicalEntityParticipant(BioPhysicalEntityParticipant in) {
		super(in);
		this.setPhysicalEntity(new BioPhysicalEntity(in.getPhysicalEntity()));
		this.setStoichiometricCoefficient(in.getStoichiometricCoefficient());
	}
	
	/**
	 * @return Returns the physicalEntity.
	 */
	public BioPhysicalEntity getPhysicalEntity() {
		return physicalEntity;
	}

	/**
	 * @param physicalEntity The physicalEntity to set.
	 */
	public void setPhysicalEntity(BioPhysicalEntity physicalEntity) {
		this.physicalEntity = physicalEntity;
	}

	/**
	 * @return Returns the stoichiometricCoefficient.
	 */
	public String getStoichiometricCoefficient() {
		return stoichiometricCoefficient;
	}

	/**
	 * @param stoichiometricCoefficient The stoichiometricCoefficient to set.
	 */
	public void setStoichiometricCoefficient(String stoichiometricCoefficient) {
		if(stoichiometricCoefficient.equals("")) {
			stoichiometricCoefficient="1";
		}
		this.stoichiometricCoefficient = stoichiometricCoefficient;
	}

	/**
	 * @return Returns the isPrimaryCompound.
	 */
	public Boolean getIsPrimaryCompound() {
		return isPrimaryCompound;
	}

	/**
	 * @param isPrimaryCompound The isPrimaryCompound to set.
	 */
	public void setIsPrimaryCompound(Boolean isPrimaryCompound) {
		this.isPrimaryCompound = isPrimaryCompound;
	}

	public BioCompartment getLocation() {
		return location;
	}

	public void setLocation(BioCompartment location) {
		this.location = location;
	}

}
