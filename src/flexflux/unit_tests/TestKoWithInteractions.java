package flexflux.unit_tests;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import parsebionet.unit_tests.utils.TestUtils;
import flexflux.analyses.KOAnalysis;
import flexflux.analyses.result.KOResult;
import flexflux.general.Bind;
import flexflux.general.CplexBind;
import flexflux.general.GLPKBind;
import flexflux.general.Vars;

public class TestKoWithInteractions extends FFUnitTest{

	private static File tempSbmlFile = null;
	private static File tempConditionFile = null;
	private static File tempRegulationFile = null;

	@Test
	public void test() {
		
		Vars.verbose = true;
		Vars.maxThread = 1;

		String solver = "GLPK";
		if (System.getProperties().containsKey("solver")) {
			solver = System.getProperty("solver");
		}

		java.nio.file.Path tmpSbml = null;
		java.nio.file.Path tmpCondition = null;
		java.nio.file.Path tmpRegulation = null;

		try {
			tmpSbml = java.nio.file.Files.createTempFile("test", ".xml");
			tmpCondition = java.nio.file.Files.createTempFile("test", ".tab");
			tmpRegulation = java.nio.file.Files.createTempFile("test", ".txt");
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			Assert.fail("Creation of the temporary files");
			e1.printStackTrace();
		}

		tempSbmlFile = tmpSbml.toFile();
		tempConditionFile = tmpCondition.toFile();
		tempRegulationFile = tmpRegulation.toFile();

		String sbmlFile = "";
		String conditionFile = "";
		String regFile = "";

		try {
			sbmlFile = TestUtils
					.copyProjectResource(
							"flexflux/unit_tests/data/ko/test.xml",
							tempSbmlFile);
			conditionFile = TestUtils
					.copyProjectResource(
							"flexflux/unit_tests/data/ko/constraintsWithVariables.txt",
							tempConditionFile);
			regFile = TestUtils
					.copyProjectResource(
							"flexflux/unit_tests/data/ko/interactions.sbml",
							tempRegulationFile);

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
		bind.loadConstraintsFile(conditionFile);
		bind.loadRegulationFile(regFile);
		
		bind.prepareSolver();

		/**
		 * Test essential genes
		 */
		KOAnalysis a = new KOAnalysis(bind, 1, null);
		KOResult res = a.runAnalysis();
		
		Set<String> refEssentialGenes = new HashSet<String>();
		refEssentialGenes.add("G2");
		Set<String> essentialGenes = res.getEssentialEntities().keySet();
		
		Assert.assertEquals(refEssentialGenes, essentialGenes);
		
		/**
		 * Test essential reactions
		 */
		a = new KOAnalysis(bind, 0, null);
		res = a.runAnalysis();
		
		Set<String> refEssentialReactions = new HashSet<String>();
		refEssentialReactions.add("R2");
		refEssentialReactions.add("R_D_EX");
		refEssentialReactions.add("R_C_EX");
		Set<String> essentialReactions = res.getEssentialEntities().keySet();
		
		Assert.assertEquals(refEssentialReactions, essentialReactions);
		
	}

}
