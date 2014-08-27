package flexflux.unit_tests;

import java.io.File;
import java.io.IOException;

import junitx.framework.FileAssert;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import parsebionet.unit_tests.TestUtils;
import flexflux.analyses.ConditionComparisonAnalysis;
import flexflux.analyses.result.AnalysisResult;
import flexflux.applications.FlexfluxConditionComparison;
import flexflux.general.ConstraintType;
import flexflux.general.Vars;

public class TestConditionComparison {

	static FlexfluxConditionComparison f;

	private static File tempSbmlFile = null;
	private static File tempConditionFile = null;
	private static File tempInteractionFile = null;
	private static File tempObjectiveFile = null;
	private static File tempResultFbaFile = null;
	private static File tempResultEssentialFile = null;
	private static File tempResultUsedFile = null;
	private static File tempResultDeadFile = null;

	private static String basePath = "";

	// The temporary folder will be removed at the end of the test
	@Rule
	public TemporaryFolder temp = new TemporaryFolder();

	@Test
	public void testMain() {

		String solver = "GLPK";
		if (System.getProperties().containsKey("solver")) {
			solver = System.getProperty("solver");
		}

		f = new FlexfluxConditionComparison();

		java.nio.file.Path tmpSbml = null;
		java.nio.file.Path tmpCondition = null;
		java.nio.file.Path tmpInteraction = null;
		java.nio.file.Path tmpObjective = null;
		java.nio.file.Path tmpResultFba = null;
		java.nio.file.Path tmpResultEssential = null;
		java.nio.file.Path tmpResultUsed = null;
		java.nio.file.Path tmpResultDead = null;

//		Vars.writeInteractionNetworkStates = true;

		try {
			tmpSbml = java.nio.file.Files.createTempFile("test", ".xml");
			tmpCondition = java.nio.file.Files.createTempFile("test", ".tab");
			tmpInteraction = java.nio.file.Files.createTempFile("test", ".txt");
			tmpObjective = java.nio.file.Files.createTempFile("test", ".tab");
			tmpResultFba = java.nio.file.Files.createTempFile("test", ".tab");
			tmpResultEssential = java.nio.file.Files.createTempFile("test",
					".tab");
			tmpResultUsed = java.nio.file.Files.createTempFile("test", ".tab");
			tmpResultDead = java.nio.file.Files.createTempFile("test", ".tab");

		} catch (IOException e1) {
			// TODO Auto-generated catch block
			Assert.fail("Creation of the temporary files");
			e1.printStackTrace();
		}

		tempSbmlFile = tmpSbml.toFile();
		tempConditionFile = tmpCondition.toFile();
		tempObjectiveFile = tmpObjective.toFile();
		tempInteractionFile = tmpInteraction.toFile();
		tempResultFbaFile = tmpResultFba.toFile();
		tempResultEssentialFile = tmpResultEssential.toFile();
		tempResultUsedFile = tmpResultUsed.toFile();
		tempResultDeadFile = tmpResultDead.toFile();

		String referenceFbaFile = "";
		String referenceEssentialFile = "";
		String referenceUsedFile = "";
		String referenceDeadFile = "";

		try {
			f.sbmlFile = TestUtils
					.copyProjectResource(
							"flexflux/unit_tests/data/conditionComparisonTest/test.xml",
							tempSbmlFile);
			f.conditionFile = TestUtils
					.copyProjectResource(
							"flexflux/unit_tests/data/conditionComparisonTest/conditions.tab",
							tempConditionFile);
			f.intFile = TestUtils
					.copyProjectResource(
							"flexflux/unit_tests/data/conditionComparisonTest/interactions.txt",
							tempInteractionFile);

			f.objectiveFile = TestUtils
					.copyProjectResource(
							"flexflux/unit_tests/data/conditionComparisonTest/objectives.txt",
							tempObjectiveFile);

			referenceFbaFile = TestUtils
					.copyProjectResource(
							"flexflux/unit_tests/data/conditionComparisonTest/resultFBA.tab",
							tempResultFbaFile);
			referenceUsedFile = TestUtils
					.copyProjectResource(
							"flexflux/unit_tests/data/conditionComparisonTest/resultUsed.tab",
							tempResultUsedFile);
			referenceEssentialFile = TestUtils
					.copyProjectResource(
							"flexflux/unit_tests/data/conditionComparisonTest/resultEssential.tab",
							tempResultEssentialFile);
			referenceDeadFile = TestUtils
					.copyProjectResource(
							"flexflux/unit_tests/data/conditionComparisonTest/resultDead.tab",
							tempResultDeadFile);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Assert.fail("problem while copying the reference files");
		}

		ConditionComparisonAnalysis a = new ConditionComparisonAnalysis(null,
				f.sbmlFile, f.intFile, f.conditionFile, "", f.objectiveFile,
				ConstraintType.DOUBLE, false, solver);

		
		AnalysisResult r = a.runAnalysis();
		

		File createdFolder;
		String dir = "";
		try {
			createdFolder = temp.newFolder("results");
			dir = createdFolder.getAbsolutePath();
		} catch (IOException e) {
			Assert.fail("Impossible to create new temporary folder");
		}

		basePath = dir + "/test_";
		
		r.writeToFile(basePath);

		String pathFileTest = basePath + "fba_results";
		File fileTest = new File(pathFileTest);
		File fileRef = new File(referenceFbaFile);

		
		FileAssert.assertEquals("Fba results are different from the reference",
				fileRef, fileTest);
		
		

		pathFileTest = basePath + "essential_reactions";
		fileTest = new File(pathFileTest);
		fileRef = new File(referenceEssentialFile);

		FileAssert.assertEquals(
				"Essential reactions are different from the reference",
				fileRef, fileTest);

		pathFileTest = basePath + "used_reactions";
		fileTest = new File(pathFileTest);
		fileRef = new File(referenceUsedFile);

		FileAssert.assertEquals(
				"Used reactions are different from the reference", fileRef,
				fileTest);

		pathFileTest = basePath + "dead_reactions";
		fileTest = new File(pathFileTest);
		fileRef = new File(referenceDeadFile);

		FileAssert.assertEquals(
				"Dead reactions are different from the reference", fileRef,
				fileTest);

	}

	@AfterClass
	public static void afterTest() {
		if (tempSbmlFile != null)
			tempSbmlFile.delete();
		if (tempConditionFile != null)
			tempConditionFile.delete();
		if (tempInteractionFile != null)
			tempInteractionFile.delete();
		if (tempObjectiveFile != null)
			tempObjectiveFile.delete();
		if (tempResultFbaFile != null)
			tempResultFbaFile.delete();

	}

}
