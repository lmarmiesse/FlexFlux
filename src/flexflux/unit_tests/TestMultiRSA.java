package flexflux.unit_tests;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;

import flexflux.applications.FlexfluxMultiRSA;
import flexflux.condition.Condition;
import flexflux.condition.ListOfConditions;
import flexflux.general.ConstraintType;
import parsebionet.unit_tests.utils.TestUtils;

public class TestMultiRSA {

	public static String outFile = "";

	@BeforeClass
	public static void init() throws IOException {

		outFile = System.getProperty("java.io.tmpdir") + "/testMultiRSA.tab";

		String interactionNetworkFile = TestUtils.copyProjectResource(
				"flexflux/unit_tests/data/rsa/interactions.sbml",
				java.nio.file.Files.createTempFile("test", "sbml").toFile());

		String conditionFile = TestUtils.copyProjectResource(
				"flexflux/unit_tests/data/rsa/conditions.tab",
				java.nio.file.Files.createTempFile("test", "tab").toFile());

		FlexfluxMultiRSA f = new FlexfluxMultiRSA();

		f.nThreads = 4;
		f.conditionFile = conditionFile;
		f.regFile = interactionNetworkFile;
		f.outName = outFile;

		Boolean flag = f.run();

		if (!flag) {
			fail();
		}

	}

	@Test
	public void test() {

		ListOfConditions conditions = new ListOfConditions();

		conditions.loadConditionFile(outFile, ConstraintType.DOUBLE, false);

		Condition c1 = conditions.get("C1");

		if (c1 == null) {
			fail("no c1 condition in the conditions");
		}

		Condition c1Test = new Condition("C1", "C1");
		c1Test.addConstraint("G1", 0.5, ConstraintType.DOUBLE);
		c1Test.addConstraint("G2", 1.0, ConstraintType.DOUBLE);
		c1Test.addConstraint("G3", 1.0, ConstraintType.DOUBLE);
		c1Test.addConstraint("T1", 0.5, ConstraintType.DOUBLE);
		c1Test.addConstraint("T2", 1.0, ConstraintType.DOUBLE);

		assertTrue("good c1", c1Test.equals(c1));

//		Condition c2 = conditions.get("C2");
//
//		if (c2 == null) {
//			fail("no c2 condition in the conditions");
//		}
//
//		Condition c2Test = new Condition("C2", "C2");
//		c2Test.addConstraint("G1", 0.5, ConstraintType.DOUBLE);
//		c2Test.addConstraint("G2", 0.0, ConstraintType.DOUBLE);
//		c2Test.addConstraint("G3", 0.0, ConstraintType.DOUBLE);
//		c2Test.addConstraint("T1", 0.5, ConstraintType.DOUBLE);
//		c2Test.addConstraint("T2", 0.0, ConstraintType.DOUBLE);
//
//		assertTrue("good c2", c2Test.equals(c2));
//
//		Condition c3 = conditions.get("C3");
//
//		if (c3 == null) {
//			fail("no c3 condition in the conditions");
//		}
//
//		Condition c3Test = new Condition("c3", "c3");
//		c3Test.addConstraint("G1", 0.5, ConstraintType.DOUBLE);
//		c3Test.addConstraint("G2", 1.0, ConstraintType.DOUBLE);
//		c3Test.addConstraint("G3", 1.0, ConstraintType.DOUBLE);
//		c3Test.addConstraint("T1", 0.5, ConstraintType.DOUBLE);
//		c3Test.addConstraint("T2", 1.0, ConstraintType.DOUBLE);
//
//		assertTrue("good c3", c3Test.equals(c3));
//
//		Condition c4 = conditions.get("C4");
//
//		if (c4 == null) {
//			fail("no c4 condition in the conditions");
//		}
//
//		/**
//		 * c4 == c2
//		 */
//		assertTrue("good c4", c2Test.equals(c4));
//
//		Condition c5 = conditions.get("C5");
//
//		if (c5 == null) {
//			fail("no c5 condition in the conditions");
//		}
//		
//		/**
//		 * c5 == c3
//		 */
//		assertTrue("good c5", c3Test.equals(c5));
		

	}

}
