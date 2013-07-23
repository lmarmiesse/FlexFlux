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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import parsebionet.biodata.BioChemicalReaction;
import parsebionet.biodata.BioComplex;
import parsebionet.biodata.BioGene;
import parsebionet.biodata.BioNetwork;
import parsebionet.biodata.BioPathway;
import parsebionet.biodata.BioPhysicalEntity;
import parsebionet.biodata.BioPhysicalEntityParticipant;
import parsebionet.biodata.BioProtein;
import parsebionet.utils.StringUtils;


/**
 * Exports in tabulated files
 * @author Fabien
 * 9 Nov 2011
 *
 */
public class BioNetwork2TabulatedFile extends BioNetwork2File {

	public BioNetwork2TabulatedFile(BioNetwork bioNetwork, Boolean onlyPrimaries, Boolean keepHolderClassCompounds, String fileName) {
		
		super(bioNetwork, onlyPrimaries, keepHolderClassCompounds, fileName);
			
	}
	/**
	 * Given a list of reaction and metabolite attributes, write the corresponding tabulated file
	 * @param attributes
	 */
	public void writeAllAttributes(String[] attributes)
	{
		try {
			for(int i=0;i<attributes.length;i++)
			{
				if(i<attributes.length-1)
					this.getWriter().write(attributes[i]+"\t");
				else
					this.getWriter().write(attributes[i]+"\n");
			}
			for(BioChemicalReaction reaction: this.getListOfReactions().values())
			{
				//System.err.println(reaction.getCompartment());
				for(int i=0;i<attributes.length;i++)
				{
					if(i<attributes.length-1)
						this.getWriter().write(getReactionValueForAttribute(reaction,attributes[i])+"\t");
					else
						this.getWriter().write(getReactionValueForAttribute(reaction,attributes[i])+"\n");
				}
			}
			for(BioPhysicalEntity metabolite: this.getListOfSpecies().values())
			{
				for(int i=0;i<attributes.length;i++)
				{
					if(i<attributes.length-1)
						this.getWriter().write(getMetaboliteValueForAttribute(metabolite,attributes[i])+"\t");
					else
						this.getWriter().write(getMetaboliteValueForAttribute(metabolite,attributes[i])+"\n");
				}
			}
			this.getWriter().close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	/**
	 * 
	 */
	public String getMetaboliteValueForAttribute(BioPhysicalEntity metabolite, String att)
	{
		String val=null;
		att=att.toLowerCase();
		if(att.equals("id"))
			val=metabolite.getId();
		if(att.equals("id_encoded"))
			val=StringUtils.sbmlEncode(metabolite.getId());
		if(att.equals("name"))
			val=metabolite.getName();
		if(att.equals("name_decoded"))
			val=StringUtils.sbmlDecode(metabolite.getName());		
		if(att.equals("compartment"))
			val=metabolite.getCompartment().getId();
		return val;
	}
	/**
	 * 
	 */
	public String getReactionValueForAttribute(BioChemicalReaction reaction, String att)
	{
		String val=null;
		att=att.toLowerCase();
		if(att.equals("id"))
			val=reaction.getId();
		if(att.equals("id_encoded"))
			val=StringUtils.sbmlEncode(reaction.getId());
		if(att.equals("name"))
			val=reaction.getName();
		if(att.equals("name_decoded"))
			val=StringUtils.sbmlDecode(reaction.getName());		
		if(att.equals("ec"))
			val=reaction.getEcNumber();
		if(att.equals("equation"))
			val=reaction.getEquation();
		if(att.equals("reversibility"))
			val=reaction.getReversiblity();
		if(att.equals("compartment"))
			if(reaction.getCompartment()!=null)
				val=reaction.getCompartment().getId();
		return val;
	}
	
	public void write() throws IOException{
		for(BioChemicalReaction reaction: this.getListOfReactions().values())
		{
			writeReaction(reaction);
		}
/*		for(Iterator iterReaction = this.getListOfReactions().keySet().iterator(); iterReaction.hasNext(); ) {
			BioChemicalReaction reaction = this.getListOfReactions().get(iterReaction.next());
			
			writeReaction(reaction);
			
		}*/
		this.getWriter().close();
			
	}
	

	
	/**
	 * writing a reaction in the file out
	 */
	public void writeReaction(BioChemicalReaction reaction) {	
		if(this.getBioNetwork().getType().equalsIgnoreCase("cyc")) {
			if(reaction.getSpontaneous() == null) {
				ArrayList<BioPhysicalEntity> enzymes = new ArrayList<BioPhysicalEntity>(reaction.getEnzList().values());
				for(int i=0; i<enzymes.size(); i++) {

					BioPhysicalEntity enzyme = enzymes.get(i);
					recursiveWrite(reaction, enzyme, enzyme);
				}
			} else {
				writeSpontaneousReaction(reaction);
			}
		}
		else {
			writeSpontaneousReaction(reaction);
		}
	}
	
	public void recursiveWrite(BioChemicalReaction reaction, BioPhysicalEntity enzyme, BioPhysicalEntity sub) {
		
		HashMap<String, BioPathway> pathways = new HashMap<String, BioPathway>(reaction.getPathwayList());
		
		HashMap<String, BioPhysicalEntityParticipant> left;
		HashMap<String, BioPhysicalEntityParticipant> right;
		
		String infoReversibility = reaction.getReversiblity();
		
		String spontaneous = "NA";
		
		if(infoReversibility.compareToIgnoreCase("irreversible-right-to-left") == 0) {
			if(this.getOnlyPrimaries() == true) {
				left = reaction.getPrimaryRightParticipantList();
				right = reaction.getPrimaryLeftParticipantList();
			}
			else {
				left = reaction.getRightParticipantList();
				right = reaction.getLeftParticipantList();
			}
		} 
		else {
			if(this.getOnlyPrimaries() == true) {
				left = reaction.getPrimaryLeftParticipantList();
				right = reaction.getPrimaryRightParticipantList();
			}
			else {
				left = reaction.getLeftParticipantList();
				right = reaction.getRightParticipantList();
			}
		}
		
		if(sub.getClass().getSimpleName().compareTo("BioProtein") == 0) {
			
			ArrayList<BioGene> genes = new ArrayList<BioGene>(((BioProtein)sub).getGeneList().values());
			
			String enzymeName = StringUtils.htmlEncode(enzyme.getName());
			
			for(int h=0;h<genes.size();h++) {
				BioGene gene = genes.get(h);
				
				if(pathways.size() == 0) {
					BioPathway pathwayNA = new BioPathway("NA", "NA");
					pathways.put("NA", pathwayNA);
				}
				
				for(Iterator iterPathway = pathways.keySet().iterator(); iterPathway.hasNext();) {
					BioPathway pathway = pathways.get(iterPathway.next());
					
					String pathwayName =  StringUtils.htmlEncode(pathway.getName());
					
					for(Iterator iterLeft = left.keySet().iterator(); iterLeft.hasNext() ; ) {
						BioPhysicalEntity cpd = left.get(iterLeft.next()).getPhysicalEntity();
						String cpdId = StringUtils.htmlEncode(cpd.getId());
						String cpdName = StringUtils.htmlEncode(cpd.getName());
						
						try {
							this.getWriter().write(reaction.getId()+"\t"+pathway.getId()+"\t"+pathwayName+"\t"+cpdId+
									"\tsubstrate\t"+cpdName+"\t"+reaction.getEcNumber()+"\t"+enzymeName+
									"\t"+sub.getId()+"\t"+enzyme.getId()+"\t"+spontaneous+"\t"+gene.getId()+"\n");
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					for (Iterator iterRight = right.keySet().iterator(); iterRight.hasNext();) {
						BioPhysicalEntity cpd = right.get(iterRight.next()).getPhysicalEntity();
						String cpdName = StringUtils.htmlEncode(cpd.getName());
						
						String cpdId = StringUtils.htmlEncode(cpd.getId());
						
						try {
							this.getWriter().write(reaction.getId()+"\t"+pathway.getId()+"\t"+pathwayName+"\t"+cpdId+
									"\tproduct\t"+cpdName+"\t"+reaction.getEcNumber()+"\t"+enzymeName+
									"\t"+sub.getId()+"\t"+enzyme.getId()+"\t"+spontaneous+"\t"+gene.getId()+"\n");
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					
				}
			}
		}
		else if(sub.getClass().getSimpleName().compareTo("BioComplex") == 0) {
			HashMap<String, BioPhysicalEntityParticipant> subs = ((BioComplex)sub).getComponentList();
			
			for(Iterator iterSub = subs.keySet().iterator(); iterSub.hasNext(); ) {
				BioPhysicalEntity subSub = subs.get(iterSub.next()).getPhysicalEntity();
				recursiveWrite(reaction, enzyme, subSub);
			}
		}
	}
	
	
	public void writeSpontaneousReaction(BioChemicalReaction reaction) {
		HashMap<String, BioPathway> pathways = new HashMap<String, BioPathway>(reaction.getPathwayList());
		
		HashMap<String, BioPhysicalEntityParticipant> left;
		HashMap<String, BioPhysicalEntityParticipant> right;
		
		String infoReversibility = reaction.getReversiblity();
		
		if(infoReversibility.compareToIgnoreCase("irreversible-right-to-left") == 0) {
			if(this.getOnlyPrimaries() == true) {
				left = reaction.getPrimaryRightParticipantList();
				right = reaction.getPrimaryLeftParticipantList();
			}
			else {
				left = reaction.getRightParticipantList();
				right = reaction.getLeftParticipantList();
			}
		} 
		else {
			if(this.getOnlyPrimaries() == true) {
				left = reaction.getPrimaryLeftParticipantList();
				right = reaction.getPrimaryRightParticipantList();
			}
			else {
				left = reaction.getLeftParticipantList();
				right = reaction.getRightParticipantList();
			}
		}
		
		if(pathways.size() == 0) {
			BioPathway pathwayNA = new BioPathway("NA", "NA");
			pathways.put("NA", pathwayNA);
		}
		
		for(Iterator iterPathway = pathways.keySet().iterator(); iterPathway.hasNext();) {
			BioPathway pathway = pathways.get(iterPathway.next());
			
			String pathwayName =  StringUtils.htmlEncode(pathway.getName());
			
			for(Iterator iterLeft = left.keySet().iterator(); iterLeft.hasNext() ; ) {
				BioPhysicalEntity cpd = left.get(iterLeft.next()).getPhysicalEntity();
				String cpdId = StringUtils.htmlEncode(cpd.getId());
				String cpdName = StringUtils.htmlEncode(cpd.getName());
				
				
				try {
					this.getWriter().write(reaction.getId()+"\t"+pathway.getId()+"\t"+pathwayName+"\t"+cpdId+
							"\tsubstrate\t"+cpdName+"\t"+reaction.getEcNumber()+"\t"+"NA"+
							"\t"+"NA"+"\t"+"NA"+"\t"+"T"+"\t"+"NA"+"\n");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			for (Iterator iterRight = right.keySet().iterator(); iterRight.hasNext();) {
				BioPhysicalEntity cpd = right.get(iterRight.next()).getPhysicalEntity();
				String cpdName = StringUtils.htmlEncode(cpd.getName());
				
				String cpdId = StringUtils.htmlEncode(cpd.getId());
				
				try {
					this.getWriter().write(reaction.getId()+"\t"+pathway.getId()+"\t"+pathwayName+"\t"+cpdId+
							"\tproduct\t"+cpdName+"\t"+reaction.getEcNumber()+"\t"+"NA"+
							"\t"+"NA"+"\t"+"NA"+"\t"+"T"+"\t"+"NA"+"\n");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
		}
	}
	
	public static void main(String[] args) throws IOException {
		
		String sbmlFile = "/home/ludo/backtrack/ArticleVersionLongue/Application/bcicWithoutCofactors.xml";
		String tabFile = "/home/ludo/work/truc";
		
		Sbml2Bionetwork reader = new Sbml2Bionetwork(sbmlFile);
		
		BioNetwork bn = reader.getBioNetwork();
		
		System.out.println(bn.printNetworkForHuman(true));
		
//		BioNetwork2TabulatedFile bnm = new BioNetwork2TabulatedFile(bn, false, true, tabFile);
//		
//		bnm.write();
		
	}
	
	
}
