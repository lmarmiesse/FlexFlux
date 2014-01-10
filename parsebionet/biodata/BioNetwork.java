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
/*
 * Created on 1 juil. 2005
 * L.C
 */
package parsebionet.biodata;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import parsebionet.utils.Counter;
import parsebionet.utils.StringUtils;
import parsebionet.utils.graphe.ScopeCompounds;
import sun.misc.Regexp;

import baobab.hypercyc.connection.JavacycPlus;

/**
 * @author Ludovic COTTRET
 * 
 */

public class BioNetwork {

	/**
	 * Logger for this class
	 */
	// private static final Logger logger = Logger.getLogger(BioNetwork.class);
	private HashMap<String, BioPathway> pathwayList = new HashMap<String, BioPathway>();

	private HashMap<String, BioPhysicalEntity> physicalEntityList = new HashMap<String, BioPhysicalEntity>();

	private HashMap<String, BioComplex> complexList = new HashMap<String, BioComplex>();

	private HashMap<String, BioProtein> proteinList = new HashMap<String, BioProtein>();

	private HashMap<String, BioGene> geneList = new HashMap<String, BioGene>();

	private HashMap<String, BioChemicalReaction> biochemicalReactionList = new HashMap<String, BioChemicalReaction>();
	
	private HashMap<String, BioCatalysis> catalysisList = new HashMap<String, BioCatalysis>();
	
	private HashMap<String, BioTransport> transportList = new HashMap<String, BioTransport>();

	private HashMap<String, BioTransportWithBiochemicalReaction> transportWithBiochemicalReactionList = new HashMap<String, BioTransportWithBiochemicalReaction>();

	private HashMap<String, BioCompartment> compartments = new HashMap<String, BioCompartment>();
	
	
	private ArrayList<String> errorList;

	private String id = "NA";

	private String name = "NA";

	private String type = "custom";
	
	private HashMap<String, UnitDefinition> unitDefinitions = new HashMap<String, UnitDefinition>();
	

	/**
	 * Constructor from scratch
	 */
	public BioNetwork() {
		super();
	}
	
	/**
	 * Extract a subNetwork from the network with a list of compartments
	 * A reaction will be added to the sub-network if at least one substrate or product is in the reaction
	 *  @param compartments : the list of compartment ids
	 *  @param withTransports : true if we want to add the transports
	 * 
	 */
	public BioNetwork getSubNetwork(Set<String> compartments, boolean withTransports)
	{
		//HashSet<String> reactions=new HashSet<String>();
		//HashSet<String> compounds=new HashSet<String>();
		
		HashMap<String, BioChemicalReaction> listOfReactions=new HashMap<String, BioChemicalReaction>();
		
		// GET ALL THE REACTIONS
		for(BioChemicalReaction reaction: this.getBiochemicalReactionList().values())
		{
			//System.out.println(reaction.getEnzList().keySet());
			if(reaction.getCompartment()!=null)
			{
				if (compartments.contains(reaction.getCompartment().getId()))
				{
					/*reactions.add(reaction.getId());
					for(BioPhysicalEntityParticipant cpdParticipant : reaction.getLeftParticipantList().values())
					{
						compounds.add(cpdParticipant.getPhysicalEntity().getId());
					}
					for(BioPhysicalEntityParticipant cpdParticipant : reaction.getRightParticipantList().values())
					{
						compounds.add(cpdParticipant.getPhysicalEntity().getId());
					}*/
					listOfReactions.put(reaction.getId(),reaction);
				}				
			}
			else
			{
				if(withTransports)
				{
					boolean transportInvolved=false;
					for(BioPhysicalEntityParticipant cpdParticipant : reaction.getLeftParticipantList().values())
					{
						if(compartments.contains(cpdParticipant.getPhysicalEntity().getCompartment().getId()))
							transportInvolved=true;
					}
					for(BioPhysicalEntityParticipant cpdParticipant : reaction.getRightParticipantList().values())
					{
						if(compartments.contains(cpdParticipant.getPhysicalEntity().getCompartment().getId()))
							transportInvolved=true;
					}
					
					if(transportInvolved)
					{
						
						/*reactions.add(reaction.getId());
						for(BioPhysicalEntityParticipant cpdParticipant : reaction.getLeftParticipantList().values())
						{
							compounds.add(cpdParticipant.getPhysicalEntity().getId());
						}
						for(BioPhysicalEntityParticipant cpdParticipant : reaction.getRightParticipantList().values())
						{
							compounds.add(cpdParticipant.getPhysicalEntity().getId());
						}*/
						listOfReactions.put(reaction.getId(),reaction);
					}
					
				}
			}
		}
		//CREATE THE NETWORK WITH THE REACTIONS
		//BioNetwork subNet=new BioNetwork(this,reactions,compounds);
		
		BioNetwork subNet=new BioNetwork(listOfReactions);
		/*for(BioChemicalReaction reaction: subNet.getBiochemicalReactionList().values())
		{
			System.out.println(reaction.getEnzList().keySet());
		}*/
		//ADD THE COMPARTMENTS TO THE NETWORK
		/*for(String compId : this.getCompartments().keySet())
		{
			if(compartments.contains(compId))
			{
				subNet.addCompartment(this.getCompartments().get(compId));
			}
		}*/
		
		
		return subNet;
	}
	
	
	/**
	 * Build a subNetwork from an original network, a list of reactions and a list of compounds
	 * @param originalNetwork
	 * @param reactions
	 * @param compounds
	 * 
	 * Takes inot account only the reactions and not the informations associated
	 * 
	 */
	public BioNetwork(BioNetwork originalNetwork, Set<String> reactions, Set<String> compounds) {

		
		for(String cpdId : compounds) {

			String cpdName = originalNetwork.getPhysicalEntityList().get(cpdId).getName();

			BioPhysicalEntity cpd = new BioPhysicalEntity(cpdId, cpdName);
			
			//BioCompartment cpt = cpd.getCompartment();
			BioCompartment cpt = originalNetwork.getPhysicalEntityList().get(cpdId).getCompartment();
			
			cpd.setCompartment(cpt);
			
			this.addCompartment(cpt);

			this.addPhysicalEntity(cpd);
		}


		for(String reacId : reactions) {
			BioChemicalReaction reaction = originalNetwork.getBiochemicalReactionList().get(reacId);

			BioChemicalReaction newReaction = new BioChemicalReaction(reaction.getId(), reaction.getName());

			newReaction.setReversibility(reaction.getReversiblity());
			newReaction.setFlag(reaction.getFlag());
			
			this.addBiochemicalReaction(newReaction);

			for(BioPhysicalEntityParticipant cpdParticipant : reaction.getLeftParticipantList().values()) {

				String cpdId = cpdParticipant.getPhysicalEntity().getId();
				String cpdName = cpdParticipant.getPhysicalEntity().getName();
				BioPhysicalEntity newCpd;
				if(! this.getPhysicalEntityList().containsKey(cpdId)) {
					newCpd = new BioPhysicalEntity(cpdId, cpdName);
					this.addPhysicalEntity(newCpd);
				}
				else  {
					newCpd = this.getPhysicalEntityList().get(cpdParticipant.getPhysicalEntity().getId());
				}

				BioPhysicalEntityParticipant newCpdParticipant = new BioPhysicalEntityParticipant(cpdParticipant.getId(), newCpd, cpdParticipant.getStoichiometricCoefficient(), cpdParticipant.getLocation());

				newReaction.addLeftParticipant(newCpdParticipant);

				if(newReaction.getReversiblity().compareToIgnoreCase("irreversible-left-to-right")==0) {
					newCpd.addReactionAsSubstrate(newReaction);
				}
				else if(newReaction.getReversiblity().compareToIgnoreCase("irreversible-right-to-left")==0) {
					newCpd.addReactionAsProduct(newReaction);
				}
				else {
					newCpd.addReactionAsSubstrate(newReaction);
					newCpd.addReactionAsProduct(newReaction);
				}
			}

			for(BioPhysicalEntityParticipant cpdParticipant : reaction.getRightParticipantList().values()) {

				String cpdId = cpdParticipant.getPhysicalEntity().getId();
				String cpdName = cpdParticipant.getPhysicalEntity().getName();
				BioPhysicalEntity newCpd;
				if(! this.getPhysicalEntityList().containsKey(cpdId)) {
					newCpd = new BioPhysicalEntity(cpdId, cpdName);
					this.addPhysicalEntity(newCpd);
				}
				else  {
					newCpd = this.getPhysicalEntityList().get(cpdParticipant.getPhysicalEntity().getId());
				}

				BioPhysicalEntityParticipant newCpdParticipant = new BioPhysicalEntityParticipant(cpdParticipant.getId(), newCpd, cpdParticipant.getStoichiometricCoefficient(), cpdParticipant.getLocation());

				newReaction.addRightParticipant(newCpdParticipant);

				if(newReaction.getReversiblity().compareToIgnoreCase("irreversible-left-to-right")==0) {
					newCpd.addReactionAsProduct(newReaction);
				}
				else if(newReaction.getReversiblity().compareToIgnoreCase("irreversible-right-to-left")==0) {
					newCpd.addReactionAsSubstrate(newReaction);
				}
				else {
					newCpd.addReactionAsSubstrate(newReaction);
					newCpd.addReactionAsProduct(newReaction);
				}
			}
		}

	}


	/**
	 * Constructor from a list of {@link BioChemicalReaction}
	 * Duplicate reactions
	 */
	public BioNetwork(HashMap<String, BioChemicalReaction> listOfReactions) {

		this.setId("BioNetwork" + (new Date()).getTime());
		
		for (BioChemicalReaction rxn : listOfReactions.values()) {
			
			BioChemicalReaction newRxn = new BioChemicalReaction(rxn);
			this.addBiochemicalReaction(newRxn);
			
			this.addUnitDefinition(newRxn.getLowerBound().unitDefinition);
			this.addUnitDefinition(newRxn.getUpperBound().unitDefinition);

		}

	}

