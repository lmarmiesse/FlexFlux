package flexflux.unit_tests;

import java.io.File;
import java.io.IOException;

import junitx.framework.FileAssert;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import parsebionet.unit_tests.utils.TestUtils;
import flexflux.analyses.BECOAnalysis;
import flexflux.analyses.result.beco.BECOResult;
import flexflux.applications.FlexfluxBECO;
import flexflux.general.ConstraintType;
import flexflux.general.Vars;
import flexflux.objective.ListOfObjectives;

public class TestBECO extends FFUnitTest{

	private static boolean verbose = true;

	static FlexfluxBECO f;

	private static File tempSbmlFile = null;
	private static File tempConditionFile = null;
	private static File tempRegulationFile = null;
	private static File tempObjectiveFile = null;
	private static File tempConstraintFile = null;
	private static File tempResultFbaFile = null;
	private static File tempResultFbaFixedFile = null;
	private static File tempResultEssentialFile = null;
	private static File tempResultZeroFluxFile = null;
	private static File tempResultMleFile = null;
	private static File tempResultEleFile = null;
	private static File tempResultConcurrentFile = null;
	private static File tempResultIndependentFile = null;
	private static File tempResultOptimaFile = null;
	private static File tempResultDeadFile = null;

	private static File tempResultEssentialGeneFile = null;
	private static File tempResultZeroFluxGeneFile = null;
	private static File tempResultMleGeneFile = null;
	private static File tempResultConcurrentGeneFile = null;
	private static File tempResultEleGeneFile = null;
	private static File tempResultIndependentGeneFile = null;
	private static File tempResultOptimaGeneFile = null;
	private static File tempResultDeadGeneFile = null;

	private static File tempResultEssentialRegulatorFile = null;
	private static File tempResultOptimaRegulatorFile = null;
	private static File tempResultNeutralRegulatorFile = null;

	private static File tempMetaDataRegulatorFile = null;
	private static File tempMetaDataGeneFile = null;
	private static File tempMetaDataReactionFile = null;

	private static String referenceFbaFile = "";
	private static String referenceEssentialFile = "";
	private static String referenceZeroFluxFile = "";
	private static String referenceMleFile = "";
	private static String referenceEleFile = "";
	private static String referenceConcurrentFile = "";
	private static String referenceIndependentFile = "";
	private static String referenceOptimaFile = "";
	private static String referenceDeadFile = "";
	
	private static String referenceFbaFixedFile = "";

	private static String referenceEssentialGeneFile = "";
	private static String referenceZeroFluxGeneFile = "";
	private static String referenceMleGeneFile = "";
	private static String referenceConcurrentGeneFile = "";
	private static String referenceEleGeneFile = "";
	private static String referenceIndependentGeneFile = "";
	private static String referenceOptimaGeneFile = "";
	private static String referenceDeadGeneFile = "";


	private static String referenceEssentialRegulatorFile = "";
	private static String referenceOptimaRegulatorFile = "";
	private static String referenceNeutralRegulatorFile = "";

	private static String metaDataGeneFile = "";
	private static String metaDataRegulatorFile = "";
	private static String metaDataReactionFile = "";

	private static String basePath = "";
	private static String basePath2 = "";

	private static String tmpPath = "/tmp/testConditionComparison";
	
	
	private static BECOResult r;

	// The temporary folder will be removed at the end of the test
	private static File tempDir;

