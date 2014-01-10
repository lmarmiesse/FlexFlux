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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import parsebionet.utils.StringUtils;

import baobab.hypercyc.connection.JavacycPlus;

/**
 * A conversion interaction in which one or more entities (substrates) undergo
 * covalent changes to become one or more other entities (products). The
 * substrates of biochemical reactionNodes are defined in terms of sums of
 * species. This is what is typically done in biochemistry, and, in principle,
 * all of the EC reactionNodes should be biochemical reactionNodes.
 * 
 * Example: ATP + H2O = ADP + Pi.
 * 
 * In this reaction, ATP is considered to be an equilibrium mixture of several
 * species, namely ATP4-, HATP3-, H2ATP2-, MgATP2-, MgHATP-, and Mg2ATP.
 * Additional species may also need to be considered if other ions (e.g. Ca2+)
 * that bind ATP are present. Similar considerations apply to ADP and to
 * inorganic phosphate (Pi). When writing biochemical reactionNodes, it is
 * important not to attach charges to the biochemical reactants and not to
 * include ions such as H+ and Mg2+ in the equation. The reaction is written in
 * the direction specified by the EC nomenclature system, if applicable,
 * regardless of the physiological direction(s) in which the reaction proceeds.
 * (This definition from EcoCyc)
 * 
 * NOTE: Polymerization reactionNodes involving large polymers whose structure
 * is not explicitly captured should generally be represented as unbalanced
 * reactionNodes in which the monomer is consumed but the polymer remains
 * unchanged, e.g. glycogen + glucose = glycogen.
 */

public class BioChemicalReaction extends BioConversion {

	private String deltaG = "";
	private String deltaH = "";
	private String deltaS = "";
	private String ecNumber = "";
	private String keq = "";
	private HashMap<String, BioCatalysis> enzrxnsList = new HashMap<String, BioCatalysis>();
	private HashMap<String, BioPhysicalEntity> enzList = new HashMap<String, BioPhysicalEntity>();
	private HashMap<String, BioPhysicalEntity> listOfSubstrates = new HashMap<String, BioPhysicalEntity>();
	private HashMap<String, BioPhysicalEntity> listOfProducts = new HashMap<String, BioPhysicalEntity>();

	private Boolean isGeneticallyPossible = null; // To indicate if all the
													// genes coding for at least
													// one enzyme are present

	private String reversibility = null;
	private HashMap<String, BioPhysicalEntity> listOfPrimarySubstrates;
	private HashMap<String, BioPhysicalEntity> listOfPrimaryProducts;

	private Set<String> cofactors = new HashSet<String>();

	private Set<String> sideCompounds = new HashSet<String>();

	private String sbmlNote = "";

	private String biocycClass = "small";

	private Boolean isGenericReaction = false;

	private Boolean hole = false;

	private Flux lowerBound = new Flux("-99999", new UnitDefinition(
			"mmol_per_gDW_per_hr", "mmol_per_gDW_per_hr"));
	private Flux upperBound = new Flux("99999", new UnitDefinition(
			"mmol_per_gDW_per_hr", "mmol_per_gDW_per_hr"));

	public static String FBA_INTERNAL = "Internal";
	public static String FBA_EXCHANGE = "Exchange";
	public static String FBA_TRANSPORT = "Transport";

	public Boolean getHole() {
		return hole;
	}

	public Set<String> getCofactors() {
		return cofactors;
	}

	public void setCofactors(Set<String> cofactors) {
		this.cofactors = cofactors;
	}

	public Set<String> getSideCompounds() {
		return sideCompounds;
	}

	public void setSideCompounds(Set<String> sideCompounds) {
		this.sideCompounds = sideCompounds;
	}

	public void setHole(Boolean hole) {
		this.hole = hole;
	}

	public Boolean getIsGenericReaction() {
		return isGenericReaction;
	}

	public void setIsGenericReaction(Boolean isGenericReaction) {
		this.isGenericReaction = isGenericReaction;
	}

	public String getBiocycClass() {
		return biocycClass;
	}

	public void setBiocycClass(String biocycClass) {
		this.biocycClass = biocycClass;
	}

	/**
	 * To build a reaction in a network
	 */
	public BioChemicalReaction() {
		super();

	}

	public BioChemicalReaction(String id) {
		super(id);

	}

	public BioChemicalReaction(String id, String name) {
		super(id, name);

	}

	public BioChemicalReaction(BioChemicalReaction rxn) {

		super(rxn);

		this.setDeltaG(rxn.getDeltaG());
		this.setDeltaH(rxn.getDeltaH());
		this.setDeltaS(rxn.getDeltaS());
		this.setEcNumber(rxn.getEcNumber());

		// copy ot the enz list
		this.copyEnzList(rxn.getEnzList());

		this.copyEnzrxnsList(rxn.getEnzrxnsList());
		for (BioCatalysis cat : this.getEnzrxnsList().values()) {
			BioPhysicalEntity enz = cat.getController().getPhysicalEntity();
			this.addEnz(enz);
		}
		this.setKeq(rxn.getKeq());
		this.setReversibility(rxn.getReversiblity());
		this.setSbmlNote(rxn.getSbmlNote());

		this.setLowerBound(rxn.getLowerBound());
		this.setUpperBound(rxn.getUpperBound());

	}

	/**
	 * 
	 */
	public String toString() {

		String str = new String();

		str = str.concat(this.getId() + " : ");

		int n = 0;
		for (String cpd : this.getLeftList().keySet()) {

			if (n != 0) {
				str = str.concat(" + ");
			}
			n++;

			str = str.concat(cpd);

		}

		if (this.getReversiblity().compareToIgnoreCase(
				"IRREVERSIBLE-RIGHT-TO-LEFT") == 0) {
			str = str.concat(" <- ");
		} else if (this.getReversiblity().compareToIgnoreCase(
				"IRREVERSIBLE-LEFT-TO-RIGHT") == 0) {
			str = str.concat(" -> ");
		} else {
			str = str.concat(" <-> ");
		}

		n = 0;
		for (String cpd : this.getRightList().keySet()) {

			if (n != 0) {
				str = str.concat(" + ");
			}
			n++;

			str = str.concat(cpd);

		}

		return str;

	}

