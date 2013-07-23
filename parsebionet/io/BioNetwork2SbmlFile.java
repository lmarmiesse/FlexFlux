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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import parsebionet.biodata.BioChemicalReaction;
import parsebionet.biodata.BioCompartment;
import parsebionet.biodata.BioComplex;
import parsebionet.biodata.BioGene;
import parsebionet.biodata.BioNetwork;
import parsebionet.biodata.BioPathway;
import parsebionet.biodata.BioPhysicalEntity;
import parsebionet.biodata.BioPhysicalEntityParticipant;
import parsebionet.biodata.BioProtein;
import parsebionet.biodata.Comment;
import parsebionet.biodata.Flux;
import parsebionet.biodata.UnitDefinition;
import parsebionet.biodata.UnitSbml;
import parsebionet.utils.StringUtils;



public class BioNetwork2SbmlFile extends BioNetwork2File {

	// To write extended sbml format
	private Boolean extended = false;



	public BioNetwork2SbmlFile() {
		super(null, null, null, null);
	}

	public BioNetwork2SbmlFile(BioNetwork bioNetwork, Boolean onlyPrimaries, Boolean keepHolderClassCompounds, String fileName) {
		super(bioNetwork, onlyPrimaries, keepHolderClassCompounds, fileName);

		this.setExtended(false);
	}


	public BioNetwork2SbmlFile(BioNetwork bioNetwork, Boolean onlyPrimaries, Boolean keepHolderClassCompounds, String fileName, Boolean extended) {
		super(bioNetwork, onlyPrimaries, keepHolderClassCompounds, fileName);

		this.setExtended(extended);
	}


	public void write() throws IOException{

		writeBegin();
		writeUnitDefinitions();
		writeCompartments();

		writeCpds();
		writeReactions();
		writeEnd();

		this.getWriter().close();
	}


	/**
	 * 
	 * @throws IOException
	 */
	private void writeBegin() throws IOException{
		String idKb = this.getBioNetwork().getId();
		String nameKb = this.getBioNetwork().getName();
		this.getWriter().write("<?xml version=\"1.0\"  encoding=\"UTF-8\"?>\n");
		this.getWriter().write("<sbml xmlns=\"http://www.sbml.org/sbml/level2\" version=\"1\" level=\"2\" xmlns:html=\"http://www.w3.org/1999/xhtml\">\n");
		this.getWriter().write("<model id=\""+idKb+"\" name=\""+nameKb+"\">\n");
	}

	/**
	 * 
	 * @throws IOException
	 */
	private void writeUnitDefinitions() throws IOException{

		HashMap<String, UnitDefinition> unitDefinitions = this.getBioNetwork().getUnitDefinitions();

		if(unitDefinitions.size() > 0) {
			this.getWriter().write("  <listOfUnitDefinitions>\n");

			for(UnitDefinition ud : unitDefinitions.values()) {

				String udId = ud.getId();
				String udName = ud.getName();

				this.getWriter().write("    <unitDefinition id=\""+udId+"\"");
				if(udName != "") {
					this.getWriter().write(" name=\""+udName+"\"");
				}

				this.getWriter().write(">\n");

				HashMap<String, UnitSbml> units = ud.getUnits();

				if(units.size()>0) {


					this.getWriter().write("      <listOfUnits>\n");

					for(UnitSbml unit : units.values()) {

						String kind= unit.getKind();
						String scale = unit.getScale();
						String exponent = unit.getExponent();
						String multiplier = unit.getMultiplier();

						this.getWriter().write("        <unit kind=\""+kind+"\"");
						if(scale != "") {
							this.getWriter().write(" scale=\""+scale+"\"");
						}
						if(exponent != "") {
							this.getWriter().write(" exponent=\""+exponent+"\"");
						}
						if(multiplier != "") {
							this.getWriter().write(" multiplier=\""+multiplier+"\"");
						}

						this.getWriter().write("/>\n");
					}
					this.getWriter().write("      </listOfUnits>\n");
				}
				this.getWriter().write("    </unitDefinition>\n");

			}

			this.getWriter().write("  </listOfUnitDefinitions>\n");

		}	


	}

