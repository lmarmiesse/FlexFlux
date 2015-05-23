/*******************************************************************************
 * Copyright INRA
 * 
 *  Contact: ludovic.cottret@toulouse.inra.fr
 * 
 * 
 * This software is governed by the CeCILL license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 * 
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 *  In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 *  The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL license and that you accept its terms.
 ******************************************************************************/
/**
 * 8 mars 2013 
 */
package flexflux.unit_tests;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import flexflux.analyses.result.FBAResult;
import flexflux.general.Bind;
import flexflux.general.Constraint;
import flexflux.general.CplexBind;
import flexflux.general.DoubleResult;
import flexflux.general.GLPKBind;
import flexflux.interaction.And;
import flexflux.interaction.IfThenInteraction;
import flexflux.interaction.Interaction;
import flexflux.interaction.Or;
import flexflux.interaction.Unique;
import flexflux.operation.OperationLe;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import parsebionet.biodata.BioEntity;
import parsebionet.unit_tests.utils.TestUtils;

/**
 * @author lmarmiesse 8 mars 2013
 * 
 */
public class TestInteraction extends FFUnitTest{

	static BioEntity a;
	static BioEntity b;
	static BioEntity c;
	static BioEntity d;
	static BioEntity e;
	static BioEntity f;
	
	static String sbmlString = "";
	static String cond1String = "";
	static String int1String = "";
	static String cond2String = "";
	static String int2String = "";

	@BeforeClass
	public static void init() {
		
		
		File file;
		try {
			file = java.nio.file.Files.createTempFile("test", ".xml")
					.toFile();

			sbmlString = TestUtils.copyProjectResource(
					"flexflux/unit_tests/data/testInteraction/test.xml", file);

			file = java.nio.file.Files.createTempFile("cond1", ".txt")
					.toFile();

			cond1String = TestUtils.copyProjectResource(
					"flexflux/unit_tests/data/testInteraction/condElseTest.txt", file);
			
			file = java.nio.file.Files.createTempFile("int1", ".txt")
					.toFile();

			int1String = TestUtils.copyProjectResource(
					"flexflux/unit_tests/data/testInteraction/intElseTest.sbml", file);
			
			file = java.nio.file.Files.createTempFile("cond2", ".txt")
					.toFile();

			cond2String = TestUtils.copyProjectResource(
					"flexflux/unit_tests/data/testInteraction/condExtMetab.txt", file);
			
			
			file = java.nio.file.Files.createTempFile("int2", ".txt")
					.toFile();

			int2String = TestUtils.copyProjectResource(
					"flexflux/unit_tests/data/testInteraction/intExtMetab.sbml", file);

		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		
		
		
		
		
		a = new BioEntity("a");
		b = new BioEntity("b");
		c = new BioEntity("c");
		d = new BioEntity("d");
		e = new BioEntity("e");
		f = new BioEntity("f");
	}

	@Test
	public void test() {

		And rel1 = new And();
		Or rel2 = new Or();
		Unique rel3 = new Unique(c);

		rel2.addRelation(new Unique(b));
		rel2.addRelation(rel3);

		rel1.addRelation(new Unique(a));
		rel1.addRelation(rel2);

		assertTrue(rel1.toString().equals(
				"(a >= 0.0 AND (b >= 0.0 OR c >= 0.0))"));

		Unique intUnique = new Unique(f, new OperationLe(), 5.0);

		Interaction i1 = new IfThenInteraction(intUnique, rel1);
		System.err.println(i1);

		assertTrue(i1
				.toString()
				.equals("IF : (a >= 0.0 AND (b >= 0.0 OR c >= 0.0)) THEN : f <= 5.0 Begins after 0.0h, lasts 0.0h."));

		Bind bind = null;

		String solver = "GLPK";
		if (System.getProperties().containsKey("solver")) {
			solver = System.getProperty("solver");
		}

		try {
			if (solver.equals("CPLEX")) {
				bind = new CplexBind();
			} else {
				bind = new GLPKBind();
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			fail("Solver error");
		}

		fbaTest(bind);

		bind = null;


		try {
			if (solver.equals("CPLEX")) {
				bind = new CplexBind();
			} else {
				bind = new GLPKBind();
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			fail("Solver error");
		}

		extMetabTest(bind);

	}

	private void fbaTest(Bind bind) {

		bind.loadSbmlNetwork(sbmlString, false);

		bind.loadConstraintsFile(cond1String);
		bind.loadRegulationFile(int1String);

		bind.prepareSolver();

		// double res = bind.FBA(new ArrayList<Constraint>(), true,
		// true).result;

		// bind.end();

		FBAResult result = new FBAResult(bind);

		DoubleResult objValue = bind.FBA(new ArrayList<Constraint>(), true,
				true);

		if (objValue.flag != 0) {

			System.err.println(objValue.result);

			System.err.println("Unfeasible");
			result.setObjValue(Double.NaN);

		} else {

			result.setObjValue(objValue.result);

		}
		result.formatResult();

//		result.plot();

		Assert.assertTrue(result.getObjValue() == 9.0);
		
//		System.out.println(bind.getSolvedValue(bind.getInteractionNetwork()
//				.getEntity("c")));
		
		Assert.assertTrue(bind.getSolvedValue(bind.getInteractionNetwork()
				.getEntity("c")) > 1.6);
		Assert.assertTrue(bind.getSolvedValue(bind.getInteractionNetwork()
				.getEntity("c")) < 1.7);

	}

	// / TEST THAT A EXTERNAL METAB AT 0 MAKES R_EX = 0

	private void extMetabTest(Bind bind) {

		bind.loadSbmlNetwork(sbmlString, false);
		bind.loadConstraintsFile(cond2String);

		bind.loadRegulationFile(int2String);

		bind.prepareSolver();

		FBAResult result = new FBAResult(bind);

		DoubleResult objValue = bind.FBA(new ArrayList<Constraint>(), true,
				true);

		if (objValue.flag != 0) {

			System.err.println(objValue.result);

			System.err.println("Unfeasible");
			result.setObjValue(Double.NaN);

		} else {

			result.setObjValue(objValue.result);

		}
		result.formatResult();

//		result.plot();

		System.out.println(result.getObjValue());

		Assert.assertTrue(result.getObjValue() == 6.0);

	}

}