	/**
	 * @param p
	 *            The participant to add in the list of the participants
	 */
	public void addLeftParticipant(BioPhysicalEntityParticipant p) {
		this.getLeftParticipantList().put(p.getId(), p);
		this.getParticipantList().put(p.getId(), p);
		this.getLeftList().put(p.getPhysicalEntity().getId(),
				p.getPhysicalEntity());
		p.getPhysicalEntity().addReactionAsSubstrate(this);
		if (this.reversibility != null && this.isReversible()) {
			p.getPhysicalEntity().addReactionAsProduct(this);
		}
	}

	/**
	 * @param p
	 *            The participant to add in the list of the participants
	 */
	public void addRightParticipant(BioPhysicalEntityParticipant p) {
		this.getRightParticipantList().put(p.getId(), p);
		this.getParticipantList().put(p.getId(), p);
		this.getRightList().put(p.getPhysicalEntity().getId(),
				p.getPhysicalEntity());
		p.getPhysicalEntity().addReactionAsProduct(this);
		if (this.reversibility != null && this.isReversible()) {
			p.getPhysicalEntity().addReactionAsSubstrate(this);
		}
	}

	/**
	 * Comparison with another reaction : if the substrates and the products
	 * have the same id, return true
	 */
	public Boolean isRedundantWith(BioChemicalReaction rxn) {

		Set<String> listOfOtherSubstrates = rxn.getLeftList().keySet();
		Set<String> listOfOtherProducts = rxn.getRightList().keySet();

		if (this.getReversiblity().equalsIgnoreCase(rxn.getReversiblity())
				&& listOfOtherProducts.equals(this.getRightList().keySet())
				&& listOfOtherSubstrates.equals(this.getLeftList().keySet())) {
			return true;
		}

		if (this.getReversiblity().equalsIgnoreCase("reversible")
				&& rxn.getReversiblity().equalsIgnoreCase("reversible")
				&& ((this.getLeftList().keySet().equals(listOfOtherSubstrates) && this
						.getRightList().keySet().equals(listOfOtherProducts)) || (this
						.getLeftList().keySet().equals(listOfOtherProducts) && this
						.getRightList().keySet().equals(listOfOtherSubstrates)))) {
			return true;
		}

		return false;

	}

	/**
	 * @return Returns the deltaG.
	 */
	public String getDeltaG() {
		return deltaG;
	}

	/**
	 * @param deltaG
	 *            The deltaG to set.
	 */
	public void setDeltaG(String deltaG) {
		this.deltaG = deltaG;
	}

	/**
	 * @return Returns the deltaH.
	 */
	public String getDeltaH() {
		return deltaH;
	}

	/**
	 * @param deltaH
	 *            The deltaH to set.
	 */
	public void setDeltaH(String deltaH) {
		this.deltaH = deltaH;
	}

	/**
	 * @return Returns the deltaS.
	 */
	public String getDeltaS() {
		return deltaS;
	}

	/**
	 * @param deltaS
	 *            The deltaS to set.
	 */
	public void setDeltaS(String deltaS) {
		this.deltaS = deltaS;
	}

	/**
	 * @return Returns the ecNumber.
	 */
	public String getEcNumber() {
		return ecNumber;
	}

	/**
	 * @param ecNumber
	 *            The ecNumber to set.
	 */
	public void setEcNumber(String ecNumber) {
		this.ecNumber = ecNumber;
	}

	/**
	 * @return Returns the keq.
	 */
	public String getKeq() {
		return keq;
	}

	/**
	 * @param keq
	 *            The keq to set.
	 */
	public void setKeq(String keq) {
		this.keq = keq;
	}

	/**
	 * @return Returns the enzrxnsList.
	 */
	public HashMap<String, BioPhysicalEntity> getEnzList() {
		return enzList;
	}

	/**
	 * @param BioPhysicalEntity
	 *            enz
	 */
	public void addEnz(BioPhysicalEntity enz) {
		enzList.put(enz.getId(), enz);
	}

	/**
	 * @return Returns the enzrxnsList.
	 */
	public HashMap<String, BioCatalysis> getEnzrxnsList() {
		return enzrxnsList;
	}

	/**
	 * @param BioCatalysis
	 *            enzrxn
	 */
	public void addEnzrxn(BioCatalysis enzrxn) {
		enzrxnsList.put(enzrxn.getId(), enzrxn);
	}

	public void setEnzrxnsList(HashMap<String, BioCatalysis> enzrxnsList) {
		this.enzrxnsList = enzrxnsList;
	}

	public void copyEnzrxnsList(HashMap<String, BioCatalysis> list) {

		this.setEnzrxnsList(new HashMap<String, BioCatalysis>());

		for (BioCatalysis cat : list.values()) {
			BioCatalysis newCat = new BioCatalysis(cat);
			this.addEnzrxn(newCat);
		}
	}

	/**
	 * 
	 */
	public void setIsGeneticallyPossible() {
		if (isGeneticallyPossible != null) {
			return;
		} else {
			isGeneticallyPossible = false;

			if (this.getSpontaneous() != null) {
				isGeneticallyPossible = true;
				return;
			} else {
				ArrayList<BioPhysicalEntity> liste = new ArrayList<BioPhysicalEntity>(
						enzList.values());

				for (int i = 0; i < liste.size(); i++) {

					BioPhysicalEntity enzyme = liste.get(i);
					String classe = enzyme.getClass().getSimpleName();

					if (classe.compareTo("BioProtein") == 0) {
						if (((BioProtein) enzyme).getGeneList().size() > 0) {
							isGeneticallyPossible = true;
							return;
						}
					} else if (classe.compareTo("BioComplex") == 0) {
						if (((BioComplex) enzyme).getIsGeneticallyPossible() == true) {
							isGeneticallyPossible = true;
							return;
						}
					}
				}
			}
		}

		isGeneticallyPossible = false;
		return;
	}

	/**
	 * @return Returns the isGeneticallyPossible.
	 */
	public Boolean getIsGeneticallyPossible() {
		if (isGeneticallyPossible == null) {
			setIsGeneticallyPossible();
		}
		return isGeneticallyPossible;
	}