	/**
	 * @param cyc : the {@link JavacycPlus} connection
	 * @param typeMetab: the type of reactionNodes in the network ha wants to deal with :
	 * 		"metab-all"		= metab-smm rxns plus all other enzyme-catalyzed reactionNodes.
	 *      "metab-smm"    	= metab-pathways reactionNodes plus all other reactionNodes not in pathways whose
	 *         					substrates are all small molecules.
	 *      "metab-pathways" = All reactionNodes found within metabolic pathways. May include a handfull 
	 *       				 	of reactionNodes whose substrates are macromolecules, e.g., ACP.  Excludes transport reactionNodes.
	 *      "enzyme"         = All enzyme-catalyzed reactionNodes of Species (instances of classes
	 *        					EC-Reactions or Unclassified-Reactions)
	 *       "small-molecule" = All reactionNodes whose substrates are all small molecules, as opposed to 
	 *       					macromolecules. Excludes transport.
	 *       "transport"      = All transport reactionNodes of Species
	 *       "dna"            = All DNA Binding Reactions
	 *       "sigtrans"       = Signal-transduction reactionNodes
	 *       "protein-small-molecule-reaction"	= One substrate is a protein and one is a small 
	 *       										molecule
	 *       "protein-binds-dna"	= One substrate is a protein and one is a DNA-binding-site
	 *       "protein-reaction"		= All substrates are proteins
	 *       "trna-reaction"		= One substrate is a tRNA
	 *       "spontaneous"			= Spontaneous reactionNodes
	 *       "non-spontaneous"		= Non-spontaneous reactionNodes
	 * @param filter: if true, keeps all the reactionNodes. If false, keeps only spontaneous reactionNodes
	 * and enzymatic reactionNodes whose each enzymatic unity is coded by a gene annotated as present
	 * in the organism
	 */
	public BioNetwork(JavacycPlus cyc, String typeMetab) {

		super();

		this.setId(cyc.currentKB());

		this.setName(cyc.getSlotValueOrNA(cyc.currentKB(), "COMMON-NAME"));

		this.setType("cyc");

		// Get all reactionNodes
		ArrayList reactions = cyc.allRxns(typeMetab);
		
		// Get all transport reactions not included
		ArrayList transportReactions = cyc.allRxns("transport");
		
		reactions.addAll(transportReactions);
		
		System.err.println("Nb de reactions in cyc : "+reactions.size());
		
		// Instances each reaction
		for (int i = 0; i < reactions.size(); i++) {

			String reactionId = (String) reactions.get(i);
			
			String reactionName = cyc.getSlotValueOrNA(reactionId,
					"COMMON-NAME");
			String ecNumber = cyc.getSlotValueOrNA(reactionId, "EC-NUMBER");
			String spontaneous = cyc.getSlotValueOrNA(reactionId,
					"SPONTANEOUS?");
			
			if (spontaneous.equalsIgnoreCase("T")) {
				spontaneous = "L-R"; // To be in adequation with the biopax
				// format
			}

			BioChemicalReaction newReaction = new BioChemicalReaction();
			
			newReaction.setId(reactionId);
			newReaction.setName(reactionName);
			newReaction.setEcNumber(ecNumber);
			if (spontaneous.compareTo("NA") != 0)
				newReaction.setSpontaneous(spontaneous);
			
			newReaction.setEcNumber(ecNumber);
			
			ArrayList reactionTypes = cyc.getInstanceAllTypes(reactionId);
			
			if(reactionTypes.contains("|Small-Molecule-Reactions|"))
				newReaction.setBiocycClass("small");
			else if(reactionTypes.contains("|Macromolecule-Reactions|"))
				newReaction.setBiocycClass("macro");

			
			ArrayList enzrxns = cyc.getSlotValues(reactionId,
					"ENZYMATIC-REACTION");
			
//			
//			for (int j = 0; j < enzrxns.size(); j++) {
//				String enzrxnId = (String) enzrxns.get(j);
//				String enzrxnName = cyc.getSlotValueOrNA(enzrxnId,
//						"COMMON-NAME");
//				String enzrxnDirection = cyc.getSlotValueOrNA(enzrxnId,
//						"REACTION-DIRECTION");
//				
//
//				if (enzrxnDirection == null
//						|| enzrxnDirection.compareTo("NA") == 0)
//					enzrxnDirection = "reversible";
//
//				if (cyc.instanceAllInstanceOfP("|Enzymatic-Reactions|",
//						enzrxnId)) {
//					if (catalysisList.containsKey(enzrxnId)) {
//						BioCatalysis existedEnzRxn = (BioCatalysis) catalysisList
//								.get(enzrxnId);
//						newReaction.addEnzrxn(existedEnzRxn);
//						existedEnzRxn.addReaction(newReaction);
//					} else {
//						BioCatalysis newEnzrxn = new BioCatalysis(enzrxnId);
//
//						BioPhysicalEntityParticipant p;
//
//						newEnzrxn.setName(enzrxnName);
//
//						newEnzrxn.setDirection(enzrxnDirection);
//
//						String enzymeId = cyc.getSlotValueOrNA(enzrxnId,
//								"ENZYME");
//
//						ArrayList<String> enzTypes = cyc.getInstanceAllTypes(enzymeId);
//
//						if (enzTypes.contains("|Protein-Complexes|")) {
//							BioComplex newComplex;
//							if (complexList.containsKey(enzymeId)) {
//								newComplex = (BioComplex) complexList
//										.get(enzymeId);
//							} else {
//								newComplex = instancesComplex(cyc, enzymeId);
//							}
//							p = new BioPhysicalEntityParticipant(newEnzrxn
//									.getId()
//									+ "__With__" + newComplex.getId(),
//									newComplex, "1", null);
//						} else {
//							BioProtein protein = instancesProtein(cyc, enzymeId);
//							p = new BioPhysicalEntityParticipant(newEnzrxn
//									.getId()
//									+ "__With__" + protein.getId(), protein,
//									"1", null);
//						}
//						newEnzrxn.setController(p);
//						newReaction.addEnzrxn(newEnzrxn);
//						newEnzrxn.addReaction(newReaction);
//
//						addCatalysis(newEnzrxn);
//					}
//
//				}
//			}

			ArrayList enzymesId = cyc.enzymesOfReaction(reactionId);

			for (int j = 0; j < enzymesId.size(); j++) {
				String enzId = (String) enzymesId.get(j);
				String enzName = cyc.getSlotValueOrNA(enzId, "COMMON-NAME");

				ArrayList<String> enzTypes = cyc.getInstanceAllTypes(enzId);

				if (enzTypes.contains("|Protein-Complexes|")) {
					BioComplex newComplex = instancesComplex(cyc, enzId);
					newReaction.addEnz(newComplex);

				} else {
					BioProtein protein = instancesProtein(cyc, enzId);
					newReaction.addEnz(protein);
				}
			}

			// Get Reversibility
//		    REVERSIBLE: The reaction occurs in both directions in physiological settings.
//		    PHYSIOL-LEFT-TO-RIGHT, PHYSIOL-RIGHT-TO-LEFT: The reaction occurs in the specified direction in physiological settings, because of several possible factors including the energetics of the reaction, local concentrations of reactants and products, and the regulation of the enzyme or its expression.
//		    IRREVERSIBLE-LEFT-TO-RIGHT, IRREVERSIBLE-RIGHT-TO-LEFT: For all practical purposes, the reaction occurs only in the specified direction in physiological settings, because of chemical properties of the reaction.
//		    LEFT-TO-RIGHT, RIGHT-TO-LEFT: The reaction occurs in the specified direction in physiological settings, but it is unknown whether the reaction is considered irreversible. 
			
			String rev = cyc.getSlotValueOrNA(reactionId, "REACTION-DIRECTION");
			
			// Get Left compounds
			ArrayList<String> leftCompounds = cyc.getSlotValues(reactionId, "LEFT");

			for (int j = 0; j < leftCompounds.size(); j++) {


				if(leftCompounds.get(j).getClass().getSimpleName().compareTo("String") != 0) {
					System.err.println("Problem with the left compounds of "+reactionId);
				} 
				else {
					
					String cpdId = (String) leftCompounds.get(j);
					
					String compartmentId = cyc.getAnnotValue(reactionId, "LEFT", cpdId, "COMPARTMENT");

					if(compartmentId=="NIL") {
						compartmentId = "NA";
					}
					
					BioCompartment compartment = new BioCompartment(compartmentId, compartmentId);
					
					this.addCompartment(compartment);
					
					
					BioPhysicalEntity cpd;
					
					String cpdIdInCompartment = cpdId+"_IN_"+compartmentId;
					
					if(! this.getPhysicalEntityList().containsKey(cpdIdInCompartment)) {
						cpd = new BioPhysicalEntity(cyc, cpdId);
						cpd.setId(cpdIdInCompartment);
						cpd.setCompartment(compartment);
					}
					else {
						cpd = this.getPhysicalEntityList().get(cpdIdInCompartment);
					}
					
					if(rev.contains("LEFT-TO-RIGHT")) {
						cpd.addReactionAsSubstrate(newReaction);
					}
					else if(rev.contains("RIGHT-TO-LEFT")) {
						cpd.addReactionAsProduct(newReaction);
					}
					else {
						cpd.addReactionAsSubstrate(newReaction);
						cpd.addReactionAsProduct(newReaction);
					}
					

					if(cpd.getIsHolderClass()) {
						newReaction.setIsGenericReaction(true);
					}

					this.addPhysicalEntity(cpd);

					BioPhysicalEntityParticipant p;

					String coeff = StringUtils.transformStoi(cyc.getAnnotValue(
							reactionId, "LEFT", cpdId, "COEFFICIENT"));

					
					p = new BioPhysicalEntityParticipant(reactionId+ "__left__"+cpdId, cpd,  coeff, compartment);


					if(! rev.contains("RIGHT-TO-LEFT")) {
						newReaction.addLeftParticipant(p);
					}
					else {
						newReaction.addRightParticipant(p);
					}
				}
			}
			
			ArrayList<String> rightCompounds = cyc.getSlotValues(reactionId, "RIGHT");

			for (int j = 0; j < rightCompounds.size(); j++) {
				

				if(rightCompounds.get(j).getClass().getSimpleName().compareTo("String") != 0) {
					System.err.println("Problem with the right compounds of "+reactionId);
				} 
				else {
					
					String cpdId = (String) rightCompounds.get(j);
					
					String compartmentId = cyc.getAnnotValue(reactionId, "RIGHT", cpdId, "COMPARTMENT");

					if(compartmentId=="NIL") {
						compartmentId = "NA";
					}
					
					BioCompartment compartment = new BioCompartment(compartmentId, compartmentId);
					
					this.addCompartment(compartment);
					
					
					BioPhysicalEntity cpd;
					
					String cpdIdInCompartment = cpdId+"_IN_"+compartmentId;
					
					if(! this.getPhysicalEntityList().containsKey(cpdIdInCompartment)) {
						cpd = new BioPhysicalEntity(cyc, cpdId);
						cpd.setId(cpdIdInCompartment);
						cpd.setCompartment(compartment);
					}
					else {
						cpd = this.getPhysicalEntityList().get(cpdIdInCompartment);
					}
					
					if(rev.contains("LEFT-TO-RIGHT")) {
						cpd.addReactionAsProduct(newReaction);
					}
					else if(rev.contains("RIGHT-TO-LEFT")) {
						cpd.addReactionAsSubstrate(newReaction);
					}
					else {
						cpd.addReactionAsSubstrate(newReaction);
						cpd.addReactionAsProduct(newReaction);
					}
					

					if(cpd.getIsHolderClass()) {
						newReaction.setIsGenericReaction(true);
					}

					this.addPhysicalEntity(cpd);

					BioPhysicalEntityParticipant p;

					String coeff = StringUtils.transformStoi(cyc.getAnnotValue(
							reactionId, "RIGHT", cpdId, "COEFFICIENT"));

					
					p = new BioPhysicalEntityParticipant(reactionId+ "__right__"+cpdId, cpd,  coeff, compartment);


					if(! rev.contains("RIGHT-TO-LEFT")) {
						newReaction.addRightParticipant(p);
					}
					else {
						newReaction.addLeftParticipant(p);
					}
				}
			}
		
			
			// Set reversibility :
			// For convenience, there are only two cases:
			// irreversible-left-to-right or reversible
			// The substrates and the products are inversed in the previous step
			// if the reaction direction is "*right-to-left*".
			// The physiol-left-to-right are considered as irreversible-left-to-right
			if(rev.equalsIgnoreCase("reversible") || rev.equalsIgnoreCase("NA")) {
				newReaction.setReversibility(true);
			}
			else {
				newReaction.setReversibility(false);
			}
			
			// Get pathways in which occurs the reaction
			ArrayList pathwayIdList = cyc.getSlotValues(reactionId,
			"IN-PATHWAY");

			for (int j = 0; j < pathwayIdList.size(); j++) {
				String pathwayId = (String) pathwayIdList.get(j);

				BioPathway pathway = instancesPathwayReaction(cyc, pathwayId, newReaction);

				if(pathway != null) {
					this.addPathway(pathway);
				}
			}
			
			// Set the primary compounds

			newReaction.setPrimaryParticipantLeftList(cyc);
			newReaction.setPrimaryParticipantRightList(cyc);
			
			if(rev.contains("RIGHT-TO-LEFT")) {
				// We change the id of the reaction to indicate that it's reverse
				newReaction.setId(reactionId+"_BACK");
			}


			addBiochemicalReaction(newReaction);
		}
		
		// Mark side compounds
		this.markSides();
		
	}

	
	/**
	 * Filter the holes, i.e. the reactions not spontaneous and not associated with an enzyme
	 * 
	 */
	
	public void removeInfeasibleReactions() {
		
		HashMap<String, BioChemicalReaction> reactions = new HashMap<String, BioChemicalReaction>(this.getBiochemicalReactionList());
		
		for(BioChemicalReaction reaction : reactions.values()) {

			if(reaction.isPossible() == false) {
				this.removeBioChemicalReaction(reaction.getId());
			}
		}
	}
	
	

