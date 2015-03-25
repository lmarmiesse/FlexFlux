package flexflux.unit_tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

import junit.framework.Assert;
import junitx.framework.FileAssert;

import org.junit.Test;

import parsebionet.unit_tests.utils.TestUtils;
import flexflux.condition.Condition;
import flexflux.condition.ListOfConditions;
import flexflux.general.ConstraintType;
import flexflux.general.SimplifiedConstraint;

public class TestListOfConditions {

	@Test
	public void testLoadConditionFile() {

		String fileRef;

		try {
			fileRef = TestUtils
					.copyProjectResource(
							"flexflux/unit_tests/data/listOfConditions/conditions.tab",
							java.nio.file.Files.createTempFile("test", ".txt")
									.toFile());

			ListOfConditions conditions = new ListOfConditions();
			conditions.loadConditionFile(fileRef, ConstraintType.DOUBLE);

			for (Condition condition : conditions) {

				System.err.println(condition.code);
				for (SimplifiedConstraint c : condition.constraints.values()) {
					System.err.println(c);
				}

			}
			assertEquals("Bad number of conditions", 3, conditions.size());

			ArrayList<String> entitiesRef = new ArrayList<String>();

			entitiesRef.add("b");
			entitiesRef.add("b2");
			entitiesRef.add("b3");

			assertEquals("Bad entities", conditions.entities, entitiesRef);

			Condition c = new Condition("1", "1");
			c.addConstraint("b", 10.0, ConstraintType.DOUBLE);
			c.addConstraint("b2", 10.0, ConstraintType.DOUBLE);
			c.addConstraint("b3", 0.0, ConstraintType.DOUBLE);

			assertTrue("contains c", conditions.contains(c));

			Condition c2 = new Condition("2", "2");
			c2.addConstraint("b", 10.0, ConstraintType.DOUBLE);
			c2.addConstraint("b2", 10.0, ConstraintType.DOUBLE);
			c2.addConstraint("b3", 10.0, ConstraintType.DOUBLE);

			assertTrue("contains c2", conditions.contains(c2));

			Condition c3 = new  Condition("3", "3");
			c3.addConstraint("b", 10.0, ConstraintType.DOUBLE);
			c3.addConstraint("b2", 10.0, ConstraintType.DOUBLE);
			assertTrue("contains c3", conditions.contains(c3));
			
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail("Impossible to get the reference file");
		}

	}

	@Test
	public void testAdd() {

		ListOfConditions conditions = new ListOfConditions();

		Condition c = new Condition("1", "1");
		c.addConstraint("b", 10.0, ConstraintType.BINARY);
		c.addConstraint("b2", 10.0, ConstraintType.BINARY);

		conditions.add(c);

		Assert.assertEquals("test adds 1", 1, conditions.size());

		ArrayList<String> refEntities = new ArrayList<String>();
		refEntities.add("b");
		refEntities.add("b2");

		Assert.assertEquals("check entities", new HashSet<String>(refEntities),
				new HashSet<String>(conditions.entities));

		Condition c2 = new Condition("2", "2");
		c2.addConstraint("b", 10.0, ConstraintType.BINARY);
		c2.addConstraint("b2", 10.0, ConstraintType.BINARY);

		conditions.add(c2);

		Assert.assertEquals("test size 2", 2, conditions.size());

		Assert.assertEquals("check non redundant entities",
				new HashSet<String>(refEntities), new HashSet<String>(
						conditions.entities));

		c2.addConstraint("b3", 10.0, ConstraintType.BINARY);

		conditions.add(c2);

		refEntities.add("b3");

		Assert.assertEquals("check entities 2",
				new HashSet<String>(refEntities), new HashSet<String>(
						conditions.entities));

		Assert.assertEquals("test size 3", 3, conditions.size());

	}

	@Test
	public void testSize() {
		ListOfConditions conditions = new ListOfConditions();

		Condition c = new Condition("1", "1");
		c.addConstraint("b", 10.0, ConstraintType.BINARY);
		c.addConstraint("b2", 10.0, ConstraintType.BINARY);

		conditions.add(c);

		Assert.assertEquals("test size 1", 1, conditions.size());

		Condition c2 = new Condition("2", "2");
		c2.addConstraint("b", 10.0, ConstraintType.BINARY);
		c2.addConstraint("b2", 10.0, ConstraintType.BINARY);
		c2.addConstraint("b3", 10.0, ConstraintType.BINARY);

		conditions.add(c2);

		Assert.assertEquals("test size 2", 2, conditions.size());

	}

	@Test
	public void testContainsCondition() {

		ListOfConditions conditions = new ListOfConditions();

		Condition c = new Condition("1", "1");
		c.addConstraint("b", 10.0, ConstraintType.BINARY);
		c.addConstraint("b2", 10.0, ConstraintType.BINARY);

		assertFalse("test empty conditions not contain condition",
				conditions.contains(c));

		conditions.add(c);

		assertTrue("test contains condition", conditions.contains(c));

		Condition c2 = new Condition("2", "2");
		c2.addConstraint("b", 10.0, ConstraintType.BINARY);
		c2.addConstraint("b2", 10.0, ConstraintType.BINARY);

		assertTrue("test contains similar condition", conditions.contains(c2));

		c2.addConstraint("b3", 10.0, ConstraintType.BINARY);

		assertFalse("test does not contains different condition",
				conditions.contains(c2));

		conditions.add(c2);

	}

	@Test
	public void testWrite() {

		String fileRef = "";

		try {
			fileRef = TestUtils
					.copyProjectResource(
							"flexflux/unit_tests/data/listOfConditions/conditions.tab",
							java.nio.file.Files.createTempFile("test", ".txt")
									.toFile());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail("Impossible to get the reference file");
		}

		String dir = "/tmp/testListOfConditions";

		try {
			dir = File.createTempFile("temp-file", "tmp").getParent()
					+ "/testListOfConditions";
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}

		File DIR = new File(dir);
		if (!DIR.exists()) {
			try {
				DIR.mkdir();
			} catch (SecurityException se) {
				fail("Impossible to create the directory " + dir);
			}
		}

		String fileTest = dir + "/conditions.tab";

		ListOfConditions conditions = new ListOfConditions();

		Condition c = new Condition("1", "1");
		c.addConstraint("b", 10.0, ConstraintType.BINARY);
		c.addConstraint("b2", 10.0, ConstraintType.BINARY);
		c.addConstraint("b3", 0.0, ConstraintType.BINARY);

		conditions.add(c);

		Condition c2 = new Condition("2", "2");
		c2.addConstraint("b", 10.0, ConstraintType.BINARY);
		c2.addConstraint("b2", 10.0, ConstraintType.BINARY);
		c2.addConstraint("b3", 10.0, ConstraintType.BINARY);

		conditions.add(c2);
		
		Condition c3 = new  Condition("3", "3");
		c3.addConstraint("b", 10.0, ConstraintType.DOUBLE);
		c3.addConstraint("b2", 10.0, ConstraintType.DOUBLE);
		
		conditions.add(c3);
		

		conditions.writeConditionFile(fileTest);

		FileAssert.assertEquals("", new File(fileRef), new File(fileTest));

	}

}