	/**
	 * 
	 * @return true if the reaction is spontaneous or if it is catalysed by an
	 *         enzyme
	 */
	public Boolean isPossible() {

		if (this.getSpontaneous() != null) {
			return true;
		} else {
			// if(this.getEnzrxnsList().size() > 0) {
			if (this.getEnzList().size() > 0) {
				return true;
			}
		}

		return false;

	}

	/**
	 * @return the reversibility of the reaction if not all enzymatic reaction
	 *         indicate the same reversibility info, returns "reversible"
	 *         Possible values of this property are: REVERSIBLE: Interaction can
	 *         occur in both directions IRREVERSIBLE-LEFT-TO-RIGHT
	 *         IRREVERSIBLE-RIGHT-TO-LEFT For all practical purposes, the
	 *         interactions occurs only in the specified direction in
	 *         physiological settings, because of chemical properties of the
	 *         reaction.
	 */
	public String getReversiblity() {
		return reversibility;
	}

	public void setReversibility(Boolean rev) {

		String oldRev = this.getReversiblity();

		if (rev == false) {
			reversibility = "irreversible-left-to-right";
			if (oldRev != null && oldRev.equalsIgnoreCase("reversible")) {
				for (BioPhysicalEntityParticipant bpe : this
						.getLeftParticipantList().values()) {
					if (!this.getRightList().containsKey(
							bpe.getPhysicalEntity().getId())) {
						bpe.getPhysicalEntity().removeReactionAsProduct(
								this.getId());
					}
				}
				for (BioPhysicalEntityParticipant bpe : this
						.getRightParticipantList().values()) {
					if (!this.getLeftList().containsKey(
							bpe.getPhysicalEntity().getId())) {
						bpe.getPhysicalEntity().removeReactionAsSubstrate(
								this.getId());
					}
				}
			}

			this.getLowerBound().value = "0.0";

		} else {
			reversibility = "reversible";
			if (oldRev != null && !oldRev.equalsIgnoreCase("reversible")) {
				for (BioPhysicalEntityParticipant bpe : this
						.getLeftParticipantList().values()) {
					bpe.getPhysicalEntity().addReactionAsProduct(this);
				}
				for (BioPhysicalEntityParticipant bpe : this
						.getRightParticipantList().values()) {
					bpe.getPhysicalEntity().addReactionAsSubstrate(this);
				}
			}
		}

	}

	public void setReversibility(String rev) {

		reversibility = rev;

		String oldRev = this.getReversiblity();

		if (rev.equalsIgnoreCase("reversible")
				&& !oldRev.equalsIgnoreCase("reversible")) {
			for (BioPhysicalEntityParticipant bpe : this
					.getLeftParticipantList().values()) {
				bpe.getPhysicalEntity().addReactionAsProduct(this);
			}
			for (BioPhysicalEntityParticipant bpe : this
					.getRightParticipantList().values()) {
				bpe.getPhysicalEntity().addReactionAsSubstrate(this);
			}
		}

		if (!rev.equalsIgnoreCase("reversible")
				&& oldRev.equalsIgnoreCase("reversible")) {
			for (BioPhysicalEntityParticipant bpe : this
					.getLeftParticipantList().values()) {
				if (!this.getRightList().containsKey(
						bpe.getPhysicalEntity().getId())) {
					bpe.getPhysicalEntity().removeReactionAsProduct(
							this.getId());
				}
			}
			for (BioPhysicalEntityParticipant bpe : this
					.getRightParticipantList().values()) {
				if (!this.getLeftList().containsKey(
						bpe.getPhysicalEntity().getId())) {
					bpe.getPhysicalEntity().removeReactionAsSubstrate(
							this.getId());
				}
			}

			this.getLowerBound().value = "0.0";
		}
	}

	public void setReversibility() {
		String rev = null;

		for (Iterator iter = enzrxnsList.keySet().iterator(); iter.hasNext();) {
			BioCatalysis enzrxn = enzrxnsList.get(iter.next());

			String direction = enzrxn.getDirection().toLowerCase();

			if (rev == null) {
				if (direction.matches("^irreversible.*")) {
					rev = direction;
					this.getLowerBound().value = "0.0";
				} else {
					this.reversibility = "reversible";
					return;
				}
			} else {
				if (direction.matches("^irreversible.*") == false) {
					this.reversibility = "reversible";
					return;
				}

				if (direction.matches("^" + rev + "$") == false) {
					this.reversibility = "reversible";
					return;
				}

				rev = direction;
			}
		}

		if (rev == null)
			this.reversibility = "reversible";
		else
			this.reversibility = rev;

		if (this.reversibility.equalsIgnoreCase("reversible")) {
			for (BioPhysicalEntityParticipant bpe : this
					.getLeftParticipantList().values()) {
				bpe.getPhysicalEntity().addReactionAsProduct(this);
			}
			for (BioPhysicalEntityParticipant bpe : this
					.getRightParticipantList().values()) {
				bpe.getPhysicalEntity().addReactionAsSubstrate(this);
			}
		}

		if (!this.reversibility.equalsIgnoreCase("reversible")) {
			for (BioPhysicalEntityParticipant bpe : this
					.getLeftParticipantList().values()) {
				if (!this.getRightList().containsKey(
						bpe.getPhysicalEntity().getId())) {
					bpe.getPhysicalEntity().removeReactionAsProduct(
							this.getId());
				}
			}
			for (BioPhysicalEntityParticipant bpe : this
					.getRightParticipantList().values()) {
				if (!this.getLeftList().containsKey(
						bpe.getPhysicalEntity().getId())) {
					bpe.getPhysicalEntity().removeReactionAsSubstrate(
							this.getId());
				}
			}
		}

	}

	/**
	 * Test the reaction : - if onlyPrimaries = true, test if the reaction
	 * occurs in a metabolic pathway, i.e. if the primary compounds can be
	 * adressed - if keepHolderClassCpd = false, test if any substrate or
	 * product of the reaction is a generic compound (e.g "an aldehyde").
	 */

	public Boolean testReaction() {
		return this.testReaction(false, true);
	}

