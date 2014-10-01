/*******************************************************************************
 *  Copyright INRA
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
package flexflux.general;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import parsebionet.biodata.BioChemicalReaction;
import parsebionet.biodata.BioComplex;
import parsebionet.biodata.BioEntity;
import parsebionet.biodata.BioGene;
import parsebionet.biodata.BioNetwork;
import parsebionet.biodata.BioPhysicalEntity;
import parsebionet.biodata.BioPhysicalEntityParticipant;
import parsebionet.biodata.BioProtein;
import parsebionet.io.Sbml2Bionetwork;
import flexflux.analyses.FBAAnalysis;
import flexflux.analyses.SteadyStateAnalysis;
import flexflux.analyses.result.FBAResult;
import flexflux.analyses.result.SteadyStateAnalysisResult;
import flexflux.input.InteractionFileReader;
import flexflux.input.SBMLQualReader;
import flexflux.interaction.And;
import flexflux.interaction.Interaction;
import flexflux.interaction.InteractionNetwork;
import flexflux.interaction.Or;
import flexflux.interaction.Relation;
import flexflux.interaction.RelationFactory;
import flexflux.interaction.Unique;
import flexflux.operation.OperationFactory;
import flexflux.thread.ThreadFactory;

/**
 * 
 * Bind handles the problem. It is the central class of FlexFlux, it is used to
 * create variables, constraints, interactions, and translate them to the
 * solver.
 * 
 * @author lmarmiesse 6 mars 2013
 */
public abstract class Bind {

	public boolean checkInteractionNetwork = true;

	/**
	 * Used for pareto analysis and conditionComparison, if set to false, the
	 * objective in the condition file is ignored.
	 */
	private boolean loadObjective = true;

	/**
	 * Objective of the problem.
	 */
	protected Objective obj;

	/**
	 * List used when several objectives are given in the condition file.
	 */
	public List<Objective> constraintObjectives = new ArrayList<Objective>();

	/**
	 * Will permit to create the right operations.
	 */
	protected OperationFactory operationFactory;
	/**
	 * To create the right relations.
	 */
	protected RelationFactory relationFactory;
	/**
	 * To create the right type of thread.
	 */
	protected ThreadFactory threadFactory;

	/**
	 * The interaction network of the problem.
	 */
	protected InteractionNetwork intNet = new InteractionNetwork();

	/**
	 * The metabolic network of the problem.
	 */
	protected BioNetwork bioNet;

	/**
	 * All problem's constraints.
	 */
	protected List<Constraint> constraints = new ArrayList<Constraint>();

	/**
	 * List of dead reactions.
	 */
	protected Collection<BioChemicalReaction> deadReactions = new ArrayList<BioChemicalReaction>();

	/**
	 * Contains the results of the last FBA performed.
	 */
	protected Map<String, Double> lastSolve = new HashMap<String, Double>();

	public Map<String, Double> getLastSolve() {
		return lastSolve;
	}

	/**
	 * Contains the constraints of just one entity and with coeff = 1, it make
	 * it easier to get and change them.
	 */
	protected Map<BioEntity, Constraint> simpleConstraints = new ConcurrentHashMap<BioEntity, Constraint>();

	/**
	 * Contains all the constraints caused by the steady state assumption.
	 */
	protected Map<BioEntity, List<Constraint>> steadyStateConstraints = new ConcurrentHashMap<BioEntity, List<Constraint>>();

	/**
	 * The solver constraint object corresponding to an entity.
	 */
	protected Map<BioEntity, Object> solverSimpleConstraints = new HashMap<BioEntity, Object>();

	/**
	 * If false the fba cannot start.
	 */
	public boolean solverPrepared = false;

	/**
	 * Map used for rFBA to link exchange reactions to external metabolites.
	 */
	protected Map<BioChemicalReaction, Map<BioEntity, Double>> exchangeInteractions = new HashMap<BioChemicalReaction, Map<BioEntity, Double>>();

	/**
	 * To handle time dependent interactions. the Relation is true if the
	 * bioEntity is > 0 . double[0] corresponds to how long it takes for the
	 * interactions to become active and double[1] corresponds to how long it
	 * stays active.
	 */
	protected Map<BioEntity, Map<Relation, double[]>> interactionsEntitiesConsequence = new HashMap<BioEntity, Map<Relation, double[]>>();

	protected Map<BioEntity, Relation> interactionsEntitiesCause = new HashMap<BioEntity, Relation>();

	/**
	 * Transforms problem entities into the right solver variables.
	 */
	public abstract void entitiesToSolverVars();

	/**
	 * List used for FBA to know which variables are constrained.
	 */
	public Set<BioEntity> constrainedEntities = new HashSet<BioEntity>();

	/**
	 * Creates a constraint for the solver.
	 * 
	 * @param c
	 *            Constraint to create
	 * @param toRemoveFromModel
	 *            is used to come back to how it was before.
	 * @param oldBounds
	 *            is used to come back to how it was before.
	 */

	protected void makeSolverConstraint(Constraint c,
			List<Object> toRemoveFromModel, Map<String, double[]> oldBounds) {

		constrainedEntities.addAll(c.getEntities().keySet());

		createSolverConstraint(c, toRemoveFromModel, oldBounds);

	}

	protected abstract void createSolverConstraint(Constraint c,
			List<Object> toRemoveFromModel, Map<String, double[]> oldBounds);

	/**
	 * Deletes solver constraints.
	 * 
	 * @param solverConstraints
	 */
	protected abstract void deleteConstraints(List<Object> solverConstraints);

	/**
	 * Changes the bounds of an entity.
	 * 
	 * @param entity
	 *            The entity to change
	 * @param bounds
	 *            New bounds
	 */
	protected abstract void changeVarBounds(String entity, double[] bounds);

	/**
	 * Transforms the porblem's objective into a solver objective.
	 */
	public abstract void makeSolverObjective();

	/**
	 * Changes the coefficient of an entity in the objective.
	 * 
	 * @param e
	 *            The entity
	 * @param d
	 *            The coefficient
	 */
	public abstract void changeObjVarValue(BioEntity e, double d);

	/**
	 * Sets the sense of the objective function.
	 * 
	 * @param maximize
	 *            if true : maximize, if false : minimize
	 */
	public abstract void setObjSense(boolean maximize);

