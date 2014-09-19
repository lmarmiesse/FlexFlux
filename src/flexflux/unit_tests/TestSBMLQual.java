package flexflux.unit_tests;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import parsebionet.biodata.BioEntity;
import flexflux.general.Bind;
import flexflux.general.CplexBind;
import flexflux.general.SBMLQualReader;
import flexflux.interaction.RelationFactory;
import flexflux.interaction.Unique;

public class TestSBMLQual {

	@Test
	public void test() {

		Bind bind = new CplexBind();

		bind.loadSbmlNetwork(
				"src/flexflux/unit_tests/data/SBMLQual/coli_core.xml", false);

		bind.loadConditionsFile("src/flexflux/unit_tests/data/SBMLQual/conditionsFBA.txt");

		SBMLQualReader.loadSbmlQual(
				"src/flexflux/unit_tests/data/SBMLQual/test_myb30.xml", bind,
				new RelationFactory());

		BioEntity entity1 = bind.getInteractionNetwork().getEntity("s_MYB30");
		BioEntity entity2 = bind.getInteractionNetwork().getEntity("s_MYB96");
		BioEntity entity3 = bind.getInteractionNetwork().getEntity("s_VLCFA");
		BioEntity entity4 = bind.getInteractionNetwork()
				.getEntity("s_Bacteria");

		assertTrue(bind.getInteractionNetworkSimpleConstraints().get(entity1)
				.getLb() == 3.5);
		assertTrue(bind.getInteractionNetworkSimpleConstraints().get(entity1)
				.getUb() == 3.5);

		assertTrue(bind.getInteractionNetworkSimpleConstraints().get(entity2)
				.getLb() == 0);
		assertTrue(bind.getInteractionNetworkSimpleConstraints().get(entity2)
				.getUb() == 0);

		assertTrue(bind.getInteractionNetworkSimpleConstraints().get(entity3)
				.getLb() == 0.2);
		assertTrue(bind.getInteractionNetworkSimpleConstraints().get(entity3)
				.getUb() == 0.2);

		// /interactions

		assertTrue(bind.getInteractionNetwork().getTargetToInteractions()
				.get(entity1)[0].getCondition().getInvolvedEntities().get(0)
				.getId().equals("s_Bacteria"));

		assertTrue(((Unique) bind.getInteractionNetwork()
				.getTargetToInteractions().get(entity1)[0].getCondition())
				.getValue() == 6.3);

		assertTrue(bind.getInteractionNetwork().getTargetToInteractions()
				.get(entity1)[0].getConsequence().getEntity() == entity1);

		assertTrue(bind.getInteractionNetwork().getTargetToInteractions()
				.get(entity1)[0].getConsequence().getValue() == 1.8);

		assertTrue(bind.getInteractionNetwork().getTargetToInteractions()
				.get(entity1)[1].getConsequence().getValue() == 0);

//		System.out.println(bind.getInteractionNetwork()
//				.getTargetToInteractions().get(entity2)[0]);

	}

}