	public Boolean testReaction(Boolean onlyPrimaries,
			Boolean keepHolderClassCpd) {

		if (onlyPrimaries == true) {
			if (this.getPathwayList().size() == 0) {
				return false;
			}

			if ((this.getPrimaryLeftParticipantList().size() == 0)
					|| (this.getPrimaryRightParticipantList().size() == 0)) {
				System.err.println("Warning : the " + this.getId()
						+ " has a problem with its primary compounds");
				return false;
			}

			if (keepHolderClassCpd == false) {
				if (this.getDoesItContainClassPrimaryCpd() == true) {
					return false;
				}
			}
		} else {

			if ((this.getLeftParticipantList().size() == 0)
					|| (this.getRightParticipantList().size() == 0)) {
				System.err.println("Warning : the " + this.getId()
						+ " has a problem with its compounds");
				return false;
			}

			if (keepHolderClassCpd == false) {
				if (this.getDoesItContainClassCpd() == true) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Set the list of substrates of the reaction. if the reaction is
	 * reversible, returns the list of left and right compounds if the reaction
	 * is irreversible-left-right, returns the list of left compounds if the
	 * reaction is irreversible-right-left, returns the list of right compounds
	 */
	public void setListOfSubstrates() {

		HashMap<String, BioPhysicalEntity> listOfSubstrates = new HashMap<String, BioPhysicalEntity>();

		String rev = this.getReversiblity();

		if (rev.compareToIgnoreCase("irreversible-left-to-right") != 0
				&& rev.compareToIgnoreCase("reversible") != 0
				&& rev.compareToIgnoreCase("irreversible-right-to-left") != 0) {
			rev = "reversible";
		}

		if (rev.compareToIgnoreCase("irreversible-left-to-right") == 0
				|| rev.compareToIgnoreCase("reversible") == 0) {

			HashMap<String, BioPhysicalEntity> left = this.getLeftList();

			listOfSubstrates.putAll(left);

		}

		if (rev.compareToIgnoreCase("irreversible-right-to-left") == 0
				|| rev.compareToIgnoreCase("reversible") == 0) {

			HashMap<String, BioPhysicalEntity> right = this.getRightList();
			listOfSubstrates.putAll(right);

		}

		this.listOfSubstrates = listOfSubstrates;
	}

	/**
	 * @return the list of substrates of the reaction. if the reaction is
	 *         reversible, returns the list of left and right compounds if the
	 *         reaction is irreversible-left-right, returns the list of left
	 *         compounds if the reaction is irreversible-right-left, returns the
	 *         list of right compounds
	 */
	public HashMap<String, BioPhysicalEntity> getListOfSubstrates() {
		this.setListOfSubstrates();

		return (this.listOfSubstrates);

	}

	/**
	 * Set the list of products of the reaction. if the reaction is reversible,
	 * returns the list of left and right compounds if the reaction is
	 * irreversible-left-right, returns the list of right compounds if the
	 * reaction is irreversible-right-left, returns the list of left compounds
	 */
	public void setListOfProducts() {

		HashMap<String, BioPhysicalEntity> listOfProducts = new HashMap<String, BioPhysicalEntity>();

		String rev = this.getReversiblity();

		if (rev.compareToIgnoreCase("irreversible-left-to-right") != 0
				&& rev.compareToIgnoreCase("reversible") != 0
				&& rev.compareToIgnoreCase("irreversible-right-to-left") != 0) {
			rev = "reversible";
		}

		if (rev.compareToIgnoreCase("irreversible-left-to-right") == 0
				|| rev.compareToIgnoreCase("reversible") == 0) {

			HashMap<String, BioPhysicalEntity> right = this.getRightList();
			listOfProducts.putAll(right);
		}

		if (rev.compareToIgnoreCase("irreversible-right-to-left") == 0
				|| rev.compareToIgnoreCase("reversible") == 0) {

			HashMap<String, BioPhysicalEntity> left = this.getLeftList();
			listOfProducts.putAll(left);

		}

		this.listOfProducts = listOfProducts;

	}

	/**
	 * @return the list of products of the reaction. if the reaction is
	 *         reversible, returns the list of left and right compounds if the
	 *         reaction is irreversible-left-right, returns the list of right
	 *         compounds if the reaction is irreversible-right-left, returns the
	 *         list of left compounds
	 */

	public HashMap<String, BioPhysicalEntity> getListOfProducts() {
		this.setListOfProducts();
		return (this.listOfProducts);
	}

	/**
	 * Set the list of primary substrates of the reaction. if the reaction is
	 * reversible, returns the list of left and right compounds if the reaction
	 * is irreversible-left-right, returns the list of left compounds if the
	 * reaction is irreversible-right-left, returns the list of right compounds
	 */
	public void setListOfPrimarySubstrates() {

		HashMap<String, BioPhysicalEntity> listOfSubstrates = new HashMap<String, BioPhysicalEntity>();

		String rev = this.getReversiblity();

		if (rev.compareToIgnoreCase("irreversible-left-to-right") != 0
				&& rev.compareToIgnoreCase("reversible") != 0
				&& rev.compareToIgnoreCase("irreversible-right-to-left") != 0) {
			rev = "reversible";
		}

		if (rev.compareToIgnoreCase("irreversible-left-to-right") == 0
				|| rev.compareToIgnoreCase("reversible") == 0) {

			HashMap<String, BioPhysicalEntity> left = this.getPrimaryLeftList();
			listOfSubstrates.putAll(left);

		}

		if (rev.compareToIgnoreCase("irreversible-right-to-left") == 0
				|| rev.compareToIgnoreCase("reversible") == 0) {

			HashMap<String, BioPhysicalEntity> right = this
					.getPrimaryRightList();
			listOfSubstrates.putAll(right);

		}

		this.listOfPrimarySubstrates = listOfSubstrates;
	}

	/**
	 * @return the list of substrates of the reaction. if the reaction is
	 *         reversible, returns the list of left and right compounds if the
	 *         reaction is irreversible-left-right, returns the list of left
	 *         compounds if the reaction is irreversible-right-left, returns the
	 *         list of right compounds
	 */

	public HashMap<String, BioPhysicalEntity> getListOfPrimarySubstrates() {
		this.setListOfPrimarySubstrates();

		return (this.listOfPrimarySubstrates);

	}

	/**
	 * Set the list of primary products of the reaction. if the reaction is
	 * reversible, returns the list of left and right compounds if the reaction
	 * is irreversible-left-right, returns the list of right compounds if the
	 * reaction is irreversible-right-left, returns the list of left compounds
	 */
	public void setListOfPrimaryProducts() {

		HashMap<String, BioPhysicalEntity> listOfProducts = new HashMap<String, BioPhysicalEntity>();

		String rev = this.getReversiblity();

		if (rev.compareToIgnoreCase("irreversible-left-to-right") != 0
				&& rev.compareToIgnoreCase("reversible") != 0
				&& rev.compareToIgnoreCase("irreversible-right-to-left") != 0) {
			rev = "reversible";
		}

		if (rev.compareToIgnoreCase("irreversible-left-to-right") == 0
				|| rev.compareToIgnoreCase("reversible") == 0) {

			HashMap<String, BioPhysicalEntity> right = this
					.getPrimaryRightList();
			listOfProducts.putAll(right);
		}

		if (rev.compareToIgnoreCase("irreversible-right-to-left") == 0
				|| rev.compareToIgnoreCase("reversible") == 0) {

			HashMap<String, BioPhysicalEntity> left = this.getPrimaryLeftList();
			listOfProducts.putAll(left);

		}

		this.listOfPrimaryProducts = listOfProducts;

	}

	/**
	 * @return the list of primary products of the reaction. if the reaction is
	 *         reversible, returns the list of left and right compounds if the
	 *         reaction is irreversible-left-right, returns the list of right
	 *         compounds if the reaction is irreversible-right-left, returns the
	 *         list of left compounds
	 */

	public HashMap<String, BioPhysicalEntity> getListOfPrimaryProducts() {
		this.setListOfPrimaryProducts();
		return (this.listOfPrimaryProducts);
	}

	public String getEquation() {
		return this.getEquation(false);
	}

	public String getEquation(Boolean encodeSbml) {

		int nb = 0;

		String out = "";

		for (BioPhysicalEntity l : this.getLeftList().values()) {
			nb++;
			if (nb > 1) {
				out = out.concat(" + ");
			}

			out = out + l.getId() + "[" + l.getCompartment().getId() + "]";

		}

		String rev = this.getReversiblity();

		if (rev.compareToIgnoreCase("irreversible-left-to-right") == 0) {
			out = out.concat(" -> ");
		} else if (rev.compareToIgnoreCase("irreversible-right-to-left") == 0) {
			out = out.concat(" <- ");
		} else {
			out = out.concat(" <-> ");
		}

		nb = 0;
		for (BioPhysicalEntity r : this.getRightList().values()) {
			nb++;
			if (nb > 1) {
				out = out.concat(" + ");
			}

			out = out + r.getId() + "[" + r.getCompartment().getId() + "]";
		}

		return out;

	}

	public String getEquationForHuman(Boolean encodeSbml) {

		int nb = 0;

		String out;

		out = "";

		for (BioPhysicalEntity l : this.getLeftList().values()) {
			nb++;
			if (nb > 1) {
				out = out.concat(" + ");
			}

			out = out + l.getName() + "[" + l.getCompartment().getId() + "]";

		}

		String rev = this.getReversiblity();

		if (rev.compareToIgnoreCase("irreversible-left-to-right") == 0) {
			out = out.concat(" -> ");
		} else if (rev.compareToIgnoreCase("irreversible-right-to-left") == 0) {
			out = out.concat(" <- ");
		} else {
			out = out.concat(" <-> ");
		}

		nb = 0;
		for (BioPhysicalEntity r : this.getRightList().values()) {
			nb++;
			if (nb > 1) {
				out = out.concat(" + ");
			}

			out = out + r.getName() + "[" + r.getCompartment().getId() + "]";
		}

		return out;

	}

	public String getSbmlNote() {
		return sbmlNote;
	}

	public void setSbmlNote(String sbmlNote) {
		this.sbmlNote = sbmlNote;
	}

	public void setEnzList(HashMap<String, BioPhysicalEntity> enzList) {
		this.enzList = enzList;
	}

	public void copyEnzList(HashMap<String, BioPhysicalEntity> enzList) {

		this.setEnzList(new HashMap<String, BioPhysicalEntity>());

		for (BioPhysicalEntity enz : enzList.values()) {
			BioPhysicalEntity newEnz = new BioPhysicalEntity(enz);
			this.getEnzList().put(newEnz.getId(), newEnz);
		}
	}

	/**
	 * 
	 * @param cpd
	 */
	public void removeLeft(String cpd) {

		this.getLeftList().remove(cpd);

		HashMap<String, BioPhysicalEntityParticipant> bpes = new HashMap<String, BioPhysicalEntityParticipant>(
				this.getLeftParticipantList());

		for (BioPhysicalEntityParticipant bpe : bpes.values()) {
			if (bpe.getPhysicalEntity().getId().compareTo(cpd) == 0) {
				this.getLeftParticipantList().remove(bpe.getId());
			}
		}
	}

	/**
	 * 
	 * @param cpd
	 */
	public void removeRight(String cpd) {

		this.getRightList().remove(cpd);

		HashMap<String, BioPhysicalEntityParticipant> bpes = new HashMap<String, BioPhysicalEntityParticipant>(
				this.getRightParticipantList());

		for (BioPhysicalEntityParticipant bpe : bpes.values()) {
			if (bpe.getPhysicalEntity().getId().compareTo(cpd) == 0) {
				this.getRightParticipantList().remove(bpe.getId());
			}
		}
	}

	public Flux getLowerBound() {
		return lowerBound;
	}

	public void setLowerBound(Flux lowerBound) {
		this.lowerBound = lowerBound;
	}

	public Flux getUpperBound() {
		return upperBound;
	}

	public void setUpperBound(Flux upperBound) {
		this.upperBound = upperBound;
	}

	public Boolean isReversible() {
		if (this.getReversiblity().compareToIgnoreCase("reversible") == 0) {
			return true;
		}
		return false;
	}

	/**
	 * @param pathway
	 *            : the pathway to add
	 */
	public void addPathway(BioPathway pathway) {
		this.getPathwayList().put(pathway.getId(), pathway);
		pathway.addReaction(this);
	}

	/**
	 * 
	 * @return a ArrayList<String> corresponding to the association between
	 *         genes and between proteins that enable the catalysis of the
	 *         reaction Ex : res.get(0) g1 and ( g2 or g3 ) res.get(1) p1 and p2
	 */
	public ArrayList<String> getGPR() {

		String geneStr = "";
		String protStr = "";
		ArrayList<String> res = new ArrayList<String>();

		int j = 0;

		for (Iterator<String> iterEnz = enzList.keySet().iterator(); iterEnz
				.hasNext();) {
			j++;

			if (j > 1) {
				protStr = protStr + " or ";
				geneStr = geneStr + " or ";
			}

			BioPhysicalEntity enzyme = enzList.get(iterEnz.next());

			String classe = enzyme.getClass().getSimpleName();

			HashMap<String, BioGene> listOfGenes = new HashMap<String, BioGene>();

			HashMap<String, BioProtein> listOfProteins = new HashMap<String, BioProtein>();

			if (classe.compareTo("BioProtein") == 0) {
				listOfProteins.put(enzyme.getId(), (BioProtein) enzyme);

				listOfGenes = ((BioProtein) enzyme).getGeneList();

			} else if (classe.compareTo("BioComplex") == 0) {

				listOfGenes = ((BioComplex) enzyme).getGeneList();

				HashMap<String, BioPhysicalEntity> componentList = ((BioComplex) enzyme)
						.getAllComponentList();

				for (Iterator<String> iterComponent = componentList.keySet()
						.iterator(); iterComponent.hasNext();) {

					BioPhysicalEntity component = componentList
							.get(iterComponent.next());

					if (component.getClass().getSimpleName()
							.compareTo("BioProtein") == 0) {
						listOfProteins.put(component.getId(),
								(BioProtein) component);
					}

				}
			}
			int k = 0;

			geneStr = geneStr + "( ";

			for (Iterator<String> iterGene = listOfGenes.keySet().iterator(); iterGene
					.hasNext();) {
				k++;

				if (k > 1) {
					geneStr = geneStr + " and ";
				}

				BioGene gene = listOfGenes.get(iterGene.next());

				geneStr = geneStr + StringUtils.htmlEncode(gene.getName());
			}

			geneStr = geneStr + " )";

			protStr = protStr + "( ";

			k = 0;

			for (Iterator<String> iterProt = listOfProteins.keySet().iterator(); iterProt
					.hasNext();) {
				k++;
				if (k > 1) {
					protStr = protStr + " and ";
				}

				BioProtein prot = listOfProteins.get(iterProt.next());
				protStr = protStr + StringUtils.htmlEncode(prot.getName());
			}

			protStr = protStr + " )";

		}

		res.add(geneStr);
		res.add(protStr);

		return res;

	}

	/**
	 * Returns the genes catalysing the reaction
	 * 
	 * @return a HashMap
	 */
	public HashMap<String, BioGene> getListOfGenes() {

		HashMap<String, BioGene> genes = new HashMap<String, BioGene>();

		for (Iterator<String> iterEnz = enzList.keySet().iterator(); iterEnz
				.hasNext();) {

			BioPhysicalEntity enzyme = enzList.get(iterEnz.next());

			String classe = enzyme.getClass().getSimpleName();

			if (classe.compareTo("BioProtein") == 0) {
				genes.putAll(((BioProtein) enzyme).getGeneList());
			} else if (classe.compareTo("BioComplex") == 0) {

				HashMap<String, BioPhysicalEntity> componentList = ((BioComplex) enzyme)
						.getAllComponentList();

				for (Iterator<String> iterComponent = componentList.keySet()
						.iterator(); iterComponent.hasNext();) {

					BioPhysicalEntity component = componentList
							.get(iterComponent.next());

					if (component.getClass().getSimpleName()
							.compareTo("BioProtein") == 0) {
						genes.putAll(((BioProtein) component).getGeneList());
					}
				}
			}
		}

		return genes;

	}

	/**
	 * Returns the names of the genes catalysing the reaction
	 * 
	 * @return a HashMap
	 */
	public Set<String> getListOfGeneNames() {

		HashMap<String, BioGene> genes = this.getListOfGenes();

		Set<String> geneNames = new HashSet<String>();

		for (BioGene gene : genes.values()) {
			geneNames.add(gene.getName());
		}

		return geneNames;

	}

	/**
	 * Add a side compound
	 * 
	 * @param cpdId
	 */
	public void addSideCompound(String cpdId) {

		this.getSideCompounds().add(cpdId);

		for (BioPhysicalEntityParticipant bpe : this.getLeftParticipantList()
				.values()) {
			BioPhysicalEntity cpd = bpe.getPhysicalEntity();

			if (cpd.getId().equalsIgnoreCase(cpdId)) {
				bpe.setIsPrimaryCompound(false);
			}

		}

		for (BioPhysicalEntityParticipant bpe : this.getRightParticipantList()
				.values()) {
			BioPhysicalEntity cpd = bpe.getPhysicalEntity();

			if (cpd.getId().equalsIgnoreCase(cpdId)) {
				bpe.setIsPrimaryCompound(false);
			}

		}

	}

	/**
	 * Add a primary compound
	 * 
	 * @param cpdId
	 */
	public void addPrimaryCompound(String cpdId) {

		this.getSideCompounds().add(cpdId);

		for (BioPhysicalEntityParticipant bpe : this.getLeftParticipantList()
				.values()) {
			BioPhysicalEntity cpd = bpe.getPhysicalEntity();

			if (cpd.getId().equalsIgnoreCase(cpdId)) {
				bpe.setIsPrimaryCompound(true);
			}

		}

		for (BioPhysicalEntityParticipant bpe : this.getRightParticipantList()
				.values()) {
			BioPhysicalEntity cpd = bpe.getPhysicalEntity();

			if (cpd.getId().equalsIgnoreCase(cpdId)) {
				bpe.setIsPrimaryCompound(true);
			}

		}

	}

	/**
	 * Add a cofactor
	 * 
	 * @param cpdId
	 */
	public void addCofactor(String cpdId) {

		this.getCofactors().add(cpdId);

		for (BioPhysicalEntityParticipant bpe : this.getLeftParticipantList()
				.values()) {
			BioPhysicalEntity cpd = bpe.getPhysicalEntity();

			if (cpd.getId().equalsIgnoreCase(cpdId)) {
				bpe.setIsCofactor(true);
			}

		}

		for (BioPhysicalEntityParticipant bpe : this.getRightParticipantList()
				.values()) {
			BioPhysicalEntity cpd = bpe.getPhysicalEntity();

			if (cpd.getId().equalsIgnoreCase(cpdId)) {
				bpe.setIsCofactor(true);
			}

		}

	}

	/**
	 * From a list of BioPhysicalEntityParticipant, select only those which are
	 * primary in at least one pathway where occurs the reaction
	 * 
	 * @param cpdsList
	 * @param cyc
	 */
	public HashMap<String, BioPhysicalEntityParticipant> getPrimaries(
			HashMap<String, BioPhysicalEntityParticipant> cpdsListHash,
			JavacycPlus cyc) {

		ArrayList<BioPathway> pathwayList = new ArrayList<BioPathway>(this
				.getPathwayList().values());

		HashMap<String, BioPhysicalEntityParticipant> result = new HashMap<String, BioPhysicalEntityParticipant>();

		ArrayList<BioPhysicalEntityParticipant> cpdsList = new ArrayList<BioPhysicalEntityParticipant>(
				cpdsListHash.values());

		for (int i = 0; i < cpdsList.size(); i++) {
			BioPhysicalEntityParticipant bpe = cpdsList.get(i);
			BioPhysicalEntity component = cpdsList.get(i).getPhysicalEntity();
			String componentId = component.getId();
			Boolean flag = false;

			// To deal with component id transformed with compartment
			// information
			String componentIdWithoutCompartment = componentId.replaceAll(
					"_IN_.*$", "");

			for (int j = 0; j < pathwayList.size(); j++) {
				BioPathway pathway = pathwayList.get(j);

				if (pathway.getReactionLayouts().get(this.getId()) == null) {
					System.err.println("Problem with layout " + pathway.getId()
							+ " " + this.getId());
					flag = true;
				} else {
					BioReactionLayout layout = pathway.getReactionLayouts()
							.get(this.getId());
					ArrayList<String> primaryLeft = layout.getLeft();
					ArrayList<String> primaryRight = layout.getRight();

					if (primaryLeft.contains(componentId)
							|| primaryRight.contains(componentId)
							|| primaryLeft
									.contains(componentIdWithoutCompartment)
							|| primaryRight
									.contains(componentIdWithoutCompartment)) {
						flag = true;
					}
				}
			}

			if (flag == true) {
				bpe.setIsPrimaryCompound(true);
				result.put(cpdsList.get(i).getId(), cpdsList.get(i));
			} else {
				bpe.setIsPrimaryCompound(false);
			}
		}

		return result;

	}

	/**
	 * @param primaryLeftParticipantList
	 *            The primaryLeftList to set.
	 */
	public void setPrimaryParticipantLeftList(JavacycPlus cyc) {

		this.primaryLeftParticipantList = getPrimaries(leftParticipantList, cyc);

	}

	/**
	 * @param primaryRightParticipantList
	 *            The primaryRightList to set.
	 */
	public void setPrimaryParticipantRightList(JavacycPlus cyc) {

		this.primaryRightParticipantList = getPrimaries(rightParticipantList,
				cyc);

	}

	/**
	 * @return if the all the substrates are in the same compartment then return
	 *         the compartment else return null
	 */
	public BioCompartment getCompartment() {
		BioCompartment compartment = null;

		Set<BioCompartment> compartmentLefts = new HashSet<BioCompartment>();
		Set<BioCompartment> compartmentRights = new HashSet<BioCompartment>();

		for (BioPhysicalEntityParticipant bpe : this.getLeftParticipantList()
				.values()) {
			BioPhysicalEntity cpd = bpe.getPhysicalEntity();
			BioCompartment cpt = cpd.getCompartment();
			compartmentLefts.add(cpt);
		}

		for (BioPhysicalEntityParticipant bpe : this.getRightParticipantList()
				.values()) {
			BioPhysicalEntity cpd = bpe.getPhysicalEntity();
			BioCompartment cpt = cpd.getCompartment();
			compartmentRights.add(cpt);
		}

		Vector<BioCompartment> compartments = new Vector<BioCompartment>();

		compartments.addAll(compartmentRights);
		compartments.addAll(compartmentLefts);
		// System.err.println("--------"+compartments);

		if (compartments.size() == 2
				&& compartments.get(0).getId()
						.equals(compartments.get(1).getId())) {
			compartment = compartments.get(0);
		}

		return compartment;
	}

	/**
	 * @return true if the reaction is a transport reaction
	 */
	public Boolean isTransportReaction() {

		Boolean flag = false;

		Set<String> compartmentLefts = new HashSet<String>();
		Set<String> compartmentRights = new HashSet<String>();

		HashMap<String, String> lefts_cpt = new HashMap<String, String>();
		HashMap<String, String> rights_cpt = new HashMap<String, String>();

		for (BioPhysicalEntityParticipant bpe : this.getLeftParticipantList()
				.values()) {

			BioPhysicalEntity cpd = bpe.getPhysicalEntity();

			BioCompartment cpt = cpd.getCompartment();

			String cpdId = cpd.getId();
			String cptId = cpt.getId();

			String PalssonSuffix = "_" + cptId.substring(0, 1).toLowerCase();
			String MetExploreSuffix = "_IN_" + cptId;

			cpdId = cpdId.replaceAll(PalssonSuffix + "$", "");
			cpdId = cpdId.replaceAll(MetExploreSuffix + "$", "");

			lefts_cpt.put(cpdId, cptId);

			compartmentLefts.add(cpt.getId());

		}

		for (BioPhysicalEntityParticipant bpe : this.getRightParticipantList()
				.values()) {

			BioPhysicalEntity cpd = bpe.getPhysicalEntity();

			BioCompartment cpt = cpd.getCompartment();

			String cpdId = cpd.getId();
			String cptId = cpt.getId();

			String PalssonSuffix = "_" + cptId.substring(0, 1).toLowerCase();
			String MetExploreSuffix = "_IN_" + cptId;

			cpdId = cpdId.replaceAll(PalssonSuffix + "$", "");
			cpdId = cpdId.replaceAll(MetExploreSuffix + "$", "");

			rights_cpt.put(cpdId, cptId);

			compartmentRights.add(cpt.getId());

		}

		Set<String> compartments = new HashSet<String>();

		compartments.addAll(compartmentRights);
		compartments.addAll(compartmentLefts);

		if (compartments.size() != compartmentLefts.size()
				|| compartments.size() != compartmentRights.size()) {
			flag = true;
		} else {
			for (String cpdId : lefts_cpt.keySet()) {
				String cptId = lefts_cpt.get(cpdId);

				if (rights_cpt.containsKey(cpdId)) {
					String cptId2 = rights_cpt.get(cpdId);

					if (!cptId.equals(cptId2)) {
						flag = true;
					}
				}
			}
		}

		return flag;
	}

	/**
	 * @return true if the reaction is an exchange reaction: - if the lefts or
	 *         the rights are empty - if one left or one right is external
	 */
	public Boolean isExchangeReaction() {

		HashMap<String, BioPhysicalEntityParticipant> lefts = this
				.getLeftParticipantList();
		HashMap<String, BioPhysicalEntityParticipant> rights = this
				.getRightParticipantList();

		if (lefts.size() == 0 || rights.size() == 0) {
			return true;
		}

		for (BioPhysicalEntityParticipant bpe : lefts.values()) {

			BioPhysicalEntity cpd = bpe.getPhysicalEntity();

			if (cpd.getBoundaryCondition() == true) {
				return true;
			}
		}

		for (BioPhysicalEntityParticipant bpe : rights.values()) {

			BioPhysicalEntity cpd = bpe.getPhysicalEntity();

			if (cpd.getBoundaryCondition() == true) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Get the type of the reaction (internal, exchange or transport)
	 * 
	 * @return
	 */
	public String getFBAType() {

		String type = FBA_INTERNAL;

		if (this.isExchangeReaction()) {
			type = FBA_EXCHANGE;
		} else if (this.isTransportReaction()) {
			type = FBA_TRANSPORT;
		}

		return type;
	}

	/**
	 * format a reaction id in the pallson way (R_***)
	 */
	public void formatIdByPalsson() {
		String id = this.getId();

		if (!id.startsWith("R_")) {
			id = "R_" + id;
		}

		this.setId(id);

		return;
	}

	/**
	 * Compute the atom balances
	 * 
	 * @return
	 */
	public HashMap<String, Double> computeAtomBalances() {

		HashMap<String, Double> balances = new HashMap<String, Double>();

		System.err.println("Balance for reaction " + this.getId());

		System.err.println("Lefts");

		for (BioPhysicalEntityParticipant bpe : this.getLeftParticipantList()
				.values()) {

			System.err.println(bpe.getPhysicalEntity().getId());

			String stoStr = bpe.getStoichiometricCoefficient();

			Double sto = 0.0;

			try {
				sto = Double.parseDouble(stoStr);
			} catch (NumberFormatException e) {
				System.err.println("Stoichiometry not valid in the reaction "
						+ this.getId());
				return new HashMap<String, Double>();
			}

			System.err.println("Sto : " + sto);

			String formula = bpe.getPhysicalEntity().getChemicalFormula();

			System.err.println("Formula : " + formula);

			if (formula.equals("NA")) {
				System.err.println("No formula for "
						+ bpe.getPhysicalEntity().getId() + " in "
						+ this.getId());
				return new HashMap<String, Double>();
			}

			String REGEX = "[A-Z]{1}[a-z]*[0-9]*";

			Pattern pattern = Pattern.compile(REGEX);
			Matcher matcher = pattern.matcher(formula);

			while (matcher.find()) {
				String group = matcher.group(0);

				String REGEX2 = "([A-Z]{1}[a-z]*)([0-9]*)";

				Pattern pattern2 = Pattern.compile(REGEX2);
				Matcher matcher2 = pattern2.matcher(group);

				matcher2.find();

				String atom = matcher2.group(1);

				String numStr = matcher2.group(2);

				if (numStr.equals("")) {
					numStr = "1.0";
				}

				Double number = Double.parseDouble(numStr);

				System.err.println(atom + ":" + number);

				if (!balances.containsKey(atom)) {
					balances.put(atom, sto * number);
				} else {
					balances.put(atom, balances.get(atom) + sto * number);
				}

			}

		}

		System.err.println("Rights");

		for (BioPhysicalEntityParticipant bpe : this.getRightParticipantList()
				.values()) {

			System.err.println(bpe.getPhysicalEntity().getId());

			String stoStr = bpe.getStoichiometricCoefficient();

			Double sto = 0.0;

			try {
				sto = Double.parseDouble(stoStr);
			} catch (NumberFormatException e) {
				System.err.println("Stoichiometry not valid in the reaction "
						+ this.getId());
				return new HashMap<String, Double>();
			}

			System.err.println("Sto : " + sto);

			String formula = bpe.getPhysicalEntity().getChemicalFormula();

			System.err.println("Formula : " + formula);

			if (formula.equals("NA")) {
				System.err.println("No formula for "
						+ bpe.getPhysicalEntity().getId() + " in "
						+ this.getId());
				return new HashMap<String, Double>();
			}

			String REGEX = "[A-Z]{1}[a-z]*[0-9]*";

			Pattern pattern = Pattern.compile(REGEX);
			Matcher matcher = pattern.matcher(formula);

			while (matcher.find()) {
				String group = matcher.group(0);

				String REGEX2 = "([A-Z]{1}[a-z]*)([0-9]*)";

				Pattern pattern2 = Pattern.compile(REGEX2);
				Matcher matcher2 = pattern2.matcher(group);

				matcher2.find();

				String atom = matcher2.group(1);

				String numStr = matcher2.group(2);

				if (numStr.equals("")) {
					numStr = "1.0";
				}

				Double number = Double.parseDouble(numStr);

				System.err.println(atom + ":" + number);

				if (!balances.containsKey(atom)) {
					balances.put(atom, -sto * number);
				} else {
					balances.put(atom, balances.get(atom) + -sto * number);
				}

			}

		}

		System.err.println(balances);

		return balances;

	}

	/**
	 * Checks if a reaction is balanced
	 * 
	 * @return
	 */
	public Boolean isBalanced() {

		Double sum = 0.0;

		HashMap<String, Double> balances = this.computeAtomBalances();

		if (balances.size() == 0) {
			return false;
		}

		for (Double value : balances.values()) {
			sum = sum + value;
		}

		if (sum != 0.0) {
			return false;
		} else {
			return true;
		}
	}

}
