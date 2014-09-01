package flexflux.unit_tests;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import parsebionet.unit_tests.TestUtils;
import flexflux.analyses.KOAnalysis;
import flexflux.general.Bind;
import flexflux.general.CplexBind;
import flexflux.general.GLPKBind;

public class TestKoWithInteractions {

	private static File tempSbmlFile = null;
	private static File tempConditionFile = null;
	private static File tempInteractionFile = null;

	@Test
	public void test() {

		String solver = "GLPK";
		if (System.getProperties().containsKey("solver")) {
			solver = System.getProperty("solver");
		}

		java.nio.file.Path tmpSbml = null;
		java.nio.file.Path tmpCondition = null;
		java.nio.file.Path tmpInteraction = null;

		try {
			tmpSbml = java.nio.file.Files.createTempFile("test", ".xml");
			tmpCondition = java.nio.file.Files.createTempFile("test", ".tab");
			tmpInteraction = java.nio.file.Files.createTempFile("test", ".txt");
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			Assert.fail("Creation of the temporary files");
			e1.printStackTrace();
		}

		tempSbmlFile = tmpSbml.toFile();
		tempConditionFile = tmpCondition.toFile();
		tempInteractionFile = tmpInteraction.toFile();

		String sbmlFile = "";
		String conditionFile = "";
		String intFile = "";

		try {
			sbmlFile = TestUtils
					.copyProjectResource(
							"flexflux/unit_tests/data/conditionComparisonTest/test.xml",
							tempSbmlFile);
			conditionFile = TestUtils
					.copyProjectResource(
							"flexflux/unit_tests/data/conditionComparisonTest/constraintsWithVariables.txt",
							tempConditionFile);
			intFile = TestUtils
					.copyProjectResource(
							"flexflux/unit_tests/data/conditionComparisonTest/interactions.txt",
							tempInteractionFile);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Assert.fail("problem while copying the reference files");
		}

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
		bind.loadConditionsFile(conditionFile);
		bind.loadInteractionsFile(intFile);
		KOAnalysis a = new KOAnalysis(bind, 1, null);
		a.runAnalysis();

	}

}
