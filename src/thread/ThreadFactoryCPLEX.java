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
package thread;

import general.Bind;
import general.Constraint;
import general.CplexBind;
import general.Objective;
import interaction.InteractionNetwork;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import parsebionet.biodata.BioEntity;
import analyses.result.FVAResult;
import analyses.result.KOResult;
import analyses.result.ReacAnalysisResult;
import analyses.result.TwoReacsAnalysisResult;

/**
 * 
 * CPLEX version of the Thread factory.
 * 
 * @author lmarmiesse 8 mars 2013
 * 
 */
public class ThreadFactoryCPLEX extends ThreadFactory {

	public ThreadFactoryCPLEX(List<Constraint> constraints,
			Map<BioEntity, Constraint> simpleConstraints,
			InteractionNetwork intNet) {
		super(constraints, simpleConstraints, intNet);
	}

	public ThreadFVA makeFVAThread(boolean interactionInSolver,
			Queue<BioEntity> ents, Queue<BioEntity> entsCopy, FVAResult result) {

		Bind bind = new CplexBind(constraints, simpleConstraints, intNet,
				bioNet, interactionInSolver);

		return new ThreadFVA(bind, ents, entsCopy, result);
	}

	public ThreadKO makeKOThread(boolean interactionInSolver,
			Queue<BioEntity> entities, KOResult result, Objective obj) {

		Bind bind = new CplexBind(constraints, simpleConstraints, intNet,
				bioNet, interactionInSolver);

		return new ThreadKO(bind, entities, result, obj);
	}

	public ThreadReac makeReacThread(boolean interactionInSolver,
			Queue<Double> fluxesQueue, Map<BioEntity, Double> entities,
			ReacAnalysisResult result, Objective obj) {
		Bind bind = new CplexBind(constraints, simpleConstraints, intNet,
				bioNet, interactionInSolver);

		return new ThreadReac(bind, fluxesQueue, entities, result, obj);
	}

	public ResolveThread makeTwoReacsThread(boolean interactionInSolver,
			Queue<double[]> fluxesQueue, TwoReacsAnalysisResult result,
			Map<BioEntity, Double> entities1, Map<BioEntity, Double> entities2,
			Objective obj) {
		Bind bind = new CplexBind(constraints, simpleConstraints, intNet,
				bioNet, interactionInSolver);

		return new ThreadTwoReacs(bind, fluxesQueue, result, entities1,
				entities2, obj);
	}

}