	private void writeCompartments() throws IOException {
		this.getWriter().write("<listOfCompartments>\n");

		for(BioCompartment compartment : this.getBioNetwork().getCompartments().values()) {
			String compartmentId = StringUtils.sbmlEncode(compartment.getId());
			String compartmentName = StringUtils.htmlEncode(compartment.getName());
			BioCompartment outsideCompartment = compartment.getOutsideCompartment();

			this.getWriter().write("<compartment id=\""+compartmentId+"\" name =\""+compartmentName+"\"");
			if(outsideCompartment != null) {
				if(outsideCompartment.getId().equals("")) {
					this.getWriter().write(" outside=\""+StringUtils.sbmlEncode(outsideCompartment.getId())+"\"");
				}
			}
			this.getWriter().write(" />\n");
		}

		this.getWriter().write("</listOfCompartments>\n");
	}

	private void writeCpds() throws IOException {


		this.getWriter().write("<listOfSpecies>\n");

		for(Iterator<String> iter = this.getListOfSpecies().keySet().iterator(); iter.hasNext();) {
			BioPhysicalEntity cpd = this.getListOfSpecies().get(iter.next());
			writeCpd(cpd);
		}

		this.getWriter().write("</listOfSpecies>\n");

	}

	private void writeCpd(BioPhysicalEntity cpd) throws IOException {

		String id = StringUtils.sbmlEncode(cpd.getId());
		String name = StringUtils.htmlEncode(cpd.getName());
		String compartmentId = StringUtils.sbmlEncode(cpd.getCompartment().getId());

		String boundaryCondition = "false";

		if(cpd.getBoundaryCondition()==true) {
			boundaryCondition = "true";
		}


		this.getWriter().write("  <species id=\""+id+"\" name=\""+name+"\" compartment=\""+compartmentId+"\" boundaryCondition=\""+boundaryCondition+"\"");

		if(this.getExtended()==true) {
			String generic="false";
			if(cpd.getIsHolderClass()) {
				generic = "true";
			}
			this.getWriter().write(" mass=\""+cpd.getMolecularWeight()+"\" formula=\""+cpd.getChemicalFormula()+"\" generic=\""+generic+"\""); 
		}

		this.getWriter().write(" />\n");


	}

