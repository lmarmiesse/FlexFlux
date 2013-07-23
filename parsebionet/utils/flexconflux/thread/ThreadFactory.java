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
package parsebionet.utils.flexconflux.thread;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import parsebionet.biodata.BioEntity;
import parsebionet.biodata.BioNetwork;
import parsebionet.utils.flexconflux.Constraint;
import parsebionet.utils.flexconflux.Objective;
import parsebionet.utils.flexconflux.analyses.result.FVAResult;
import parsebionet.utils.flexconflux.analyses.result.KOResult;
import parsebionet.utils.flexconflux.analyses.result.ReacAnalysisResult;
import parsebionet.utils.flexconflux.analyses.result.TwoReacsAnalysisResult;
import parsebionet.utils.flexconflux.interaction.InteractionNetwork;

/**
 * 
 * 
 * Class to create the right type of thread.
 * 
 * To create the right Bind for the solver, this class must be extended for each
 * solver.
 * 
 * @author lmarmiesse 8 mars 2013
 * 
 */
public abstract class ThreadFactory {

	/**
	 * Bind constraints to copy.
	 */
	protected List<Constraint> constraints;
	/**
	 * Bind simple constraints to copy.
	 */
	protected Map<BioEntity, Constraint> simpleConstraints = new HashMap<BioEntity, Constraint>();
	/**
	 * Bind interaction network to copy.
	 */
	protected InteractionNetwork intNet;
	/**
	 * Bind bioNetwork to copy.
	 */
	protected BioNetwork bioNet;

	public ThreadFactory(List<Constraint> constraints,
			Map<BioEntity, Constraint> simpleConstraints,
			InteractionNetwork intNet) {
		this.constraints = constraints;
		this.simpleConstraints = simpleConstraints;
		this.intNet = intNet;
	}

	/**
	 * 
	 * Makes a thread for an FVA analysis.
	 * 
	 * @param interactionInSolver
	 *            Whether or not interactions are in the solver.
	 * @param entQueue
	 *            The queue of entities to perform the FVA on for maximization.
	 * @param entQueueCopy
	 *            entQueue The queue of entities to perform the FVA on for
	 *            minimization.
	 * @param result
	 *            An empty FVA result.
	 * @return A FVA thread.
	 */
	public abstract ThreadFVA makeFVAThread(boolean interactionInSolver,
			Queue<BioEntity> entQueue, Queue<BioEntity> entQueueCopy,
			FVAResult result);

	/**
	 * 
	 * Makes a thread for a KO analysis.
	 * 
	 * @param interactionInSolver
	 *            Whether or not interactions are in the solver.
	 * @param reacsQueue
	 *            The queue of entities to perform the KO on.
	 * @param koResult
	 *            An empty KO result.
	 * @param obj
	 *            The objective function.
	 * @return A KO thread
	 */
	public abstract ThreadKO makeKOThread(boolean interactionInSolver,
			Queue<BioEntity> reacsQueue, KOResult koResult, Objective obj);

	public void setBioNet(BioNetwork bioNet) {
		this.bioNet = bioNet;
	}

	/**
	 * 
	 * Makes a thread for an analysis with a varying variable.
	 * 
	 * @param interactionInSolver
	 *            Whether or not interactions are in the solver.
	 * @param fluxesQueue
	 *            The queue of fluxes.
	 * @param entities
	 *            Map containing the entity to make vary.
	 * @param result
	 *            an empty ReacAnalysis result.
	 * @param obj
	 *            The objective function.
	 * @return A Thread for an analysis with a varying variable.
	 */
	public abstract ThreadReac makeReacThread(boolean interactionInSolver,
			Queue<Double> fluxesQueue, Map<BioEntity, Double> entities,
			ReacAnalysisResult result, Objective obj);

	/**
	 * 
	 * Makes a thread for an analysis with two varying variables.
	 * 
	 * @param interactionInSolver
	 *            Whether or not interactions are in the solver.
	 * @param fluxesQueue
	 *            The queue of fluxes.
	 * @param result
	 *            an empty TwoReacsAnalysis result.
	 * @param entities1
	 *            Map containing the first entity to make vary.
	 * @param entities2
	 *            Map containing the second entity to make vary.
	 * @param obj
	 *            The objective function.
	 * @return A thread for an analysis with two varying variables.
	 */
	public abstract ResolveThread makeTwoReacsThread(
			boolean interactionInSolver, Queue<double[]> fluxesQueue,
			TwoReacsAnalysisResult result, Map<BioEntity, Double> entities1,
			Map<BioEntity, Double> entities2, Objective obj);

}