	/**
	 * @return Returns the pathwayList
	 */
	public HashMap<String, BioPathway> getPathwayList() {
		return pathwayList;
	}

	/**
	 * Add a pathway in the list
	 * 
	 * @param o
	 *            the object to add
	 */
	public void addPathway(BioPathway o) {
		this.pathwayList.put(o.getId(), o);
	}

	/**
	 * @return Returns the errorList.
	 */
	public ArrayList getErrorList() {
		return errorList;
	}

	/**
	 * @param errorList
	 *            The errorList to set.
	 */
	public void setErrorList(ArrayList<String> errorList) {
		this.errorList = errorList;
	}

	/**
	 * Add an error in the list
	 * 
	 * @param o
	 *            the object to add
	 */
	public void addError(String message) {
		this.errorList.add(message);
	}

	/**
	 * @return Returns the complexList.
	 */
	public HashMap<String, BioComplex> getComplexList() {
		return complexList;
	}

	/**
	 * Add a complex
	 * 
	 * @param o
	 *            the object to add
	 */
	public void addComplex(BioComplex o) {
		this.complexList.put(o.getId(), o);
	}

	/**
	 * @return Returns the physicalEntityList.
	 */
	public HashMap<String, BioPhysicalEntity> getPhysicalEntityList() {
		return physicalEntityList;
	}

	/**
	 * Add a physical entity in the list
	 * 
	 * @param o
	 *            the object to add
	 */
	public void addPhysicalEntity(BioPhysicalEntity o) {

		this.physicalEntityList.put(o.getId(), o);
		this.compartments.put(o.getCompartment().getId(), o.getCompartment());
		
	}
	
	/**
	 * Removes a compound from a network.
	 */
	public void removeCompound(String id) {
		
		if(this.getPhysicalEntityList().containsKey(id) == true) {
			
			HashMap<String, BioChemicalReaction> RP = this.getListOfReactionsAsProduct(id);
			HashMap<String, BioChemicalReaction> RC = this.getListOfReactionsAsSubstrate(id);
			
			HashMap<String, BioChemicalReaction> reactions = new HashMap<String, BioChemicalReaction>();
			reactions.putAll(RP);
			reactions.putAll(RC);
			
			this.getPhysicalEntityList().remove(id);
			
			for(BioChemicalReaction rxn : reactions.values()) {
				
				Set<String> left = rxn.getLeftList().keySet();
				Set<String> right = rxn.getRightList().keySet();
				
				if(left.contains(id)== true) {
					rxn.removeLeftCpd(rxn.getLeftList().get(id));
				}
				
				if(right.contains(id)== true) {
					rxn.removeRightCpd(rxn.getRightList().get(id));
				}
				
				if(rxn.getLeftList().size()==0 || rxn.getRightList().size()==0) {
					this.removeBioChemicalReaction(rxn.getId());
				}
			}
		}
	}
	
	/**
	 * Remove several compounds
	 * @param compounds
	 */
	public void removeCompounds(Set<String> compounds) {
		
		for(String cpd : compounds) {
			this.removeCompound(cpd);
		}
		
		return;
		
	}
	
	
	/**
	 * Removes a reaction from a network. Removes also the compounds which become orphan
	 */
	public void removeBioChemicalReaction(String id) {
		
		if(this.getBiochemicalReactionList().containsKey(id) == true) {
			BioChemicalReaction rxn = this.getBiochemicalReactionList().get(id);
			
			HashMap<String, BioPhysicalEntity> left = rxn.getLeftList();
			HashMap<String, BioPhysicalEntity> right = rxn.getRightList();
			HashMap<String, BioPhysicalEntityParticipant> leftP = rxn.getLeftParticipantList();
			HashMap<String, BioPhysicalEntityParticipant> rightP = rxn.getRightParticipantList();
			
			this.getBiochemicalReactionList().remove(id);
			
			HashMap<String, BioPhysicalEntity> leftAndRight = new HashMap<String, BioPhysicalEntity>();
			leftAndRight.putAll(left);
			leftAndRight.putAll(right);
			
			HashMap<String, BioPhysicalEntityParticipant> leftAndRightP = new HashMap<String, BioPhysicalEntityParticipant>();
			leftAndRightP.putAll(leftP);
			leftAndRightP.putAll(rightP);
			
			
			for(String cpdId : leftAndRight.keySet()) {
				
				if(this.getPhysicalEntityList().containsKey(cpdId)) {
					
					this.getPhysicalEntityList().get(cpdId).removeReactionAsProduct(id);
					this.getPhysicalEntityList().get(cpdId).removeReactionAsSubstrate(id);
					
					if(this.getPhysicalEntityList().get(cpdId).getReactionsAsProduct().size() == 0 
							&& this.getPhysicalEntityList().get(cpdId).getReactionsAsSubstrate().size() == 0) {
						// The compound does not occur in any reaction any more
						this.getPhysicalEntityList().remove(cpdId);
					}
				}
				
			}
			
		}
		
	}
	
	
	/**
	 * 
	 * 
	 * @return the distribution of the reactionNodes and the compounds in the distinct connected
	 * components of the network
	 */
	public HashMap<String, Integer> clusterDistribution() {
		
		int n = -1;
		
		HashMap<String, Integer> distrib = new HashMap<String, Integer>();
		
		if(this.getPhysicalEntityList().size() == 0) {
			return distrib;
		}
		
		Set<String> ids = new HashSet<String>();
		
		ids.addAll(this.getPhysicalEntityList().keySet());
		ids.addAll(this.getBiochemicalReactionList().keySet());
		
		while(distrib.keySet().equals(ids) == false) {
			for(String cpd : this.getPhysicalEntityList().keySet()) {
				if(distrib.keySet().contains(cpd) == false) {
					n++;
					parcoursRecursif(cpd, distrib, n);
				}
			}
		}
		
		return distrib;
		
	}
	
	/**
	 * 
	 * @param cpdId
	 * @param distrib
	 * @param n
	 */
	private void parcoursRecursif(String cpdId, HashMap<String, Integer> distrib, int n) {
		
		if(distrib.keySet().contains(cpdId)) {
			return;
		}
		
		distrib.put(cpdId, n);
		
		HashMap<String, BioChemicalReaction> RP = this.getPhysicalEntityList().get(cpdId).getReactionsAsProduct();
		HashMap<String, BioChemicalReaction> RS = this.getPhysicalEntityList().get(cpdId).getReactionsAsSubstrate();
		
		for(BioChemicalReaction rxn : RP.values()) {
			
			distrib.put(rxn.getId(), n);
			
			Set<String> substrates = rxn.getListOfSubstrates().keySet();
			
			for(String substrate : substrates) {
				parcoursRecursif(substrate, distrib, n);
			}
			
		}
		
		for(BioChemicalReaction rxn : RS.values()) {
			
			distrib.put(rxn.getId(), n);
			
			Set<String> products = rxn.getListOfProducts().keySet();
			
			for(String product : products) {
				parcoursRecursif(product, distrib, n);
			}
			
		}
		
		return;
		
	}
	
	

	/**
	 * @return Returns the proteinList.
	 */
	public HashMap<String, BioProtein> getProteinList() {
		return proteinList;
	}

	/**
	 * Add a protein in the list
	 * 
	 * @param o
	 *            the object to add
	 */
	public void addProtein(BioProtein o) {
		this.proteinList.put(o.getId(), o);
		
		for(BioGene gene : o.getGeneList().values()) {
			this.addGene(gene);
		}
		
	}


	/**
	 * @return Returns the biochemicalReactionList.
	 */
	public HashMap<String, BioChemicalReaction> getBiochemicalReactionList() {
		return biochemicalReactionList;
	}

	/**
	 * Add a biochemical reaction in the list
	 * 
	 * @param o
	 *            the object to add
	 */
	public void addBiochemicalReaction(BioChemicalReaction o) {

		for(BioPhysicalEntity cpd : o.getLeftList().values()) {
			if(! this.getPhysicalEntityList().containsKey(cpd.getId())) {
				this.addPhysicalEntity(cpd);
			}
			
			this.getPhysicalEntityList().get(cpd.getId()).addReactionAsSubstrate(o);
			if(o.isReversible()) {
				this.getPhysicalEntityList().get(cpd.getId()).addReactionAsProduct(o);
			}
			
		}
		
		for(BioPhysicalEntity cpd : o.getRightList().values()) {
			if(! this.getPhysicalEntityList().containsKey(cpd.getId())) {
				this.addPhysicalEntity(cpd);
			}
			
			this.getPhysicalEntityList().get(cpd.getId()).addReactionAsProduct(o);
			if(o.isReversible()) {
				this.getPhysicalEntityList().get(cpd.getId()).addReactionAsSubstrate(o);
			}
		}

		this.biochemicalReactionList.put(o.getId(), o);
		
	}

	/**
	 * @return Returns the catalysisList.
	 */
	public HashMap<String, BioCatalysis> getCatalysisList() {
		return catalysisList;
	}

	/**
	 * Add a catalysis in the list
	 * 
	 * @param o
	 *            the object to add
	 */
	public void addCatalysis(BioCatalysis o) {
		this.catalysisList.put(o.getId(), o);
	}



	/**
	 * @return Returns the transportList.
	 */
	public HashMap<String, BioTransport> getTransportList() {
		return transportList;
	}

	/**
	 * Add a transport in the list
	 * 
	 * @param o
	 *            the object to add
	 */
	public void addTransport(BioTransport o) {
		this.transportList.put(o.getId(), o);
	}

	/**
	 * @return Returns the transportWithBiochemicalReactionList.
	 */
	public HashMap<String, BioTransportWithBiochemicalReaction> getTransportWithBiochemicalReactionList() {
		return transportWithBiochemicalReactionList;
	}

	/**
	 * Add a transport with bioche mical reactionin the list
	 * 
	 * @param o
	 *            the object to add
	 */
	public void addTransportWithBiochemicalReaction(
			BioTransportWithBiochemicalReaction o) {
		this.transportWithBiochemicalReactionList.put(o.getId(), o);
		this.biochemicalReactionList.put(o.getId(), o);
	}

	/**
	 * @return Returns the geneList.
	 */
	public HashMap<String, BioGene> getGeneList() {
		return geneList;
	}

	/**
	 * @param a
	 *            gene to add
	 */
	public void addGene(BioGene gene) {
		geneList.put(gene.getId(), gene);
	}

	/**
	 * Instances all monomers or sub-complexes in a Ecocyc complex
	 * 
	 * @param JavacycPlus
	 *            cyc : the BioCyc connection
	 * @param String
	 *            complexId : the complex id
	 */
	private BioComplex instancesComplex(JavacycPlus cyc, String complexId) {

		BioComplex complex;

		if (complexList.containsKey(complexId)) {
			complex = (BioComplex) complexList.get(complexId);
			return complex;
		} else {
			String enzymeName = cyc.getSlotValueOrNA(complexId, "COMMON-NAME");
			complex = new BioComplex(complexId, enzymeName);

			ArrayList components = cyc.monomersOfProtein(complexId);

			for (int i = 0; i < components.size(); i++) {
				String componentId = (String) components.get(i);

				ArrayList enzTypes = cyc.getInstanceAllTypes(componentId);

				String coefficient = cyc.getAnnotValue(complexId, "COMPONENTS",
						componentId, "COEFFICIENT");

				if (enzTypes.contains("|Protein-Complexes|")) {
					if(componentId.compareTo(complexId)!=0) {
						BioComplex newComplex = instancesComplex(cyc, componentId);
						BioPhysicalEntityParticipant p = new BioPhysicalEntityParticipant(
								complexId + "__With__" + newComplex.getId(),
								newComplex, coefficient, null);
						complex.addComponent(p);
					}

				} else {
					BioProtein newProtein = instancesProtein(cyc, componentId);
					BioPhysicalEntityParticipant p = new BioPhysicalEntityParticipant(
							complexId + "__With__" + newProtein.getId(),
							newProtein, coefficient, null);
					complex.addComponent(p);
				}
			}

			return (complex);
		}

	}

