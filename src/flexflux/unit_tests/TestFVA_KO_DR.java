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
 * 13 mars 2013 
 */
package flexflux.unit_tests;

import static org.junit.Assert.fail;
import flexflux.analyses.DRAnalysis;
import flexflux.analyses.FVAAnalysis;
import flexflux.analyses.KOAnalysis;
import flexflux.analyses.result.DRResult;
import flexflux.analyses.result.FVAResult;
import flexflux.analyses.result.KOResult;
import flexflux.general.Bind;
import flexflux.general.CplexBind;
import flexflux.general.GLPKBind;
import flexflux.general.Vars;
import flexflux.interaction.InteractionNetwork;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import parsebionet.biodata.BioEntity;
import parsebionet.biodata.BioNetwork;
import parsebionet.unit_tests.utils.TestUtils;

/**
 * @author lmarmiesse 13 mars 2013
 * 
 */
public class TestFVA_KO_DR extends FFUnitTest {

	static Bind bind;

	static BioNetwork n;
	static InteractionNetwork i;

	static String coliFileString = "";
	static String condFileString = "";
	static String metFVAformatedFileString = "";
	static String metKOformatedFileString = "";
	static String KOgenesFileString = "";

	@BeforeClass
	public static void init() {

		File file;
		try {
			file = java.nio.file.Files.createTempFile("coli", ".xml").toFile();

			coliFileString = TestUtils.copyProjectResource(
					"flexflux/unit_tests/data/FVA_KO_DR/coli_core.xml", file);

			file = java.nio.file.Files.createTempFile("condFileString", ".txt")
					.toFile();

			condFileString = TestUtils.copyProjectResource(
					"flexflux/unit_tests/data/FVA_KO_DR/condColiTest", file);

			file = java.nio.file.Files.createTempFile(
					"metFVAformatedFileString", ".txt").toFile();

			metFVAformatedFileString = TestUtils.copyProjectResource(
					"flexflux/unit_tests/data/FVA_KO_DR/metFVAformated", file);

			file = java.nio.file.Files.createTempFile(
					"metKOformatedFileString", ".txt").toFile();

			metKOformatedFileString = TestUtils.copyProjectResource(
					"flexflux/unit_tests/data/FVA_KO_DR/metKOformated", file);

			file = java.nio.file.Files.createTempFile("KOgenesFileString",
					".txt").toFile();

			KOgenesFileString = TestUtils.copyProjectResource(
					"flexflux/unit_tests/data/FVA_KO_DR/KOgenes", file);

		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		String solver = "GLPK";
		if (System.getProperties().containsKey("solver")) {
			solver = System.getProperty("solver");
		}
		
		System.out.println(solver);

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

		Vars.maxThread = 8;

		bind.loadSbmlNetwork(coliFileString, false);
		n = bind.getBioNetwork();
		i = bind.getInteractionNetwork();

		bind.loadConstraintsFile(condFileString);

		bind.prepareSolver();

	}

	@Test
	public void test() {

		go();

	}

	public void go() {

		FVAAnalysis fva = new FVAAnalysis(bind, null, null);
		FVAResult result = fva.runAnalysis();
		try {
			BufferedReader in = new BufferedReader(new FileReader(
					metFVAformatedFileString));

			String line;
			while ((line = in.readLine()) != null) {

				String[] splittedLine = line.split("\t");

				String name = splittedLine[0].replaceAll("\\s+", "");

				double min = Double.parseDouble(splittedLine[1]);
				double max = Double.parseDouble(splittedLine[2]);

				Assert.assertTrue(Math.abs(result.getValuesForEntity(bind
						.getInteractionNetwork().getEntity(name))[0] - min) < 0.001);

				Assert.assertTrue(Math.abs(result.getValuesForEntity(bind
						.getInteractionNetwork().getEntity(name))[1] - max) < 0.001);

			}

			in.close();
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// KO reactions
		KOAnalysis ko = new KOAnalysis(bind, 0, null);
		KOResult resultKo = ko.runAnalysis();

		try {
			BufferedReader in = new BufferedReader(new FileReader(
					metKOformatedFileString));

			String line;
			while ((line = in.readLine()) != null) {

				String[] splittedLine = line.split("\t");

				String name = splittedLine[0].replaceAll("\\s", "");

				double value = Double.parseDouble(splittedLine[1]);
				
				Assert.assertTrue(Math.abs(resultKo.getValueForEntity(bind
						.getInteractionNetwork().getEntity(name)) - value) < 0.001);

			}
			
			in.close();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// KO genes
		KOAnalysis koGenes = new KOAnalysis(bind, 1, null);
		KOResult resultKoGenes = koGenes.runAnalysis();

//		resultKoGenes.plot();
//		
//		
//		int a= 1;
//		
//		while(a==1){
//			
//		}
//		
		try {
			BufferedReader in = new BufferedReader(new FileReader(
					KOgenesFileString));

			String line;
			while ((line = in.readLine()) != null) {

				String[] splittedLine = line.split("\t");

				String name = splittedLine[0].replaceAll("\\s", "");

				double value = Double.parseDouble(splittedLine[1]);
				
				
				double simuResult = Math.abs(resultKoGenes.getValueForEntity(bind
						.getInteractionNetwork().getEntity(name)));
				
				if (Double.isNaN(simuResult)){
					simuResult=0.0;
				}
				
				Assert.assertTrue((simuResult - value) < 0.001);

			}
			
			in.close();
			

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// DR
		DRAnalysis dr = new DRAnalysis(bind, 0.000001);
		DRResult resultDr = dr.runAnalysis();

		Set<String> dead = resultDr.getDeadReactions().keySet();
		
		Assert.assertTrue(dead.size() == 8);

		Set<String> testDead = new HashSet<String>();

		testDead.add("R_EX_mal_L_e");
		testDead.add("R_EX_fru_e");
		testDead.add("R_EX_fum_e");
		testDead.add("R_EX_gln_L_e");
		testDead.add("R_GLNabc");
		testDead.add("R_MALt2_2");
		testDead.add("R_FUMt2_2");
		testDead.add("R_FRUpts2");

		Assert.assertEquals(testDead, dead);

	}
}
