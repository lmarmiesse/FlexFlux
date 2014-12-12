package flexflux.unit_tests;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import parsebionet.biodata.BioEntity;
import flexflux.general.Bind;
import flexflux.general.CplexBind;
import flexflux.general.GLPKBind;
import flexflux.input.SBMLQualReader;
import flexflux.interaction.RelationFactory;
import flexflux.interaction.Unique;

public class TestSBMLQual extends FFUnitTest{

	@Test
	public void test() {
		
		Bind bind=null;

		String solver = "GLPK";
		if (System.getProperties().containsKey("solver")) {
			solver = System.getProperty("solver");
		}
		
		try {
			if (solver.equals("CPLEX")) {
				bind = new CplexBind();
			} else {
				bind = new GLPKBind();
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			fail("Solver error");
		}

		bind.loadSbmlNetwork(
				"src/flexflux/unit_tests/data/SBMLQual/coli_core.xml", false);

		bind.loadConstraintsFile("src/flexflux/unit_tests/data/SBMLQual/conditionsFBA.txt");

		SBMLQualReader.loadSbmlQual(
				"src/flexflux/unit_tests/data/SBMLQual/test_myb30.xml", bind.getInteractionNetwork(),
				new RelationFactory());

		BioEntity entity1 = bind.getInteractionNetwork().getEntity("s_MYB30");
		BioEntity entity2 = bind.getInteractionNetwork().getEntity("s_MYB96");
		BioEntity entity3 = bind.getInteractionNetwork().getEntity("s_VLCFA");
		BioEntity entity4 = bind.getInteractionNetwork()
				.getEntity("s_Bacteria");

		
		
		assertTrue(bind.getInteractionNetwork().getInitialState(entity1) == 3);

		assertTrue(bind.getInteractionNetwork().getInitialState(entity2) == 1);

		assertTrue(bind.getInteractionNetwork().getInitialState(entity3) == 0);

		// /interactions

		assertTrue(bind.getInteractionNetwork().getTargetToInteractions()
				.get(entity1).getConditionalInteractions().get(0).getCondition().getInvolvedEntities().get(0)
				.getId().equals("s_Bacteria"));
		
		
	
		

		assertTrue(((Unique) bind.getInteractionNetwork().getTargetToInteractions()
				.get(entity1).getConditionalInteractions().get(0).getCondition())
				.getValue() == 6);

		assertTrue(bind.getInteractionNetwork().getTargetToInteractions()
				.get(entity1).getConditionalInteractions().get(0).getConsequence().getEntity() == entity1);

		
		assertTrue(bind.getInteractionNetwork().getTargetToInteractions()
				.get(entity1).getConditionalInteractions().get(0).getConsequence().getValue() == 1);

		assertTrue(bind.getInteractionNetwork().getTargetToInteractions()
				.get(entity1).getdefaultInteraction().getConsequence().getValue() == 0);

		
		assertTrue(bind.getInteractionNetwork().getConstraintFromState(entity1, 0).getLb()==0.0);
		assertTrue(bind.getInteractionNetwork().getConstraintFromState(entity1, 0).getUb()==0.5);
		
		assertTrue(bind.getInteractionNetwork().getStateFromValue(entity1, 0.7)==1);
		
		
//		System.out.println(bind.getInteractionNetwork().getTargetToInteractions()
//				.get(entity2).getConditionalInteractions().get(0));

	}

}
