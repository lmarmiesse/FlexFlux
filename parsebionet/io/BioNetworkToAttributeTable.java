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
 * 10 juin 2011 
 */
package parsebionet.io;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import parsebionet.biodata.BioChemicalReaction;
import parsebionet.biodata.BioNetwork;
import parsebionet.biodata.BioPathway;
import parsebionet.biodata.BioPhysicalEntity;
import parsebionet.biodata.BioPhysicalEntityParticipant;
import parsebionet.utils.StringUtils;


/**
 * @author ludo
 * 10 juin 2011
 *
 */
public class BioNetworkToAttributeTable {

	BioNetwork network;
	String outputFile;
	
	public BioNetworkToAttributeTable(BioNetwork bioNetwork, String outputFile) {
		
		this.network = bioNetwork;
		this.outputFile = outputFile;
		
	}
	
	public void writeAttributes(Boolean sbmlCoded) throws IOException {
		
		FileWriter fw = new FileWriter(this.outputFile);
		
		
		fw.write("id\tsbml type\tsbml name\tmass\tformula\tpathways\tec\trev\tcompartment\tgeneRules\tprotRules\tlb\tub\n");
		
		for(BioPhysicalEntity cpd : this.network.getPhysicalEntityList().values()) {
			
			String cpdId = cpd.getId();
			
			String cpdName = StringUtils.getNotFormattedString(cpd.getName());
			
			String mass = cpd.getMolecularWeight();
			
			String formula = cpd.getChemicalFormula();
		
			Set<String> pathways = new HashSet<String>();
			
			for(BioChemicalReaction rxn : cpd.getReactionsAsSubstrate().values()) {
				
				HashMap<String, BioPathway> rxnPathways = rxn.getPathwayList();
				
				for(BioPathway pathway : rxnPathways.values()) {
					
					String pathwayName = StringUtils.getNotFormattedString(pathway.getName());
					
					pathways.add(pathwayName);
					
				}
				
			}
			
			for(BioChemicalReaction rxn : cpd.getReactionsAsProduct().values()) {
				
				HashMap<String, BioPathway> rxnPathways = rxn.getPathwayList();
				
				for(BioPathway pathway : rxnPathways.values()) {
					
					String pathwayName = StringUtils.getNotFormattedString(pathway.getName());
					
					pathways.add(pathwayName);
					
				}
			}
			
			String pathwaysStr = "";
			
			int i = 0;
			for(String pathwayStr : pathways) {
				i++;
				
				if (i==1) {
					pathwaysStr = pathwayStr;
				}
				else {
					pathwaysStr = pathwaysStr+" _+_ "+pathwayStr;
				}
			}
			
			String compartmentName = "NA";
			
			if(cpd.getCompartment() != null) {
				compartmentName = cpd.getCompartment().getName();
			}
			
			if(sbmlCoded) {
				cpdId = StringUtils.sbmlEncode(cpdId);
			}
			
			fw.write(cpdId+"\tspecies\t"+cpdName+"\t"+mass+"\t"+formula+"\t"+pathwaysStr+"\tNA\t"+compartmentName+"\tNA\tNA\tNA\tNA\n");
			
		}
		
		for(BioChemicalReaction rxn : this.network.getBiochemicalReactionList().values()) {
			
			String rxnId = rxn.getId();
			String rxnName = StringUtils.getNotFormattedString(rxn.getName());
			String rxnFormula = StringUtils.getNotFormattedString(rxn.getEquation());
			
			String pathwaysStr="";
			
			int i = 0;
			
			for(BioPathway pathway : rxn.getPathwayList().values()) {
				
				i++;
				
				String pathwayName = StringUtils.getNotFormattedString(pathway.getName());

				if(i==1) {
					pathwaysStr = pathwayName;
				}
				else {
					pathwaysStr = pathwaysStr+" _+_ "+pathwayName;
				}
				
			}
			
			String ec = rxn.getEcNumber();

			String rev = "false";
			
			if(rxn.isReversible()) {
				rev = "true";
			}
			
			Set<String> compartmentIds = new HashSet<String>();
			
			for(BioPhysicalEntityParticipant bpe : rxn.getLeftParticipantList().values()) {
				
				BioPhysicalEntity cpd = bpe.getPhysicalEntity();
				
				String compartmentId = "NA";
				
				if(cpd.getCompartment() != null) {
				
					String compartimentId = cpd.getCompartment().getId();
				}
				
				compartmentIds.add(compartmentId);
				
			}
			
			String compartmentsStr = "";
			
			i = 0;
			
			for(String compartmentId : compartmentIds) {
				i++;
				
				if(i==1) {
					compartmentsStr = compartmentId;
				}
				else {
					compartmentsStr = compartmentsStr+" _+_ "+compartmentId;
				}				
			}
			
			String lb = rxn.getLowerBound().value;
			String ub = rxn.getUpperBound().value;
			
			
			String geneRules = rxn.getGPR().get(0);
			String proteinRules = rxn.getGPR().get(1);
			
			if(sbmlCoded) {
				rxnId = StringUtils.sbmlEncode(rxnId);
			}
			
			fw.write(rxnId+"\treaction\t"+rxnName+"\tNA\t"+rxnFormula+"\t"+pathwaysStr+"\t"+ec+"\t"+rev+"\t"+compartmentsStr+"\t"+geneRules+"\t"+proteinRules+"\t"+lb+"\t"+ub+"\n");
			
		}
		
		fw.close();
		
	}

}
