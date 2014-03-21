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
package unit_tests;

import static org.junit.Assert.assertTrue;
import general.Bind;
import general.CplexBind;
import general.GLPKBind;
import interaction.And;
import interaction.Interaction;
import interaction.Or;
import interaction.Unique;
import interaction.cplex.AndCPLEX;
import interaction.cplex.EqInteractionCplex;
import interaction.cplex.IfThenInteractionCplex;
import interaction.cplex.OrCPLEX;
import interaction.cplex.UniqueCPLEX;
import operation.OperationLeCPLEX;

import org.junit.BeforeClass;
import org.junit.Test;

import analyses.Analysis;
import analyses.FBAAnalysis;
import analyses.result.AnalysisResult;
import parsebionet.biodata.BioEntity;

/**
 * @author lmarmiesse 8 mars 2013
 * 
 */
public class TestInteraction {

	static BioEntity a;
	static BioEntity b;
	static BioEntity c;
	static BioEntity d;
	static BioEntity e;
	static BioEntity f;

	@BeforeClass
	public static void init() {
		a = new BioEntity("a");
		b = new BioEntity("b");
		c = new BioEntity("c");
		d = new BioEntity("d");
		e = new BioEntity("e");
		f = new BioEntity("f");
	}

	@Test
	public void test() {

		And rel1= new AndCPLEX();
		Or rel2 = new OrCPLEX();
		Unique rel3 = new UniqueCPLEX(c);
		
		
		rel2.addRelation(new UniqueCPLEX(b));
		rel2.addRelation(rel3);
		
		rel1.addRelation(new UniqueCPLEX(a));
		rel1.addRelation(rel2);
		
		assertTrue(rel1.toString().equals("(a >= 0.0 AND (b >= 0.0 OR c >= 0.0))"));
		
		Unique intUnique = new UniqueCPLEX(f,new OperationLeCPLEX(),5.0);
		
		Interaction i1 = new IfThenInteractionCplex(intUnique,rel1);
		System.out.println(i1);
		
		assertTrue(i1.toString().equals("IF : (a >= 0.0 AND (b >= 0.0 OR c >= 0.0)) THEN : f <= 5.0"));
		
		
		Unique u1 = new UniqueCPLEX(b);
		Unique u2 = new UniqueCPLEX(f);
		Interaction i2 = new EqInteractionCplex(u1,u2);
		
		System.out.println(i2);
		
		
		fbaTest(new GLPKBind(false));
		fbaTest(new CplexBind(false));
		
		
		
	}

	private void fbaTest(Bind bind) {

		bind.loadSbmlNetwork("Data/coli_core.xml", false);
		
		bind.loadConditionsFile("Data/condElseTest.txt");
		bind.loadInteractionsFile("Data/intElseTest.txt");
		
		
		for (Interaction inter : bind.getInteractionNetwork().getAddedInteractions()){
			System.out.println(inter);
		}
		
		
		bind.prepareSolver();
		
	
		
		
		Analysis analysis = new FBAAnalysis(bind);
		AnalysisResult result = analysis.runAnalysis();
		
//		result.plot();
		
		bind.end();
		
//		boolean a=false;
//		
//		while (!a){
//			
//		}
		
	}
}