	/**
	 * Instances a protein and the corresponding genes
	 */
	private BioProtein instancesProtein(JavacycPlus cyc, String proteinId) {

		if (proteinList.containsKey(proteinId)) {
			return (BioProtein) proteinList.get(proteinId);
		} else {
			String proteinName = cyc.getSlotValueOrNA(proteinId, "COMMON-NAME");
			BioProtein newProtein = new BioProtein(proteinId, proteinName);
			ArrayList geneIdList = cyc.getSlotValues(proteinId, "GENE");
			for (int j = 0; j < geneIdList.size(); j++) {
				String geneId = (String) geneIdList.get(j);

				String geneName = cyc.getSlotValueOrNA(geneId, "COMMON-NAME");
				
				// To avoid the pseudo genes annotated by Mage :
				if(geneName.endsWith("pseudo") == false) {
				
					BioGene gene;
					if (!geneList.containsKey(geneId)) {
						gene = new BioGene(geneId, geneName);
					} else {
						gene = (BioGene) geneList.get(geneId);
					}
					newProtein.addGene(gene);
					gene.addProtein(newProtein);
					newProtein.addGene(gene);
					addGene(gene);
				}
			}

			return newProtein;
		}
	}

	/**
	 * Instances a pathway and the corresponding super pathways
	 */
	private BioPathway instancesPathwayReaction(JavacycPlus cyc,
			String pathwayId, BioInteraction interaction) {

		BioPathway pathway;
		
		if (cyc.instanceAllInstanceOfP("|Pathways|", pathwayId)) {
			
			if (pathwayList.containsKey(pathwayId)) {
				pathway = (BioPathway) this.pathwayList.get(pathwayId);
			} else {
				String pathwayName = cyc.getSlotValueOrNA(pathwayId,
						"COMMON-NAME");
				pathway = new BioPathway(pathwayId, pathwayName);
			}

			if (!pathway.getListOfInteractions()
					.containsKey(interaction.getId())) {
				pathway.getListOfInteractions().put(interaction.getId(), interaction);
			}

			if (!interaction.getPathwayList().containsKey(pathway.getId())) {
				interaction.addPathway(pathway);
			}

			ArrayList superPathwayIdList = cyc.getSlotValues(pathwayId,
					"SUPER-PATHWAYS");

			for (int i = 0; i < superPathwayIdList.size(); i++) {
				String superPathwayId = (String) superPathwayIdList.get(i);
				BioPathway superPathway;
				
				if (pathwayList.containsKey(superPathwayId)) {
					superPathway = (BioPathway) pathwayList.get(superPathwayId);

				} else {
					superPathway = new BioPathway(superPathwayId);
				}
				superPathway = instancesPathwayReaction(cyc, superPathwayId,
						interaction);

				if(superPathway != null)
				{
					pathway.addSuperPathway(superPathway);
					superPathway.addSubPathway(pathway);
				}
			}
			pathway.setReactionLayouts(cyc);
			return pathway;
		} else {
			return null;
		}
	}

	/**
	 * @return Returns the id.
	 */
	public String getId() {
		return id;
	}

	/**
	 * @param id
	 *            The id to set.
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * @return Returns the name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param id in the encoded mode
	 * @return physical entity with the same id
	 */
	public BioPhysicalEntity getBioPhysicalEntityById(String id)
	{
		BioPhysicalEntity entity=null;
		for(BioPhysicalEntity metabolite : getPhysicalEntityList().values())
		{
			//System.err.println("ID "+metabolite.getId()+" -- "+metabolite.getName());
			if (StringUtils.sbmlEncode(metabolite.getId()).equals(id))
			{
				entity=metabolite;
				break;
			}
		}
		return entity;
	}
	
	/**
	 * @param name
	 *            The name to set.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @param cpd
	 * @return the list of reactionNodes which involves the compound cpd as
	 *         substrate
	 */
	public HashMap<String, BioChemicalReaction> getListOfReactionsAsSubstrate(String cpd) {
		
		HashMap<String, BioChemicalReaction> reactionsAsSubstrate = new HashMap<String, BioChemicalReaction>();
		
		
		if (this.getPhysicalEntityList().containsKey(cpd) == false) {
			return reactionsAsSubstrate;
		}
		
		
		for (BioChemicalReaction rxn : this.getBiochemicalReactionList().values()) {
			
			HashMap<String, BioPhysicalEntity> listOfSubstrates = rxn.getListOfSubstrates();
			
			if (listOfSubstrates.containsKey(cpd)) {
				reactionsAsSubstrate.put(rxn.getId(), rxn);
			}
		}
		
		return reactionsAsSubstrate;

	}
	
	
	public String getNewReactionId(String prefix) {
		
		String id;
		
		int n = 1;
		
		id = prefix+n;
		
		while(this.getBiochemicalReactionList().containsKey(id)) {
			n++;
			id = prefix+n;
		}
		
		return id;
		
	}
	
	public String getNewMetaboliteId(String prefix, String suffix) {
		
		String id;
		
		int n = 1;
		
		id = prefix+n+suffix;
		
		while(this.getPhysicalEntityList().containsKey(id)) {
			n++;
			id = prefix+n+suffix;
		}
		
		return id;
		
	}
	

	/**
	 * @param cpd
	 * @return the list of reactionNodes which involves the compound cpd as
	 *         substrate
	 */

	public HashMap<String, BioChemicalReaction> getListOfReactionsAsPrimarySubstrate(
			String cpd) {
		
		HashMap<String, BioChemicalReaction> reactionsAsSubstrate = new HashMap<String, BioChemicalReaction>();
		
		
		if (this.getPhysicalEntityList().containsKey(cpd) == false) {
			return reactionsAsSubstrate;
		}
		
		
		for (BioChemicalReaction rxn : this.getBiochemicalReactionList().values()) {
			
			HashMap<String, BioPhysicalEntity> listOfSubstrates = rxn.getListOfPrimarySubstrates();
			
			if (listOfSubstrates.containsKey(cpd)) {
				reactionsAsSubstrate.put(rxn.getId(), rxn);
			}
		}
		
		return reactionsAsSubstrate;

	}

	/**
	 * @param cpd
	 * @return the list of reactionNodes which involves the compound cpd as product
	 */

	public HashMap<String, BioChemicalReaction> getListOfReactionsAsProduct(
			String cpd) {

		HashMap<String, BioChemicalReaction> reactionsAsProduct = new HashMap<String, BioChemicalReaction>();
		
		
		if (this.getPhysicalEntityList().containsKey(cpd) == false) {
			return reactionsAsProduct;
		}
		
		
		for (BioChemicalReaction rxn : this.getBiochemicalReactionList().values()) {
			
			HashMap<String, BioPhysicalEntity> listOfProducts = rxn.getListOfProducts();
			
			if (listOfProducts.containsKey(cpd)) {
				reactionsAsProduct.put(rxn.getId(), rxn);
			}
		}
		
		return reactionsAsProduct;

	}

	/**
	 * @param cpd
	 * @return the list of reactionNodes which involves the compound cpd as primary
	 *         product
	 */

	public HashMap<String, BioChemicalReaction> getListOfReactionsAsPrimaryProduct(
			String cpd) {

		HashMap<String, BioChemicalReaction> reactionsAsProduct = new HashMap<String, BioChemicalReaction>();
		
		
		if (this.getPhysicalEntityList().containsKey(cpd) == false) {
			return reactionsAsProduct;
		}
		
		
		for (BioChemicalReaction rxn : this.getBiochemicalReactionList().values()) {
			
			HashMap<String, BioPhysicalEntity> listOfProducts = rxn.getListOfPrimaryProducts();
			
			if (listOfProducts.containsKey(cpd)) {
				reactionsAsProduct.put(rxn.getId(), rxn);
			}
		}
		
		return reactionsAsProduct;
	}

	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * @param type
	 *            the type to set
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * Compare the network with another network by comparing the ids of their
	 * reactionNodes and of their compounds
	 * 
	 * @param otherNetwork
	 * @return
	 */
	public Boolean haveTheSameReactions(BioNetwork otherNetwork) {

		Set<String> rxnIds = this.getBiochemicalReactionList().keySet();
		Set<String> otherRxnIds = otherNetwork.getBiochemicalReactionList()
				.keySet();

		Set<String> cpdIds = this.getPhysicalEntityList().keySet();
		Set<String> otherCpdIds = otherNetwork.getPhysicalEntityList().keySet();

		if (rxnIds.equals(otherRxnIds) && cpdIds.equals(otherCpdIds)) {
			return true;
		}

		return false;

	}
	
	/**
	 * @param left
	 * @param right
	 * @return the list of the reactionNodes which have these left and right compounds
	 */
	public HashMap<String, BioChemicalReaction> reactionsWith(Set<String> left, Set<String> right) {
		
		HashMap<String, BioChemicalReaction> listOfReactions = new HashMap<String, BioChemicalReaction>();
		
		for(BioChemicalReaction rxn : this.getBiochemicalReactionList().values()) {
			
			Set<String> l = rxn.getLeftList().keySet();
			Set<String> r = rxn.getRightList().keySet();
			
			
			if(rxn.getReversiblity().compareToIgnoreCase("irreversible-left-to-right")==0) {
				if(l.equals(left) && r.equals(right))
					listOfReactions.put(rxn.getId(), rxn);
			}
			else if(rxn.getReversiblity().compareToIgnoreCase("irreversible-right-to-left")==0) {
				if(l.equals(right) && r.equals(left))
					listOfReactions.put(rxn.getId(), rxn);
			}
			else {
				if((l.equals(right) && r.equals(left)) || (l.equals(left) && r.equals(right)))
					listOfReactions.put(rxn.getId(), rxn);
			}
		}
		
		return listOfReactions;
		
		
	}
	
	/**
	 * Returns the reactions that contains the metabolites in the set1 and
	 * the metabolites in the set2 in each side.
	 * For instance : if set1 = A and set2 = B
	 * Returns :
	 * R1 : A +C -> B + D
	 * R2 : B + E -> A +F
	 * 
	 */
	public HashMap<String, BioChemicalReaction> reactionsThatInvolvesAtLeast(Set<String> set1, Set<String> set2) {
		
		
		HashMap<String, BioChemicalReaction> listOfReactions = new HashMap<String, BioChemicalReaction>();
	
		for(BioChemicalReaction reaction : this.getBiochemicalReactionList().values()) {
			
			Set<String> lefts = reaction.getLeftList().keySet();
			Set<String> rights = reaction.getRightList().keySet();
			
			if((lefts.containsAll(set1) && rights.containsAll(set2)) ||
					(lefts.containsAll(set2) && rights.containsAll(set1))){
				listOfReactions.put(reaction.getId(), reaction);
			}
		}
		
		return listOfReactions;
		
	}
	
	
	
	/**
	 * @param left
	 * @return the list of the reactionNodes which have these substrates
	 */
	public HashMap<String, BioChemicalReaction> reactionsWithTheseSubstrates(Set<String> left) {
		
		HashMap<String, BioChemicalReaction> listOfReactions = new HashMap<String, BioChemicalReaction>();
		
		for(BioChemicalReaction rxn : this.getBiochemicalReactionList().values()) {
			
			Set<String> substrates = rxn.getListOfSubstrates().keySet();
			
			if(substrates.equals(left)) 
				listOfReactions.put(rxn.getId(), rxn);
			
		}
		
		return listOfReactions;
		
		
	}
	
	/**
	 * Write the network as a list of reactions
	 * If a compound doesn't occur in any reaction, it will be indicated at the end.
	 */
	public String networkAsString(Boolean encodeSbml) {
		
		String out="";
		
		for(BioChemicalReaction rxn : this.getBiochemicalReactionList().values()) {
			out = out.concat(rxn.getEquation(encodeSbml)+"\n");
		}
		
		for(BioPhysicalEntity cpd : this.getPhysicalEntityList().values()) {
			if(cpd.getReactionsAsSubstrate().size() == 0 
					&& cpd.getReactionsAsProduct().size() == 0 ) {
				
				if(encodeSbml) {
					out = out.concat(StringUtils.sbmlEncode(cpd.getId())+"\n");
				}
				else {
					out = out.concat(cpd.getId()+"\n");
				}
			}
		}
			
		return out;
	}
	
