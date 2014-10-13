package flexflux.unit_tests;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import parsebionet.unit_tests.TestUtils;
import flexflux.analyses.PFBAAnalysis;
import flexflux.analyses.result.PFBAResult;
import flexflux.general.Bind;
import flexflux.general.CplexBind;
import flexflux.general.GLPKBind;

public class TestPFBA extends FFUnitTest{

	private static Bind b=null;
	
	private static File tempSbmlFile = null;
	private static File tempConstraintFile = null;
	private static File tempInteractionFile = null;
	
	static PFBAResult res = null;
	
	@BeforeClass
	public static void init() throws ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		
		String solver = "GLPK";
		if (System.getProperties().containsKey("solver")) {
			solver = System.getProperty("solver");
		}
		
		if(solver.equals("CPLEX"))
		{
			b = new CplexBind();
		}
		else {
			b = new GLPKBind();
		}
		
		java.nio.file.Path tmpSbml = null;
		java.nio.file.Path tmpConstraint = null;
		java.nio.file.Path tmpInteraction = null;
		
		try {
			tmpSbml = java.nio.file.Files.createTempFile("test", ".xml");
			tmpConstraint = java.nio.file.Files.createTempFile("test", ".tab");
			tmpInteraction = java.nio.file.Files.createTempFile("test", ".tab");
		}
		 catch (IOException e1) {
				// TODO Auto-generated catch block
				Assert.fail("Creation of the temporary files");
				e1.printStackTrace();
			}

			tempSbmlFile = tmpSbml.toFile();
			tempConstraintFile = tmpConstraint.toFile();
			tempInteractionFile = tmpInteraction.toFile();
			
			String sbmlFile=null;
			String constraintFile=null;
			String interactionFile = null;
			
			try {
				sbmlFile = TestUtils
						.copyProjectResource(
								"flexflux/unit_tests/data/pfba/test.xml",
								tempSbmlFile);
				constraintFile = TestUtils
						.copyProjectResource(
								"flexflux/unit_tests/data/pfba/constraints.txt",
								tempConstraintFile);
				interactionFile = TestUtils
						.copyProjectResource(
								"flexflux/unit_tests/data/pfba/interactions.sbml",
								tempInteractionFile);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Assert.fail("problem while copying the input files");
			}
		
			
			b.loadSbmlNetwork(sbmlFile, false);
			b.loadConditionsFile(constraintFile);
			b.loadInteractionsFile(interactionFile);
			
			b.prepareSolver();
			
			PFBAAnalysis a = new PFBAAnalysis(b, true);

			res = a.runAnalysis();
			
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
	
//	@Test
	public void testZeroFluxReactions() {
		
		Set<String> test = res.zeroFluxReactions.keySet();
		
		Set<String> ref = new HashSet<String>();
		
		ref.add("R2");
		ref.add("R_I_EX");
		
		Assert.assertEquals(ref, test);
		
	}
	
	@Test
	public void testMleReactions() {
		
		Set<String> test = res.mleReactions.keySet();
		
		Set<String> ref = new HashSet<String>();
		
		// R1 uses H 
//		ref.add("R1");
//		ref.add("R_B_EX");
//		ref.add("R_C_EX");
		
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
		
//		ref.add("R3");
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
	
	
		
}