	@BeforeClass
	public static void init() throws IOException {

		Vars.writeInteractionNetworkStates = false;
		
		Vars.maxThread = 20;
//		Vars.maxThread = 2;
		
		

		String solver = "GLPK";
		if (System.getProperties().containsKey("solver")) {
			solver = System.getProperty("solver");
		}

		String inchlibPath = "";
		if (System.getProperties().containsKey("inchlibPath")) {
			inchlibPath = System.getProperty("inchlibPath");
		}

		f = new FlexfluxBECO();

		java.nio.file.Path tmpSbml = null;
		java.nio.file.Path tmpCondition = null;
		java.nio.file.Path tmpRegulation = null;
		java.nio.file.Path tmpObjective = null;
		java.nio.file.Path tmpConstraint = null;
		java.nio.file.Path tmpResultFba = null;
		java.nio.file.Path tmpResultFbaFixed = null;
		java.nio.file.Path tmpResultEssential = null;
		java.nio.file.Path tmpResultZeroFlux = null;
		java.nio.file.Path tmpResultMle = null;
		java.nio.file.Path tmpResultEle = null;
		java.nio.file.Path tmpResultConcurrent = null;
		java.nio.file.Path tmpResultIndependent = null;
		java.nio.file.Path tmpResultOptima = null;
		java.nio.file.Path tmpResultDead = null;

		java.nio.file.Path tmpResultEssentialGenes = null;
		java.nio.file.Path tmpResultZeroFluxGenes = null;
		java.nio.file.Path tmpResultMleGenes = null;
		java.nio.file.Path tmpResultConcurrentGenes = null;
		java.nio.file.Path tmpResultEleGenes = null;
		java.nio.file.Path tmpResultIndependentGenes = null;
		java.nio.file.Path tmpResultOptimaGenes = null;
		java.nio.file.Path tmpResultDeadGenes = null;

		java.nio.file.Path tmpResultEssentialRegulators = null;
		java.nio.file.Path tmpResultOptimaRegulators = null;
		java.nio.file.Path tmpResultNeutralRegulators = null;

		java.nio.file.Path tmpMetaDataRegulators = null;
		java.nio.file.Path tmpMetaDataGenes = null;
		java.nio.file.Path tmpMetaDataReactions = null;

		try {
			tmpSbml = java.nio.file.Files.createTempFile("test", ".xml");
			tmpCondition = java.nio.file.Files.createTempFile("test", ".tab");
			tmpRegulation = java.nio.file.Files.createTempFile("test", ".txt");
			tmpObjective = java.nio.file.Files.createTempFile("test", ".tab");
			tmpConstraint = java.nio.file.Files.createTempFile("test", ".tab");
			tmpResultFba = java.nio.file.Files.createTempFile("test", ".tab");
			tmpResultFbaFixed = java.nio.file.Files.createTempFile("test", ".tab");
			tmpResultEssential = java.nio.file.Files.createTempFile("test",
					".tab");
			tmpResultZeroFlux = java.nio.file.Files.createTempFile("test",
					".tab");
			tmpResultMle = java.nio.file.Files.createTempFile("test", ".tab");
			tmpResultEle = java.nio.file.Files.createTempFile("test", ".tab");
			tmpResultConcurrent = java.nio.file.Files.createTempFile("test",
					".tab");
			tmpResultIndependent = java.nio.file.Files.createTempFile("test",
					".tab");
			tmpResultOptima = java.nio.file.Files
					.createTempFile("test", ".tab");
			tmpResultDead = java.nio.file.Files
					.createTempFile("test", ".tab");

			tmpResultEssentialGenes = java.nio.file.Files.createTempFile(
					"test", ".tab");
			tmpResultZeroFluxGenes = java.nio.file.Files.createTempFile("test",
					".tab");
			tmpResultMleGenes = java.nio.file.Files.createTempFile("test",
					".tab");
			tmpResultConcurrentGenes = java.nio.file.Files.createTempFile(
					"test", ".tab");
			tmpResultEleGenes = java.nio.file.Files.createTempFile("test",
					".tab");
			tmpResultIndependentGenes = java.nio.file.Files.createTempFile(
					"test", ".tab");
			tmpResultOptimaGenes = java.nio.file.Files.createTempFile("test",
					".tab");
			tmpResultEssentialRegulators = java.nio.file.Files.createTempFile(
					"test", ".tab");
			tmpResultOptimaRegulators = java.nio.file.Files.createTempFile(
					"test", ".tab");
			tmpResultNeutralRegulators = java.nio.file.Files.createTempFile(
					"test", ".tab");
			tmpMetaDataGenes = java.nio.file.Files.createTempFile("test",
					".tab");
			tmpMetaDataRegulators = java.nio.file.Files.createTempFile("test",
					".tab");
			tmpMetaDataReactions = java.nio.file.Files.createTempFile("test",
					".tab");
			
			tmpResultDeadGenes = java.nio.file.Files
					.createTempFile("test", ".tab");

		} catch (IOException e1) {
			// TODO Auto-generated catch block
			Assert.fail("Creation of the temporary files");
			e1.printStackTrace();
		}

		tempSbmlFile = tmpSbml.toFile();
		tempConditionFile = tmpCondition.toFile();
		tempObjectiveFile = tmpObjective.toFile();
		tempRegulationFile = tmpRegulation.toFile();
		tempConstraintFile = tmpConstraint.toFile();

		tempResultFbaFile = tmpResultFba.toFile();
		tempResultFbaFixedFile = tmpResultFbaFixed.toFile();
		tempResultEssentialFile = tmpResultEssential.toFile();
		tempResultZeroFluxFile = tmpResultZeroFlux.toFile();
		tempResultMleFile = tmpResultMle.toFile();
		tempResultEleFile = tmpResultEle.toFile();
		tempResultConcurrentFile = tmpResultConcurrent.toFile();
		tempResultIndependentFile = tmpResultIndependent.toFile();
		tempResultOptimaFile = tmpResultOptima.toFile();
		tempResultDeadFile = tmpResultDead.toFile();
		
		tempResultEssentialGeneFile = tmpResultEssentialGenes.toFile();
		tempResultZeroFluxGeneFile = tmpResultZeroFluxGenes.toFile();
		tempResultMleGeneFile = tmpResultMleGenes.toFile();
		tempResultConcurrentGeneFile = tmpResultConcurrentGenes.toFile();
		tempResultEleGeneFile = tmpResultEleGenes.toFile();
		tempResultIndependentGeneFile = tmpResultIndependentGenes.toFile();
		tempResultOptimaGeneFile = tmpResultOptimaGenes.toFile();
		tempResultDeadGeneFile = tmpResultDeadGenes.toFile();

		tempResultEssentialRegulatorFile = tmpResultEssentialRegulators
				.toFile();
		tempResultOptimaRegulatorFile = tmpResultOptimaRegulators.toFile();
		tempResultNeutralRegulatorFile = tmpResultNeutralRegulators.toFile();

		tempMetaDataGeneFile = tmpMetaDataGenes.toFile();
		tempMetaDataReactionFile = tmpMetaDataReactions.toFile();
		tempMetaDataRegulatorFile = tmpMetaDataRegulators.toFile();

		try {
			f.sbmlFile = TestUtils
					.copyProjectResource(
							"flexflux/unit_tests/data/conditionComparisonTest/test.xml",
							tempSbmlFile);
			f.conditionFile = TestUtils
					.copyProjectResource(
							"flexflux/unit_tests/data/conditionComparisonTest/conditions.tab",
							tempConditionFile);
			f.regFile = TestUtils
					.copyProjectResource(
							"flexflux/unit_tests/data/conditionComparisonTest/interactions.sbml",
							tempRegulationFile);

			f.objectiveFile = TestUtils
					.copyProjectResource(
							"flexflux/unit_tests/data/conditionComparisonTest/objectives.txt",
							tempObjectiveFile);

			f.constraintFile = TestUtils
					.copyProjectResource(
							"flexflux/unit_tests/data/conditionComparisonTest/constraints.txt",
							tempConstraintFile);

			referenceFbaFile = TestUtils
					.copyProjectResource(
							"flexflux/unit_tests/data/conditionComparisonTest/resultFBA.tab",
							tempResultFbaFile);
			
			referenceFbaFixedFile = TestUtils
					.copyProjectResource(
							"flexflux/unit_tests/data/conditionComparisonTest/resultFBA_fixed.tab",
							tempResultFbaFixedFile);
			
			referenceEssentialFile = TestUtils
					.copyProjectResource(
							"flexflux/unit_tests/data/conditionComparisonTest/resultEssential.tab",
							tempResultEssentialFile);
			referenceZeroFluxFile = TestUtils
					.copyProjectResource(
							"flexflux/unit_tests/data/conditionComparisonTest/resultZeroFlux.tab",
							tempResultZeroFluxFile);

			referenceMleFile = TestUtils
					.copyProjectResource(
							"flexflux/unit_tests/data/conditionComparisonTest/resultMle.tab",
							tempResultMleFile);

			referenceEleFile = TestUtils
					.copyProjectResource(
							"flexflux/unit_tests/data/conditionComparisonTest/resultEle.tab",
							tempResultEleFile);

			referenceConcurrentFile = TestUtils
					.copyProjectResource(
							"flexflux/unit_tests/data/conditionComparisonTest/resultConcurrent.tab",
							tempResultConcurrentFile);

			referenceIndependentFile = TestUtils
					.copyProjectResource(
							"flexflux/unit_tests/data/conditionComparisonTest/resultIndependent.tab",
							tempResultIndependentFile);

			referenceOptimaFile = TestUtils
					.copyProjectResource(
							"flexflux/unit_tests/data/conditionComparisonTest/resultOptima.tab",
							tempResultOptimaFile);
			
			referenceDeadFile = TestUtils
					.copyProjectResource(
							"flexflux/unit_tests/data/conditionComparisonTest/resultDead.tab",
							tempResultDeadFile);

			referenceEssentialGeneFile = TestUtils
					.copyProjectResource(
							"flexflux/unit_tests/data/conditionComparisonTest/resultEssentialGenes.tab",
							tempResultEssentialGeneFile);

			referenceZeroFluxGeneFile = TestUtils
					.copyProjectResource(
							"flexflux/unit_tests/data/conditionComparisonTest/resultZeroFluxGenes.tab",
							tempResultZeroFluxGeneFile);

			referenceMleGeneFile = TestUtils
					.copyProjectResource(
							"flexflux/unit_tests/data/conditionComparisonTest/resultMleGenes.tab",
							tempResultMleGeneFile);

			referenceEleGeneFile = TestUtils
					.copyProjectResource(
							"flexflux/unit_tests/data/conditionComparisonTest/resultEleGenes.tab",
							tempResultEleGeneFile);

			referenceConcurrentGeneFile = TestUtils
					.copyProjectResource(
							"flexflux/unit_tests/data/conditionComparisonTest/resultConcurrentGenes.tab",
							tempResultConcurrentGeneFile);

			referenceIndependentGeneFile = TestUtils
					.copyProjectResource(
							"flexflux/unit_tests/data/conditionComparisonTest/resultIndependentGenes.tab",
							tempResultIndependentGeneFile);

			referenceOptimaGeneFile = TestUtils
					.copyProjectResource(
							"flexflux/unit_tests/data/conditionComparisonTest/resultOptimaGenes.tab",
							tempResultOptimaGeneFile);

			referenceDeadGeneFile = TestUtils
					.copyProjectResource(
							"flexflux/unit_tests/data/conditionComparisonTest/resultDeadGenes.tab",
							tempResultDeadGeneFile);
			
			referenceEssentialRegulatorFile = TestUtils
					.copyProjectResource(
							"flexflux/unit_tests/data/conditionComparisonTest/resultEssentialRegulators.tab",
							tempResultEssentialRegulatorFile);

			referenceOptimaRegulatorFile = TestUtils
					.copyProjectResource(
							"flexflux/unit_tests/data/conditionComparisonTest/resultOptimaRegulators.tab",
							tempResultOptimaRegulatorFile);

			referenceNeutralRegulatorFile = TestUtils
					.copyProjectResource(
							"flexflux/unit_tests/data/conditionComparisonTest/resultNeutralRegulators.tab",
							tempResultNeutralRegulatorFile);

			metaDataGeneFile = TestUtils
					.copyProjectResource(
							"flexflux/unit_tests/data/conditionComparisonTest/metaDataGenes.csv",
							tempMetaDataGeneFile);

			metaDataReactionFile = TestUtils
					.copyProjectResource(
							"flexflux/unit_tests/data/conditionComparisonTest/metaDataReactions.csv",
							tempMetaDataReactionFile);

			metaDataRegulatorFile = TestUtils
					.copyProjectResource(
							"flexflux/unit_tests/data/conditionComparisonTest/metaDataRegulators.csv",
							tempMetaDataRegulatorFile);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Assert.fail("problem while copying the reference files");
		}
		
		if(verbose)
		{
			Vars.verbose = true;
		}

		ListOfObjectives objectives = new ListOfObjectives();
		
		Boolean flag = objectives.loadObjectiveFile(f.objectiveFile);
		
		if (flag == false) {
			Assert.fail("Error in reading the objective file");
		}
		
		// Conditions are not fixed
		BECOAnalysis a = new BECOAnalysis(null,
				f.sbmlFile, f.regFile, f.conditionFile, f.constraintFile,
				objectives, false, solver,
				metaDataReactionFile, metaDataGeneFile, metaDataRegulatorFile,
				",", inchlibPath, false, false, false, 0.0, 6, false);

		r = a.runAnalysis();

		tempDir = new File(tmpPath);
		if (!tempDir.exists()) {
			tempDir.mkdir();
		}

		basePath = tmpPath + "/test_";

		r.writeToFile(basePath);
		
		r.writeCytoscapeFiles(basePath+"/cytoscape", true);
		
		// Conditions are fixed
		a = new BECOAnalysis(null,
				f.sbmlFile, f.regFile, f.conditionFile, f.constraintFile,
				objectives, false, solver,
				metaDataReactionFile, metaDataGeneFile, metaDataRegulatorFile,
				",", inchlibPath, false, false, false, 0.0, 6, true);

		r = a.runAnalysis();

		tempDir = new File(tmpPath);
		if (!tempDir.exists()) {
			tempDir.mkdir();
		}

		basePath2 = tmpPath + "/test_fixed";

		r.writeToFile(basePath2);
		
		r.writeCytoscapeFiles(basePath2+"/cytoscape", true);

	}

