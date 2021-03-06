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
import parsebionet.biodata.BioEntity;
import parsebionet.biodata.BioNetwork;
import parsebionet.biodata.BioPhysicalEntity;
import parsebionet.biodata.BioPhysicalEntityParticipant;
import parsebionet.io.Sbml2Bionetwork;
import flexflux.analyses.FBAAnalysis;
import flexflux.analyses.RSAAnalysis;
import flexflux.analyses.result.FBAResult;
import flexflux.analyses.result.RSAAnalysisResult;
import flexflux.input.ConstraintsFileReader;
import flexflux.input.SBMLQualReader;
import flexflux.interaction.Interaction;
import flexflux.interaction.InteractionNetwork;
import flexflux.interaction.Relation;
import flexflux.interaction.RelationFactory;
import flexflux.objective.Objective;
import flexflux.operation.OperationFactory;

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
	 * objective in the constraints file is ignored.
	 */
	private boolean loadObjective = true;

	/**
	 * Objective of the problem.
	 */
	protected Objective obj;

	/**
	 * List used when several objectives are given in the constraints file.
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
	 */

	protected void makeSolverConstraint(Constraint c,
			List<Object> toRemoveFromModel) {

		constrainedEntities.addAll(c.getEntities().keySet());

		createSolverConstraint(c, toRemoveFromModel);
		
	}

	protected abstract void createSolverConstraint(Constraint c,
			List<Object> toRemoveFromModel);

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
	 * Make null all the variables and clear the solver and end the solver
	 */
	public void clearAll() {
		this.clearSolver();
		this.end();
		this.constraintObjectives = null;
//		this.intNet.allNull();
		this.intNet = null;
		this.bioNet = null;
		this.constraints = null;
		this.deadReactions = null;
		this.simpleConstraints = null;
		this.steadyStateConstraints = null;
		this.solverSimpleConstraints = null;
		this.exchangeInteractions = null;
		this.interactionsEntitiesConsequence = null;
		this.interactionsEntitiesCause = null;
		this.constrainedEntities = null;
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
	public DoubleResult FBA(List<Constraint> constraintsToAdd, boolean saveResults, boolean checkInteractions) {

		if (solverPrepared) {

			if (checkInteractions) {
				// treatment to ensure there is no problem

				Map<BioEntity, Constraint> oldSimpleConstraints = new HashMap<BioEntity, Constraint>();
				// we add the simple constraints to be taken into account when
				// checking interactions
				Set<BioEntity> hadNoSimpleConstraint = new HashSet<BioEntity>();

				// we update the simpleConstraints for the RSA (it needs
				// simpleConstraints)
				for (Constraint constr : constraintsToAdd) {

					if (constr.getEntities().size() == 1) {
						for (BioEntity ent : constr.getEntities().keySet()) {
							if (constr.getEntities().get(ent) == 1.0) {

								/**
								 * If a constraint already exists for this
								 * entity, we keep it in oldSimpleConstraint
								 */

								if (simpleConstraints.containsKey(ent)) {
									oldSimpleConstraints.put(ent, simpleConstraints.get(ent));

									constraints.remove(simpleConstraints.get(ent));

								} else {
									hadNoSimpleConstraint.add(ent);
								}

								/**
								 * We add the new constraint in simpleConstraint
								 */
								simpleConstraints.put(ent, constr);
							}
						}
					}

				}

				if (checkInteractionNetwork) {

					List<Constraint> intNetSteadyStateConstraints = new ArrayList<Constraint>();

					RSAAnalysis ssa = new RSAAnalysis(this.getInteractionNetwork(), simpleConstraints);
					RSAAnalysisResult res = ssa.runAnalysis();

					if (Vars.writeInteractionNetworkStates) {
						res.writeToFile(statesFileName);
					}

					for (Constraint c : res.getSteadyStateConstraints()) {
						intNetSteadyStateConstraints.add(c);
					}

					// we update the simpleConstraints for the GPR (it needs
					// simpleConstraints)
					for (Constraint constr : intNetSteadyStateConstraints) {

						if (constr.getEntities().size() == 1) {
							for (BioEntity ent : constr.getEntities().keySet()) {

								if (constr.getEntities().get(ent) == 1.0) {
									/**
									 * If a constraint already exists for this
									 * entity, we keep it in oldSimpleConstraint
									 */
									if (simpleConstraints.containsKey(ent)
											&& !hadNoSimpleConstraint
													.contains(ent)) {
										oldSimpleConstraints.put(ent,
												simpleConstraints.get(ent));

									}

									/**
									 * We replace the simple constraint by the
									 * one computed by the RSA
									 */
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
						GPRConstraints.addAll(intNet
								.getInteractionToConstraints().get(i));
					}
				}

				// we go back to the original simpleConstraints
				for (Constraint constr : constraintsToAdd) {
					if (constr.getEntities().size() == 1) {
						for (BioEntity ent : constr.getEntities().keySet()) {
							if (constr.getEntities().get(ent) == 1.0) {
								/**
								 * If the constraint is a simple constraint from
								 * the beginning, it means that this constraint
								 * is hardly defined : we replace the constraint
								 * by the original one
								 */
								if (oldSimpleConstraints.containsKey(ent)) {
									simpleConstraints.put(ent,
											oldSimpleConstraints.get(ent));
									/**
									 * Else, we remove it ????
									 */
								} else {
									simpleConstraints.remove(ent);
								}

							}
						}
					}
				}

				constraintsToAdd.addAll(GPRConstraints);

			}

//			if (checkInteractions) {
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
					if (c.getEntities().size() == 1 && c.getLb() == 0 && c.getUb() == 0) {
						BioEntity b = null;
						for (BioEntity ent : c.getEntities().keySet()) {
							b = ent;
						}
						if (b.getClass().getSimpleName().equals("BioPhysicalEntity")) {

							BioPhysicalEntity metab = (BioPhysicalEntity) b;

							if (metab.getBoundaryCondition()) {
								// now we need to find the exchangReaction
								// concerned
								// and change one of its bound

								for (String reacName : metab.getReactionsAsSubstrate().keySet()) {
									BioChemicalReaction reac = metab.getReactionsAsSubstrate().get(reacName);

									//
									if (simpleConstraints.containsKey(reac)) {
										
										if (reac.getLeftList().containsKey(metab.getId())
												&& reac.getLeftList().size() == 1) {

											double lb = simpleConstraints.get(reac).getLb();
											double ub = 0.0;

											Map<BioEntity, Double> constMap = new HashMap<BioEntity, Double>();
											constMap.put(reac, 1.0);

											extMetabConstraints.add(new Constraint(constMap, lb, ub));
										}

										if (reac.getRightList().containsKey(metab.getId())
												&& reac.getRightList().size() == 1) {

											double lb = 0.0;

											double ub = simpleConstraints.get(reac).getUb();

											Map<BioEntity, Double> constMap = new HashMap<BioEntity, Double>();
											constMap.put(reac, 1.0);

											extMetabConstraints.add(new Constraint(constMap, lb, ub));
										}

									}
								}

							}
						}
					}
				}
				constraintsToAdd.addAll(extMetabConstraints);
//			}

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
	private DoubleResult goWithConstraints(List<Constraint> constraints, boolean saveResults) {

		// objects represent a constraint for the solver
		List<Object> toRemoveFromModel = new ArrayList<Object>();
		Map<String, double[]> oldBounds = new HashMap<String, double[]>();

		for(BioEntity e : this.simpleConstraints.keySet()) {
			Constraint c = this.simpleConstraints.get(e);
			
			double bounds[] = {c.getLb(), c.getUb()};
			
			oldBounds.put(e.getId(),bounds);
			
		}
		
		
		for (Constraint c : constraints) {

			makeSolverConstraint(c, toRemoveFromModel);
		}

		DoubleResult result = go(saveResults);

		// we go back to how it was
		deleteConstraints(toRemoveFromModel);

		// TODO : check, this change the results of TDFVA
//		for (String entity : oldBounds.keySet()) {
//			changeVarBounds(entity, oldBounds.get(entity));
//
//		}

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
	public Bind(List<Constraint> constraints2, Map<BioEntity, Constraint> simpleConstraints, InteractionNetwork intNet,
			BioNetwork bioNet) {

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
			setNetworkAndConstraints(parser.getBioNetwork());
	}

	/**
	 * 
	 * @param network
	 */
	public void setNetwork(BioNetwork network) {
		this.bioNet = network;
	}

	/**
	 * 
	 * Sets the problem's network and adds constraints and interactions included
	 * in it.
	 * 
	 * @param network
	 *            Network to give the problem.
	 */
	public void setNetworkAndConstraints(BioNetwork network) {

		if (network == null) {

			System.err.println("Error : could not load sbml file");
			System.exit(0);
		}

		this.bioNet = network;
		deadReactions = bioNet.trim();

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
		intNet.addNetworkEntities(bioNet);

		// creates the steady state constraints
		makeSteadyStateConstraints();

		// sets the reaction bounds
		setReactionsBounds();

		// GPR to interaction
		intNet.gprInteractions(bioNet,relationFactory,operationFactory);

	}

	/**
	 * Creates interactions from the interaction file.
	 * 
	 * @param path
	 *            Path to the interaction file.
	 */
	public void loadRegulationFile(String path) {

		intNet = SBMLQualReader.loadSbmlQual(path, intNet, relationFactory);

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
	 * Loads the objective, creates variables and constraints from the
	 * constraints file
	 * 
	 * @param path
	 *            Path to the constraints file.
	 */
	public void loadConstraintsFile(String path) {

		ConstraintsFileReader r = new ConstraintsFileReader(path, intNet, constraints, simpleConstraints,
				steadyStateConstraints, bioNet, loadObjective);

		r.readConstraintsFile();

		if (loadObjective) {

			obj = r.obj;
			constraintObjectives = r.constraintObjectives;
		}

	}

	/**
	 * 
	 * 
	 * @return The entity corresponding to the sum of the absolute value of all
	 *         the reaction fluxes.
	 */

	public BioEntity createFluxesSummation() {

		if (intNet.getEntity(Vars.FluxSumKeyWord) == null) {

			ConstraintsFileReader r = new ConstraintsFileReader("", intNet, constraints, simpleConstraints,
					steadyStateConstraints, bioNet, true);

			return r.createFluxesSummation();
		}
		return intNet.getEntity(Vars.FluxSumKeyWord);

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
	public Objective makeObjectiveFromString(String expr, boolean maximize, String name) {

		ConstraintsFileReader r = new ConstraintsFileReader("", intNet, constraints, simpleConstraints,
				steadyStateConstraints, bioNet, true);

		return r.makeObjectiveFromString(expr, maximize, name);

	}

	/**
	 * 
	 * Transforms all problem entities, constraints an interaction into solver
	 * entities, constraints an interactions. Without this step, the solver
	 * cannot solve the problem.
	 * 
	 */
	public void prepareSolver() {

		constrainedEntities.clear();
		
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
				System.err.println(Vars.libertyPercentage + "% of non optimality");
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
			double delta = Math.abs(resultFba.getObjValue()) * Vars.libertyPercentage / 100;

			Constraint c = new Constraint(constMap, lb - delta, ub + delta);

			constraints.add(c);

			makeSolverConstraint(c, null);

		}

		if (realObj != null && constraintObjectives.size() > 0) {
			this.setObjective(realObj);
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

		Map<String, BioChemicalReaction> reactionsMap = bioNet.getBiochemicalReactionList();

		Map<String, BioPhysicalEntity> metabolitesMap = bioNet.getPhysicalEntityList();

		for (String name : metabolitesMap.keySet()) {
			// we only keep internal metabolites
			if (!metabolitesMap.get(name).getBoundaryCondition()) {
				metabIndiceMap.put(name, metabIndice);
				metabIndice++;
			}

		}

		// stoe matrix
		double[][] sMatrix = new double[reactionsMap.size()][metabolitesMap.size()];

		// we go through each reaction
		for (String reacName : reactionsMap.keySet()) {

			BioChemicalReaction reac = (BioChemicalReaction) intNet.getEntity(reacName);
			reacIndiceMap.put(reacName, reacIndice);

			// we get the reactants
			Map<String, BioPhysicalEntityParticipant> reactants = reac.getLeftParticipantList();

			for (String reactantName : reactants.keySet()) {
				BioPhysicalEntityParticipant bpep = reactants.get(reactantName);
				BioPhysicalEntity reactant = bpep.getPhysicalEntity();
				// we only keep internal metabolites
				if (!reactant.getBoundaryCondition()) {
					// we set the value in the matrix
					sMatrix[reacIndice][metabIndiceMap.get(reactant.getId())] = Double
							.parseDouble(bpep.getStoichiometricCoefficient()) * -1;
				}
				// if it is external we add it to the map for exchange reactions
				// interactions
				else {

					if (!exchangeInteractions.containsKey(reac)) {

						Map<BioEntity, Double> map = new HashMap<BioEntity, Double>();

						map.put(reactant, Double.parseDouble(bpep.getStoichiometricCoefficient()));

						exchangeInteractions.put(reac, map);
					} else {
						exchangeInteractions.get(reac).put(reactant,
								Double.parseDouble(bpep.getStoichiometricCoefficient()));
					}
				}

			}

			// we get the products
			Map<String, BioPhysicalEntityParticipant> products = reac.getRightParticipantList();

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

					// if (reac.isReversible()) {

					if (!exchangeInteractions.containsKey(reac)) {

						Map<BioEntity, Double> map = new HashMap<BioEntity, Double>();

						map.put(product, Double.parseDouble(bpep.getStoichiometricCoefficient()) * -1);

						exchangeInteractions.put(reac, map);
					} else {
						exchangeInteractions.get(reac).put(product,
								Double.parseDouble(bpep.getStoichiometricCoefficient()) * -1);
					}

					// }
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

					constraintMap.put(reactionsMap.get(reaction), sMatrix[reacIndiceMap.get(reaction)][ligne]);

				}
			}
			Constraint c = new Constraint(constraintMap, 0.0, 0.0);

			for (BioEntity ent : c.getEntities().keySet()) {

				if (!steadyStateConstraints.containsKey(ent.getId())) {

					List<Constraint> cList = new ArrayList<Constraint>();
					cList.add(c);

					steadyStateConstraints.put(reactionsMap.get(ent.getId()), cList);
				} else {

					steadyStateConstraints.get(reactionsMap.get(ent.getId())).add(c);

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
		Map<String, BioChemicalReaction> reactionsMap = bioNet.getBiochemicalReactionList();
		// we go through each reaction to create the bound constraints
		for (String reacName : reactionsMap.keySet()) {
			BioChemicalReaction reaction = reactionsMap.get(reacName);

			double lb = Double.parseDouble(reaction.getLowerBound().value);
			double ub = Double.parseDouble(reaction.getUpperBound().value);

			if (reaction.isReversible() == false && lb < 0) {
				lb = 0;
				if (Vars.verbose) {
					System.err.println("Warning : irreversible reaction with < 0 lower bound");
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
	 * Transforms all problem's constraints into solver constraints.
	 * 
	 */
	protected void constraintsToSolverConstraints() {

		// the fixed constraints
		for (Constraint constraint : constraints) {
			makeSolverConstraint(constraint, null);
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

	public abstract void init();

	public abstract void end();

	public Objective getObjective() {
		return obj;
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

		if (lastSolve.containsKey(ent.getId())) {
			return lastSolve.get(ent.getId());
		}
		return Double.NaN;
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
	 * 
	 * @param interactionNetwork
	 */
	public void setInteractionNetwork(InteractionNetwork interactionNetwork) {
		this.intNet = interactionNetwork;
	}

	/**
	 * 
	 * @param constraints
	 */
	public void setConstraints(List<Constraint> constraints) {
		this.constraints = constraints;
	}

	/**
	 * Computes reduced costs and shadow prices
	 * 
	 * @param fileName
	 */
	public abstract void sensitivityAnalysis(String fileName);

	/**
	 * Create a new Bind with the good constructor
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
	public Bind createNewBind() throws ClassNotFoundException, NoSuchMethodException, SecurityException,
			InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {

		String className = this.getClass().getName();

		Class<?> myClass = Class.forName(className);

		Constructor<?> ctor = myClass.getConstructor();

		Bind newBind = (Bind) ctor.newInstance();

		return newBind;

	}

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
	public Bind lightCopy() throws ClassNotFoundException, NoSuchMethodException, SecurityException,
			InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {

		String className = this.getClass().getName();

		Class<?> myClass = Class.forName(className);

		Constructor<?> ctor = myClass.getConstructor();

		Bind newBind = (Bind) ctor.newInstance();

		newBind.getConstraints().addAll(this.getConstraints());
		newBind.getSimpleConstraints().putAll(this.getSimpleConstraints());
		newBind.setInteractionNetwork(this.getInteractionNetwork());
		newBind.setNetwork(this.getBioNetwork());
		newBind.init();

		return newBind;

	}

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
	public Bind copy() throws ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException {

		String className = this.getClass().getName();

		Class<?> myClass = Class.forName(className);

		Constructor<?> ctor = myClass.getConstructor();

		Bind newBind = (Bind) ctor.newInstance();

		if (this.getObjective() != null) {

			newBind.setObjective(new Objective(this.getObjective()
					.getEntities(), this.getObjective().getCoeffs(), this
					.getObjective().getName(), this.getObjective()
					.getMaximize()));
		} else {
			newBind.setObjective(null);
		}


		
		/**
		 * Adds the "normal" constraints
		 */
		for (Constraint c : this.constraints) {
			Constraint newC;

			if (c.getNot() && c.getLb() == c.getUb()) {
				newC = new Constraint(c.getEntities(), c.getLb(), c.getNot());
			} else {
				newC = new Constraint(c.getEntities(), c.getLb(), c.getUb());
			}

			newBind.constraints.add(newC);

			if (c.getEntities().size() == 1) {
				newBind.simpleConstraints.put(c.getEntities().keySet().iterator().next(), newC);
			}
		}
		
//		newBind.simpleConstraints = new HashMap<BioEntity, Constraint>(
//				this.simpleConstraints);
		
		/**
		 * Adds the simple constraints
		 */
		for (BioEntity b : this.simpleConstraints.keySet()) {
			Constraint c = this.simpleConstraints.get(b);

			Constraint newC = new Constraint(c.getEntities(), c.getLb(), c.getUb());

			newBind.addSimpleConstraint(b, newC);
		}



		newBind.exchangeInteractions = new HashMap<BioChemicalReaction, Map<BioEntity, Double>>();
		for (BioChemicalReaction r : this.exchangeInteractions.keySet()) {
			Map<BioEntity, Double> interactions = new HashMap<BioEntity, Double>(
					this.exchangeInteractions.get(r));
			newBind.exchangeInteractions.put(r, interactions);

		}

		newBind.steadyStateConstraints = new HashMap<BioEntity, List<Constraint>>();
		for (BioEntity e : this.steadyStateConstraints.keySet()) {
			
			List<Constraint> constraints = new ArrayList<Constraint>();
			
			for (Constraint c : this.steadyStateConstraints.get(e)) {
				Constraint newC;

				if (c.getNot() && c.getLb() == c.getUb()) {
					newC = new Constraint(c.getEntities(), c.getLb(), c.getNot());
				} else {
					newC = new Constraint(c.getEntities(), c.getLb(), c.getUb());
				}
				
				constraints.add(newC);

			}
			
			newBind.steadyStateConstraints.put(e, constraints);
			
		}

		newBind.setNetwork(this.bioNet);

		newBind.setInteractionNetwork(this.getInteractionNetwork().copy());
		
		newBind.init();

		return newBind;

	}

}