	public String networkAsString() {
		return this.networkAsString(false);
	}
	
	public String printNetworkForHuman(Boolean encodeSbml) {
		
		String out="";
		
		for(BioChemicalReaction rxn : this.getBiochemicalReactionList().values()) {
			out = out.concat(rxn.getEquationForHuman(encodeSbml)+"\n");
		}
		
		for(BioPhysicalEntity cpd : this.getPhysicalEntityList().values()) {
			if(cpd.getReactionsAsSubstrate().size() == 0 
					&& cpd.getReactionsAsProduct().size() == 0 ) {
				
				out = out.concat(StringUtils.htmlEncode(cpd.getName())+"\n");
			}
		}
			
		return out;
	}
		
	
	/**
	 * @param type in : in-degree
	 * 			   out : out-degree
	 * 			   all : in-degree + out-degree
	 * @return a TreeBidiMap where the keys are the compound ids and the values the number of links
	 * with reactions. If type equals "in", the degree is the number of reactions where the compound occurs as a
	 * product. If type equals "out", the degree is the number of reactions where the compound occurs as a 
	 * substrate.  If type equals "all", the degree is the number of reactions where the compound occurs as a 
	 * substrate or as product.
	 */
	public Counter degreeCompounds(String type) {
		
		Counter res = new Counter();
		
		if(type.compareToIgnoreCase("all")!=0 && type.compareToIgnoreCase("in")!=0 && type.compareToIgnoreCase("out")!=0) {
			System.err.println("Error in degreeCompounds in the type of degree");
			System.exit(1);
		}
		
		for(String cpd : this.getPhysicalEntityList().keySet()) {
			int deg = 0;
			if(type.compareToIgnoreCase("all") == 0) {
				HashMap<String, BioChemicalReaction> reactions = new HashMap<String, BioChemicalReaction>();
				reactions.putAll(this.getPhysicalEntityList().get(cpd).getReactionsAsSubstrate());
						
				reactions.putAll(this.getPhysicalEntityList().get(cpd).getReactionsAsProduct());
				deg = reactions.size();
			}
			else if(type.compareToIgnoreCase("in") == 0) {
				deg = this.getPhysicalEntityList().get(cpd).getReactionsAsProduct().size();
			}
			else if(type.compareToIgnoreCase("out") == 0) {
				deg = this.getPhysicalEntityList().get(cpd).getReactionsAsSubstrate().size();
			}
			
			res.put(cpd, deg);
		}
		
		return res;
		
	}
	
	/**
	 * @param cofactorFile : file where there is a list of cofactor transformation to mark
	 * Mark in each reaction the compounds corresponding to cofactors.
	 * If a compound appears always as a cofactor, mark it as a cofactor.
	 * @throws IOException 
	 */
	public void markCofactors(String cofactorFile) throws IOException {
		
		FileInputStream in = new FileInputStream(cofactorFile);
		InputStreamReader ipsr=new InputStreamReader(in);
		BufferedReader br=new BufferedReader(ipsr);
		String ligne;

		Set<String> compartmentIds = new HashSet<String>();

		if(this.getCompartments().size() > 0 ) {
			// In the biocyc networks built by MetExplore, the metabolites are duplicated in each compartment
			// The information about the compartment is added as suffix in each metabolite label
			// ex : ATP_IN_cytoplasm
			compartmentIds = this.getCompartments().keySet();
		}


		Set<ArrayList<ArrayList<String>>> cofactorPairs = new HashSet<ArrayList<ArrayList<String>>>();


		while ((ligne=br.readLine())!=null){
			if(! ligne.matches("^#.*")) {
				String[] tab = ligne.split("\\t");

				String cof1 = tab[0];
				String[] str = cof1.split("\\+");

				ArrayList<String> cofs1 = new ArrayList<String>();
				for(int i = 0; i<str.length;i++) {
					cofs1.add(str[i]);
				}

				String cof2 = tab[1];
				str = cof2.split("\\+");

				ArrayList<String> cofs2 = new ArrayList<String>();
				for(int i = 0; i<str.length;i++) {
					cofs2.add(str[i]);
				}

				ArrayList<ArrayList<String>> pair = new ArrayList<ArrayList<String>>();
				pair.add(cofs1);
				pair.add(cofs2);

				cofactorPairs.add(pair);

				for(String compartmentId : compartmentIds) {
					// We duplicate the pairs for each compartment
					ArrayList<String> cofs1Compt = new ArrayList<String>();

					for(String x : cofs1) {
						cofs1Compt.add(x+"_IN_"+compartmentId);
					}

					for(String compartmentId2 : compartmentIds) {

						ArrayList<String> cofs2Compt = new ArrayList<String>();

						for(String x : cofs2) {
							cofs2Compt.add(x+"_IN_"+compartmentId2);
						}

						ArrayList<ArrayList<String>> pairCpt = new ArrayList<ArrayList<String>>();

						pairCpt.add(cofs1Compt);
						pairCpt.add(cofs2Compt);

						cofactorPairs.add(pairCpt);
					}
				}
			}
		}

		in.close();
		
		for(ArrayList<ArrayList<String>> pairs : cofactorPairs) {

			ArrayList<String> cofs1 = pairs.get(0);
			ArrayList<String> cofs2 = pairs.get(1);



			if(this.getPhysicalEntityList().containsKey(cofs1.get(0)) 
					&& this.getPhysicalEntityList().containsKey(cofs2.get(0))) {

				HashMap<String, BioChemicalReaction> listOfReactions = new HashMap<String, BioChemicalReaction>(this.getBiochemicalReactionList());

				for(BioChemicalReaction reaction : listOfReactions.values()) {

					HashMap<String, BioPhysicalEntityParticipant> leftP = reaction.getLeftParticipantList();
					HashMap<String, BioPhysicalEntityParticipant> rightP = reaction.getRightParticipantList();

					HashMap<String, BioPhysicalEntity> left = reaction.getLeftList();
					HashMap<String, BioPhysicalEntity> right = reaction.getRightList();

					if(left.containsKey(cofs1.get(0)) && right.containsKey(cofs2.get(0))) {

						for(BioPhysicalEntityParticipant bp : leftP.values()) {

							if(cofs1.contains(bp.getPhysicalEntity().getId())) {
								reaction.addCofactor(bp.getPhysicalEntity().getId());
							}

						}

						for(BioPhysicalEntityParticipant bp : rightP.values()) {

							if(cofs2.contains(bp.getPhysicalEntity().getId())) {
								reaction.addCofactor(bp.getPhysicalEntity().getId());
							}

						}

					}
					else if(left.containsKey(cofs2.get(0)) && right.containsKey(cofs1.get(0))) {

						for(BioPhysicalEntityParticipant bp : leftP.values()) {

							if(cofs2.contains(bp.getPhysicalEntity().getId())) {
								reaction.addCofactor(bp.getPhysicalEntity().getId());
							}

						}

						for(BioPhysicalEntityParticipant bp : rightP.values()) {

							if(cofs1.contains(bp.getPhysicalEntity().getId())) {
								reaction.addCofactor(bp.getPhysicalEntity().getId());
							}

						}

					}
				}
			}
		}
			

		// If a compound is a cofactor in each reaction it occurs, mark it as a cofactor
		for(BioPhysicalEntity cpd : this.getPhysicalEntityList().values()) {
			
			ArrayList <BioChemicalReaction> reactions = new ArrayList<BioChemicalReaction>();
			
			reactions.addAll(cpd.getReactionsAsSubstrate().values());
			reactions.addAll(cpd.getReactionsAsProduct().values());
			
			Boolean isCof = true;
			
			int nb = reactions.size();
			int i = nb;

			while(i > 0 && isCof == true) {

				i--;	

				BioChemicalReaction rxn = reactions.get(i);

				HashMap<String, BioPhysicalEntityParticipant> participants = new HashMap<String, BioPhysicalEntityParticipant>();

				participants.putAll(rxn.getLeftParticipantList());
				participants.putAll(rxn.getRightParticipantList());

				for(BioPhysicalEntityParticipant bp : participants.values()) {
					if(bp.getId().compareTo(cpd.getId())==0) {
						isCof=bp.getIsCofactor();
					}
				}
			}
		}
	}
	
	/**
	 * Mark the compounds as side if they occur as side compound in each reaction of the network
 	 */
	public void markSides() {
		
		// If a compound is a cofactor in each reaction it occurs, mark it as a cofactor
		for(BioPhysicalEntity cpd : this.getPhysicalEntityList().values()) {
			
			ArrayList <BioChemicalReaction> reactions = new ArrayList<BioChemicalReaction>();
			
			reactions.addAll(cpd.getReactionsAsSubstrate().values());
			reactions.addAll(cpd.getReactionsAsProduct().values());
			
			Boolean isSide = true;
			
			int nb = reactions.size();
			int i = nb;

			while(i > 0 && isSide == true) {

				i--;	

				BioChemicalReaction rxn = reactions.get(i);
				
				HashMap<String, BioPhysicalEntity> primaries = new HashMap<String, BioPhysicalEntity>();
				
				primaries.putAll(rxn.getPrimaryLeftList());
				primaries.putAll(rxn.getPrimaryRightList());
				
				
				if(primaries.containsKey(cpd.getId())) {
					isSide=false;
				}
				
			}
			
			cpd.setIsSide(isSide);
			
		}
		
	}
	
	/**
	 * Double the reversible reactions
	 */
	public BioNetwork doubleReversibleReactions() {
		
		BioNetwork newNetwork = new BioNetwork(this, this.getBiochemicalReactionList().keySet(), this.getPhysicalEntityList().keySet());
		
		Set<BioChemicalReaction> reactions = new HashSet<BioChemicalReaction>(this.getBiochemicalReactionList().values());
		
		for(BioChemicalReaction rxn : reactions) {
			
			if(rxn.getReversiblity().compareToIgnoreCase("reversible")==0) {
				
				String originalId = rxn.getId();
				String originalName = rxn.getName();
				
				rxn.setId(originalId+"__F");
				rxn.setName(originalName+"__F");
				
				rxn.setReversibility("irreversible-left-to-right");
				
				BioChemicalReaction rxn2 = new BioChemicalReaction();
				
				rxn2.setId(originalId+"__B");
				rxn2.setName(originalName+"__B");
				rxn2.setLeftList(rxn.getRightList());
				rxn2.setRightList(rxn.getLeftList());
				rxn2.setLeftParticipantList(rxn.getRightParticipantList());
				rxn2.setRightParticipantList(rxn.getLeftParticipantList());
				
				rxn2.setReversibility("irreversible-left-to-right");
				
				newNetwork.addBiochemicalReaction(rxn2);
				newNetwork.getBiochemicalReactionList().remove(originalId);
				newNetwork.addBiochemicalReaction(rxn);
				
				for(BioPhysicalEntity cpd : rxn.getLeftList().values()) {
					cpd.removeReactionAsSubstrate(originalId);
					cpd.removeReactionAsProduct(originalId);
				}
				
				for(BioPhysicalEntity cpd : rxn.getRightList().values()) {
					cpd.removeReactionAsSubstrate(originalId);
					cpd.removeReactionAsProduct(originalId);
				}
				
				for(BioPhysicalEntity cpd : rxn.getLeftList().values()) {
					cpd.addReactionAsProduct(rxn2);
					cpd.addReactionAsSubstrate(rxn);
				}
				
				for(BioPhysicalEntity cpd : rxn.getRightList().values()) {
					cpd.addReactionAsProduct(rxn);
					cpd.addReactionAsSubstrate(rxn2);
				}
				
				
				
			}
			
		}
		
		return newNetwork;
		
	}
	
