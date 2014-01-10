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
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import metabolome.util.MassType;
import metabolome.util.MolecularFormula;

import baobab.hypercyc.connection.Javacyc;
import baobab.hypercyc.connection.JavacycPlus;

/**
 * @author ludo
 *
 */
public class BioPhysicalEntity extends BioEntity {
	
	private String molecularWeight = "NA";
	private String chemicalFormula = "NA";
	private BioChemicalStructure structure;
	private String type = "NA";	
	private Boolean isCofactor=false;
	
	private String charge = "0";
	
	private Boolean isSide=false;
	
	private Boolean boundaryCondition=false;
	
	private String smiles="NA";
	
	private String caas="NA";
	
	public Boolean getBoundaryCondition() {
		return boundaryCondition;
	}

	public void setBoundaryCondition(Boolean boundaryCondition) {
		this.boundaryCondition = boundaryCondition;
	}


	private HashMap<String, BioChemicalReaction> reactionsAsSubstrate = new HashMap<String, BioChemicalReaction>();
	private HashMap<String, BioChemicalReaction> reactionsAsProduct = new HashMap<String, BioChemicalReaction>();
	
	public void addReactionAsSubstrate(BioChemicalReaction rxn) {
		this.reactionsAsSubstrate.put(rxn.getId(), rxn);
	}
	
	public void addReactionAsProduct(BioChemicalReaction rxn) {
		this.reactionsAsProduct.put(rxn.getId(), rxn);
	}
	
	public void removeReactionAsSubstrate(String id) {
		this.reactionsAsSubstrate.remove(id);
	}
	
	public HashMap<String, BioChemicalReaction> getReactionsAsSubstrate() {
		return reactionsAsSubstrate;
	}

	public void setReactionsAsSubstrate(
			HashMap<String, BioChemicalReaction> reactionsAsSubstrate) {
		this.reactionsAsSubstrate = reactionsAsSubstrate;
	}

	public HashMap<String, BioChemicalReaction> getReactionsAsProduct() {
		return reactionsAsProduct;
	}

	public void setReactionsAsProduct(
			HashMap<String, BioChemicalReaction> reactionsAsProduct) {
		this.reactionsAsProduct = reactionsAsProduct;
	}

	public void removeReactionAsProduct(String id) {
		this.reactionsAsProduct.remove(id);
	}
	
	public Boolean getIsSide() {
		return isSide;
	}
	public void setIsSide(Boolean isSide) {
		this.isSide = isSide;
	}
	public Boolean getIsCofactor() {
		return isCofactor;
	}
	public void setIsCofactor(Boolean isCofactor) {
		this.isCofactor = isCofactor;
	}
	
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getChemicalFormula() {
		return chemicalFormula;
	}
	public void setChemicalFormula(String chemicalFormula) {
		this.chemicalFormula = chemicalFormula;
	}
	public BioChemicalStructure getStructure() {
		return structure;
	}
	public void setStructure(BioChemicalStructure structure) {
		this.structure = structure;
	}
	public String getMolecularWeight() {
		return molecularWeight;
	}
	public void setMolecularWeight(String molecularWeight) {
		this.molecularWeight = molecularWeight;
	}
	
	/**
	 * Computes the molecular weight from the formula
	 */
	public void setMolecularWeight() {
		
		MolecularFormula molecularFormula = null;
		
		try{
			molecularFormula = new MolecularFormula(this.getChemicalFormula());
		}catch (RuntimeException e) { 

		}
		
        if(molecularFormula!=null)
        {
            double mass=molecularFormula.getMass(MassType.MONOISOTOPIC);
            
            mass = mass * 100000;
            mass = Math.round(mass);
            mass = mass / 100000;
            
            this.setMolecularWeight(Double.toString(mass));
            
        }
		
	}
	
	
	public BioPhysicalEntity(String id){
		super(id);
	}
	public BioPhysicalEntity(String id, String name){
		super(id, name);
	}
	
	public BioPhysicalEntity(BioPhysicalEntity pe) {
		super(pe);
		this.setMolecularWeight(pe.getMolecularWeight());
		this.setChemicalFormula(pe.getChemicalFormula());
		this.setBoundaryCondition(pe.getBoundaryCondition());
		
	}
	
	public BioPhysicalEntity(JavacycPlus cyc, String cpdId) {
		String cpdName = cyc.getSlotValueOrNA(cpdId, "COMMON-NAME");
		this.setId(cpdId);
		this.setName(cpdName);
		String weight = cyc.getSlotValueOrNA(cpdId, "MOLECULAR-WEIGHT");
		this.setMolecularWeight(weight);
		ArrayList<String> formulaArray = cyc.getSlotValues(cpdId, "CHEMICAL-FORMULA");
		
		String formula = formulaArray.toString();
		formula = formula.replace("(", "");
		formula = formula.replace(")", "");
		formula = formula.replace("[", "");
		formula = formula.replace("]", "");
		formula = formula.replace(" ", "");
		formula = formula.replace(",", "");
		
		String REGEX = "[A-Za-z]([A-Za-z]*)\\d*";
        
		Pattern pattern = Pattern.compile(REGEX);
        Matcher matcher = pattern.matcher(formula);
        
        while(matcher.find()) {
        	String str = matcher.group(1);   	
        	formula = formula.replaceFirst(str, str.toLowerCase());
        }
        
        this.setChemicalFormula(formula);
		
        // Computes the weight from the formula. If not, let the molecular weight
        // as indicated by BioCyc
        this.setMolecularWeight();
		
		
		ArrayList<String> cpdTypes = cyc.getInstanceAllTypes(cpdId);

		Boolean isClass = false;

		if (cyc.coercibleToClass(cpdId)) {
			isClass = true;
		}
		
		this.setIsHolderClass(isClass);
		
		String type="";
		
		if (cpdTypes.contains("|Compounds|")) {
			type="small-molecule";
		} else if (cpdTypes.contains("|Proteins|")) {
			type="protein";
		} else if (cpdTypes.contains("Protein-Complexes|")) {
			type="complex";
		} else if (cpdTypes.contains("|Dna|")) {
			type="dna";
		} else if (cpdTypes.contains("|Rna|")) {
			type="rna";
		} else {
			type="undefined";
		}
		
		this.setType(type);
		
		

	}
	
	public String toString() {
		
		return "<"+this.getId()+">";
		
	}
	
	
	public Boolean test(Boolean onlyPrimaries, Boolean keepHolder) {
		
		
		if(onlyPrimaries && this.isSide) {
			return false;
		}
		
		if(keepHolder==false && this.getIsHolderClass()) {
			return false;
		}
		
		
		return true;
		
	}

	public String getCharge() {
		return charge;
	}

	public void setCharge(String charge) {
		this.charge = charge;
	}

	public String getSmiles() {
		return smiles;
	}

	public void setSmiles(String smiles) {
		this.smiles = smiles;
	}

	public String getCaas() {
		return caas;
	}

	public void setCaas(String caas) {
		this.caas = caas;
	}

	/**
	 * format a metabolite id in the pallson way (M_***_c)
	 */
	public void formatIdByPalsson () {
		
		String id = this.getId();
		
		if(! id.startsWith("M_")) {
			id = "M_"+id;
		}
		
		String compartmentId = this.getCompartment().getId();
		
		if(! id.endsWith("_"+compartmentId)) {
			id = id+"_"+compartmentId;
		}
		
		this.setId(id);
		
		return;
	}
	
}
