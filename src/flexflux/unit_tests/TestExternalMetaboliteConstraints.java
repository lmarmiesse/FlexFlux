package flexflux.unit_tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;

import parsebionet.unit_tests.TestUtils;
import flexflux.general.Bind;
import flexflux.general.Constraint;
import flexflux.general.CplexBind;
import flexflux.general.DoubleResult;
import flexflux.general.GLPKBind;

public class TestExternalMetaboliteConstraints extends FFUnitTest{
	
	

	private static File tempSbmlFile = null;
	private static File tempConditionFile = null;
	private static File tempConditionFile2 = null;
	private static File tempInteractionFile = null;

	@Test
	public void test() {
		
		String solver = "GLPK";
		if (System.getProperties().containsKey("solver")) {
			solver = System.getProperty("solver");
		}
		
		java.nio.file.Path tmpSbml = null;
		java.nio.file.Path tmpCondition = null;
		java.nio.file.Path tmpCondition2 = null;
		java.nio.file.Path tmpInteraction = null;

		try {
			tmpSbml = java.nio.file.Files.createTempFile("test", ".xml");
			tmpCondition = java.nio.file.Files.createTempFile("test", ".tab");
			tmpCondition2 = java.nio.file.Files.createTempFile("test", ".tab");
			tmpInteraction = java.nio.file.Files.createTempFile("test", ".txt");
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			Assert.fail("Creation of the temporary files");
			e1.printStackTrace();
		}

		tempSbmlFile = tmpSbml.toFile();
		tempConditionFile = tmpCondition.toFile();
		tempConditionFile2 = tmpCondition2.toFile();
		tempInteractionFile = tmpInteraction.toFile();

		String sbmlFile = "";
		String conditionFile = "";
		String conditionFile2 = "";
		String regFile = "";

		try {
			sbmlFile = TestUtils
					.copyProjectResource(
							"flexflux/unit_tests/data/ko/test.xml",
							tempSbmlFile);
			conditionFile = TestUtils
					.copyProjectResource(
							"flexflux/unit_tests/data/externalMetaboliteConstraints/constraintsWithVariables.txt",
							tempConditionFile);
			
			conditionFile2 = TestUtils
					.copyProjectResource(
							"flexflux/unit_tests/data/externalMetaboliteConstraints/constraintsWithVariables.txt",
							tempConditionFile2);
			
			regFile = TestUtils
					.copyProjectResource(
							"flexflux/unit_tests/data/externalMetaboliteConstraints/interactions.sbml",
							tempInteractionFile);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Assert.fail("problem while copying the reference files");
		}
		
		// 1. test setting external metabolite to 0 when the exchange reaction equals to 0
		Bind bind = null;

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

		bind.loadSbmlNetwork(sbmlFile, false);
		bind.loadConstraintsFile(conditionFile);
		bind.loadRegulationFile(regFile);
		
		bind.prepareSolver();
		
		DoubleResult objValue = bind.FBA(new ArrayList<Constraint>(),
				true, true);
		
		Assert.assertEquals(0.0, objValue.result,0);
		
		// 2. test setting external metabolite to 0 when the exchange reaction is reversed
		bind = null;

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

		bind.loadSbmlNetwork(sbmlFile, false);
		bind.loadConstraintsFile(conditionFile2);
		bind.loadRegulationFile(regFile);

		bind.prepareSolver();

		objValue = bind.FBA(new ArrayList<Constraint>(),
				true, true);

		assertEquals(0.0, objValue.result, 0);
		
	}

}