	@Test
	public void testFba() {

		String pathFileTest = basePath + "/fba_results.csv";
		File fileTest = new File(pathFileTest);
		File fileRef = new File(referenceFbaFile);

		FileAssert.assertEquals("Fba results are different from the reference",
				fileRef, fileTest);
		

	}

	@Test
	public void testEssential() {

		String pathFileTest = basePath + "/essentialReactions.tsv";
		File fileTest = new File(pathFileTest);
		File fileRef = new File(referenceEssentialFile);

		FileAssert.assertEquals(
				"Essential reactions are different from the reference",
				fileRef, fileTest);

	}

	@Test
	public void testZeroFlux() {

		String pathFileTest = basePath + "/zeroFluxReactions.tsv";
		File fileTest = new File(pathFileTest);
		File fileRef = new File(referenceZeroFluxFile);

		FileAssert.assertEquals(
				"Zero Flux reactions are different from the reference",
				fileRef, fileTest);

	}

	@Test
	public void testMle() {

		String pathFileTest = basePath + "/mleReactions.tsv";
		File fileTest = new File(pathFileTest);
		File fileRef = new File(referenceMleFile);

		FileAssert.assertEquals(
				"Mle reactions are different from the reference", fileRef,
				fileTest);

	}

	@Test
	public void testConcurrent() {

		String pathFileTest = basePath + "/concurrentReactions.tsv";
		File fileTest = new File(pathFileTest);
		File fileRef = new File(referenceConcurrentFile);

		FileAssert.assertEquals(
				"Concurrent reactions are different from the reference",
				fileRef, fileTest);

	}

