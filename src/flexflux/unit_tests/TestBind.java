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
 * 8 mars 2013 
 */
package flexflux.unit_tests;

import flexflux.general.Bind;
import flexflux.general.Constraint;
import flexflux.general.CplexBind;
import flexflux.general.GLPKBind;
import flexflux.interaction.And;
import flexflux.interaction.Interaction;
import flexflux.interaction.InteractionNetwork;
import flexflux.interaction.Relation;
import flexflux.interaction.Unique;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import parsebionet.biodata.BioEntity;
import parsebionet.biodata.BioNetwork;
import parsebionet.biodata.BioPhysicalEntity;
import parsebionet.io.Sbml2Bionetwork;

/**
 * @author lmarmiesse 8 mars 2013
 * 
 */
public class TestBind {

	static Bind bind = new CplexBind();
//	static Bind bind = new GLPKBind();
	static BioNetwork n;
	static InteractionNetwork i;

	@BeforeClass
	public static void init() {

		bind.loadSbmlNetwork("Data/coli.xml", false);
		n = bind.getBioNetwork();
		i = bind.getInteractionNetwork();
	}

	@Test
	public void GeneralTest() {

		go();
		bind = new GLPKBind();

		bind.loadSbmlNetwork("Data/coli.xml", false);
		n = bind.getBioNetwork();
		i = bind.getInteractionNetwork();
		go();

	}

	public void go() {

		// all the reaction are loaded
		Assert.assertEquals(2214, n.getBiochemicalReactionList().size());

		Assert.assertTrue("Network entities are not added correctly", n
				.getPhysicalEntityList().size()
				+ n.getBiochemicalReactionList().size()
				+ n.getProteinList().size() + n.getGeneList().size() >= i
				.getNumEntities().size());

		// entities are added
		Assert.assertNotNull(i.getEntity("M_10fthf_c"));
		Assert.assertNotNull(i.getEntity("R_12DGR120tipp"));
		Assert.assertNotNull(i.getEntity("b0241"));
		Assert.assertNull(i.getEntity(""));

		// the right number of constraints are added
		Map<String, BioPhysicalEntity> metabolitesMap = n
				.getPhysicalEntityList();

		int intMet = 0;
		for (String name : metabolitesMap.keySet()) {
			if (metabolitesMap.get(name).getBoundaryCondition() == false) {
				intMet++;
			}
		}

		Assert.assertEquals(bind.getConstraints().size(), intMet
				+ n.getBiochemicalReactionList().size()
				+ bind.getDeadReactions().size());

		// constraint for the metabolite M_10fthf_c
		boolean isConstraintWellFormed = false;
		boolean revBound = false;
		boolean irrevBound = false;
		for (Constraint c : bind.getConstraints()) {
			Map<String, Double> entities = c.getEntityNames();

			if (entities.containsKey("R_AICART")
					&& entities.get("R_AICART") == -1
					&& entities.containsKey("R_Ec_biomass_iAF1260_core_59p81M")
					&& entities.get("R_Ec_biomass_iAF1260_core_59p81M") == -2.23E-4
					// && entities.containsKey("R_FMETTRS")
					// && entities.get("R_FMETTRS") == -1
					&& entities.containsKey("R_FTHFD")
					&& entities.get("R_FTHFD") == -1
					&& entities.containsKey("R_GARFT")
					&& entities.get("R_GARFT") == -1
					&& entities.containsKey("R_MTHFC")
					&& entities.get("R_MTHFC") == 1
					&& entities.containsKey("R_ULA4NFT")
					&& entities.get("R_ULA4NFT") == -1 && entities.size() == 6) {
				isConstraintWellFormed = true;
			}

			if (entities.size() == 1 && entities.containsKey("R_HYXNtex")
					&& entities.get("R_HYXNtex") == 1) {
				revBound = c.getLb() == -999999 && c.getUb() == 999999;
			}
			if (entities.size() == 1 && entities.containsKey("R_GLUt4pp")
					&& entities.get("R_GLUt4pp") == 1) {
				irrevBound = c.getLb() == 0 && c.getUb() == 999999;
			}
		}

		Assert.assertTrue("Steady state constraint not formed properly",
				isConstraintWellFormed);
		Assert.assertTrue(revBound);
		Assert.assertTrue(irrevBound);

		// the gpr interaction are well formed
		boolean gpr = false;
		for (Interaction interaction : i.getGPRInteractions()) {

			if (((Unique) interaction.getConsequence()).getEntity().getId()
					.equals("R_GLCptspp")) {
				List<Relation> rels = ((And) interaction.getCondition())
						.getList();
				for (Relation r : rels) {
					gpr = true;
				}
			}

		}
		Assert.assertTrue(gpr);

		Sbml2Bionetwork parser = new Sbml2Bionetwork("Data/test.xml", false);

		BioNetwork network = parser.getBioNetwork();
		bind.setNetwork(network);

		Assert.assertTrue(bind.getConstraints().size() == 13);
		Assert.assertTrue(bind.getInteractionNetwork().getNumEntities().size() == 17);

		// starting tests on analysis and parsing files

		bind.loadConditionsFile("Data/condTest");
		bind.loadInteractionsFile("Data/intTest");

		bind.prepareSolver();
		Assert.assertTrue(bind.isMIP());

		double res = bind.FBA(new ArrayList<Constraint>(), true, true).result;

		Assert.assertTrue(res == 14.0);

		Assert.assertTrue(Math.abs(bind.getSolvedValue(new BioEntity("d")) - 40.0) < 0.001);

//		Assert.assertTrue(bind.getSolvedValue(new BioEntity("e")) == 5.0);

//		Assert.assertTrue(bind.getSolvedValue(new BioEntity("f")) == 122.0);
		Assert.assertTrue(bind.getSolvedValue(new BioEntity("g")) == 58.0);

		Bind bind2 = new CplexBind();
		bind2.setNetwork(network);

		Assert.assertTrue(bind2.getConstraints().size() == 13);
		Assert.assertTrue(bind2.getInteractionNetwork().getNumEntities().size() == 17);

		bind2.loadConditionsFile("Data/condTest");
		bind2.loadInteractionsFile("Data/intTest");

		bind2.prepareSolver();

		Assert.assertTrue(bind2.isMIP());

		Assert.assertTrue(bind2.FBA(new ArrayList<Constraint>(), true, false).result == 14.0);

		Assert.assertTrue(Math.abs(bind2.getSolvedValue(new BioEntity("d")) - 40.0) < 0.001);
		Assert.assertTrue(bind2.getSolvedValue(new BioEntity("e")) == 4.0);

	}

}
