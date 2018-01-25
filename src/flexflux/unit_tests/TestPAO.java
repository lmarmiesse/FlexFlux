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
 * 
 */
package flexflux.unit_tests;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.fail;
import org.junit.BeforeClass;
import org.junit.Test;

import parsebionet.unit_tests.utils.TestUtils;
import flexflux.analyses.KOAnalysis;
import flexflux.analyses.result.KOResult;
import flexflux.general.Bind;
import flexflux.general.CplexBind;
import flexflux.general.GLPKBind;
import flexflux.general.Vars;
import junit.framework.Assert;
import parsebionet.biodata.BioNetwork;

public class TestPAO extends FFUnitTest {

	static Bind bind;

	static BioNetwork n;

	static String sbmlFile = "";
	static String conditionFile = "";

	@BeforeClass
	public static void init() {

		File file;

		try {
			
			file = java.nio.file.Files.createTempFile("sbmlFile", ".txt").toFile();


			sbmlFile = TestUtils.copyProjectResource("flexflux/unit_tests/data/ko/pao/paonew100629_corrected_Units.xml",
					file);

			file = java.nio.file.Files.createTempFile("conditionFile", ".txt").toFile();


			conditionFile = TestUtils
					.copyProjectResource("flexflux/unit_tests/data/ko/pao/Constraints_MMGLUPAME_BIOMASS_PA.tab", file);


		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		String solver = "GLPK";
		if (System.getProperties().containsKey("solver")) {
			solver = System.getProperty("solver");
		}
		
		Vars.maxThread = 1;
		
		try {
			if (solver.equals("CPLEX")) {
				bind = new CplexBind();
				Vars.maxThread = 2;
			} else {
				bind = new GLPKBind();
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			fail("Solver error");
		}
		
		bind.loadSbmlNetwork(sbmlFile, false);
		n = bind.getBioNetwork();
		
		bind.loadConstraintsFile(conditionFile);

		bind.prepareSolver();
		
	}
	
	
//	@Test
	public void test() {
		
		KOAnalysis koGenes = new KOAnalysis(bind, 1, null);
		KOResult resultKoGenes = koGenes.runAnalysis();
		
		Double val = resultKoGenes.getValueForEntity(n.getGeneList().get("PA0018"));
		
		
		for(Double value : resultKoGenes.getMap().values()) {
			if(value > 0.3)
			{
				Assert.fail(value+" : Value greater than the optimal !");
			}
		}
		
		Assert.assertEquals(Double.NaN, Vars.round(val));
		
	}
	
	
	
	
	

}
