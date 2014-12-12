package flexflux.unit_tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import junitx.framework.FileAssert;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import parsebionet.unit_tests.TestUtils;
import flexflux.analyses.ERAAnalysis;
import flexflux.analyses.result.ERAResult;
import flexflux.condition.ListOfConditions;
import flexflux.general.Bind;
import flexflux.general.ConstraintType;
import flexflux.general.CplexBind;
import flexflux.general.GLPKBind;
import flexflux.general.Vars;
import flexflux.objective.ListOfObjectives;

public class TestEra extends FFUnitTest{

	private static boolean verbose = true;
	private static boolean plot = true;

	private static String tmpPath = "/tmp/testEra";

	private static ERAResult r;

	// The temporary folder will be removed at the end of the test
	private static File tempDir;

	static int nbSim = 100;

	@BeforeClass
	public static void init() throws IOException {

		
		Vars.verbose = verbose;

		Vars.writeInteractionNetworkStates = false;

		Vars.maxThread = 10;

		String solver = "GLPK";
		if (System.getProperties().containsKey("solver")) {
			solver = System.getProperty("solver");
		}

		String inchlibPath = "";
		if (System.getProperties().containsKey("inchlibPath")) {
			inchlibPath = System.getProperty("inchlibPath");
		}


		String sbmlFile = TestUtils.copyProjectResource(
				"flexflux/unit_tests/data/era/test.xml", java.nio.file.Files
						.createTempFile("test", ".xml").toFile());

		String regFile = TestUtils.copyProjectResource(
				"flexflux/unit_tests/data/era/interactions.sbml",
				java.nio.file.Files.createTempFile("test", ".sbml").toFile());

		String objectiveFile = TestUtils.copyProjectResource(
				"flexflux/unit_tests/data/era/objectives.txt",
				java.nio.file.Files.createTempFile("test", ".txt").toFile());

		String constraintFile = TestUtils.copyProjectResource(
				"flexflux/unit_tests/data/era/constraints.txt",
				java.nio.file.Files.createTempFile("test", ".txt").toFile());
		
		String conditionFile = TestUtils.copyProjectResource(
				"flexflux/unit_tests/data/era/conditions.tab",
				java.nio.file.Files.createTempFile("test", ".tab").toFile());


		ListOfObjectives objectives = new ListOfObjectives();
		
		Boolean flag = objectives.loadObjectiveFile(objectiveFile);

		if (flag == false) {
			System.err.println("Error in reading the objective file "
					+ objectiveFile);
			System.exit(0);
		}
		
		/**
		 * Load the condition file
		 */
		ListOfConditions conditions = new ListOfConditions();
		
		flag = conditions.loadConditionFile(conditionFile, ConstraintType.DOUBLE);
		
		if (flag == false) {
			System.err.println("Error in reading the condition file "
					+ objectiveFile);
			System.exit(0);
		}

		Bind bind = null;

		try {
			if (solver.equals("CPLEX")) {
				bind = new CplexBind();
			} else if (solver.equals("GLPK")) {
				Vars.maxThread = 1;
				bind = new GLPKBind();
			} else {
				fail("Unknown solver name");
			}
		} catch (UnsatisfiedLinkError e) {
			fail("Error, the solver "
					+ solver
					+ " cannot be found. Check your solver installation and the configuration file, or choose a different solver (-sol).");
		} catch (NoClassDefFoundError e) {
			fail("Error, the solver "
					+ solver
					+ " cannot be found. There seems to be a problem with the .jar file of "
					+ solver + ".");
		}

		bind.loadSbmlNetwork(sbmlFile, false);

		bind.setLoadObjective(false);

		if (constraintFile != "") {
			bind.loadConstraintsFile(constraintFile);
		}

		bind.loadRegulationFile(regFile);

		bind.prepareSolver();

		ERAAnalysis a = new ERAAnalysis(bind, objectives,
				conditions);

		r = a.runAnalysis();

		tempDir = new File(tmpPath);
		if (!tempDir.exists()) {
			tempDir.mkdir();
		}

		r.inchlibPath = inchlibPath;

		r.writeToFile(tmpPath);

	}

	@Test
	public void testObjCondCount() throws IOException {
		
		 String refFile = TestUtils.copyProjectResource(
					"flexflux/unit_tests/data/era/resultsActivatedObjectives.txt",
					java.nio.file.Files.createTempFile("test", ".txt").toFile());
		 
		 String testFile = tmpPath+"/activatedObjectives.tsv";
		 
		 FileAssert.assertEquals("activated objective files are different", new File(refFile), new File(testFile));
		 
	}
	
	@Test
	public void testObjInputMatrix() {
		assertEquals("test G1 maxR_OBJ", new Integer(4), r.getObjInputMatrix().get("G1").get("maxR_OBJ"));
		assertEquals("test G1 minR_OBJ2", new Integer(4), r.getObjInputMatrix().get("G1").get("minR_OBJ2"));
		
		assertEquals("test G2 maxR_OBJ", new Integer(4), r.getObjInputMatrix().get("G2").get("maxR_OBJ"));
		assertEquals("test G2 minR_OBJ2", new Integer(4), r.getObjInputMatrix().get("G2").get("minR_OBJ2"));
		
		assertEquals("test G3 maxR_OBJ", new Integer(4), r.getObjInputMatrix().get("G3").get("maxR_OBJ"));
		assertEquals("test G3 minR_OBJ2", new Integer(5), r.getObjInputMatrix().get("G3").get("minR_OBJ2"));
		
		assertEquals("test G8 maxR_OBJ", new Integer(4), r.getObjInputMatrix().get("G8").get("maxR_OBJ"));
		assertEquals("test G8 minR_OBJ2", new Integer(4), r.getObjInputMatrix().get("G8").get("minR_OBJ2"));
		
		assertEquals("test T1 maxR_OBJ", new Integer(2), r.getObjInputMatrix().get("T1").get("maxR_OBJ"));
		assertEquals("test T1 minR_OBJ2", new Integer(2), r.getObjInputMatrix().get("T1").get("minR_OBJ2"));
		
		assertEquals("test T2 maxR_OBJ", new Integer(2), r.getObjInputMatrix().get("T2").get("maxR_OBJ"));
		assertEquals("test T2 minR_OBJ2", new Integer(2), r.getObjInputMatrix().get("T2").get("minR_OBJ2"));
	}
	
	
	
	@AfterClass
	public static void afterTest() {

		// tempDir.delete();
		if (plot) {
			r.plot();
		}

	}

}
