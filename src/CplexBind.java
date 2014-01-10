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
 * 6 mars 2013 
 */
package src;

import ilog.concert.IloConstraint;
import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.concert.IloObjective;
import ilog.concert.IloObjectiveSense;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplexModeler;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import parsebionet.biodata.BioEntity;
import parsebionet.biodata.BioNetwork;
import src.interaction.Interaction;
import src.interaction.InteractionNetwork;
import src.interaction.cplex.RelationFactoryCPLEX;
import src.operation.OperationFactoryCPLEX;
import src.thread.ThreadFactoryCPLEX;

/**
 * 
 * CPLEX version of Bind. It transforms porblem's variables, constraints and
 * interactions into Objects usable by the CPLEX solver.
 * 
 * 
 * @author lmarmiesse 6 mars 2013
 * 
 */
public class CplexBind extends Bind {

	private IloCplexModeler model = new IloCplexModeler();
	private IloCplex cplex;
	private IloObjective cplexObj;

	/**
	 * 
	 * Map to match entities names to a CPLEX variable.
	 * 
	 */
	public Map<String, IloNumVar> vars = new HashMap<String, IloNumVar>();

	
	/**
	 * 
	 * Initialization of CPLEX parameters and of the right factories.
	 * 
	 */
	
	public void init() {
		try {
			cplex = new IloCplex();
			cplex.setOut(null);
			cplex.setWarning(null);

			// cplex parameters
			cplex.setParam(IloCplex.IntParam.Threads, 1);
			cplex.setParam(IloCplex.DoubleParam.EpRHS,
					Math.pow(10, -Vars.decimalPrecision));

			// cplex.setParam(IloCplex.IntParam.MIRCuts,-1);

			// cplex.setParam(IloCplex.DoubleParam.EpLin,1.0E-15);

			// System.out.println(cplex.getParam(IloCplex.IntParam.MIRCuts));

			// cplex.setParam(IloCplex.IntParam.AdvInd, 0);

			// cplex.setParam(IloCplex.IntParam.ParallelMode, -1);
			// System.out.println(IloCplex.IntParam.Threads.getValue());

			// System.out.println(cplex.getParam(IloCplex.IntParam.Threads));
			// System.out.println(cplex.getParam(IloCplex.IntParam.AdvInd));

		} catch (IloException e) {
			System.err.println("Concert exception '" + e + "' caught");
		}

		// creation of the right factories
		this.operationFactory = new OperationFactoryCPLEX();
		this.relationFactory = new RelationFactoryCPLEX();
		this.threadFactory = new ThreadFactoryCPLEX(constraints,
				simpleConstraints, intNet);
	}

	public CplexBind(boolean interactionInSolver) {

		super(interactionInSolver);
		init();

	}

	public CplexBind(List<Constraint> constraints,
			Map<BioEntity, Constraint> simpleConstraints,
			InteractionNetwork intNet, BioNetwork bioNet,
			boolean interactionInSolver) {
		super(constraints, simpleConstraints, intNet, bioNet,
				interactionInSolver);
		init();
	}

	public void entitiesToSolverVars() {
		vars.clear();

		try {

			model = new IloCplexModeler();
			cplex.setModel(model);

			for (BioEntity entity : intNet.getNumEntities()) {
				// System.out.println(entity.getId());
				// what default value ?

				vars.put(entity.getId(),
						cplex.numVar(0, Double.MAX_VALUE, entity.getId()));
			}
			for (BioEntity entity : intNet.getIntEntities()) {
				// System.out.println(entity.getId());
				vars.put(entity.getId(),
						cplex.intVar(0, Integer.MAX_VALUE, entity.getId()));
			}
			for (BioEntity entity : intNet.getBinaryEntities()) {

				// System.out.println(entity.getId());

				vars.put(entity.getId(), cplex.boolVar(entity.getId()));

			}

		} catch (IloException e) {
			e.printStackTrace();
		}

	}