	@Test
	public void testEle() {

		String pathFileTest = basePath + "/eleReactions.tsv";
		File fileTest = new File(pathFileTest);
		File fileRef = new File(referenceEleFile);

		FileAssert.assertEquals(
				"Ele reactions are different from the reference", fileRef,
				fileTest);

	}

	@Test
	public void testIndependent() {

		String pathFileTest = basePath + "/independentReactions.tsv";
		File fileTest = new File(pathFileTest);
		File fileRef = new File(referenceIndependentFile);

		FileAssert
				.assertEquals(
						"Objective independent reactions are different from the reference",
						fileRef, fileTest);

	}

	@Test
	public void testOptima() {

		String pathFileTest = basePath + "/optimaReactions.tsv";
		File fileTest = new File(pathFileTest);
		File fileRef = new File(referenceOptimaFile);

		FileAssert.assertEquals(
				"pFBA optimal reactions are different from the reference",
				fileRef, fileTest);

	}

	@Test
	public void testEssentialGenes() {

		String pathFileTest = basePath + "/essentialGenes.tsv";
		File fileTest = new File(pathFileTest);
		File fileRef = new File(referenceEssentialGeneFile);

		FileAssert.assertEquals(
				"Essential genes are different from the reference", fileRef,
				fileTest);

	}