	private void writeReactions() throws IOException { 
		this.getWriter().write("<listOfReactions>\n");
		for(Iterator<String> iter=this.getListOfReactions().keySet().iterator(); iter.hasNext(); ) {
			String reversibility;
			BioChemicalReaction reaction = this.getListOfReactions().get(iter.next());
			String infoReversibility = reaction.getReversiblity();

			HashMap<String, BioPhysicalEntityParticipant> left;
			HashMap<String, BioPhysicalEntityParticipant> right;

			if(infoReversibility.compareToIgnoreCase("irreversible-right-to-left") == 0 ||
					infoReversibility.compareToIgnoreCase("irreversible-left-to-right") == 0) {
				reversibility = "false";
			}
			else {
				reversibility = "true";
			}

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


			if(this.getExtended()==false) {
				this.getWriter().write("  <reaction id=\""+StringUtils.sbmlEncode(reaction.getId())+"\" name=\""+StringUtils.htmlEncode(reaction.getName())+"\" reversible=\""+reversibility+"\">\n");
			}
			else {
				// Write the EC number

				String ec = reaction.getEcNumber();

				String holder = "false";
				if(reaction.getIsGenericReaction())
					holder = "true";

				String type = reaction.getBiocycClass();
				if(!(type.compareTo("small") == 0 || type.compareTo("macro") == 0)) {
					type = "NA";
				}

				String hole="false";

				// indicate if the reaction is a hole or not
				if(! reaction.isPossible()) {
					hole="true";
				}

				this.getWriter().write("  <reaction id=\""+StringUtils.sbmlEncode(reaction.getId())+"\" name=\""+StringUtils.htmlEncode(reaction.getName())+"\" reversible=\""+reversibility+"\" ec=\""+ec+"\" hole=\""+hole+"\" generic=\""+holder+"\" type=\""+type+"\" >\n");
				
				// Write score and status
				this.getWriter().write("    <score>"+reaction.getScore()+"</score>\n");
				this.getWriter().write("    <status>"+reaction.getStatus()+"</status>\n");
				
				HashSet<String> pmids = reaction.getPmids();
				
				for(String pmid : pmids) {
					this.getWriter().write("    <pmid>"+pmid+"</pmid>\n");
				}
				
				
				// Write comments
				for(Comment comment : reaction.getUserComments()) {
					this.getWriter().write("    <comment>\n");
					this.getWriter().write("      <annotator>"+comment.getUserIdentifier()+"</annotator>\n");
					this.getWriter().write("      <text>\n        <![CDATA["+comment.getComment()+"]]>\n      </text>\n");
					this.getWriter().write("    </comment>\n");
				}
				
			}

			if(this.getExtended() == false) {

				this.getWriter().write("    <notes>\n");

				String geneStr = "";
				String protStr = "";

				if(reaction.getSpontaneous() != null) {
					geneStr = "      <html:p>GENE_ASSOCIATION: NA</html:p>\n";
					protStr = "      <html:p>PROTEIN_ASSOCIATION: NA</html:p>\n";
				}
				else {

					//					Writing the associations with the proteins and the genes corresponding to the reaction
					HashMap<String, BioPhysicalEntity> enzList = reaction.getEnzList();

					geneStr = "      <html:p>GENE_ASSOCIATION: ";
					protStr = "      <html:p>PROTEIN_ASSOCIATION: ";

					int j=0;

					for(Iterator<String> iterEnz = enzList.keySet().iterator(); iterEnz.hasNext();){
						j++;

						if(j > 1) {
							protStr = protStr+" or ";
							geneStr = geneStr+" or ";
						}

						BioPhysicalEntity enzyme = enzList.get(iterEnz.next());
						String classe = enzyme.getClass().getSimpleName();

						HashMap<String, BioGene> listOfGenes = new HashMap<String, BioGene>();

						HashMap<String, BioProtein> listOfProteins = new HashMap<String, BioProtein>();

						if(classe.compareTo("BioProtein") == 0) {
							listOfProteins.put(enzyme.getId(), (BioProtein)enzyme);

							listOfGenes = ((BioProtein)enzyme).getGeneList();

						}
						else if (classe.compareTo("BioComplex") == 0) {

							listOfGenes = ((BioComplex)enzyme).getGeneList();

							HashMap <String, BioPhysicalEntity> componentList = ((BioComplex)enzyme).getAllComponentList();

							for(Iterator<String> iterComponent = componentList.keySet().iterator(); iterComponent.hasNext(); ) {

								BioPhysicalEntity component = componentList.get(iterComponent.next());

								if(component.getClass().getSimpleName().compareTo("BioProtein") == 0) {
									listOfProteins.put(component.getId(), (BioProtein)component);
								}

							}
						}
						int k = 0;

						geneStr = geneStr + "( ";

						for(Iterator<String> iterGene = listOfGenes.keySet().iterator(); iterGene.hasNext(); ) {
							k ++;

							if(k > 1) {
								geneStr = geneStr+ " and "; 
							}

							BioGene gene = listOfGenes.get(iterGene.next());

							geneStr = geneStr + StringUtils.htmlEncode(gene.getName());
						}

						geneStr = geneStr + " )";


						protStr = protStr + "( ";

						k = 0;

						for(Iterator<String> iterProt = listOfProteins.keySet().iterator(); iterProt.hasNext();) {
							k ++;

							if(k > 1) {
								protStr = protStr+ " and "; 
							}

							BioProtein prot = listOfProteins.get(iterProt.next());

							protStr = protStr +StringUtils.htmlEncode(prot.getName());
						}

						protStr = protStr + " )";

					}
					geneStr = geneStr+"</html:p>\n";
					protStr = protStr+"</html:p>\n";

				}


				this.getWriter().write(geneStr);
				this.getWriter().write(protStr);


				// Write the pathway where occurs the reaction

				HashMap<String, BioPathway> pathwaysList = reaction.getPathwayList();

				if(pathwaysList.size() == 0) {
					this.getWriter().write("      <html:p>SUBSYSTEM: NA</html:p>\n");
				} else {
					for(Iterator<String> iterPathway = pathwaysList.keySet().iterator(); iterPathway.hasNext(); ) {
						BioPathway pathway = pathwaysList.get(iterPathway.next());

						this.getWriter().write("      <html:p>SUBSYSTEM: "+StringUtils.htmlEncode(pathway.getName())+"</html:p>\n");
					}
				}
				// Write the EC number

				String ec = reaction.getEcNumber();

				this.getWriter().write("      <html:p>PROTEIN_CLASS: "+ec+"</html:p>\n");

				// Write the list of cofactors if they exist
				Set<String> cofactors = new HashSet<String>();

				for(BioPhysicalEntityParticipant cpdP : left.values()) {
					if(cpdP.getIsCofactor() == true) {
						cofactors.add(cpdP.getPhysicalEntity().getId());
					}
				}

				for(BioPhysicalEntityParticipant cpdP : right.values()) {
					if(cpdP.getIsCofactor() == true) {
						cofactors.add(cpdP.getPhysicalEntity().getId());
					}
				}

				for(String cpd : cofactors) {
					this.getWriter().write("      <html:p>COFACTOR: "+StringUtils.sbmlEncode(cpd)+"</html:p>\n");
				}

				cofactors.addAll(reaction.getCofactors());

				Set<String> sides = new HashSet<String>();

				// Write the compounds reported as side-compounds in BioCyc
				if(reaction.getPrimaryLeftList().size() > 0 || reaction.getPrimaryRightList().size() >0) {

					for(String cpd : reaction.getLeftList().keySet()) {
						if(reaction.getPrimaryLeftList().containsKey(cpd) == false) {
							sides.add(cpd);
						}
					}

					for(String cpd : reaction.getRightList().keySet()) {
						if(reaction.getPrimaryRightList().containsKey(cpd) == false) {
							sides.add(cpd);
						}
					}
				}

				sides.addAll(reaction.getSideCompounds());

				for(String cpd : sides) {
					this.getWriter().write("      <html:p>SIDE: "+StringUtils.sbmlEncode(cpd)+"</html:p>\n");
				}


				String holder = "false";
				if(reaction.getIsGenericReaction())
					holder = "true";

				this.getWriter().write("      <html:p>GENERIC: "+holder+"</html:p>\n");

				String type = reaction.getBiocycClass();
				if(type.compareTo("") != 0) {
					this.getWriter().write("      <html:p>TYPE: "+type+"</html:p>\n");
				}

				// indicate if the reaction is a hole or not
				if(reaction.isPossible()) {
					this.getWriter().write("      <html:p>HOLE: "+false+"</html:p>\n");
				}
				else {
					this.getWriter().write("      <html:p>HOLE: "+true+"</html:p>\n");
				}

				
				// Writes the score and the status
				this.getWriter().write("      <html:p>SCORE: "+reaction.getScore()+"</html:p>\n");
				this.getWriter().write("      <html:p>STATUS: "+reaction.getStatus()+"</html:p>\n");
				
				HashSet<String> pmids = reaction.getPmids();
				
				for(String pmid : pmids) {
					this.getWriter().write("      <html:p>PMID: "+pmid+"</html:p>\n");
				}
				
				// Write comments
				for(Comment comment : reaction.getUserComments()) {
					this.getWriter().write("      <html:p>COMMENT:"+comment.getUserIdentifier()+":\n");
					this.getWriter().write("        <![CDATA["+comment.getComment()+"]]>\n");
					this.getWriter().write("      </html:p>\n");
				}
				
				
				this.getWriter().write("    </notes>\n");
			}
			else {
				// Extended home version of sbml

				//				Writing the associations with the proteins and the genes corresponding to the reaction
				HashMap<String, BioPhysicalEntity> enzList = reaction.getEnzList();

				for(Iterator<String> iterEnz = enzList.keySet().iterator(); iterEnz.hasNext();){

					BioPhysicalEntity enzyme = enzList.get(iterEnz.next());
					String classe = enzyme.getClass().getSimpleName();

					this.getWriter().write("    <enzyme id=\""+StringUtils.sbmlEncode(enzyme.getId())+"\" name=\""+StringUtils.htmlEncode(enzyme.getName())+"\">\n"); 

					HashMap<String, BioProtein> listOfProteins = new HashMap<String, BioProtein>();

					if(classe.compareTo("BioProtein") == 0) {
						listOfProteins.put(enzyme.getId(), (BioProtein)enzyme);
					}
					else if (classe.compareTo("BioComplex") == 0) {

						HashMap <String, BioPhysicalEntity> componentList = ((BioComplex)enzyme).getAllComponentList();

						for(Iterator<String> iterComponent = componentList.keySet().iterator(); iterComponent.hasNext(); ) {

							BioPhysicalEntity component = componentList.get(iterComponent.next());

							if(component.getClass().getSimpleName().compareTo("BioProtein") == 0) {
								listOfProteins.put(component.getId(), (BioProtein)component);
							}

						}
					}

					for(Iterator<String> iterProt = listOfProteins.keySet().iterator(); iterProt.hasNext();) {

						BioProtein prot = listOfProteins.get(iterProt.next());

						this.getWriter().write("      <protein id=\""+StringUtils.sbmlEncode(prot.getId())+"\" name=\""+StringUtils.htmlEncode(prot.getName())+"\">\n"); 

						for(BioGene gene : prot.getGeneList().values()) {
							this.getWriter().write("        <gene id=\""+StringUtils.sbmlEncode(gene.getId())+"\" name=\""+StringUtils.htmlEncode(gene.getName())+"\" />\n"); 
						}

						this.getWriter().write("      </protein>\n");

					}

					this.getWriter().write("    </enzyme>\n");


				}

				// Write the pathway where occurs the reaction

				HashMap<String, BioPathway> pathwaysList = reaction.getPathwayList();

				for(BioPathway pathway : pathwaysList.values()) {

					this.getWriter().write("    <pathway id=\""+StringUtils.sbmlEncode(pathway.getId())+"\" name=\""+StringUtils.htmlEncode(pathway.getName())+"\" />\n");

				}

				// Write the list of cofactors and sides if they exist
				Set<String> cofactors = new HashSet<String>();
				Set<String> sides = new HashSet<String>();



				for(BioPhysicalEntityParticipant cpdP : left.values()) {
					if(cpdP.getIsCofactor() == true) {
						cofactors.add(cpdP.getPhysicalEntity().getId());
					}
				}

				for(BioPhysicalEntityParticipant cpdP : right.values()) {
					if(cpdP.getIsCofactor() == true) {
						cofactors.add(cpdP.getPhysicalEntity().getId());
					}
				}

				if(cofactors.size()>0) {

					this.getWriter().write("    <cofactors>\n");

					for(String cpd : cofactors) {
						this.getWriter().write("      <speciesReference species=\""+StringUtils.sbmlEncode(cpd)+"\" />\n");
					}

					this.getWriter().write("    </cofactors>\n");

				}


				// Write the compounds reported as side-compounds in BioCyc
				//				if(reaction.getPrimaryLeftParticipantList().size() > 0 || reaction.getPrimaryRightParticipantList().size() >0) {

				for(BioPhysicalEntityParticipant bp : reaction.getLeftParticipantList().values()) {
					if(bp.getIsPrimaryCompound()==false)
						sides.add(bp.getPhysicalEntity().getId());
				}

				for(BioPhysicalEntityParticipant bp : reaction.getRightParticipantList().values()) {
					if(bp.getIsPrimaryCompound()==false)
						sides.add(bp.getPhysicalEntity().getId());
				}
				//				}


				if(sides.size()>0) {

					this.getWriter().write("    <side-compounds>\n");

					for(String cpd : sides) {
						this.getWriter().write("      <speciesReference species=\""+StringUtils.sbmlEncode(cpd)+"\" />\n");
					}

					this.getWriter().write("    </side-compounds>\n");

				}

			}



			// Writing the left and right compounds
			Iterator<String> iterCpd;
			
			if(left.size()>0) {
				this.getWriter().write("    <listOfReactants>\n");

				for(iterCpd = left.keySet().iterator(); iterCpd.hasNext(); ) {
					BioPhysicalEntityParticipant cpd = left.get(iterCpd.next());
					String idCpd = StringUtils.sbmlEncode(cpd.getPhysicalEntity().getId());
					String stoe = cpd.getStoichiometricCoefficient();
					this.getWriter().write("      <speciesReference species=\""+idCpd+"\" stoichiometry=\""+stoe+"\"/>\n");
				}

				this.getWriter().write("    </listOfReactants>\n");
			}

			if(right.size()>0) {
				this.getWriter().write("    <listOfProducts>\n");

				for(iterCpd = right.keySet().iterator(); iterCpd.hasNext(); ) {
					BioPhysicalEntityParticipant cpd = right.get(iterCpd.next());
					String idCpd = StringUtils.sbmlEncode(cpd.getPhysicalEntity().getId());
					String stoe = cpd.getStoichiometricCoefficient();

					this.getWriter().write("      <speciesReference species=\""+idCpd+"\" stoichiometry=\""+stoe+"\"/>\n");
				}

				this.getWriter().write("    </listOfProducts>\n");
			}
			// Write flux informations

			if(this.getBioNetwork().getUnitDefinitions().size()>0) {


				Flux lb = reaction.getLowerBound();
				Flux ub = reaction.getUpperBound();

				String lbValue;
				String ubValue;

				String lbUnits="mmol_per_gDW_per_hr";
				String ubUnits = "mmol_per_gDW_per_hr";

				if(lb == null) {
					if(reaction.isReversible()) {
						lbValue = "-9999999";
					}
					else {
						lbValue = "0";
					}
				}
				else {
					lbValue = lb.value;
					lbUnits = lb.unitDefinition.getId();
				}

				if(ub==null){
					ubValue="9999999";
				}
				else {
					ubValue = ub.value;
					ubUnits = lb.unitDefinition.getId();
				}

				this.getWriter().write("    <kineticLaw>\n");
				this.getWriter().write("    <math xmlns=\"http://www.w3.org/1998/Math/MathML\">\n");
				this.getWriter().write("      <ci> FLUX_VALUE </ci>\n");
				this.getWriter().write("    </math>\n");
				this.getWriter().write("    <listOfParameters>\n");
				this.getWriter().write("      <parameter id=\"LOWER_BOUND\" value=\""+lbValue+"\" units=\""+lbUnits+"\"/>\n");
				this.getWriter().write("      <parameter id=\"UPPER_BOUND\" value=\""+ubValue+"\" units=\""+ubUnits+"\"/>\n");			
				this.getWriter().write("    </listOfParameters>\n");
				this.getWriter().write("    </kineticLaw>\n");
			}


			this.getWriter().write("  </reaction>\n");

		}

		this.getWriter().write("</listOfReactions>\n");

	}

	private void writeEnd(){

		try {
			this.getWriter().write("</model>\n");
			this.getWriter().write("</sbml>\n");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}

	public Boolean getExtended() {
		return extended;
	}

	public void setExtended(Boolean extended) {
		this.extended = extended;
	}


}

