package flexflux.unit_tests;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import parsebionet.unit_tests.TestUtils;
import flexflux.analyses.era.ERAAnalysis;
import flexflux.analyses.era.InputRandomParameters;
import flexflux.analyses.result.ERAResult;
import flexflux.applications.FlexfluxERA;
import flexflux.general.Bind;
import flexflux.general.CplexBind;
import flexflux.general.GLPKBind;
import flexflux.general.Vars;

public class TestEra extends FFUnitTest{

	private static boolean verbose = true;
	private static boolean plot = true;

	private static String tmpPath = "/tmp/testEra";

	private static ERAResult r;

	// The temporary folder will be removed at the end of the test
	private static File tempDir;

	static FlexfluxERA f;

	@BeforeClass
	public static void init() throws IOException {

		int nbSim = 100;
		
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

		f = new FlexfluxERA();

		f.sbmlFile = TestUtils.copyProjectResource(
				"flexflux/unit_tests/data/era/test.xml", java.nio.file.Files
						.createTempFile("test", ".xml").toFile());

		f.intFile = TestUtils.copyProjectResource(
				"flexflux/unit_tests/data/era/interactions.txt",
				java.nio.file.Files.createTempFile("test", ".txt").toFile());

		f.objectiveFile = TestUtils.copyProjectResource(
				"flexflux/unit_tests/data/era/objectives.txt",
				java.nio.file.Files.createTempFile("test", ".txt").toFile());

		f.constraintFile = TestUtils.copyProjectResource(
				"flexflux/unit_tests/data/era/constraints.txt",
				java.nio.file.Files.createTempFile("test", ".txt").toFile());

		f.inputRandomParameterFile = TestUtils.copyProjectResource(
				"flexflux/unit_tests/data/era/inputs.txt", java.nio.file.Files
						.createTempFile("test", ".txt").toFile());

		f.solver = solver;

		f.nbSim = nbSim;
		f.meanGaussian = 10.0;
		f.stdGaussian = 100.0;
		f.minInputs = 5;
		f.maxInputs = 20;

		HashMap<String, String> objectives = f
				.loadObjectiveFile(f.objectiveFile);

		if (objectives == null) {
			System.err.println("Error in reading the objective file "
					+ f.objectiveFile);
			System.exit(0);
		}

		ArrayList<InputRandomParameters> inputRandomParameterList = f
				.loadInputRandomParameterFile(f.inputRandomParameterFile);
		if (inputRandomParameterList == null) {
			System.err.println("Error in reading the input file "
					+ f.inputRandomParameterFile);
			System.exit(0);
		}

		Bind bind = null;

		try {
			if (f.solver.equals("CPLEX")) {
				bind = new CplexBind();
			} else if (f.solver.equals("GLPK")) {
				Vars.maxThread = 1;
				bind = new GLPKBind();
			} else {
				fail("Unknown solver name");
			}
		} catch (UnsatisfiedLinkError e) {
			fail("Error, the solver "
					+ f.solver
					+ " cannot be found. Check your solver installation and the configuration file, or choose a different solver (-sol).");
		} catch (NoClassDefFoundError e) {
			fail("Error, the solver "
					+ f.solver
					+ " cannot be found. There seems to be a problem with the .jar file of "
					+ f.solver + ".");
		}

		bind.loadSbmlNetwork(f.sbmlFile, f.extended);

		bind.setLoadObjective(false);

		if (f.constraintFile != "") {
			bind.loadConditionsFile(f.constraintFile);
		}

		bind.loadInteractionsFile(f.intFile);

		bind.prepareSolver();

		ERAAnalysis a = new ERAAnalysis(bind, f.nbSim, objectives,
				inputRandomParameterList, f.meanGaussian, f.stdGaussian,
				f.minInputs, f.maxInputs);

		r = a.runAnalysis();

		tempDir = new File(tmpPath);
		if (!tempDir.exists()) {
			tempDir.mkdir();
		}

		r.inchlibPath = inchlibPath;

		r.writeToFile(tmpPath);

	}

	@Test
	public void testDistribution() {
		ArrayList<Integer> nbActivatedInputs = r.getNumberOfActivatedInputs();
		
		int min = Collections.min(nbActivatedInputs);
		int max = Collections.max(nbActivatedInputs);
		
		if(min < f.minInputs) {
			fail("Error in the minimum number of activated inputs");
		}
		
		if(max > f.maxInputs) {
			fail("Error in the minimum number of activated inputs");
		}
		
	}
	
	
	@Test
	/**
	 * Test if the input RSp0837 that has a weight of 1000 is always present
	 */
	public void testWeight() {
		if(r.getInputOccurences().get("RSp0837")==0) {
			fail("Strange : the input RSp0837 is not activated in any simulation while its weight is 1000 !");
		}
	}
	

	@AfterClass
	public static void afterTest() {

		// tempDir.delete();
		if (plot) {
			r.plot();
		}

	}

}