	public void makeSolverConstraint(Constraint constraint,
			List<Object> toRemoveFromModel, Map<String, double[]> oldBounds) {

		try {
			double ub = constraint.getUb();
			double lb = constraint.getLb();
			Map<BioEntity, Double> entities = constraint.getEntities();

			IloNumExpr[] somme = new IloNumExpr[entities.size()];

			int i = 0;
			for (BioEntity entity : entities.keySet()) {
				somme[i] = (IloNumExpr) cplex.prod(entities.get(entity),
						vars.get(entity.getId()));

				i++;
			}

			IloConstraint cplexConstraint;
			if (!constraint.getNot()) {
				cplexConstraint = model.range(lb, cplex.sum(somme), ub);
				if (toRemoveFromModel != null) {
					toRemoveFromModel.add(cplexConstraint);
				}
				model.add(cplexConstraint);

			} else {
				cplexConstraint = cplex.not(model.range(lb, cplex.sum(somme),
						ub));
				if (toRemoveFromModel != null) {
					toRemoveFromModel.add(cplexConstraint);
				}
				model.add(cplexConstraint);
			}

			// if the constraint is just of the type : A = 5, we set
			// the bounds of the var A
			if (entities.size() == 1 && !constraint.getNot()
					&& constraint.getOverWritesBounds()) {

				for (BioEntity entity : entities.keySet()) {
					// if it is a "simple" constraint

					if (entities.get(entity) == 1.0) {

						if (oldBounds != null) {
							oldBounds.put(entity.getId(),
									new double[] {
											vars.get(entity.getId()).getLB(),
											vars.get(entity.getId()).getUB() });
						}

						if (solverSimpleConstraints.containsKey(entity)) {
							model.remove((IloConstraint) solverSimpleConstraints
									.get(entity));
							solverSimpleConstraints.remove(entity);
						} else {
							solverSimpleConstraints
									.put(entity, cplexConstraint);
						}
						vars.get(entity.getId()).setLB(lb);
						vars.get(entity.getId()).setUB(ub);
					}
				}
			}

		} catch (IloException e) {
			e.printStackTrace();
		}

	}