	/**
	 * Clears the solver's model.
	 */
	protected abstract void clear();

	/**
	 * Clears the solver.
	 * 
	 */
	protected void clearSolver() {

		clear();
		solverPrepared = false;

	}

	/**
	 * @return true : the problem is a MIP, false : it is not
	 */
	public abstract boolean isMIP();

	/**
	 * Make the solver perform the solves.
	 * 
	 * @param saveResults
	 * @return An object containing the objective value and a flag that says if
	 *         the problem was feasible or not.
	 */
	protected abstract DoubleResult go(boolean saveResults);

	/**
	 * 
	 * Takes constraints, checks interactions for those constraints, and
	 * performs FBA.
	 * 
	 * @param constraintsToAdd
	 *            List of constraints to add before the FBA.
	 * @param saveResults
	 *            Determines if the results will be saved.
	 * @param checkInteractions
	 *            Determines if all interactions will be checked.
	 * @return A doubleResult object containing the result and a flag saying if
	 *         the problem was feasible
	 */
	public DoubleResult FBA(List<Constraint> constraintsToAdd,
			boolean saveResults, boolean checkInteractions) {

		if (solverPrepared) {

			if (checkInteractions) {
				// treatment to ensure there is no problem

				Map<BioEntity, Constraint> oldSimpleConstraints = new HashMap<BioEntity, Constraint>();
				// we add the simple constraints to be taken into account when
				// checking interactions
				Set<BioEntity> hadNoSimpleConstraint = new HashSet<BioEntity>();

				for (Constraint constr : constraintsToAdd) {

					if (constr.getEntities().size() == 1) {
						for (BioEntity ent : constr.getEntities().keySet()) {
							if (constr.getEntities().get(ent) == 1.0) {
								if (simpleConstraints.containsKey(ent)) {
									oldSimpleConstraints.put(ent,
											simpleConstraints.get(ent));
								} else {
									hadNoSimpleConstraint.add(ent);
								}
								simpleConstraints.put(ent, constr);
							}
						}
					}

				}

				if (checkInteractionNetwork) {

					List<Constraint> intNetSteadyStateConstraints = new ArrayList<Constraint>();
					
					SteadyStateAnalysis ssa = new SteadyStateAnalysis(this,this.getInteractionNetwork(),simpleConstraints);
					SteadyStateAnalysisResult res = ssa.runAnalysis();
					
					if(Vars.writeInteractionNetworkStates){
						res.writeToFile(statesFileName);
					}
					
					for (Constraint c : res.getSteadyStateConstraints()) {
						intNetSteadyStateConstraints.add(c);
					}

					for (Constraint constr : intNetSteadyStateConstraints) {

						if (constr.getEntities().size() == 1) {
							for (BioEntity ent : constr.getEntities().keySet()) {
								if (constr.getEntities().get(ent) == 1.0) {
									if (simpleConstraints.containsKey(ent)
											&& !hadNoSimpleConstraint
													.contains(ent)) {
										oldSimpleConstraints.put(ent,
												simpleConstraints.get(ent));

									}
									simpleConstraints.put(ent, constr);

								}
							}
						}

					}
					constraintsToAdd.addAll(intNetSteadyStateConstraints);
				}

				List<Constraint> GPRConstraints = new ArrayList<Constraint>();
				for (Interaction i : intNet.getGPRInteractions()) {
					if (i.getCondition().isTrue(simpleConstraints)) {
						GPRConstraints.addAll(intNet.getInteractionToConstraints().get(i));
					}
				}
				//

				for (Constraint constr : constraintsToAdd) {
					if (constr.getEntities().size() == 1) {
						for (BioEntity ent : constr.getEntities().keySet()) {
							if (constr.getEntities().get(ent) == 1.0) {

								if (oldSimpleConstraints.containsKey(ent)) {
									simpleConstraints.put(ent,
											oldSimpleConstraints.get(ent));
								} else {
									simpleConstraints.remove(ent);
								}

							}
						}
					}
				}

				constraintsToAdd.addAll(GPRConstraints);

			}

			// /////////////////
			// /////////////////
			// if a constraint is an external metab set to 0, we change the
			// bound of the exchange reaction
			List<Constraint> extMetabConstraints = new ArrayList<Constraint>();

			Set<Constraint> constraintsToTest = new HashSet<Constraint>();
			constraintsToTest.addAll(constraintsToAdd);
			// constraintsToTest.addAll(constraints);
			constraintsToTest.addAll(simpleConstraints.values());

			for (Constraint c : constraintsToTest) {
				if (c.getEntities().size() == 1 && c.getLb() == 0
						&& c.getUb() == 0) {
					BioEntity b = null;
					for (BioEntity ent : c.getEntities().keySet()) {
						b = ent;
					}
					if (b.getClass().getSimpleName()
							.equals("BioPhysicalEntity")) {
						BioPhysicalEntity metab = (BioPhysicalEntity) b;
						if (metab.getBoundaryCondition()) {
							// now we need to find the exchangReaction concerned
							// and change one of its bound

							for (String reacName : metab
									.getReactionsAsSubstrate().keySet()) {
								BioChemicalReaction reac = metab
										.getReactionsAsSubstrate()
										.get(reacName);

								//
								if (simpleConstraints.containsKey(reac)) {

									if (reac.getLeftList().containsKey(
											metab.getId())
											&& reac.getLeftList().size() == 1) {

										double lb = simpleConstraints.get(reac)
												.getLb();
										double ub = 0.0;

										Map<BioEntity, Double> constMap = new HashMap<BioEntity, Double>();
										constMap.put(reac, 1.0);

										extMetabConstraints.add(new Constraint(
												constMap, lb, ub));
									}

									if (reac.getRightList().containsKey(
											metab.getId())
											&& reac.getRightList().size() == 1
											&& reac.isReversible() == true) {

										double lb = 0.0;

										double ub = simpleConstraints.get(reac)
												.getUb();

										Map<BioEntity, Double> constMap = new HashMap<BioEntity, Double>();
										constMap.put(reac, 1.0);

										extMetabConstraints.add(new Constraint(
												constMap, lb, ub));
									}

								}
							}

						}
					}
				}
			}
			constraintsToAdd.addAll(extMetabConstraints);
			// ////////////////
			// ////////////////

			return goWithConstraints(constraintsToAdd, saveResults);

		} else {
			System.err.println("Error : unprepared solver");
			return new DoubleResult(0, 1);
		}

	}