	@Test
	public void testZeroFluxGenes() {

		String pathFileTest = basePath + "/zeroFluxGenes.tsv";
		File fileTest = new File(pathFileTest);
		File fileRef = new File(referenceZeroFluxGeneFile);

		FileAssert.assertEquals(
				"Zero Flux genes are different from the reference", fileRef,
				fileTest);

	}

	@Test
	public void testMleGenes() {

		String pathFileTest = basePath + "/mleGenes.tsv";
		File fileTest = new File(pathFileTest);
		File fileRef = new File(referenceMleGeneFile);

		FileAssert.assertEquals("Mle genes are different from the reference",
				fileRef, fileTest);

	}

	@Test
	public void testConcurrentGenes() {

		String pathFileTest = basePath + "/concurrentGenes.tsv";
		File fileTest = new File(pathFileTest);
		File fileRef = new File(referenceConcurrentGeneFile);

		FileAssert.assertEquals(
				"Concurrent genes are different from the reference", fileRef,
				fileTest);

	}

	@Test
	public void testEleGenes() {

		String pathFileTest = basePath + "/eleGenes.tsv";
		File fileTest = new File(pathFileTest);
		File fileRef = new File(referenceEleGeneFile);

		FileAssert.assertEquals("Ele genes are different from the reference",
				fileRef, fileTest);

	}

