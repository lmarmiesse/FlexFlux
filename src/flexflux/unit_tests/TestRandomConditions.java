package flexflux.unit_tests;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import parsebionet.unit_tests.TestUtils;
import flexflux.analyses.randomConditions.ListOfInputRandomParameters;
import flexflux.analyses.randomConditions.RandomConditions;
import flexflux.analyses.result.RandomConditionsResult;
import flexflux.general.ConstraintType;
import flexflux.general.Vars;

public class TestRandomConditions {

	private static boolean verbose = true;
	private static boolean plot = true;

	private static String tmpPath = "/tmp/testRandom";

	private static RandomConditionsResult r;

	// The temporary folder will be removed at the end of the test
	private static File tempDir;
	
	static int nbSim = 1000;
	static double meanGaussian = 10.0;
	static double stdGaussian = 100.0;
	static int minInputs = 5;
	static int maxInputs = 20;
	
	@BeforeClass
	public static void init() throws IOException {
		
		Vars.verbose = verbose;
		
		Vars.maxThread = 10;
		
		String inputRandomParameterFile = TestUtils.copyProjectResource(
				"flexflux/unit_tests/data/random/inputs.txt", java.nio.file.Files
				.createTempFile("test", ".txt").toFile());
		
		
		ListOfInputRandomParameters inputRandomParameterList = new ListOfInputRandomParameters();
		inputRandomParameterList.loadInputRandomParameterFile(inputRandomParameterFile);
		
		
		RandomConditions a = new RandomConditions(nbSim, inputRandomParameterList, meanGaussian, stdGaussian, minInputs, maxInputs, ConstraintType.DOUBLE);
		
		r = a.runAnalysis();
		
	}
	
	@Test
	public void testDistribution() {
		ArrayList<Integer> nbActivatedInputs = r.getNumberOfActivatedInputs();
		
		int min = Collections.min(nbActivatedInputs);
		int max = Collections.max(nbActivatedInputs);
		
		if(min < minInputs) {
			fail("Error in the minimum number of activated inputs");
		}
		
		if(max > maxInputs) {
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
	
	@Test
	public void testNumberOfDistinctConditions() {
			assertEquals("Bad number of distinct conditions", nbSim, r.getConditions().size());
	}
	
	@Test
	public void testWrite() {
		r.writeToFile(tmpPath);
	}
	
	
	@AfterClass
	public static void afterTest() {

		// tempDir.delete();
		if (plot) {
			r.plot();
		}

	}
	

}
