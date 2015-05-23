package flexflux.unit_tests;

import org.junit.Assert;
import org.junit.Test;

import flexflux.condition.Condition;
import flexflux.general.ConstraintType;

public class TestCondition {
	
	@Test
	public void testAddConstraint() {
		
		Condition c1 = new Condition("1", "1");
		c1.addConstraint("b1", 1,ConstraintType.INTEGER);
		
		Assert.assertEquals(1, c1.size());
		
		c1.addConstraint("b2", 0,ConstraintType.INTEGER);
		
		Assert.assertEquals(2, c1.size());
		
		c1.addConstraint("b2", 0,ConstraintType.INTEGER);
		
		Assert.assertEquals(2, c1.size());
		
		
	}

	@Test
	public void testEquals() {
		Condition c1 = new Condition("1", "1");
		Condition c2 = new Condition("2", "2");
		Condition c3 = new Condition("3", "3");
		
		c1.addConstraint("b1", 1,ConstraintType.INTEGER);
		c2.addConstraint("b1", 1,ConstraintType.INTEGER);
		Assert.assertTrue("Test equality between two conditions", c1.equals(c2));
		
		c3.addConstraint("b_1", 1,ConstraintType.INTEGER);
		Assert.assertFalse("Test equality between two conditions", c1.equals(c3));
		
		c2.addConstraint("b2", 1,ConstraintType.INTEGER);
		Assert.assertFalse("Test equality between two conditions", c1.equals(c2));
		
		
	}
	
}