	@Test
	public void testIndependentGenes() {

		String pathFileTest = basePath + "/independentGenes.tsv";
		File fileTest = new File(pathFileTest);
		File fileRef = new File(referenceIndependentGeneFile);

		FileAssert.assertEquals(
				"Independent genes are different from the reference", fileRef,
				fileTest);

	}

	@Test
	public void testOptimaGenes() {

		String pathFileTest = basePath + "/optimaGenes.tsv";
		File fileTest = new File(pathFileTest);
		File fileRef = new File(referenceOptimaGeneFile);

		FileAssert.assertEquals(
				"pFBA optimal genes are different from the reference", fileRef,
				fileTest);

	}

	@Test
	public void testEssentialRegulators() {

		String pathFileTest = basePath + "/essentialRegulators.tsv";
		File fileTest = new File(pathFileTest);
		File fileRef = new File(referenceEssentialRegulatorFile);

		FileAssert.assertEquals(
				"Essential regulators are different from the reference",
				fileRef, fileTest);
	}

	@Test
	public void testOptimaRegulators() {

		String pathFileTest = basePath + "/optimaRegulators.tsv";
		File fileTest = new File(pathFileTest);
		File fileRef = new File(referenceOptimaRegulatorFile);

		FileAssert.assertEquals(
				"Optimal regulators are different from the reference", fileRef,
				fileTest);

	}

	@Test
	public void testNeutralRegulators() {

		String pathFileTest = basePath + "/neutralRegulators.tsv";
		File fileTest = new File(pathFileTest);
		File fileRef = new File(referenceNeutralRegulatorFile);

		FileAssert.assertEquals(
				"Neutral regulators are different from the reference", fileRef,
				fileTest);

	}
	
	
	@Test
	public void testDead() {

		String pathFileTest = basePath + "/deadReactions.tsv";
		File fileTest = new File(pathFileTest);
		File fileRef = new File(referenceDeadFile);

		FileAssert.assertEquals(
				"Dead reactions are different from the reference", fileRef,
				fileTest);
	}
	
	@Test
	public void testDeadGenes() {

		String pathFileTest = basePath + "/deadGenes.tsv";
		File fileTest = new File(pathFileTest);
		File fileRef = new File(referenceDeadGeneFile);

		FileAssert.assertEquals(
				"Dead genes are different from the reference", fileRef,
				fileTest);
	}

	
	/**
	 * Test while fixing conditions
	 */
	
	@Test
	public void testFba_fixed() {

		String pathFileTest = basePath2 + "/fba_results.csv";
		File fileTest = new File(pathFileTest);
		File fileRef = new File(referenceFbaFixedFile);

		FileAssert.assertEquals("Fba results are different from the reference",
				fileRef, fileTest);

	}
	
	
	
	@AfterClass
	public static void afterTest() {

//		 tempDir.delete();

	}

}
