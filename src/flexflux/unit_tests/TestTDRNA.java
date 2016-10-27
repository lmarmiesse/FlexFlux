package flexflux.unit_tests;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import flexflux.analyses.TDRNAAnalysis;
import flexflux.analyses.result.TDRNAAnalysisResult;
import flexflux.input.SBMLQualReader;
import flexflux.interaction.InteractionNetwork;
import flexflux.interaction.RelationFactory;
import parsebionet.biodata.BioEntity;
import parsebionet.unit_tests.utils.TestUtils;

public class TestTDRNA extends FFUnitTest {

	@Test
	public void test() {

		String sbmlQualFile = "";
		File file;
		try {
			file = java.nio.file.Files.createTempFile("regFile", ".xml").toFile();

			sbmlQualFile = TestUtils.copyProjectResource("flexflux/unit_tests/data/TDRNA/qual.sbml", file);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		InteractionNetwork intNet = SBMLQualReader.loadSbmlQual(sbmlQualFile, new InteractionNetwork(),
				new RelationFactory());

		TDRNAAnalysis tdrna = new TDRNAAnalysis(intNet, 70, 0.1);
		
		TDRNAAnalysisResult res = tdrna.runAnalysis();
		
		
		Assert.assertTrue(res.getStatesList().get(5).get(intNet.getEntity("c"))==1);
		Assert.assertTrue(res.getStatesList().get(15).get(intNet.getEntity("c"))==0);
		Assert.assertTrue(res.getStatesList().get(38).get(intNet.getEntity("c"))==1);
		Assert.assertTrue(res.getStatesList().get(60).get(intNet.getEntity("c"))==0);

	}

}