	/**
	 * Eliminate all the reactions involved in scopes not containing interestingCpds
	 * Be careful : does not work with reversible reactions
	 * @param interestingCpds
	 */
	public BioNetwork compressAroundMetabolites(Set<String> interestingCpds, Boolean useReversibleReactionOnlyOnce) {
		
		Set<String> bs = new HashSet<String>(this.getPhysicalEntityList().keySet());
		
		// 1. we keep only the network produced by the scope of the interesting cpds
		
		ScopeCompounds sc1 = new ScopeCompounds(this, interestingCpds, bs, "", new HashSet<String>(), useReversibleReactionOnlyOnce, true);
		
		sc1.compute();
		
		sc1.createScopeNetwork();
		
		BioNetwork networkCompressed = sc1.getScopeNetwork();
		
//		
//		try {
//			System.out.println("Ecriture de scope.xml");
//			sc1.writeScopeAsSbml("/home/ludo/work/scope.xml");
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		
//		Set<String> alreadyScoped = new HashSet<String>();
//		
//		Set<BioPhysicalEntity> cpds = new HashSet<BioPhysicalEntity>(networkCompressed.getPhysicalEntityList().values());
//		
//		for(BioPhysicalEntity cpd : cpds) {
//			
//			if(networkCompressed.getPhysicalEntityList().containsKey(cpd.getId()) && ! alreadyScoped.contains(cpd.getId())) {
//				
//				alreadyScoped.add(cpd.getId());
//				
//				Set<String> in = new HashSet<String>();
//				in.add(cpd.getId());
//				
//				Set<BioChemicalReaction> RUs = new HashSet<BioChemicalReaction>(cpd.getReactionsAsSubstrate().values());
//				
//				for(BioChemicalReaction RU : RUs) {
//					
////					System.out.println("RU : "+RU);
//					
//					HashMap<String, BioChemicalReaction> reactionsToAvoid = new HashMap<String, BioChemicalReaction>(cpd.getReactionsAsSubstrate());
//					reactionsToAvoid.remove(RU.getId());
//					
//					HashMap<String, String> linksRemoved = new HashMap<String, String>();
//					
//					
//					// We remove each link between cpd and reactionsToAvoid
//					// and we store them into a HashMap
//		
//					for(BioChemicalReaction rxn : reactionsToAvoid.values()) {
//							
//						if(rxn.getLeftList().containsKey(cpd.getId())) {
//							rxn.removeLeft(cpd.getId());
//							cpd.removeReactionAsSubstrate(rxn.getId());
//							linksRemoved.put(rxn.getId(), "L");
//						}
//						
//						if(rxn.getReversiblity().equalsIgnoreCase("reversible") &&
//								rxn.getRightList().containsKey(cpd.getId())) {
//							rxn.removeRight(cpd.getId());
//							cpd.removeReactionAsSubstrate(rxn.getId());
//							linksRemoved.put(rxn.getId(), "R");
//						}
//						
//					}
//					
//					HashMap<String, BioPhysicalEntity> products = RU.getListOfProducts();
//
//
//					for(BioPhysicalEntity product : products.values()) {
//
//						// We remove the link between the other products and RU
//						Set<BioPhysicalEntity> productsRemoved = new HashSet<BioPhysicalEntity>();
//						
//						HashMap<String, BioPhysicalEntity> newProducts = new HashMap<String, BioPhysicalEntity>(RU.getRightList());
//						
//						for(BioPhysicalEntity otherProduct : newProducts.values()) {
//							if(! otherProduct.equals(product)) {
//								RU.removeRight(otherProduct.getId());
//								otherProduct.removeReactionAsProduct(RU.getId());
//								productsRemoved.add(otherProduct);
//							}
//						}
//						ScopeCompounds sc = new ScopeCompounds(networkCompressed, in, bs, "", new HashSet<String>(), false, true);
//
//						Boolean stop = false;
//
//						while(sc.run() != 0) { // While new compounds are added
//
//
//							for(String interestingCpd : interestingCpds) {
//								if(cpd.getId().compareTo(interestingCpd) != 0 && sc.getCurrentCpdsSteps().containsKey(interestingCpd)) {
//									stop = true;
//								}
//							}
//
//							if(stop) {
//								break;
//							}
//
//						}
//
//						if(stop == false) { 
//							// We remove the link between the reaction and the product
//							RU.removeRight(product.getId());
//							product.removeReactionAsProduct(RU.getId());
//						}
//
//						// We reput the products removed
//						for(BioPhysicalEntity otherProduct : productsRemoved) {
//							RU.addRightParticipant(new BioPhysicalEntityParticipant(otherProduct));
//							otherProduct.addReactionAsProduct(RU);
//						}
//					}
//
//					for(String rxnId : linksRemoved.keySet()) {
//						
//						if(networkCompressed.getBiochemicalReactionList().containsKey(rxnId)) {
//
//							String side = linksRemoved.get(rxnId);
//							BioChemicalReaction rxn = networkCompressed.getBiochemicalReactionList().get(rxnId);
//
//							if(side.equals("L")) {
//								rxn.addLeftParticipant(new BioPhysicalEntityParticipant(cpd));
//							}
//
//							if(side.equals("R")) {
//								rxn.addRightParticipant(new BioPhysicalEntityParticipant(cpd));
//							}
//
//							cpd.addReactionAsSubstrate(rxn);
//							
//							networkCompressed.addPhysicalEntity(cpd);
//							
//						}
//						
//					}
//					
//					if(RU.getRightList().size()==0 || RU.getLeftList().size()==0) {
//						networkCompressed.removeBioChemicalReaction(RU.getId());
//					}
//					
//				}
//			}
//		}
//		
//		// Final step : we recompute the scope from the interesting compounds
//		sc1 = new ScopeCompounds(networkCompressed, interestingCpds, bs, "", new HashSet<String>(), false, true);
//		sc1.compute();
//		
//		sc1.createScopeNetwork();
//		
//		networkCompressed = sc1.getScopeNetwork();
		
		return networkCompressed;
	}
	
	/**
	 * 
	 */
	public void compressIdenticalReactions() {
		
		ArrayList<BioChemicalReaction> reactions = new ArrayList<BioChemicalReaction>(this.getBiochemicalReactionList().values());
		
		int l = reactions.size();
		
		for(int i=0; i < l;i++) {
			
			BioChemicalReaction rxn1 = reactions.get(i);
			
			Boolean identical = false;
			
			String id = rxn1.getId();
			String name = rxn1.getName();
			
			if(this.getBiochemicalReactionList().containsKey(rxn1.getId())) {
				
				for(int j=i+1; j <l;j++) {
					BioChemicalReaction rxn2 = reactions.get(j);
					if(this.getBiochemicalReactionList().containsKey(rxn2.getId())) {
						if(rxn2.isRedundantWith(rxn1)) {
							identical = true;
							this.removeBioChemicalReaction(rxn2.getId());
							id = id+"__or__"+rxn2.getId();
							name = name+"__or__"+rxn2.getName();
						}
					}
					
				}
			
				if(identical) {
					this.getBiochemicalReactionList().remove(rxn1.getId());
					rxn1.setId(id);
					rxn1.setName(name);
					this.addBiochemicalReaction(rxn1);
				}
			}
		}
	}
	
	
	public void setBiochemicalReactionList(
			HashMap<String, BioChemicalReaction> biochemicalReactionList) {
		this.biochemicalReactionList = biochemicalReactionList;
	}


	public void setCatalysisList(HashMap<String, BioCatalysis> catalysisList) {
		this.catalysisList = catalysisList;
	}



	public void setComplexList(HashMap<String, BioComplex> complexList) {
		this.complexList = complexList;
	}

	public void setGeneList(HashMap<String, BioGene> geneList) {
		this.geneList = geneList;
	}

	public void setPathwayList(HashMap<String, BioPathway> pathwayList) {
		this.pathwayList = pathwayList;
	}

	public void setPhysicalEntityList(
			HashMap<String, BioPhysicalEntity> physicalEntityList) {
		this.physicalEntityList = physicalEntityList;
	}

	public void setProteinList(HashMap<String, BioProtein> proteinList) {
		this.proteinList = proteinList;
	}

	public void setTransportList(HashMap<String, BioTransport> transportList) {
		this.transportList = transportList;
	}

	public void setTransportWithBiochemicalReactionList(
			HashMap<String, BioTransportWithBiochemicalReaction> transportWithBiochemicalReactionList) {
		this.transportWithBiochemicalReactionList = transportWithBiochemicalReactionList;
	}

	public Boolean isEmpty() {
		
		Boolean flag = false;
		
		if(this.getPhysicalEntityList().size() == 0)
			flag = true;
		
		return flag;
	}
	
	public void addCompartment(BioCompartment compartment) {
		this.compartments.put(compartment.getId(), compartment);
	}

	public HashMap<String, BioCompartment> getCompartments() {
		return compartments;
	}

	public void setCompartments(HashMap<String, BioCompartment> compartments) {
		this.compartments = compartments;
	}

	public HashMap<String, UnitDefinition> getUnitDefinitions() {
		return unitDefinitions;
	}

	public void setUnitDefinitions(HashMap<String, UnitDefinition> unitsDefinition) {
		this.unitDefinitions = unitsDefinition;
	}
	
	public void addUnitDefinition(UnitDefinition ud) {
		this.unitDefinitions.put(ud.getId(), ud);
	}
	
	/** 
	 * Remove all the reactions whose the 'hole' flag is set to true
	 */
	public void removeHoles() {
		
		Set<String> reactionIds = new HashSet<String>(this.getBiochemicalReactionList().keySet());
		
		for(String reactionId : reactionIds) {
			
			BioChemicalReaction reaction = this.getBiochemicalReactionList().get(reactionId);
			
			if(reaction.getHole()) {
				this.removeBioChemicalReaction(reactionId);
			}
		}
	}
	
	/**
	 * Remove all the reactions that contain generic metabolites
	 */
	public void removeGeneric() {
		
		Set<String> reactionIds = new HashSet<String>(this.getBiochemicalReactionList().keySet());
		
		for(String reactionId : reactionIds) {
			
			BioChemicalReaction reaction = this.getBiochemicalReactionList().get(reactionId);
			
			Boolean flag = false;
			
			for(BioPhysicalEntityParticipant leftP : reaction.getLeftParticipantList().values()) {
				if(leftP.getPhysicalEntity().getIsHolderClass()) {
					flag=true;
					break;
				}
			}
			
			if(! flag) {
				for(BioPhysicalEntityParticipant rightP : reaction.getRightParticipantList().values()) {
					if(rightP.getPhysicalEntity().getIsHolderClass()) {
						flag=true;
						break;
					}
				}
			}
			
			if(flag) {
				this.removeBioChemicalReaction(reactionId);
			}
		}
	}
	
	/**
	 * Remove the big molecule reactions considering the biocycClass flag
	 * of each reaction
	 */
	public void removeBigMoleculeReactions() {
		
		Set<String> reactionIds = new HashSet<String>(this.getBiochemicalReactionList().keySet());
		
		for(String reactionId : reactionIds) {
			
			BioChemicalReaction reaction =  this.getBiochemicalReactionList().get(reactionId);
			
			if(reaction.getBiocycClass().equalsIgnoreCase("macro")) {
				this.removeBioChemicalReaction(reactionId);
			}
		}
	}
	
	

