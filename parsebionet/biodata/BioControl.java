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

/**
 * An interaction in which one entity regulates, modifies, or otherwise
 * influences another. Two types of control interactions are defined: activation
 * and inhibition. Since this class is a superclass for specific types of
 * control, instances of the control class should only be generated when none of
 * its subclasses are applicable. One example of an instance of this class would
 * be a small molecule that inhibits a pathway by an unknown mechanism.
 */

public class BioControl extends BioPhysicalInteraction {

	/**
	 * Defines the nature of the control relationship between the CONTROLLER and
	 * the CONTROLLED entities.
	 * 
	 * The following terms are possible values:
	 * 
	 * ACTIVATION: General activation
	 * 
	 * The following term can not be used in the catalysis class: INHIBITION:
	 * General inhibition
	 * 
	 * The following terms can only be used in the modulation class:
	 * INHIBITION-ALLOSTERIC Allosteric inhibitors decrease the specified enzyme
	 * activity by binding reversibly to the enzyme and inducing a
	 * conformational change that decreases the affinity of the enzyme to its
	 * substrates without affecting its VMAX. Allosteric inhibitors can be
	 * competitive or noncompetitive inhibitors, therefore, those inhibition
	 * categories can be used in conjunction with this category.
	 * 
	 * INHIBITION-COMPETITIVE Competitive inhibitors are compounds that
	 * competitively inhibit the specified enzyme activity by binding reversibly
	 * to the enzyme and preventing the substrate from binding. Binding of the
	 * inhibitor and substrate are mutually exclusive because it is assumed that
	 * the inhibitor and substrate can both bind only to the free enzyme. A
	 * competitive inhibitor can either bind to the active site of the enzyme,
	 * directly excluding the substrate from binding there, or it can bind to
	 * another site on the enzyme, altering the conformation of the enzyme such
	 * that the substrate can not bind to the active site.
	 * 
	 * INHIBITION-IRREVERSIBLE Irreversible inhibitors are compounds that
	 * irreversibly inhibit the specified enzyme activity by binding to the
	 * enzyme and dissociating so slowly that it is considered irreversible. For
	 * example, alkylating agents, such as iodoacetamide, irreversibly inhibit
	 * the catalytic activity of some enzymes by modifying cysteine side chains.
	 * 
	 * INHIBITION-NONCOMPETITIVE Noncompetitive inhibitors are compounds that
	 * noncompetitively inhibit the specified enzyme by binding reversibly to
	 * both the free enzyme and to the enzyme-substrate complex. The inhibitor
	 * and substrate may be bound to the enzyme simultaneously and do not
	 * exclude each other. However, only the enzyme-substrate complex (not the
	 * enzyme-substrate-inhibitor complex) is catalytically active.
	 * 
	 * INHIBITION-OTHER Compounds that inhibit the specified enzyme activity by
	 * a mechanism that has been characterized, but that cannot be clearly
	 * classified as irreversible, competitive, noncompetitive, uncompetitive,
	 * or allosteric.
	 * 
	 * INHIBITION-UNCOMPETITIVE Uncompetitive inhibitors are compounds that
	 * uncompetitively inhibit the specified enzyme activity by binding
	 * reversibly to the enzyme-substrate complex but not to the enzyme alone.
	 * 
	 * INHIBITION-UNKMECH Compounds that inhibit the specified enzyme activity
	 * by an unknown mechanism. The mechanism is defined as unknown, because
	 * either the mechanism has yet to be elucidated in the experimental
	 * literature, or the paper(s) curated thus far do not define the mechanism,
	 * and a full literature search has yet to be performed.
	 * 
	 * ACTIVATION-UNKMECH Compounds that activate the specified enzyme activity
	 * by an unknown mechanism. The mechanism is defined as unknown, because
	 * either the mechanism has yet to be elucidated in the experimental
	 * literature, or the paper(s) curated thus far do not define the mechanism,
	 * and a full literature search has yet to be performed.
	 * 
	 * ACTIVATION-NONALLOSTERIC Nonallosteric activators increase the specified
	 * enzyme activity by means other than allosteric.
	 * 
	 * ACTIVATION-ALLOSTERIC Allosteric activators increase the specified enzyme
	 * activity by binding reversibly to the enzyme and inducing a
	 * conformational change that increases the affinity of the enzyme to its
	 * substrates without affecting its VMAX.
	 * 
	 */
	private String controlType;
	private BioConversion controlled;
	private BioPhysicalEntityParticipant controller;
	
	public BioControl(String id) {
		super(id);
	}
	
	public BioControl(BioControl in) {
		super(in);
		this.setControlType(in.getControlType());
		this.setControlled(new BioConversion(in.getControlled()));
		this.setController(new BioPhysicalEntityParticipant(in.getController()));
	}
	
	/**
	 * @return Returns the controlType.
	 */
	public String getControlType() {
		return controlType;
	}

	/**
	 * @param controlType The controlType to set.
	 */
	public void setControlType(String controlType) {
		this.controlType = controlType;
		// To be completed to verify the BioPaxConstant Control Type
	}

	/**
	 * @return Returns the controlled.
	 */
	public BioConversion getControlled() {
		return controlled;
	}

	/**
	 * @param controlled The controlled to set.
	 */
	public void setControlled(BioConversion controlled) {
		this.controlled = controlled;
	}

	/**
	 * @return Returns the controller.
	 */
	public BioPhysicalEntityParticipant getController() {
		return controller;
	}

	/**
	 * @param controller The controller to set.
	 */
	public void setController(BioPhysicalEntityParticipant controller) {
		this.controller = controller;
	}



}
