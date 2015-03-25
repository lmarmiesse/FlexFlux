package flexflux.unit_tests;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import parsebionet.biodata.BioEntity;
import parsebionet.unit_tests.utils.TestUtils;
import flexflux.analyses.RSAAnalysis;
import flexflux.analyses.result.RSAAnalysisResult;
import flexflux.general.Constraint;
import flexflux.input.ConstraintsFileReader;
import flexflux.input.SBMLQualReader;
import flexflux.interaction.InteractionNetwork;
import flexflux.interaction.RelationFactory;

public class TestRSA extends FFUnitTest {

	@Test
	public void test() {

		String sbmlQualFile = "";
		String consFile = "";

		File file;
		try {
			file = java.nio.file.Files.createTempFile("regFile", ".xml")
					.toFile();

			sbmlQualFile = TestUtils.copyProjectResource(
					"flexflux/unit_tests/data/rsa/lacOperon.sbml", file);

			file = java.nio.file.Files.createTempFile("constraints", ".txt")
					.toFile();

			consFile = TestUtils.copyProjectResource(
					"flexflux/unit_tests/data/rsa/ConstraintsRSA.txt", file);

		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		InteractionNetwork intNet = SBMLQualReader.loadSbmlQual(sbmlQualFile,
				new InteractionNetwork(), new RelationFactory());

		Map<BioEntity, Constraint> simpleConstraints = new HashMap<BioEntity, Constraint>();

		ConstraintsFileReader r = new ConstraintsFileReader(consFile, intNet);
		r.readConstraintsFile();

		simpleConstraints = r.simpleConstraints;

		RSAAnalysis rsa = new RSAAnalysis(intNet, simpleConstraints);
		
		RSAAnalysisResult res = rsa.runAnalysis();
		
		Assert.assertTrue(res.getSteadyStateConstraints().size()==15);
		
		for (Constraint c : res.getSteadyStateConstraints()){
			
			if(((BioEntity) c.getEntities().keySet().toArray()[0]).getId().equals("M_lcts_b")){
				Assert.assertTrue(c.getUb()==5.8);
				Assert.assertTrue(c.getLb()==5.8);
			}
			if(((BioEntity) c.getEntities().keySet().toArray()[0]).getId().equals("allolactose")){
				Assert.assertTrue(c.getUb()==1);
				Assert.assertTrue(c.getLb()==1);
			}
			if(((BioEntity) c.getEntities().keySet().toArray()[0]).getId().equals("betaGal")){
				Assert.assertTrue(c.getUb()==1);
				Assert.assertTrue(c.getLb()==1);
			}
		}
		
		Assert.assertTrue(res.getStatesList().size()==6);
		
		
//		res.plot();
//		
//		while (true){
//			
//		}
	}
}