	/**
	 * Remove in each reaction the metabolites flagged as side compounds
	 */
	public void removeSide() {
		
		Set<String> reactionIds = new HashSet<String>(this.getBiochemicalReactionList().keySet());
		
		for(String reactionId : reactionIds) {
			
			BioChemicalReaction reaction = this.getBiochemicalReactionList().get(reactionId);
			
			HashMap<String, BioPhysicalEntityParticipant> leftParticipants = new HashMap<String, BioPhysicalEntityParticipant>(reaction.getLeftParticipantList());
			
			for(BioPhysicalEntityParticipant bpe : leftParticipants.values()) {
				
				if(bpe.getIsPrimaryCompound() == false) {
					reaction.removeLeft(bpe.getPhysicalEntity().getId());
				}
				
			}
			
			HashMap<String, BioPhysicalEntityParticipant> rightParticipants = new HashMap<String, BioPhysicalEntityParticipant>(reaction.getRightParticipantList());

			for(BioPhysicalEntityParticipant bpe : rightParticipants.values()) {
				
				if(bpe.getIsPrimaryCompound() == false) {
					reaction.removeRight(bpe.getPhysicalEntity().getId());
				}
				
			}
			
			if(reaction.getLeftList().size()==0 || reaction.getRightList().size()==0) {
				this.removeBioChemicalReaction(reactionId);
			}
			
		}
	}
	
	/**
	 * In each reaction, remove the metabolite considered as cofactor
	 */
	public void removeCofactors() {
		
		Set<String> reactionIds = new HashSet<String>(this.getBiochemicalReactionList().keySet());
		
		for(String reactionId : reactionIds) {
			
			BioChemicalReaction reaction = this.getBiochemicalReactionList().get(reactionId);
			
			HashMap<String, BioPhysicalEntityParticipant> leftParticipants = new HashMap<String, BioPhysicalEntityParticipant>(reaction.getLeftParticipantList());
			
			for(BioPhysicalEntityParticipant bpe : leftParticipants.values()) {
				
				if(bpe.getIsCofactor() == true) {
					reaction.removeLeft(bpe.getPhysicalEntity().getId());
				}
				
			}
			
			HashMap<String, BioPhysicalEntityParticipant> rightParticipants = new HashMap<String, BioPhysicalEntityParticipant>(reaction.getRightParticipantList());

			for(BioPhysicalEntityParticipant bpe : rightParticipants.values()) {
				
				if(bpe.getIsCofactor() == true) {
					reaction.removeRight(bpe.getPhysicalEntity().getId());
				}
			}
			
			if(reaction.getLeftList().size()==0 || reaction.getRightList().size()==0) {
				this.removeBioChemicalReaction(reactionId);
			}
			
		}
	}
	
	/**
	 * Remove the reactions that are not involved in the pathways that are in the input set
	 * Do not remove the reactions that are not involved in any pathway
	 * @param pathwayIds
	 */
	public void filterByPathways(Set<String> pathwayIds) {
		
		Set<String> reactionIds = new HashSet<String>(this.getBiochemicalReactionList().keySet());
		
		for(String reactionId : reactionIds) {
			
			BioChemicalReaction reaction = this.getBiochemicalReactionList().get(reactionId);
			
			Set<String> pathwayReactionIds = reaction.getPathwayList().keySet();

			if(pathwayReactionIds.size()>0) {

				Boolean flag = true;

				for(String pathwayId : pathwayReactionIds) {
					if(pathwayIds.contains(pathwayId)) {
						flag=false;
						break;
					}
				}

				if(flag) {
					this.removeBioChemicalReaction(reactionId);
				}

			}
		}
	}
	
	/**
	 * 
	 * Remove the compounds that are not in the input set
	 */
	public void filterByMetabolites(Set<String> metaboliteIds) {
		
		Set<String> reactionIds = new HashSet<String>(this.getBiochemicalReactionList().keySet());
		
		for(String reactionId : reactionIds) {

			if(this.getBiochemicalReactionList().containsKey(reactionId)) {
				BioChemicalReaction reaction = this.getBiochemicalReactionList().get(reactionId);

				HashMap<String, BioPhysicalEntityParticipant> leftParticipants = new HashMap<String, BioPhysicalEntityParticipant>(reaction.getLeftParticipantList());

				for(BioPhysicalEntityParticipant bpe : leftParticipants.values()) {

					if(! metaboliteIds.contains(bpe.getPhysicalEntity().getId())) {
						reaction.removeLeft(bpe.getPhysicalEntity().getId());
						this.removeCompound(bpe.getPhysicalEntity().getId());
					}

				}

				HashMap<String, BioPhysicalEntityParticipant> rightParticipants = new HashMap<String, BioPhysicalEntityParticipant>(reaction.getRightParticipantList());

				for(BioPhysicalEntityParticipant bpe : rightParticipants.values()) {

					if(! metaboliteIds.contains(bpe.getPhysicalEntity().getId())) {
						reaction.removeRight(bpe.getPhysicalEntity().getId());
						this.removeCompound(bpe.getPhysicalEntity().getId());
					}
				}

				if(reaction.getLeftList().size()==0 && reaction.getRightList().size()==0) {
					this.removeBioChemicalReaction(reactionId);
				}
			}
		}
	}
	
	
	/**
	 * Filter the network by a list of reactions
	 * @param reactionIds
	 */
	public void filterByReactions(Set<String> reactionsToKeep) {
		
		HashSet<String> reactions = new HashSet<String>(this.getBiochemicalReactionList().keySet());		
		
		for(String reactionId : reactions) {
			if(! reactionsToKeep.contains(reactionId)) {
				this.removeBioChemicalReaction(reactionId);
			}
		}
		
		return;
	}
	
	/** 
	 * Remove all the reactions that are not in a pathway
	 */
	public void getOnlyPathwayReactions() {
		
		Set<String> reactionIds = new HashSet<String>(this.getBiochemicalReactionList().keySet());
		
		for(String reactionId : reactionIds) {
			
			BioChemicalReaction reaction = this.getBiochemicalReactionList().get(reactionId);
			
			if(reaction.getPathwayList().size()==0) {
				this.removeBioChemicalReaction(reactionId);
			}
		}
	}
	
	
	
	/**
	 * Returns the set of reactions catalysed by a gene
	 * 
	 * @param geneId : String 
	 * @return a Set of Strings
	 * TODO : test it
	 */
	public Set<String> getReactionsFromGene(String geneId) {
		
		Set<String> reactions = new HashSet<String>();
		
		for(BioChemicalReaction reaction : this.getBiochemicalReactionList().values()) {
			
			HashMap<String, BioGene> genes = reaction.getListOfGenes();
			
			if(genes.containsKey(geneId)) {
				reactions.add(reaction.getId());
			}
		}
		
		return reactions;
		
	}
	

	
	/**
	 * Reads a gene association written in a Palsson way.
	 * Creates genes if they don't exist.
	 * Create proteins and enzymes from genes.
	 * @param reactionId
	 * @param gpr
	 * @return true if no error, false otherwise
	 * TODO : test the gpr
	 */
	public Boolean setGeneAssociationFromString(String reactionId, String gpr) {
		
		Boolean flag = true;

		BioChemicalReaction rxn = this.getBiochemicalReactionList().get(reactionId);
		
		rxn.getEnzList().clear();
		rxn.getEnzrxnsList().clear();
		
		String[] tab;

		ArrayList<String[]> genesAssociated = new ArrayList<String[]>();

		if(! gpr.equals("") && ! gpr.equals("NA")) {

			if(gpr.contains(" or ")) {
				tab =  gpr.split(" or ");
			}
			else {
				tab = new String[1];
				tab[0] = gpr;
			}

			for(String genesAssociatedStr : tab) {

				genesAssociatedStr = genesAssociatedStr.replaceAll("[\\(\\)]", "");

				String[] tab2;

				if(genesAssociatedStr.contains(" and ")) {
					tab2 =  genesAssociatedStr.split(" and ");
				}
				else {
					tab2 = new String[1];
					tab2[0] = genesAssociatedStr;
				}

				int n = tab2.length;

				for(int k = 0; k < n; k++) {
					tab2[k] = tab2[k].replaceAll(" ", "");
				}

				genesAssociated.add(tab2);

			}
		}
		
		for(int k = 0; k < genesAssociated.size(); k++) {
    		String [] tabGenes = genesAssociated.get(k);
    		String enzymeId = StringUtils.implode(tabGenes, "_and_");
    		
    		BioComplex enzyme;

			if(!this.getComplexList().containsKey(enzymeId)) {
				enzyme = new BioComplex(enzymeId, enzymeId);

				this.addComplex(enzyme);
			}

			enzyme = this.getComplexList().get(enzymeId);

			rxn.addEnz(enzyme);
			
			BioProtein protein;

			if(!this.getProteinList().containsKey(enzymeId)) {
				protein = new BioProtein(enzymeId, enzymeId);
				this.addProtein(protein);
			}
			protein = this.getProteinList().get(enzymeId);
			enzyme.addComponent(new BioPhysicalEntityParticipant(protein));

			for(int u=0; u<tabGenes.length;u++) {
				String geneId = tabGenes[u];
				BioGene gene;
				if(!this.getGeneList().containsKey(geneId)) {
					gene = new BioGene(geneId, geneId);
					this.addGene(gene);
				}

				gene = this.getGeneList().get(geneId);

				protein.addGene(gene);
			}
    	}
		

		return flag;

	}
	
	/**
	 * 
	 * Iteratively removes the dead reactions
	 * 
	 * @return
	 */
	public Collection<BioChemicalReaction> trim()
	{
		HashSet<BioChemicalReaction> allRemovedReactions=new HashSet<BioChemicalReaction>();
		Collection<BioChemicalReaction> removed=removeOrphanReactions();
		while(!removed.isEmpty())
		{
			allRemovedReactions.addAll(removed);
			removed=removeOrphanReactions();
		}
		return allRemovedReactions;
	}
	
	/**
	 * 
	 * @return
	 */
	private Collection<BioChemicalReaction> removeOrphanReactions()
	{
		HashSet<BioChemicalReaction> removedReactions=new HashSet<BioChemicalReaction>();
		
		HashMap<String, BioPhysicalEntity> orphans = this.getOrphanMetabolites();
		
		
		for(BioPhysicalEntity metabolite:orphans.values())
		{
			
			HashMap<String, BioChemicalReaction> reactionsP = new HashMap<String, BioChemicalReaction>(metabolite.getReactionsAsProduct());
			for(BioChemicalReaction reaction : reactionsP.values())
			{
				if(this.getBiochemicalReactionList().containsKey(reaction.getId())) {
					removedReactions.add(reaction);
					this.removeBioChemicalReaction(reaction.getId());
				}
			}
			
			
			HashMap<String, BioChemicalReaction> reactionsS = new HashMap<String, BioChemicalReaction>(metabolite.getReactionsAsSubstrate());

			for(BioChemicalReaction reaction : reactionsS.values())
			{
				if(this.getBiochemicalReactionList().containsKey(reaction.getId())) {
					removedReactions.add(reaction);
					this.removeBioChemicalReaction(reaction.getId());
				}
			}		
		}
		
		return removedReactions;
	}
	
	/**
	 * Methods inspired from Surrey FBA, the aim is to get the orphan metabolites.
	 * An orphan is an internal metabolite (boundaryCondition==false) and not produced or not consumed
	 * 
	 * @return
	 */
	public HashMap<String, BioPhysicalEntity> getOrphanMetabolites()
	{
		HashMap<String, BioPhysicalEntity> orphanMetabolites=new HashMap<String, BioPhysicalEntity>();
		
		for(BioPhysicalEntity cpd : this.getPhysicalEntityList().values()) {
			if(! cpd.getBoundaryCondition()) {
				HashMap<String, BioChemicalReaction> reactions = new HashMap<String, BioChemicalReaction>();
				
//				HashMap<String, BioChemicalReaction> reactionsP = this.getListOfReactionsAsProduct(cpd.getId());
				HashMap<String, BioChemicalReaction> reactionsP = cpd.getReactionsAsProduct();
//				HashMap<String, BioChemicalReaction> reactionsS = this.getListOfReactionsAsSubstrate(cpd.getId());
				HashMap<String, BioChemicalReaction> reactionsS = cpd.getReactionsAsSubstrate();
				
				reactions.putAll(reactionsP);
				reactions.putAll(reactionsS);
				
				Set<String> rp = reactionsP.keySet();
				rp.retainAll(this.getBiochemicalReactionList().keySet());
				Set<String> rs = reactionsS.keySet();
				rs.retainAll(this.getBiochemicalReactionList().keySet());

				
				Set<String> rxns = reactions.keySet();
				
				rxns.retainAll(this.getBiochemicalReactionList().keySet());
				
				if(rxns.size()<2) {
					orphanMetabolites.put(cpd.getId(), cpd);
				}
				else {
					if(rp.size()==0 || rs.size()==0) {
						orphanMetabolites.put(cpd.getId(), cpd);
					}
				}
			}
		}
		
		return orphanMetabolites;
	}
	