	/**
	 * 
	 * Performs the solver solve with constraints.
	 * 
	 * @param constraints
	 *            Constraints to add before the FBA is done.
	 * @param saveResults
	 *            Determines if the results will be saved.
	 * @return A doubleResult object containing the result and a flag saying if
	 *         the problem was feasible
	 */
	private DoubleResult goWithConstraints(List<Constraint> constraints,
			boolean saveResults) {

		// objects represent a constraint for the solver
		List<Object> toRemoveFromModel = new ArrayList<Object>();
		Map<String, double[]> oldBounds = new HashMap<String, double[]>();

		for (Constraint c : constraints) {

			makeSolverConstraint(c, toRemoveFromModel, oldBounds);
		}

		DoubleResult result = go(saveResults);

		// we go back to how it was
		deleteConstraints(toRemoveFromModel);

		for (String entity : oldBounds.keySet()) {
			changeVarBounds(entity, oldBounds.get(entity));

		}

		return result;
	}

	/**
	 * Constructor used to copy the bind.
	 * 
	 * 
	 * @param constraints2
	 *            Constraints to copy.
	 * @param simpleConstraints
	 *            Simple constraints to copy.
	 * @param intNet
	 *            Interaction network to copy.
	 * @param bioNet
	 *            BioNetwork to copy.
	 */
	public Bind(List<Constraint> constraints2,
			Map<BioEntity, Constraint> simpleConstraints,
			InteractionNetwork intNet, BioNetwork bioNet) {

		this.constraints.addAll(constraints2);
		this.simpleConstraints.putAll(simpleConstraints);

		// // we copy the interaction network
		// for (BioEntity ent : intNet.getNumEntities()) {
		// this.intNet.addNumEntity(ent);
		// }
		// for (BioEntity ent : intNet.getIntEntities()) {
		// this.intNet.addIntEntity(ent);
		// }
		// for (BioEntity ent : intNet.getBinaryEntities()) {
		// this.intNet.addBinaryEntity(ent);
		// }
		//
		// for (Interaction inter : intNet.getGPRInteractions()) {
		// this.intNet.addGPRIntercation(inter);
		// }
		//
		// for (BioEntity target : intNet.getTargetToInteractions().keySet()) {
		// this.intNet.addTargetInteractions(target, intNet
		// .getTargetToInteractions().get(target)[0], intNet
		// .getTargetToInteractions().get(target)[1]);
		// }

		this.intNet = intNet;

		this.bioNet = bioNet;
	}

	public Bind() {

	}

	/**
	 * Loads an SBML file.
	 * 
	 * @param path
	 *            Path to the SBML file.
	 * @param ext
	 *            If true : uses extended SBML format.
	 */
	public void loadSbmlNetwork(String path, boolean ext) {
		Sbml2Bionetwork parser = new Sbml2Bionetwork(path, ext);
		setNetwork(parser.getBioNetwork());
	}

	/**
	 * 
	 * Sets the problem's network and adds constraints and interactions included
	 * in it.
	 * 
	 * @param network
	 *            Network to give the problem.
	 * @param ext
	 *            If true : uses extended SBML format.
	 */
	public void setNetwork(BioNetwork network) {

		if (network == null) {

			System.err.println("Error : could not load sbml file");
			System.exit(0);
		}

		this.bioNet = network;
		deadReactions = bioNet.trim();

		threadFactory.setBioNet(bioNet);
		simpleConstraints.clear();
		constraints.clear();
		intNet.clear();
		clear();

		// we add the trimed reactions as set to 0
		for (BioChemicalReaction trimed : deadReactions) {

			intNet.addNumEntity(trimed);

			Map<BioEntity, Double> constMap = new HashMap<BioEntity, Double>();
			constMap.put(trimed, 1.0);
			constraints.add(new Constraint(constMap, 0.0, 0.0));
		}

		solverPrepared = false;

		// adds networks entites
		addNetworkEntities();

		// creates the steady state constraints
		makeSteadyStateConstraints();

		// sets the reaction bounds
		setReactionsBounds();

		// GPR to interaction
		gprInteractions();

	}

	/**
	 * Creates interactions from the interaction file.
	 * 
	 * @param path
	 *            Path to the interaction file.
	 */
	public void loadInteractionsFile(String path) {

		if (path.endsWith(".xml") || path.endsWith(".sbml")) {
			intNet = SBMLQualReader.loadSbmlQual(path, intNet, relationFactory);
		}
		else{
			intNet = InteractionFileReader.readInteractionFile(path, intNet, relationFactory);
		}

	}

	/**
	 * 
	 * Adds an entity to the problem.
	 * 
	 * @param b
	 *            Entity to add.
	 * @param integer
	 *            If it is an integer entity.
	 * @param binary
	 *            If it is a binary entity.
	 */
	public void addRightEntityType(BioEntity b, boolean integer, boolean binary) {
		if (binary) {
			intNet.addBinaryEntity(b);
			return;
		}
		if (integer && !binary) {
			intNet.addIntEntity(b);
			return;
		}
		if (!integer && !binary) {
			intNet.addNumEntity(b);
			return;
		}
	}

