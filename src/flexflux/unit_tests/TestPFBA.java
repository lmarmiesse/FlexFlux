package flexflux.unit_tests;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import junitx.framework.FileAssert;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import parsebionet.biodata.BioNetwork;
import parsebionet.io.Sbml2Bionetwork;
import parsebionet.unit_tests.utils.TestUtils;
import flexflux.analyses.PFBAAnalysis;
import flexflux.analyses.result.PFBAResult;
import flexflux.general.Bind;
import flexflux.general.CplexBind;
import flexflux.general.GLPKBind;

public class TestPFBA extends FFUnitTest {

	private static Bind b = null;

	private static File tempSbmlFile = null;
	private static File tempConstraintFile = null;
	private static File tempInteractionFile = null;

	/**
	 * Network without trimming dead reactions
	 */
	private static BioNetwork networkRef;

	static PFBAResult res = null;

	static String tmpDir = "/tmp/testPfba";

	@BeforeClass
	public static void init() throws ClassNotFoundException,
			NoSuchMethodException, SecurityException, InstantiationException,
			IllegalAccessException, IllegalArgumentException,
			InvocationTargetException {

		String solver = "GLPK";
		if (System.getProperties().containsKey("solver")) {
			solver = System.getProperty("solver");
		}

		if (solver.equals("CPLEX")) {
			b = new CplexBind();
		} else {
			b = new GLPKBind();
		}

		java.nio.file.Path tmpSbml = null;
		java.nio.file.Path tmpConstraint = null;
		java.nio.file.Path tmpInteraction = null;

		try {
			tmpSbml = java.nio.file.Files.createTempFile("test", ".xml");
			tmpConstraint = java.nio.file.Files.createTempFile("test", ".tab");
			tmpInteraction = java.nio.file.Files.createTempFile("test", ".tab");
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			Assert.fail("Creation of the temporary files");
			e1.printStackTrace();
		}

		tempSbmlFile = tmpSbml.toFile();
		tempConstraintFile = tmpConstraint.toFile();
		tempInteractionFile = tmpInteraction.toFile();

		String sbmlFile = null;
		String constraintFile = null;
		String regulationFile = null;

		try {
			sbmlFile = TestUtils.copyProjectResource(
					"flexflux/unit_tests/data/pfba/test.xml", tempSbmlFile);
			constraintFile = TestUtils.copyProjectResource(
					"flexflux/unit_tests/data/pfba/constraints.txt",
					tempConstraintFile);
			regulationFile = TestUtils.copyProjectResource(
					"flexflux/unit_tests/data/pfba/interactions.sbml",
					tempInteractionFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Assert.fail("problem while copying the input files");
		}

		Sbml2Bionetwork s2b = new Sbml2Bionetwork(sbmlFile);
		s2b.convert();
		networkRef = s2b.getBioNetwork();

		b.loadSbmlNetwork(sbmlFile, false);
		b.loadConstraintsFile(constraintFile);
		b.loadRegulationFile(regulationFile);

		b.prepareSolver();

		PFBAAnalysis a = new PFBAAnalysis(b, true);

		res = a.runAnalysis();

		// Creation of tmpDir
		File dir = new File(tmpDir);
		if (!dir.exists()) {
			try {
				dir.mkdirs();

			} catch (SecurityException se) {
				se.printStackTrace();
				System.err.println("Security Exception during creation of "
						+ tmpDir);
			}
		}

	}

	@Test
	public void testEssential() {

		Set<String> test = res.essentialReactions.keySet();

		Set<String> ref = new HashSet<String>();

		ref.add("R8");
		ref.add("R1");
		ref.add("R_B_EX");
		ref.add("R_C_EX");
		ref.add("R_OBJ_EX");
		ref.add("R_A_EX");

		Assert.assertEquals(ref, test);

	}

	@Test
	public void testZeroFluxReactions() {

		Set<String> test = res.zeroFluxReactions.keySet();

		Set<String> ref = new HashSet<String>();

		// because of lb=ub=0 for R_I_Ex
		ref.add("R2");
		ref.add("R_I_EX");
		// because of T2 = 0
		ref.add("R3");
		ref.add("R_E_EX");
		ref.add("R_F_EX");

		Assert.assertEquals(ref, test);

	}

	@Test
	public void testMleReactions() {

		Set<String> test = res.mleReactions.keySet();

		Set<String> ref = new HashSet<String>();

		// R1 uses H
		// ref.add("R1");
		// ref.add("R_B_EX");
		// ref.add("R_C_EX");

		Assert.assertEquals(ref, test);

	}

	@Test
	public void testConcurrentReactions() {

		Set<String> test = res.concurrentReactions.keySet();

		Set<String> ref = new HashSet<String>();

		ref.add("R7");
		ref.add("R_OBJ2_EX");

		Assert.assertEquals(ref, test);

	}

	@Test
	public void testEleReactions() {

		Set<String> test = res.eleReactions.keySet();

		Set<String> ref = new HashSet<String>();

		ref.add("R4");
		ref.add("R6");

		Assert.assertEquals(ref, test);

	}

	@Test
	public void testOptimaReactions() {

		Set<String> test = res.optimaReactions.keySet();

		Set<String> ref = new HashSet<String>();

		// ref.add("R3");
		ref.add("R5");

		Assert.assertEquals(ref, test);

	}

	@Test
	public void testObjectiveIndependantReactions() {

		Set<String> test = res.objectiveIndependentReactions.keySet();

		Set<String> ref = new HashSet<String>();

		ref.add("R_P_EX");
		ref.add("R_Q_EX");
		ref.add("RAV");

		Assert.assertEquals(ref, test);
	}

	@Test
	public void testEssentialGenes() {

		Set<String> test = res.essentialGenes.keySet();

		Set<String> ref = new HashSet<String>();

		ref.add("G8");
		ref.add("G1");

		Assert.assertEquals(ref, test);
	}

	@Test
	public void testZeroFluxGenes() {

		Set<String> test = res.zeroFluxGenes.keySet();

		Set<String> ref = new HashSet<String>();

		ref.add("G9");
		ref.add("G3");
		ref.add("G2");

		Assert.assertEquals(ref, test);
	}

	@Test
	public void testMleGenes() {

		Set<String> test = res.mleGenes.keySet();

		Set<String> ref = new HashSet<String>();

		Assert.assertEquals(ref, test);
	}

	@Test
	public void testEleGenes() {

		Set<String> test = res.eleGenes.keySet();

		Set<String> ref = new HashSet<String>();

		ref.add("G4");
		ref.add("G6");

		Assert.assertEquals(ref, test);
	}

	@Test
	public void testObjectiveIndependantGenes() {

		Set<String> test = res.objectiveIndependentGenes.keySet();

		Set<String> ref = new HashSet<String>();

		ref.add("G10");

		Assert.assertEquals(ref, test);
	}

	@Test
	public void testOptimaGenes() {

		Set<String> test = res.optimaGenes.keySet();

		Set<String> ref = new HashSet<String>();

		ref.add("G5");

		Assert.assertEquals(ref, test);
	}

	@Test
	public void testConcurrentGenes() {

		Set<String> test = res.concurrentGenes.keySet();

		Set<String> ref = new HashSet<String>();

		ref.add("G7");

		Assert.assertEquals(ref, test);
	}

	@Test
	public void testGetReactionClassification() {

		HashMap<String, String> classif = res.getReactionClassification();

		Assert.assertEquals(
				"test the number of reactions in the classification",
				networkRef.getBiochemicalReactionList().size(), classif.size());
		Assert.assertEquals("test independent reaction in the classif",
				"independent", classif.get("R_P_EX"));
		Assert.assertEquals("test optima reaction in the classif", "optima",
				classif.get("R5"));
		Assert.assertEquals("test ele reaction in the classif", "ele",
				classif.get("R4"));
		Assert.assertEquals("test concurrent reaction in the classif",
				"concurrent", classif.get("R7"));
		Assert.assertEquals("test zeroFlux reaction in the classif",
				"zeroFlux", classif.get("R2"));
		Assert.assertEquals("test dead reaction in the classif", "dead",
				classif.get("DEAD"));

	}

	@Test
	public void testWriteCytoscapeClassifAttributes() {

		String fileTest = tmpDir + "/classif.attr";

		res.writeCytoscapeClassifAttribute(fileTest, true);

		try {
			String fileRef = TestUtils.copyProjectResource(
					"flexflux/unit_tests/data/pfba/classif.attr",
					java.nio.file.Files.createTempFile("test", ".attr")
							.toFile());

			FileAssert.assertEquals("test classif attribute file", new File(
					fileRef), new File(fileTest));

		} catch (IOException e) {
			Assert.fail("Impossible to get the reference file");
		}

	}
	
	@Test
	public void testWriteCytoscapeGenericAttributes() {

		String fileTest = tmpDir + "/genericAttributes.tab";

		res.writeCytoscapeGenericAttributes(fileTest, true);

		try {
			String fileRef = TestUtils.copyProjectResource(
					"flexflux/unit_tests/data/pfba/genericAttributes.tab",
					java.nio.file.Files.createTempFile("test", ".tab")
							.toFile());

			FileAssert.assertEquals("test generic attribute file", new File(
					fileRef), new File(fileTest));

		} catch (IOException e) {
			Assert.fail("Impossible to get the reference file");
		}

	}

	@AfterClass
	public static void afterTests() {

		new File(tmpDir).delete();

	}

}
