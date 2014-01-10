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
 * 
 * Save a bioNetwork in a model file compatible with surreyFBA.
 * Each row corresponds to a reaction.
 * Ex :
 *  r1	2 H2 + O2 = 2 H2O 0	100	g1 and ( g2 or g3 )	#comment
 * 
 * Ludo Cottret
 * 31 mai 2011 
 */
package parsebionet.io;

import java.io.IOException;

import org.jaxen.function.RoundFunction;

import parsebionet.biodata.BioChemicalReaction;
import parsebionet.biodata.BioNetwork;
import parsebionet.biodata.BioPathway;
import parsebionet.biodata.BioPhysicalEntity;
import parsebionet.biodata.BioPhysicalEntityParticipant;
import parsebionet.biodata.Flux;


/**
 * @author ludo
 * 31 mai 2011
 *
 */
public class BioNetwork2SurreyFBA extends BioNetwork2File {

	private Boolean addFake = false;
	private String externalSuffix = "";
	
	// To print the complete table : not usable in SurreyFba but used in metexplore/fba
	private Boolean completeTable = false;
	
	
	public BioNetwork2SurreyFBA(BioNetwork network, String fileOut, Boolean addFake, String ext, Boolean completeTable) {
		super(network, fileOut);
		
		this.addFake = addFake;
		this.externalSuffix = ext;
		this.completeTable = completeTable;
	}
	
	public void save() throws IOException {
		
		for(BioChemicalReaction rxn : this.getBioNetwork().getBiochemicalReactionList().values()) {
			
			String id = rxn.getId();
			String reactionName = rxn.getName();
			
			String pathways = "";
			
			for(BioPathway pathway : rxn.getPathwayList().values()) {
				
				String pathwayName = pathway.getName();
				
				if(pathways=="") {
					pathways = pathwayName;
				}
				else {
					pathways = pathways+" AND "+pathwayName;
				}
				
			}
			
			
			Flux lb = rxn.getLowerBound();
			Flux ub = rxn.getUpperBound();
			
			String lbValue;
			String ubValue;
			
			if(lb == null) {
				if(rxn.isReversible()) {
					lbValue = "-9999999";
				}
				else {
					lbValue = "0";
				}
			}
			else {
				lbValue = lb.value;
			}

			if(ub==null){
				ubValue="9999999";
			}
			else {
				ubValue = ub.value;
			}
			
			String rule = rxn.getGPR().get(0);
			
			int nb = 0;
			String equationWithIds = "";
			String equationWithNames = "";

			Boolean leftExchange=false;

			if(addFake) {
				if(rxn.getLeftParticipantList().size()==0) {
					// The left side is empty
					// Create a fake external left metabolite for each right compound

					for(BioPhysicalEntityParticipant bp : rxn.getRightParticipantList().values()) {

						BioPhysicalEntity cpd = bp.getPhysicalEntity();
						String cpdId = cpd.getId();
						String cpdName = cpd.getName();

						String newId = cpdId+"_fake";
						String newName = cpdName+"_fake";

						BioPhysicalEntity newCpd;

						if(this.getBioNetwork().getPhysicalEntityList().containsKey(newId)) {
							newCpd = this.getBioNetwork().getPhysicalEntityList().get(newId);
						}
						else {
							newCpd = new BioPhysicalEntity(newId, newName);
							this.getBioNetwork().addPhysicalEntity(newCpd);
							newCpd.setBoundaryCondition(true);
						}

						BioPhysicalEntityParticipant bpe = new BioPhysicalEntityParticipant(newCpd);

						rxn.addLeftParticipant(bpe);

					}

				}

				if(rxn.getRightParticipantList().size()==0) {
					// The right side is empty
					// Create a fake external right metabolite for each right compound

					for(BioPhysicalEntityParticipant bp : rxn.getLeftParticipantList().values()) {

						BioPhysicalEntity cpd = bp.getPhysicalEntity();
						String cpdId = cpd.getId();
						String cpdName = cpd.getName();

						String newId = cpdId+"_fake";
						String newName = cpdName+"_fake";

						BioPhysicalEntity newCpd;

						if(this.getBioNetwork().getPhysicalEntityList().containsKey(newId)) {
							newCpd = this.getBioNetwork().getPhysicalEntityList().get(newId);
						}
						else {
							newCpd = new BioPhysicalEntity(newId, newName);
							this.getBioNetwork().addPhysicalEntity(newCpd);
							newCpd.setBoundaryCondition(true);
						}

						BioPhysicalEntityParticipant bpe = new BioPhysicalEntityParticipant(newCpd);

						rxn.addRightParticipant(bpe);

					}
				}
			}
			
			for(BioPhysicalEntityParticipant p : rxn.getLeftParticipantList().values()) {
				
				BioPhysicalEntity cpd = p.getPhysicalEntity();
				
				String cpdId = cpd.getId();
				// To be compliant with surreyFBA
				if(cpd.getBoundaryCondition()) {
					cpdId = cpdId+externalSuffix;
					leftExchange = true;
				}
				
				
				String coeff = p.getStoichiometricCoefficient();
				
				nb ++;
				if(nb > 1) {
					equationWithIds = equationWithIds.concat(" + ");
					equationWithNames = equationWithNames.concat(" + ");
				}
				
				equationWithIds = equationWithIds+coeff+" "+cpdId;
				equationWithNames = equationWithNames+coeff+" "+cpd.getName();

			}

			equationWithIds = equationWithIds.concat(" = ");
			equationWithNames = equationWithNames.concat(" = ");

			nb = 0;
			Boolean rightExchange=false;
			
			for(BioPhysicalEntityParticipant p : rxn.getRightParticipantList().values()) {

				BioPhysicalEntity cpd = p.getPhysicalEntity();
				String cpdId = cpd.getId();
				// To be compliant with surreyFBA
				if(cpd.getBoundaryCondition()) {
					cpdId = cpdId+externalSuffix;
					rightExchange=true;
				}
				
				String coeff = p.getStoichiometricCoefficient();

				nb ++;
				if(nb > 1) {
					equationWithIds = equationWithIds.concat(" + ");
					equationWithNames = equationWithNames.concat(" + ");
				}

				equationWithIds = equationWithIds+coeff+" "+cpdId;
				equationWithNames = equationWithNames+coeff+" "+cpd.getName();

			}
			
			if(rxn.getLeftParticipantList().size()==0) {
				leftExchange=true;
			}
			
			if(rxn.getRightParticipantList().size()==0) {
				rightExchange=true;
			}
			
			
			String type = rxn.getFBAType();
			
			String exchangeDirection="NA";
			
			if(type.equalsIgnoreCase("Exchange")) {
				if(leftExchange) {
					exchangeDirection="left";
				}
				if(rightExchange) {
					exchangeDirection="right";
				}
			}
			
			String rev = "F";
			if (rxn.isReversible()) {
				rev = "T";
			}
			
			String ec = rxn.getEcNumber();
			if(this.completeTable) {
				this.getWriter().write(reactionName+"\t"+id+"\t"+lbValue+"\t"+ubValue+"\t"+type+"\t"+equationWithNames+"\t"+equationWithIds+"\t"+rule+"\t"+rev+"\t"+pathways+"\t"+exchangeDirection+"\t"+ec+"\n");
			}
			else {
				this.getWriter().write(id+"\t"+equationWithIds+"\t"+lbValue+"\t"+ubValue+"\t#"+equationWithNames+"\n");
			}
		}
		
		this.getWriter().close();
		
	}
	

}
