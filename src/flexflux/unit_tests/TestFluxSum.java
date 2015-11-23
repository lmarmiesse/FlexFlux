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

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import parsebionet.biodata.BioEntity;
import parsebionet.biodata.BioNetwork;
import parsebionet.biodata.BioPhysicalEntity;
import parsebionet.io.Sbml2Bionetwork;
import parsebionet.unit_tests.utils.TestUtils;
import flexflux.analyses.Analysis;
import flexflux.analyses.FBAAnalysis;
import flexflux.analyses.result.FBAResult;
import flexflux.general.Bind;
import flexflux.general.Constraint;
import flexflux.general.CplexBind;
import flexflux.general.GLPKBind;
import flexflux.general.Vars;
import flexflux.interaction.And;
import flexflux.interaction.Interaction;
import flexflux.interaction.InteractionNetwork;
import flexflux.interaction.Relation;
import flexflux.interaction.Unique;

/**
 * @author lmarmiesse 8 mars 2013
 * 
 */
public class TestFluxSum extends FFUnitTest {

	static Bind bind = null;

	@BeforeClass
	public static void init() throws IOException {

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

		String metabNetworkFile = TestUtils.copyProjectResource("flexflux/unit_tests/data/fluxsum/toyModel.xml",
				java.nio.file.Files.createTempFile("test", "sbml").toFile());

		String constraintsFile = TestUtils.copyProjectResource("flexflux/unit_tests/data/fluxsum/constraints.txt",
				java.nio.file.Files.createTempFile("test", "tab").toFile());

		Vars.maxThread = 1;

		bind.loadSbmlNetwork(metabNetworkFile, false);

		bind.loadConstraintsFile(constraintsFile);

		bind.prepareSolver();

	}

	@Test
	public void test() {

		Analysis analysis = new FBAAnalysis(bind);
		FBAResult result = (FBAResult) analysis.runAnalysis();

		double calc = result.getObjValue();

		double sum = Math.abs(bind.getLastSolve().get("R1")) + Math.abs(bind.getLastSolve().get("R2"))
				+ Math.abs(bind.getLastSolve().get("R3")) + Math.abs(bind.getLastSolve().get("R4"))
				+ Math.abs(bind.getLastSolve().get("R5")) + Math.abs(bind.getLastSolve().get("R6"))
				+ Math.abs(bind.getLastSolve().get("R7")) + Math.abs(bind.getLastSolve().get("R8"))
				+ Math.abs(bind.getLastSolve().get("R9")) + Math.abs(bind.getLastSolve().get("R10"))
				+ Math.abs(bind.getLastSolve().get("R_EX_A")) + Math.abs(bind.getLastSolve().get("R_EX_B"))
				+ Math.abs(bind.getLastSolve().get("R_EX_F")) + Math.abs(bind.getLastSolve().get("R_EX_G"));

		Assert.assertEquals(sum,calc ,0);
	}

}