	protected void interactionsToSolverConstraints(
			List<Interaction> interactions) {

		try {
			for (Interaction i : interactions) {
				// we create the logical constraints
				cplex.add((IloConstraint) i.makeInteraction(this));

			}
		} catch (IloException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void makeSolverObjective() {

		BioEntity[] entities = obj.getEntities();
		double[] coeffs = obj.getCoeffs();
		boolean maximize = obj.getMaximize();
		String name = obj.getName();

		try {

			if (model.getObjective() == null) {
				IloNumVar[] objVars = new IloNumVar[vars.size()];
				double[] objVals = new double[vars.size()];

				if (maximize) {

					cplexObj = cplex.addMaximize();

				} else {
					cplexObj = cplex.addMinimize();
				}

				// the first time we set all vals to 0
				int i = 0;

				for (String entName : vars.keySet()) {

					cplex.setLinearCoef(cplexObj, 0, vars.get(entName));
				}

				for (i = 0; i < entities.length; i++) {
					cplex.setLinearCoef(cplexObj, coeffs[i],
							vars.get(entities[i].getId()));

				}

			} else {

				for (String entName : vars.keySet()) {

					cplex.setLinearCoef(cplexObj, 0, vars.get(entName));
				}

				if (maximize) {
					cplexObj.setSense(IloObjectiveSense.Maximize);
				} else {
					cplexObj.setSense(IloObjectiveSense.Minimize);
				}
			}

			for (int i = 0; i < entities.length; i++) {
				cplex.setLinearCoef(cplexObj, coeffs[i],
						vars.get(entities[i].getId()));

			}

		} catch (IloException e) {
			e.printStackTrace();
		}
	}

	// change a single variable value in the objective function
	public void changeObjVarValue(BioEntity e, double value) {

		try {
			cplex.setLinearCoef(cplexObj, value, vars.get(e.getId()));
		} catch (IloException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

	}

	public void setObjSense(boolean maximize) {
		try {
			if (maximize) {
				cplexObj.setSense(IloObjectiveSense.Maximize);
			} else {
				cplexObj.setSense(IloObjectiveSense.Minimize);
			}
		} catch (IloException e) {
			e.printStackTrace();
		}
	}

	public synchronized DoubleResult go(boolean saveResults) {
		try {

			while (cplex.getNMIPStarts() > 0) {

				cplex.deleteMIPStarts(0);

			}

			if (cplex.solve()) {

				if (saveResults) {
					for (String name : vars.keySet()) {
						try {
							lastSolve.put(name, cplex.getValue(vars.get(name)));
							// System.out.println(name+" : "+
							// cplex.getValue(vars.get(name)));
						} catch (Exception e) {

						}
					}
				}

				Iterator it0 = cplex.rangeIterator();
				while (it0.hasNext()) {
					IloRange r = (IloRange) it0.next();
				}

				return new DoubleResult(cplex.getObjValue(), 0);

			} else {

				// System.out.println("Solution status = " + cplex.getStatus());

				boolean displayConflict = false;

				if (displayConflict) {

					// code that prints the constraints that make the LP
					// infeasible

					System.out
							.println("Solution status = " + cplex.getStatus());
					System.out
							.println("Model Infeasible, Calling CONFLICT REFINER");

					// IloRange[] rng =
					// int numVars = 0;
					//
					// // calculate the number of non-boolean variables
					// for (int c1 = 0; c1 < model.getNumVars().length; c1++)
					// if (lp.getNumVar(c1).getType() != IloNumVarType.Bool)
					// numVars++;

					int numVars = cplex.getNcols() - cplex.getNbinVars();

					// find the number of SOSs in the model
					int numSOS = cplex.getNSOSs();
					System.out.println("Number of SOSs=" + numSOS);

					// cols : number of variables
					// rows : nb of ranges

					int numConstraints = cplex.getNrows() + 2 * numVars
							+ numSOS;
					IloConstraint[] constraints = new IloConstraint[numConstraints];

					IloRange[] ranges = new IloRange[cplex.getNrows()];

					Iterator it = cplex.rangeIterator();
					int i = 0;
					while (it.hasNext()) {
						ranges[i] = (IloRange) it.next();
						i++;
					}

					for (int c1 = 0; c1 < ranges.length; c1++) {
						constraints[c1] = ranges[c1];
					}

					int numVarCounter = 0;
					// add variable bounds to the constraints array
					for (String s : vars.keySet()) {
						IloNumVar var = vars.get(s);
						if (var.getType() != IloNumVarType.Bool) {
							constraints[ranges.length + 2 * numVarCounter] = cplex
									.le(var.getLB(), var);
							constraints[ranges.length + 2 * numVarCounter]
									.setName(var.toString() + "_LB");
							constraints[ranges.length + 2 * numVarCounter + 1] = cplex
									.ge(var.getUB(), var);
							constraints[ranges.length + 2 * numVarCounter + 1]
									.setName(var.toString() + "_UB");
							numVarCounter++;
						}
					}

					System.out.println(constraints.length);

					double[] prefs = new double[constraints.length];
					for (int c1 = 0; c1 < constraints.length; c1++) {
						prefs[c1] = 1.0;
					}
					if (cplex.refineConflict(constraints, prefs)) {
						System.out
								.println("Conflict Refinement process finished: Printing Conflicts");
						IloCplex.ConflictStatus[] conflict = cplex
								.getConflict(constraints);
						int numConConflicts = 0;
						int numBoundConflicts = 0;
						int numSOSConflicts = 0;
						for (int c2 = 0; c2 < constraints.length; c2++) {
							if (conflict[c2] == IloCplex.ConflictStatus.Member) {
								System.out.println("  Proved  : "
										+ constraints[c2]);
								if (c2 < ranges.length)
									numConConflicts++;
								else if (c2 < ranges.length + 2 * numVarCounter)
									numBoundConflicts++;
								else
									numSOSConflicts++;

							} else if (conflict[c2] == IloCplex.ConflictStatus.PossibleMember) {
								System.out.println("  Possible  : "
										+ constraints[c2]);
								if (c2 < ranges.length)
									numConConflicts++;
								else if (c2 < ranges.length + 2 * numVarCounter)
									numBoundConflicts++;
								else
									numSOSConflicts++;
							}
						}
						System.out.println("Conflict Summary:");
						System.out.println("  Constraint conflicts     = "
								+ numConConflicts);
						System.out.println("  Variable Bound conflicts = "
								+ numBoundConflicts);
						System.out.println("  SOS conflicts            = "
								+ numSOSConflicts);
					} else {
						System.out.println("Conflict could not be refined");
					}
				}
			}

			return new DoubleResult(0, 1);
		} catch (IloException e) {
			e.printStackTrace();
		}

		return new DoubleResult(0, 1);

	}

	public void clear() {
		try {
			cplex.clearModel();
		} catch (IloException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void end() {
		cplex.end();
	}

	public void setMIPStart() {
	}

	public boolean isMIP() {
		return cplex.isMIP();
	}

	protected void deleteConstraints(List<Object> solverConstraints) {

		try {
			for (Object constraint : solverConstraints) {
				model.remove((IloRange) constraint);
			}
		} catch (IloException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	protected void changeVarBounds(String entity, double[] bounds) {
		try {
			vars.get(entity).setLB(bounds[0]);

			vars.get(entity).setUB(bounds[1]);
		} catch (IloException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