	/**
	 * 
	 * Loads the objective, creates variables and constraints from the condition
	 * file
	 * 
	 * @param path
	 *            Path to the condition file.
	 */
	public void loadConditionsFile(String path) {

		try {

			// variables for the objective
			List<String> objString = new ArrayList<String>();
			Map<String, Boolean> objStringMap = new HashMap<String, Boolean>();

			boolean isError = false;
			boolean integer = false;
			boolean binary = false;
			boolean equations = false;

			// is used to check for errors
			Set<BioEntity> fileEntities = new HashSet<BioEntity>();

			BufferedReader in = new BufferedReader(new FileReader(path));

			String line;
			int nbLine = 1;
			while ((line = in.readLine()) != null) {
				if (line.startsWith("#")) {
					nbLine++;
					continue;
				}
				if (line.startsWith("INTEGER")) {
					integer = true;
					binary = false;
				} else if (line.startsWith("EQUATIONS")) {
					equations = true;
				} else if (line.startsWith("BINARY")) {
					binary = true;
					integer = false;
				} else if (line.startsWith("obj :")) {

					boolean maximize = true;

					if (line.contains("MIN(")) {
						maximize = false;
					}

					String expr = (String) line.subSequence(
							line.indexOf("(") + 1, line.indexOf(")"));

					objString.add(expr);
					objStringMap.put(expr, maximize);
					// we create the objective at the end
				}

				else if (!line.equals("")) {
					// when it's not the equations
					if (!equations) {
						
						String expr[] = line.replaceAll("\t", " ").split(" ");

						if (binary) {

							if (expr.length < 1 || expr.length > 3) {

								System.err
										.println("Warning : Error in condition file line "
												+ nbLine
												+ " , binary misformed");
								nbLine++;
								continue;
							}
						} else {

							if (expr.length < 2 || expr.length > 3) {

								System.err
										.println("Warning : Error in condition file line "
												+ nbLine);
								// we go to the next line
								nbLine++;
								continue;

							}
						}

						// if the entity does not exist yet

						boolean knownEntity = true;

						if (intNet.getEntity(expr[0]) == null) {

							// if it is the sum of fluxes
							if (expr[0].equals(Vars.FluxSumKeyWord)) {
								// we create the corresponding entity
								createFluxesSummation();
							} else {

								knownEntity = false;
								// System.err
								// .println("Warning : new entity created : "
								// + expr[0]);
								BioEntity b = new BioEntity(expr[0]);
								b.setId(expr[0]);
								addRightEntityType(b, integer, binary);
							}
						}

						BioEntity entity = intNet.getEntity(expr[0]);
						
						if (knownEntity) {
							if (binary || integer) {
								setRightEntityType(entity, integer, binary);
							}
						}

						if (fileEntities.contains(entity)) {
							System.err.println("Error in condition file line "
									+ nbLine + ", entity " + expr[0]
									+ " is set more than once");

							isError = true;
						}

						// if there is already a constraint on this entity, we
						// delete it
						Constraint toDelete = null;
						for (Constraint c : constraints) {
							Map<BioEntity, Double> entities = c.getEntities();

							if (entities.size() == 1
									&& entities.containsKey(entity)) {

								if (entities.get(entity) == 1) {
									// System.err
									// .println("Warning : condition file line "
									// + nbLine
									// +
									// " : this constraint removes an existing one");
									toDelete = c;

									break;
								}
							}
						}
						if (toDelete != null) {
							constraints.remove(toDelete);
						}

						fileEntities.add(intNet.getEntity(expr[0]));

						if (binary) {
							if (expr.length == 1) {
								nbLine++;
								continue;
							}
						}

						double lb = 0;
						double ub = 0;

						try {
							lb = Double.parseDouble(expr[1]);

							if (expr.length == 2) {
								ub = Double.parseDouble(expr[1]);
							} else {
								ub = Double.parseDouble(expr[2]);
							}
						} catch (NumberFormatException e) {
							System.err
									.println("Warning : Error in condition file line "
											+ nbLine);
							nbLine++;
							continue;
						}

						if (binary) {
							if ((lb != 1 && lb != 0) || (ub != 1 && ub != 0)) {
								System.err
										.println("Warning : Error in condition file line "
												+ nbLine
												+ " , binary bounds must be 0 or 1");
								nbLine++;
								continue;
							}
						}

						if (lb > ub) {
							System.err
									.println("Warning : Error in condition file line "
											+ nbLine
											+ " , lower bound is higher than upper bound");
							nbLine++;
							continue;
						}

						Map<BioEntity, Double> constraintMap = new HashMap<BioEntity, Double>();
						constraintMap.put(entity, 1.0);

						Constraint c = new Constraint(constraintMap, lb, ub);
						constraints.add(c);
						
						simpleConstraints.put(entity, c);

					}
					// when the "EQUATIONS" line was passed
					else {

						String[] equation = line.replaceAll("\\s", "").split(
								"<=|>=|=|<|>");

						if (equation.length != 2) {
							System.err
									.println("Warning : Error in condition file line "
											+ nbLine + " missformed equation");
							nbLine++;
							continue;
						}

						Map<BioEntity, Double> leftMap = makeFormulaFromString(equation[0]);

						Map<BioEntity, Double> rightMap = makeFormulaFromString(equation[1]);

						Map<BioEntity, Double> finalEquation = new HashMap<BioEntity, Double>();

						for (BioEntity b : leftMap.keySet()) {
							finalEquation.put(b, leftMap.get(b));
						}

						double bound = 0;

						for (BioEntity b : rightMap.keySet()) {
							if (b != null) {
								finalEquation.put(b, rightMap.get(b) * -1);
							} else {
								bound = rightMap.get(b);
							}
						}

						// we seperate the different cases
						// order is important
						if (line.contains("<=")) {

							Constraint c = new Constraint(finalEquation,
									-Double.MAX_VALUE, bound);
							constraints.add(c);

						} else if (line.contains(">=")) {

							Constraint c = new Constraint(finalEquation, bound,
									Double.MAX_VALUE);
							constraints.add(c);
						}

						else if (line.contains("=")) {
							Constraint c = new Constraint(finalEquation, bound,
									bound);
							constraints.add(c);
						} else if (line.contains("<")) {
							if (Vars.cheat) {

								Constraint c = new Constraint(finalEquation,
										-Double.MAX_VALUE, bound - Vars.epsilon);
								constraints.add(c);

							} else {
								Constraint c = new Constraint(finalEquation,
										-Double.MAX_VALUE, bound);
								constraints.add(c);
								c = new Constraint(finalEquation, bound, true);
								constraints.add(c);
							}

						} else if (line.contains(">")) {
							if (Vars.cheat) {

								Constraint c = new Constraint(finalEquation,
										bound + Vars.epsilon, Double.MAX_VALUE);
								constraints.add(c);

							} else {
								Constraint c = new Constraint(finalEquation,
										bound, Double.MAX_VALUE);
								constraints.add(c);

								c = new Constraint(finalEquation, bound, true);
								constraints.add(c);
							}
						}

					}
				}

				nbLine++;
			}

			if (loadObjective) {

				String realObjString = objString.get(objString.size() - 1);

				obj = makeObjectiveFromString(realObjString,
						objStringMap.get(realObjString), realObjString);

				objString.remove(realObjString);
				objStringMap.remove(realObjString);

				for (String objStr : objString) {
					constraintObjectives.add(makeObjectiveFromString(objStr,
							objStringMap.get(objStr), objStr));
				}

			}

			if (isError) {
				System.err.println("Condition file not conform");
				System.exit(1);
			}

			in.close();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * Changes the type of an entity to a new one.
	 * 
	 * @param entity
	 *            Entity to change.
	 * @param integer
	 *            If it is an integer entity.
	 * @param binary
	 *            If it is a binary entity.
	 */

	public void setRightEntityType(BioEntity entity, boolean integer,
			boolean binary) {

		intNet.removeNumEntity(entity);

		if (binary) {

			intNet.addBinaryEntity(entity);

		} else if (integer) {
			intNet.addIntEntity(entity);

		}

	}

	/**
	 * 
	 * 
	 * @return The entity corresponding to the sum of the absolute value of all
	 *         the reaction fluxes.
	 */

	public BioEntity createFluxesSummation() {

		BioEntity FluxesSummation = new BioEntity(Vars.FluxSumKeyWord);

		intNet.addNumEntity(FluxesSummation);

		Map<BioEntity, Double> summationEntities = new HashMap<BioEntity, Double>();

		for (String entName : bioNet.getBiochemicalReactionList().keySet()) {

			BioEntity ent = bioNet.getBiochemicalReactionList().get(entName);

			if (simpleConstraints.get(ent).getLb() < 0) {

				for (Constraint c : steadyStateConstraints.get(ent)) {

					double coeff = c.getEntities().get(ent);
					c.getEntities().remove(ent);

					BioEntity irrevReac1 = new BioEntity(ent.getId()
							+ Vars.Irrev1);

					intNet.addNumEntity(irrevReac1);

					c.getEntities().put(irrevReac1, coeff);

					BioEntity irrevReac2 = new BioEntity(ent.getId()
							+ Vars.Irrev2);

					intNet.addNumEntity(irrevReac2);

					c.getEntities().put(irrevReac2, coeff * -1);

					Map<BioEntity, Double> constMap = new HashMap<BioEntity, Double>();

					constMap.put(irrevReac1, 1.0);

					Constraint c1 = new Constraint(constMap, 0.0,
							simpleConstraints.get(ent).getUb());

					Map<BioEntity, Double> constMap2 = new HashMap<BioEntity, Double>();

					constMap2.put(irrevReac2, 1.0);

					Constraint c2 = new Constraint(constMap2, 0.0,
							simpleConstraints.get(ent).getLb() * -1);

					constraints.add(c1);
					simpleConstraints.put(irrevReac1, c1);
					constraints.add(c2);
					simpleConstraints.put(irrevReac2, c2);

					Map<BioEntity, Double> constMap3 = new HashMap<BioEntity, Double>();

					constMap3.put(ent, 1.0);
					constMap3.put(irrevReac1, -1.0);
					constMap3.put(irrevReac2, 1.0);

					Constraint c3 = new Constraint(constMap3, 0.0, 0.0);

					constraints.add(c3);

					summationEntities.put(irrevReac1, 1.0);

					summationEntities.put(irrevReac2, 1.0);

				}
			} else {

				summationEntities.put(
						bioNet.getBiochemicalReactionList().get(entName), 1.0);

			}

		}

		// the entity is equal to the summ of all fluxes
		summationEntities.put(FluxesSummation, -1.0);

		Constraint summationConstraint = new Constraint(summationEntities, 0.0,
				0.0);

		constraints.add(summationConstraint);

		return FluxesSummation;

	}

	/**
	 * * creates an objective from a string.
	 * 
	 * @param expr
	 *            string used to create the objective.
	 * @param maximize
	 *            Boolean that determines if the objectives maximizes of
	 *            minimizes the function.
	 * @param name
	 *            The name of the objective.
	 * @return The created objective.
	 */
	public Objective makeObjectiveFromString(String expr, boolean maximize,
			String name) {

		String vars[] = expr.split("\\+");

		BioEntity[] entities = new BioEntity[vars.length];
		double[] coeffs = new double[vars.length];

		int i = 0;

		for (String var : vars) {

			String parts[] = var.trim().split("\\*");

			if (parts.length < 2) {

				if (intNet.getEntity(parts[0]) == null) {

					// if the objectife function is the sum of fluxes
					if (parts[0].equals(Vars.FluxSumKeyWord)) {

						entities[i] = createFluxesSummation();
						coeffs[i] = 1;

					}

					else {
						System.err.println("Error : a member of the objective "
								+ parts[0] + " is unknown");

						System.exit(0);

					}
				}
				entities[i] = intNet.getEntity(parts[0]);
			}

			// to handle if there is no coefficient
			if (parts.length > 1) {

				if (intNet.getEntity(parts[1]) == null) {

					// if the objectife function is the sum of fluxes
					if (parts[1].equals(Vars.FluxSumKeyWord)) {

						entities[i] = createFluxesSummation();

						try {
							coeffs[i] = Double.parseDouble(parts[0]);
						} catch (Exception e) {
							System.err
									.println("Error in condition file line, objective coefficient must be a number");

							System.exit(0);
						}

					}

					else {

						System.err.println("Error : a member of the objective "
								+ parts[1] + " is unknown");

						System.exit(0);
					}
				}
				entities[i] = intNet.getEntity(parts[1]);
				try {
					coeffs[i] = Double.parseDouble(parts[0]);
				} catch (Exception e) {
					System.err
							.println("Error in condition file line, objective coefficient must be a number");

					System.exit(0);
				}
			} else {
				coeffs[i] = 1;
			}

			i++;
		}

		Objective objec = new Objective(entities, coeffs, name, maximize);

		return objec;

	}

	/**
	 * Creates an equation from a string.
	 * 
	 * @param s
	 *            String to create the equation from.
	 * @return A map of entities and their coefficients.
	 */
	private Map<BioEntity, Double> makeFormulaFromString(String s) {

		Map<BioEntity, Double> map = new HashMap<BioEntity, Double>();

		String equation = s.replaceAll("\\s", "");
		equation = equation.replaceAll("-", "\\+-");

		String[] members = equation.split("\\+");

		if (members.length == 1 && !members[0].contains("*")) {

			BioEntity entity = intNet.getEntity(members[0]);

			if (entity != null) {
				map.put(entity, 1.0);
			} else {
				double coeff;
				try {
					coeff = Double.parseDouble(members[0]);
					map.put(null, coeff);

				} catch (Exception e) {
					System.err.println("Error : in condition file variable "
							+ members[0] + " unknown");
					System.exit(0);
				}

			}

		} else {
			for (String member : members) {
				if (!member.equals("")) {
					String parts[] = member.split("\\*");

					// if there is no coefficient
					if (parts.length == 1) {

						BioEntity entity = intNet.getEntity(parts[0]);

						if (entity != null) {
							map.put(entity, 1.0);
						} else {
							System.err
									.println("Error : in condition file variable "
											+ parts[0] + " unknown");
							System.exit(0);
						}

					}

					// if there is a coefficient
					else {
						double coeff;
						// we try to see if the coefficient is on the left
						try {
							coeff = Double.parseDouble(parts[0]);
							BioEntity entity = intNet.getEntity(parts[1]);

							if (entity != null) {
								map.put(entity, coeff);
							} else {

								System.err
										.println("Error : in condition file variable "
												+ parts[1] + " unknown");
								System.exit(0);
							}

						}
						// and then on the right
						catch (Exception e) {

							System.err
									.println("Error :  in condition file, coefficient must be on the left side");
							System.exit(0);

						}

					}
				}
			}
		}
		return map;
	}

	/**
	 * 
	 * Transforms all problem entities, constraints an interaction into solver
	 * entities, constraints an interactions. Without this step, the solver
	 * cannot solve the problem.
	 * 
	 */
	public void prepareSolver() {

		clearSolver();
		
		entitiesToSolverVars();

		constraintsToSolverConstraints();

		if (obj != null) {
			makeSolverObjective();
		}

		solverPrepared = true;

		Objective realObj = obj;

		// if there are constraint objective, we treat them
		if (constraintObjectives.size() != 0) {
			if (Vars.verbose) {
				System.err.println(Vars.libertyPercentage
						+ "% of non optimality");
			}
		}
		for (Objective constObj : constraintObjectives) {

			this.setObjective(constObj);

			FBAAnalysis fba = new FBAAnalysis(this);
			FBAResult resultFba = fba.runAnalysis();

			// we built the corresponding constraint
			Map<BioEntity, Double> constMap = new HashMap<BioEntity, Double>();

			for (int i = 0; i < constObj.getEntities().length; i++) {
				constMap.put(constObj.getEntities()[i], constObj.getCoeffs()[i]);
			}

			double lb = resultFba.getObjValue();
			double ub = resultFba.getObjValue();
			double delta = Math.abs(resultFba.getObjValue())
					* Vars.libertyPercentage / 100;

			Constraint c = new Constraint(constMap, lb - delta, ub + delta);

			constraints.add(c);

			makeSolverConstraint(c, null, null);

		}

		if (realObj != null && constraintObjectives.size() > 0) {
			this.setObjective(realObj);
		}

	}

	/**
	 * 
	 * Adds entities to the problem from the metaboblic network.
	 * 
	 */
	private void addNetworkEntities() {

		List<BioEntity> entities = new ArrayList<BioEntity>();
		// we add the reactions
		Map<String, BioChemicalReaction> reactionsMap = bioNet
				.getBiochemicalReactionList();

		for (String reacName : reactionsMap.keySet()) {
			entities.add(reactionsMap.get(reacName));
		}

		// we add the metabolites
		Map<String, BioPhysicalEntity> metabolitesMap = bioNet
				.getPhysicalEntityList();

		for (String metabName : metabolitesMap.keySet()) {
			entities.add(metabolitesMap.get(metabName));
		}

		// we add the genes
		Map<String, BioGene> genesMap = bioNet.getGeneList();
		for (String geneName : genesMap.keySet()) {
			entities.add(genesMap.get(geneName));
		}

		// we add the proteins
		Map<String, BioProtein> proteinsMap = bioNet.getProteinList();
		for (String protName : proteinsMap.keySet()) {
			entities.add(proteinsMap.get(protName));
		}

		for (BioEntity be : entities) {
			intNet.addNumEntity(be);
		}
	}

	/**
	 * Adds steady-state constraints to the problem from the metabolic network.
	 */
	private void makeSteadyStateConstraints() {

		int reacIndice = 0;
		int metabIndice = 0;
		// indices for the stoechiometry matrix
		Map<String, Integer> metabIndiceMap = new HashMap<String, Integer>();
		Map<String, Integer> reacIndiceMap = new HashMap<String, Integer>();

		Map<String, BioChemicalReaction> reactionsMap = bioNet
				.getBiochemicalReactionList();

		Map<String, BioPhysicalEntity> metabolitesMap = bioNet
				.getPhysicalEntityList();

		for (String name : metabolitesMap.keySet()) {
			// we only keep internal metabolites
			if (!metabolitesMap.get(name).getBoundaryCondition()) {
				metabIndiceMap.put(name, metabIndice);
				metabIndice++;
			}

		}

		// stoe matrix
		double[][] sMatrix = new double[reactionsMap.size()][metabolitesMap
				.size()];

		// we go through each reaction
		for (String reacName : reactionsMap.keySet()) {

			BioChemicalReaction reac = (BioChemicalReaction) intNet
					.getEntity(reacName);
			reacIndiceMap.put(reacName, reacIndice);

			// we get the reactants
			Map<String, BioPhysicalEntityParticipant> reactants = reac
					.getLeftParticipantList();

			for (String reactantName : reactants.keySet()) {
				BioPhysicalEntityParticipant bpep = reactants.get(reactantName);
				BioPhysicalEntity reactant = bpep.getPhysicalEntity();
				// we only keep internal metabolites
				if (!reactant.getBoundaryCondition()) {
					// we set the value in the matrix
					sMatrix[reacIndice][metabIndiceMap.get(reactant.getId())] = Double
							.parseDouble(bpep.getStoichiometricCoefficient())
							* -1;
				}
				// if it is external we add it to the map for exchange reactions
				// interactions
				else {

					if (!exchangeInteractions.containsKey(reac)) {

						Map<BioEntity, Double> map = new HashMap<BioEntity, Double>();

						map.put(reactant, Double.parseDouble(bpep
								.getStoichiometricCoefficient()));

						exchangeInteractions.put(reac, map);
					} else {
						exchangeInteractions.get(reac).put(
								reactant,
								Double.parseDouble(bpep
										.getStoichiometricCoefficient()));
					}
				}

			}

			// we get the products
			Map<String, BioPhysicalEntityParticipant> products = reac
					.getRightParticipantList();

			for (String productName : products.keySet()) {

				BioPhysicalEntityParticipant bpep = products.get(productName);
				BioPhysicalEntity product = bpep.getPhysicalEntity();
				// we only keep internal metabolites
				if (!product.getBoundaryCondition()) {
					sMatrix[reacIndice][metabIndiceMap.get(product.getId())] = Double
							.parseDouble(bpep.getStoichiometricCoefficient());
				}
				// if it is external we add it to the map for exchange reactions
				// interactions
				else {

//					if (reac.isReversible()) {

						if (!exchangeInteractions.containsKey(reac)) {

							Map<BioEntity, Double> map = new HashMap<BioEntity, Double>();

							map.put(product,
									Double.parseDouble(bpep
											.getStoichiometricCoefficient())
											* -1);

							exchangeInteractions.put(reac, map);
						} else {
							exchangeInteractions.get(reac).put(
									product,
									Double.parseDouble(bpep
											.getStoichiometricCoefficient())
											* -1);
						}

//					}
				}

			}

			reacIndice += 1;
		}

		// we create the constraints
		for (String metab : metabIndiceMap.keySet()) {

			Map<BioEntity, Double> constraintMap = new HashMap<BioEntity, Double>();
			int ligne = metabIndiceMap.get(metab);
			for (String reaction : reacIndiceMap.keySet()) {
				if (sMatrix[reacIndiceMap.get(reaction)][ligne] != 0.0) {

					constraintMap.put(reactionsMap.get(reaction),
							sMatrix[reacIndiceMap.get(reaction)][ligne]);

				}
			}
			Constraint c = new Constraint(constraintMap, 0.0, 0.0);

			for (BioEntity ent : c.getEntities().keySet()) {

				if (!steadyStateConstraints.containsKey(ent.getId())) {

					List<Constraint> cList = new ArrayList<Constraint>();
					cList.add(c);

					steadyStateConstraints.put(reactionsMap.get(ent.getId()),
							cList);
				} else {

					steadyStateConstraints.get(reactionsMap.get(ent.getId()))
							.add(c);

				}
			}

			constraints.add(c);

		}

	}

	/**
	 * 
	 * Sets the bounds of reaction entities from the bioNetwork values.
	 * 
	 */
	private void setReactionsBounds() {
		Map<String, BioChemicalReaction> reactionsMap = bioNet
				.getBiochemicalReactionList();
		// we go through each reaction to create the bound constraints
		for (String reacName : reactionsMap.keySet()) {
			BioChemicalReaction reaction = reactionsMap.get(reacName);

			double lb = Double.parseDouble(reaction.getLowerBound().value);
			double ub = Double.parseDouble(reaction.getUpperBound().value);

			if (reaction.isReversible() == false && lb < 0) {
				lb = 0;
				if (Vars.verbose) {
					System.err
							.println("Warning : irreversible reaction with < 0 lower bound");
				}
			}

			Map<BioEntity, Double> constraintMap = new HashMap<BioEntity, Double>();
			constraintMap.put(reaction, 1.0);
			Constraint c = new Constraint(constraintMap, lb, ub);
			constraints.add(c);
			simpleConstraints.put(reaction, c);

		}

	}

	/**
	 * 
	 * Adds GPR interactions to the problem.
	 * 
	 */
	private void gprInteractions() {

		Map<String, BioChemicalReaction> reactionsMap = bioNet
				.getBiochemicalReactionList();

		for (String name : reactionsMap.keySet()) {

			// genes invovled in this GPR, to add the interaction to the
			// interactionsToCheck
			List<BioGene> involvedGenes = new ArrayList<BioGene>();

			// we crate the interaction : if gene = 0 then reaction = 0
			// so we transform the gpr from R = G1 AND G2
			// to : if G1=0 OR G2=0 then R=0
			// so we invert AND and OR

			Map<String, BioPhysicalEntity> enzymes = reactionsMap.get(name)
					.getEnzList();

			if (enzymes.size() != 0) {

				Relation rel1 = null;

				// if the genes are here
				Relation rel1Active = null;
				// if there are more than 1 enzyme, we create a And
				if (enzymes.size() > 1) {
					rel1 = (And) relationFactory.makeAnd();
					rel1Active = (Or) relationFactory.makeOr();

				}

				for (String enzName : enzymes.keySet()) {

					BioPhysicalEntity enzyme = enzymes.get(enzName);

					String classe = enzyme.getClass().getSimpleName();

					HashMap<String, BioGene> listOfGenes = new HashMap<String, BioGene>();

					// HashMap<String, BioProtein> listOfProteins = new
					// HashMap<String, BioProtein>();

					if (classe.compareTo("BioProtein") == 0) {
						// listOfProteins.put(enzyme.getId(),
						// (BioProtein)enzyme);

						listOfGenes = ((BioProtein) enzyme).getGeneList();

					} else if (classe.compareTo("BioComplex") == 0) {

						listOfGenes = ((BioComplex) enzyme).getGeneList();

					}

					Relation rel2 = null;

					Relation rel2Active = null;
					if (listOfGenes.size() > 1) {
						rel2 = (Or) relationFactory.makeOr();
						rel2Active = (And) relationFactory.makeAnd();
					}

					for (String geneName : listOfGenes.keySet()) {

						involvedGenes.add((BioGene) intNet.getEntity(geneName));

						Unique unique = (Unique) relationFactory.makeUnique(
								intNet.getEntity(geneName),
								operationFactory.makeEq(), 0);

						Unique uniqueActive = (Unique) relationFactory
								.makeUnique(intNet.getEntity(geneName),
										operationFactory.makeGt(), 0.0);

						if (rel2 != null) {
							((Or) rel2).addRelation(unique);
						} else {
							rel2 = unique;
						}

						if (rel2Active != null) {
							((And) rel2Active).addRelation(uniqueActive);
						} else {
							rel2Active = uniqueActive;
						}

					}

					if (rel1 != null) {
						((And) rel1).addRelation(rel2);
					} else {
						rel1 = rel2;
					}

					if (rel1Active != null) {
						((Or) rel1Active).addRelation(rel2Active);
					} else {
						rel1Active = rel2Active;
					}

				}

				// if the genes are not there, the reaction is not there

				Unique reacEqZero = (Unique) relationFactory.makeUnique(
						intNet.getEntity(name), operationFactory.makeEq(), 0.0);

				Interaction inter = relationFactory.makeIfThenInteraction(
						reacEqZero, rel1);

				intNet.addGPRIntercation(inter);
				inter.setTimeInfos(new double[] { 0.0, 0.0 });

			}

		}

	}

	/**
	 * 
	 * Transforms all problem's constraints into solver constraints.
	 * 
	 */
	protected void constraintsToSolverConstraints() {

		// the fixed constraints
		for (Constraint constraint : constraints) {
			makeSolverConstraint(constraint, null, null);
		}

	}

	public BioNetwork getBioNetwork() {
		return bioNet;
	}

	public InteractionNetwork getInteractionNetwork() {
		return intNet;
	}

	public List<Constraint> getConstraints() {
		return constraints;
	}

	/**
	 * Prints the constraints
	 */
	public void displayConstraints() {
		for (Constraint c : constraints) {
			System.err.println(c);
		}
	}

	/**
	 * Sets the objective.
	 * 
	 * @param obj
	 *            Objective to set.
	 */
	public void setObjective(Objective obj) {
		this.obj = obj;
		if (solverPrepared) {
			makeSolverObjective();
		}
	}

	public abstract void end();

	public Objective getObjective() {
		return obj;
	}

	public ThreadFactory getThreadFactory() {
		return threadFactory;
	}

	public Map<BioChemicalReaction, Map<BioEntity, Double>> getExchangeInteractions() {
		return exchangeInteractions;
	}

	public Map<BioEntity, Constraint> getSimpleConstraints() {
		return simpleConstraints;
	}

	public void setSimpleConstraints(Map<BioEntity, Constraint> c) {
		simpleConstraints = c;
	}

	public boolean isLastSolveEmpty() {
		return lastSolve.size() == 0;
	}

	public void resetLastSolve() {
		lastSolve = new HashMap<String, Double>();
	}

	/**
	 * 
	 * Return the value of the last solve for a given entity.
	 * 
	 * @param ent
	 *            The entity to get the value.
	 * @return Returns the calculated value for an entity for the last solve.
	 */

	public double getSolvedValue(BioEntity ent) {

		return lastSolve.get(ent.getId());
	}

	public Map<BioEntity, Map<Relation, double[]>> getInteractionsEntities() {
		return interactionsEntitiesConsequence;
	}

	public Map<BioEntity, Relation> getInteractionsEntitiesCause() {
		return interactionsEntitiesCause;
	}

	public void setLoadObjective(boolean load) {

		loadObjective = load;
	}

	public Collection<BioChemicalReaction> getDeadReactions() {
		return deadReactions;
	}

	public String statesFileName = "/tmp/statesFileName";

	/**
	 * 
	 * @param e
	 * @param c
	 */
	public void addSimpleConstraint(BioEntity e, Constraint c) {
		this.simpleConstraints.put(e, c);
	}

	/**
	 * Computes reduced costs and shadow prices
	 * 
	 * @param fileName
	 */
	public abstract void sensitivityAnalysis(String fileName);

	/**
	 * Copy the Bind Be careful, the entities are not duplicated
	 * 
	 * @return
	 * @throws ClassNotFoundException
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 */
	public Bind copy() throws ClassNotFoundException, NoSuchMethodException,
			SecurityException, InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException {

		String className = this.getClass().getName();

		Class<?> myClass = Class.forName(className);

		Constructor<?> ctor = myClass.getConstructor();

		Bind newBind = (Bind) ctor.newInstance();

		for (Constraint c : this.constraints) {
			Constraint newC;

			if (c.getNot() && c.getLb() == c.getUb()) {
				newC = new Constraint(c.getEntities(), c.getLb(), c.getNot());
			} else {
				newC = new Constraint(c.getEntities(), c.getLb(), c.getUb());
			}

			newBind.constraints.add(newC);

			if (c.getEntities().size() == 1) {
				newBind.simpleConstraints.put(c.getEntities().keySet()
						.iterator().next(), newC);
			}

		}

		// we copy the interaction network
		for (BioEntity ent : intNet.getNumEntities()) {
			newBind.intNet.addNumEntity(ent);
		}
		for (BioEntity ent : intNet.getIntEntities()) {
			newBind.intNet.addIntEntity(ent);
		}
		for (BioEntity ent : intNet.getBinaryEntities()) {
			newBind.intNet.addBinaryEntity(ent);
		}

		for (Interaction inter : intNet.getGPRInteractions()) {
			newBind.intNet.addGPRIntercation(inter);
		}

		for (BioEntity ent : intNet.getInitialConstraints().keySet()) {
			newBind.intNet.addInitialConstraint(ent, intNet
					.getInitialConstraints().get(ent));
			;
		}

		for (Interaction inter : intNet.getGPRInteractions()) {
			newBind.intNet.addGPRIntercation(inter);
		}

		for (Interaction inter : intNet.getInteractionToConstraints().keySet()) {
			newBind.intNet.addInteractionToConstraints(inter);
		}

		for (BioEntity ent : intNet.getTargetToInteractions().keySet()) {
			newBind.intNet.setTargetToInteractions(ent, intNet
					.getTargetToInteractions().get(ent));
		}

		newBind.setNetwork(this.bioNet);

		return newBind;

	}

}