	/**
	 * Create exchange reactions for each orphan metabolite
	 * 
	 * @param withExternal. Boolean. if true, create an external metabolite
	 * @param suffix to add at the end of the external metabolite
	 * @compartmentId : the id of the compartment in which the external metabolites will be added
	 */
	public void addExchangeReactionsToOrphans(Boolean withExternal, String suffix, String compartmentId) {
		
		HashMap<String, BioPhysicalEntity> orphans = this.getOrphanMetabolites();
		
		for(BioPhysicalEntity orphan : orphans.values()) {
			
			this.addExchangeReactionToMetabolite(orphan.getId(), withExternal, suffix, compartmentId);
			
		}
	}
	
	/**
	 * Adds an exchange reaction to a metabolite
	 * @param cpdId
	 * @param withExternal
	 * @param suffix
	 * @param compartmentId
	 */
	public String addExchangeReactionToMetabolite(String cpdId, Boolean withExternal, String suffix, String compartmentId) {
		
		if(! this.getPhysicalEntityList().containsKey(cpdId)) {
			return null;
		}
		
		BioChemicalReaction rxn = new BioChemicalReaction(this.getNewReactionId("R_EX_"));
		
		if(withExternal) {
			BioPhysicalEntity cpd = new BioPhysicalEntity(this.getNewMetaboliteId("M_", suffix));
			
			BioCompartment compartment;
			
			if(this.getCompartments().containsKey(compartmentId)) {
				compartment = this.getCompartments().get(compartmentId);
			}
			else {
				if(compartmentId==null || compartmentId.equals("")) {
					compartmentId = "NA";
				}
				compartment = new BioCompartment(compartmentId, compartmentId);
				
				this.addCompartment(compartment);
				
			}

			cpd.setBoundaryCondition(true);
			cpd.setCompartment(compartment);
			
			rxn.addLeftParticipant(new BioPhysicalEntityParticipant(cpd));
		}
		rxn.addRightParticipant(new BioPhysicalEntityParticipant(this.getPhysicalEntityList().get(cpdId)));
		rxn.setReversibility(true);
		
		this.addBiochemicalReaction(rxn);
		
		return rxn.getId();
		
	}
	
	
	/**
	 * Compute the choke point reactions
	 * A "chokepoint reaction" is defined as a reaction that 
	 * either uniquely consumes a specific metabolite
	 * or uniquely produces a specific metabolite
	 * 
	 * @return the set of choke point reaction identifiers
	 */
	public Set<String> getChokeReactions() {
		
		Set<String> chokes = new HashSet<String>();
		
		for(BioChemicalReaction rxn : this.getBiochemicalReactionList().values()) {
			
			HashMap<String, BioPhysicalEntity> substrates = rxn.getListOfSubstrates();
			
			Boolean isChoke=false;
			
			for(BioPhysicalEntity substrate : substrates.values()) {
				
				HashMap<String, BioChemicalReaction> rs = this.getListOfReactionsAsSubstrate(substrate.getId());
				
				rs.remove(rxn.getId());
				
				if(rs.size()==0) {
					HashMap<String, BioChemicalReaction> rp = this.getListOfReactionsAsProduct(substrate.getId());
					
					rp.remove(rxn.getId());
					
					if(rp.size()>0) {
						isChoke=true;
						break;
					}
				}
			}
			
			if(! isChoke) {
				HashMap<String, BioPhysicalEntity> products = rxn.getListOfProducts();
				
				for(BioPhysicalEntity product : products.values()) {
					
					HashMap<String, BioChemicalReaction> rp = this.getListOfReactionsAsProduct(product.getId());
					
					rp.remove(rxn.getId());
					
					if(rp.size()==0) {
						HashMap<String, BioChemicalReaction> rs = this.getListOfReactionsAsSubstrate(product.getId());

						rs.remove(rxn.getId());

						if(rs.size()>0) {
							isChoke=true;
							break;
						}
					}
				}
			}
			
			if(isChoke) {
				chokes.add(rxn.getId());
			}
		}
		
		return chokes;
		
	}
	
	/**
	 * Compute the choke point metabolites
	 * A "chokepoint metabolite" is defined as a metabolite that is
	 *  either uniquely consumed by a specific reaction or uniquely produced by a specific reaction.
	 * 
	 * @return the set of choke point metabolite identifiers
	 */
	public Set<String> getChokeMetabolites() {
		
		Set<String> chokes = new HashSet<String>();
		
		for(BioPhysicalEntity cpd : this.getPhysicalEntityList().values()) {
			
			Boolean flag = false;

			HashMap<String, BioChemicalReaction> rs = this.getListOfReactionsAsSubstrate(cpd.getId());

			if(rs.size()==1) {
				flag = true;
			}
			else {
				HashMap<String, BioChemicalReaction> rp = this.getListOfReactionsAsProduct(cpd.getId());
				if(rp.size()==1) {
					flag = true;
				}
			}
			
			if(flag) {
				chokes.add(cpd.getId());
			}
		}
		
		return chokes;
		
	}
	
	
	/**
	 * Returns an array with two codes corresponding to the local topology of a metabolite in the network
	 * @param cpdId
	 * @return an array with the first element corresponding to :
	 * 		-1 : not in the network
	 * 		0 : not a choke point
	 * 		1 : choke point not consumed
	 * 		2 : choke point not produced
	 * 		3 : choke point consumed by one reaction and produced by one reaction
	 * 		
	 * 		The second element corresponds to 
	 * 			-1 : not in the network
	 * 			0 : in a path
	 * 			1 : source
	 * 			2 : dead-end
	 * 			3 : source or dead-end
	 * 			4 : isolated metabolite
	 */
	public ArrayList<Integer> getLocalTopology(String cpdId) {
		
		ArrayList<Integer> res = new ArrayList<Integer>();
		
		res.add(0);
		res.add(0);
		
		if(! this.getPhysicalEntityList().containsKey(cpdId)) {
			res.set(0, -1);
			res.set(1, -1);
			return res;
		}
		
		HashMap<String, BioChemicalReaction> rs = this.getListOfReactionsAsSubstrate(cpdId);
		HashMap<String, BioChemicalReaction> rp = this.getListOfReactionsAsProduct(cpdId);
		
		if(rp.size() == 1 && rs.size() != 1) {
			res.set(0, 1);
		}
		else if(rp.size() != 1 && rs.size() == 1) {
			res.set(0, 2);
		}
		else if(rp.size() == 1 && rs.size() == 1) {
			res.set(0, 3);
		}
		else {
			res.set(0, 0);
		}
		
		
		if(rs.size()==0 && rp.size()==0) {
			res.set(1, 4);
		}
		else if(rs.size()>0 && rp.size()>0) {
			if(rs.size()==1 && rp.size()==1 && rs.equals(rp)) {
				res.set(1, 3);
			}
			else {
				res.set(1, 0);
			}
		}
		else if(rs.size()>0 && rp.size()==0) {
			res.set(1, 1);
		}
		else if(rp.size()>0 && rs.size()==0) {
			res.set(1, 2);
		}
		
		
		return res;
	}
	
	
	/**
	 * Returns the list of pathways where a compound is involved
	 * @param cpdId
	 * @return a HashMap<String, BioPathway> 
	 */
	public HashMap<String, BioPathway> getPathwaysOfCompound(String cpdId) {
		
		HashMap<String, BioPathway> pathways = new HashMap<String, BioPathway>();
		
		if(this.getPhysicalEntityList().containsKey(cpdId)) {
			
			HashMap<String, BioChemicalReaction> rs = this.getListOfReactionsAsSubstrate(cpdId);
			HashMap<String, BioChemicalReaction> rp = this.getListOfReactionsAsProduct(cpdId);

			HashMap<String, BioChemicalReaction> reactions = new HashMap<String, BioChemicalReaction>(rs);
			reactions.putAll(rp);
			
			for(BioChemicalReaction r : reactions.values()) {
				pathways.putAll(r.getPathwayList());
			}
		}
		
		return pathways;
	}
	
	
	/**
	 * Replace all the  compartments by one compartment "NA"
	 * If the suffix is "_IN_*", it is removed
	 * 
	 */
	public void removeCompartments() {
		
		// We replace all the compartments by one not specified
		BioCompartment cptNA = new BioCompartment("NA", "NA");
		this.setCompartments(new HashMap<String, BioCompartment>());
		this.addCompartment(cptNA);

		HashMap<String, BioPhysicalEntity> cpds = new HashMap<String, BioPhysicalEntity>(this.getPhysicalEntityList());

		for(BioPhysicalEntity cpd : cpds.values()) {

			this.getPhysicalEntityList().remove(cpd.getId());

			String newId = cpd.getId().replaceAll("_IN_.*", "");

			cpd.setId(newId);

			cpd.setCompartment(cptNA);

			this.addPhysicalEntity(cpd);

		}
		
	}
	
	/**
	 * Tests an objective function
	 * @param obj
	 * @return
	 */
	public Boolean testObjectiveFunction(String obj) {
		
		Boolean flag = true;
		
		// The objective function has the following format : 1 R1 + 2.5 R3 
		
		String tab[] = obj.split(" \\+ ");
				
		for(String member : tab) {
			
			String tab2[] = member.split(" ");
			
			String reactionId;
			
			if(tab2.length == 1) {
				reactionId = tab2[0];
			}
			else if(tab2.length==2) {
				reactionId = tab2[1];
				String coeff = tab2[0];
				
				try {
					Double.parseDouble(coeff);
				} catch (NumberFormatException e) {
					System.err.println(coeff+" is not a double");
					return false;
				}
				
			}
			else {
				System.err.println("Objective function badly formatted");
				return false;
			}
			
			if(! this.getBiochemicalReactionList().containsKey(reactionId)) {
				System.err.println("The reaction "+reactionId+ " is not in the network");
				return false;
			}
			
			
		}
		
		return flag;
		
	}
	
	/**
	 * Returns the list of exchange reactions for a metabolite
	 * @param cpdId
	 * @return
	 */
	public HashMap<String, BioChemicalReaction> getExchangeReactionsOfMetabolite(String cpdId) {
		
		HashMap<String, BioChemicalReaction> reactions = new HashMap<String, BioChemicalReaction>();
		HashMap<String, BioChemicalReaction> ex_reactions = new HashMap<String, BioChemicalReaction>();
		
		if(! this.getPhysicalEntityList().containsKey(cpdId)) {
			System.err.println("[Warning] getExchangeReactionsOfMetabolite : "+cpdId+" is not in the network !");
			return ex_reactions;
		}
		
		reactions.putAll(this.getListOfReactionsAsProduct(cpdId));
		reactions.putAll(this.getListOfReactionsAsSubstrate(cpdId));
		
		for(BioChemicalReaction reaction : reactions.values()) {
			
			if(reaction.isExchangeReaction()) {
				ex_reactions.put(reaction.getId(), reaction);
			}
			
		}
		
		return ex_reactions;
		
	}
	
	
	/**
	 * Compute the atom balances for all the reactions
	 * @return
	 */
	public HashMap<String, HashMap<String, Double>> computeBalanceAllReactions() {
		
		HashMap<String, HashMap<String, Double>> balances = new HashMap<String, HashMap<String,Double>>();
		
		for(BioChemicalReaction rxn : this.getBiochemicalReactionList().values()) {
			
			String id = rxn.getId();
			
			
			HashMap<String, Double> balance = rxn.computeAtomBalances();
			
			balances.put(rxn.getId(), balance);
		}
		
		return balances;
		
		
	}
	
	
	
}
