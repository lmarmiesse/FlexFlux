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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

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
import flexflux.analyses.result.FBAResult;
import flexflux.interaction.And;
import flexflux.interaction.Interaction;
import flexflux.interaction.InteractionNetwork;
import flexflux.interaction.InversedRelation;
import flexflux.interaction.Or;
import flexflux.interaction.Relation;
import flexflux.interaction.RelationFactory;
import flexflux.interaction.RelationWithList;
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

	private Map<BioEntity, Double> defaultValues = new HashMap<BioEntity, Double>();

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
	 * Links an interactions to a list of constraints to check if the
	 * interaction is true.
	 */
	protected Map<Interaction, List<Constraint>> intToConstraint = new HashMap<Interaction, List<Constraint>>();

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
	protected Map<BioEntity, Constraint> interactionNetworkSimpleConstraints = new ConcurrentHashMap<BioEntity, Constraint>();

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
	private boolean solverPrepared = false;

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
	public List<BioEntity> constrainedEntities = new ArrayList<BioEntity>();

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

				Map<BioEntity, Constraint> oldSimpleConstraint = new HashMap<BioEntity, Constraint>();
				// we add the simple constraints to be taken into account when
				// checking interactions
				Set<BioEntity> hadNoSimpleConstraint = new HashSet<BioEntity>();

				for (Constraint constr : constraintsToAdd) {

					if (constr.getEntities().size() == 1) {
						for (BioEntity ent : constr.getEntities().keySet()) {
							if (constr.getEntities().get(ent) == 1.0) {
								if (simpleConstraints.containsKey(ent)) {
									oldSimpleConstraint.put(ent,
											simpleConstraints.get(ent));
								} else {
									hadNoSimpleConstraint.add(ent);
								}
								simpleConstraints.put(ent, constr);

							}
						}
					}

				}

				List<Constraint> intNetSteadyStateConstraints = new ArrayList<Constraint>();
				for (Constraint c : findInteractionNetworkSteadyState()) {
					// c.setOverWritesBounds(false);
					intNetSteadyStateConstraints.add(c);
				}

				for (Constraint constr : intNetSteadyStateConstraints) {

					if (constr.getEntities().size() == 1) {
						for (BioEntity ent : constr.getEntities().keySet()) {
							if (constr.getEntities().get(ent) == 1.0) {
								if (simpleConstraints.containsKey(ent)
										&& !hadNoSimpleConstraint.contains(ent)) {
									oldSimpleConstraint.put(ent,
											simpleConstraints.get(ent));

								}
								simpleConstraints.put(ent, constr);

							}
						}
					}

				}

				List<Constraint> GPRConstraints = new ArrayList<Constraint>();
				for (Interaction i : intNet.getGPRInteractions()) {
					if (i.getCondition().isTrue(simpleConstraints)) {
						GPRConstraints.addAll(intToConstraint.get(i));
					}
				}

				//
				constraintsToAdd.addAll(intNetSteadyStateConstraints);
				constraintsToAdd.addAll(GPRConstraints);

				for (Constraint constr : constraintsToAdd) {
					if (constr.getEntities().size() == 1) {
						for (BioEntity ent : constr.getEntities().keySet()) {
							if (constr.getEntities().get(ent) == 1.0) {

								if (oldSimpleConstraint.containsKey(ent)) {
									simpleConstraints.put(ent,
											oldSimpleConstraint.get(ent));
								} else {
									simpleConstraints.remove(ent);
								}

							}
						}
					}
				}

			}

			// /////////////////
			// /////////////////
			// if a constraint is an external metab set to 0, we change the
			// bound of the exchange reaction
			List<Constraint> extMetabConstraints = new ArrayList<Constraint>();

			List<Constraint> constraintsToTest = new ArrayList<Constraint>();
			constraintsToTest.addAll(constraintsToAdd);
			constraintsToTest.addAll(constraints);
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
								double lb = simpleConstraints.get(reac).getLb();
								double ub = 0.0;

								Map<BioEntity, Double> constMap = new HashMap<BioEntity, Double>();
								constMap.put(reac, 1.0);

								extMetabConstraints.add(new Constraint(
										constMap, lb, ub));
							}
							for (String reacName : metab
									.getReactionsAsProduct().keySet()) {
								BioChemicalReaction reac = metab
										.getReactionsAsProduct().get(reacName);
								double lb = 0.0;
								double ub = simpleConstraints.get(reac).getUb();

								Map<BioEntity, Double> constMap = new HashMap<BioEntity, Double>();
								constMap.put(reac, 1.0);

								extMetabConstraints.add(new Constraint(
										constMap, lb, ub));
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
			InteractionNetwork intNet, BioNetwork bioNet,
			Map<BioEntity, Constraint> interactionNetworkSimpleConstraints) {

		this.constraints.addAll(constraints2);
		this.simpleConstraints.putAll(simpleConstraints);
		this.interactionNetworkSimpleConstraints
				.putAll(interactionNetworkSimpleConstraints);

		// we copy the interaction network
		for (BioEntity ent : intNet.getNumEntities()) {
			this.intNet.addNumEntity(ent);
		}
		for (BioEntity ent : intNet.getIntEntities()) {
			this.intNet.addIntEntity(ent);
		}
		for (BioEntity ent : intNet.getBinaryEntities()) {
			this.intNet.addBinaryEntity(ent);
		}
		for (Interaction inter : intNet.getAddedInteractions()) {
			this.intNet.addAddedIntercation(inter);
		}
		for (Interaction inter : intNet.getGPRInteractions()) {
			this.intNet.addGPRIntercation(inter);
		}

		for (BioEntity target : intNet.getTargetToInteractions().keySet()) {
			this.intNet.addTargetInteractions(target, intNet
					.getTargetToInteractions().get(target)[0], intNet
					.getTargetToInteractions().get(target)[1]);
		}

		// this.intNet = intNet;

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
		setNetwork(parser.getBioNetwork(), ext);
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
	public void setNetwork(BioNetwork network, boolean ext) {

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
		gprInteractions(ext);

	}

	/**
	 * Creates interactions from the interaction file.
	 * 
	 * @param path
	 *            Path to the interaction file.
	 */
	public void loadInteractionsFile(String path) {

		try {
			BufferedReader in = new BufferedReader(new FileReader(path));

			String line;
			int nbLine = 1;
			while ((line = in.readLine()) != null) {
				if (line.startsWith("#") || line.equals("")) {
					nbLine++;
					continue;
				}

				Relation ifRelation = null;
				Unique thenRelation = null;
				Unique elseRelation = null;

				double thenBegins = 0.0, thenLasts = 0.0, elseBegins = 0.0, elseLasts = 0.0;

				Pattern pattern = Pattern
						.compile("IF\\[([^\\]]*)\\]THEN\\[(.*)\\]ELSE\\[(.*)\\]");

				Matcher matcher = pattern.matcher(line);

				if (matcher.find()) {

					if (matcher.groupCount() != 3) {
						System.err.println(line);
						System.err.println("Error in interaction file line "
								+ nbLine + ", interaction not conform");

						System.exit(0);
					}

					String[] ifPart = matcher.group(1).split("\\]\\[");
					ifRelation = makeRelationFromString(ifPart[0], nbLine);

					String[] thenPart = matcher.group(2).split("\\]\\[");
					thenRelation = makeUniqueFromCondition(thenPart[0], nbLine);

					if (thenPart.length > 1) {
						thenBegins = Double.parseDouble(thenPart[1]);
					}
					if (thenPart.length > 2) {
						thenLasts = Double.parseDouble(thenPart[2]);
					}

					String[] elsePart = matcher.group(3).split("\\]\\[");
					elseRelation = makeUniqueFromCondition(elsePart[0], nbLine);

					if (elsePart.length > 1) {
						elseBegins = Double.parseDouble(elsePart[1]);
					}
					if (elsePart.length > 2) {
						elseLasts = Double.parseDouble(elsePart[2]);
					}

					// ///ERROR HANDLING
					String thenEntity = thenPart[0].replaceAll("\\s", "")
							.split("<=|>=|=|<|>|\\*")[0];

					String elseEntity = elsePart[0].replaceAll("\\s", "")
							.split("<=|>=|=|<|>|\\*")[0];

					if (!thenEntity.equals(elseEntity)) {
						System.err
								.println("Error in interaction file line "
										+ nbLine
										+ ", not the same entity in the THEN and the ELSE part");
						System.exit(0);
					}
					// ///

					// /////////// we create and add the interactions
					Interaction thenInteraction = relationFactory
							.makeIfThenInteraction(thenRelation, ifRelation);

					intNet.addAddedIntercation(thenInteraction);
					thenInteraction.setTimeInfos(new double[] { thenBegins,
							thenLasts });

					Relation ifInversedRelation = new InversedRelation(
							ifRelation);

					// the else interaction
					Interaction elseInteraction = relationFactory
							.makeIfThenInteraction(elseRelation,
									ifInversedRelation);

					intNet.addAddedIntercation(elseInteraction);
					elseInteraction.setTimeInfos(new double[] { elseBegins,
							elseLasts });

					intNet.addTargetInteractions(elseRelation.getEntity(),
							thenInteraction, elseInteraction);
					// /////////

				}

				else if (line.replace("\t", " ").split(" ").length == 2) {

					String[] splitLine = line.replace("\t", " ").split(" ");

					// if the entity does not exist yet
					if (intNet.getEntity(splitLine[0]) == null) {

						System.err
								.println("Error : unknown variable in interaction file : "
										+ splitLine[0] + " line " + nbLine);
						System.exit(0);
					}

					BioEntity ent = intNet.getEntity(splitLine[0]);
					double initValue = 0.0;
					try {
						initValue = Double.parseDouble(splitLine[1]);
					} catch (Exception e) {

						System.err.println("Error in interaction file line "
								+ nbLine + " init value must be a number");
						System.exit(0);
					}

					Map<BioEntity, Double> constMap = new HashMap<BioEntity, Double>();
					constMap.put(ent, 1.0);
					interactionNetworkSimpleConstraints.put(ent,
							new Constraint(constMap, initValue, initValue));

				}

				else {
					System.err.println(line);
					System.err.println("Error in interaction file line "
							+ nbLine + ", interaction not conform");

					System.exit(0);
				}

				nbLine++;
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
								System.err
										.println("Warning : new entity created : "
												+ expr[0]);
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
									System.err
											.println("Warning : condition file line "
													+ nbLine
													+ " : this constraint removes an existing one");
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

		entitiesToSolverVars();

		constraintsToSolverConstraints();

		if (obj != null) {
			makeSolverObjective();
		}

		solverPrepared = true;

		// we associate each interaction to the constraint(s) they could provoke
		for (Interaction inter : intNet.getAddedInteractions()) {

			intToConstraint.put(inter, inter.getConsequence()
					.createConstraints());

		}
		for (Interaction inter : intNet.getGPRInteractions()) {

			intToConstraint.put(inter, inter.getConsequence()
					.createConstraints());

		}

		Objective realObj = obj;

		// if there are constraint objective, we treat them
		if (constraintObjectives.size() != 0) {
			System.err.println(Vars.libertyPercentage + "% of non optimality");
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
			System.err.println("New constraint : \n" + c);

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

					if (reac.isReversible()) {

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

					}
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
				System.err
						.println("Warning : irreversible reaction with < 0 lower bound");
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
	 * @param ext
	 *            true : uses extended SBML format.
	 */
	private void gprInteractions(boolean ext) {

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
			// System.err.println(name);

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

						// HashMap <String, BioPhysicalEntity> componentList =
						// ((BioComplex)enzyme).getAllComponentList();

						// for(Iterator<String> iterComponent =
						// componentList.keySet().iterator();
						// iterComponent.hasNext(); ) {
						//
						// BioPhysicalEntity component =
						// componentList.get(iterComponent.next());
						//
						// if(component.getClass().getSimpleName().compareTo("BioProtein")
						// == 0) {
						// listOfProteins.put(component.getId(),
						// (BioProtein)component);
						// }
						//
						// }
					}
					// for (String protName : listOfProteins.keySet()){
					// System.err.println("prot : " + protName);
					// }

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

				// intNet.addTargetInteractions(intNet.getEntity(name), inter,
				// relationFactory.makeIfThenInteraction(
				// new UndeterminedUnique(intNet.getEntity(name)),
				// new InversedRelation(rel1)));

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
		System.err.println(constraints.size() + " constraints");
		for (Constraint c : constraints) {
			System.err.println(c);
		}
	}

	/**
	 * Creates a Relation from a string. This is used when parsing the
	 * interaction file.
	 * 
	 * @param line
	 *            String to create the Relation from.
	 * @param nbLine
	 *            Line number of the interaction file.
	 * @return The created Relation.
	 */
	private Relation makeRelationFromString(String line, int nbLine) {

		String ifExpr = line;

		String[] conditions = ifExpr.split(" AND | OR ");

		Relation rel = null;
		// no Parentheses
		if (!line.contains("(")) {

			if (line.contains(" AND ") && line.contains(" OR ")) {

				System.err.println("Error in interaction file line " + nbLine
						+ ", AND and OR must be separated by parentheses");
				return null;
			}

			if (conditions.length > 1) {

				if (line.contains(" AND ")) {
					rel = (And) relationFactory.makeAnd();
				} else if (line.contains(" OR ")) {
					rel = (Or) relationFactory.makeOr();
				} else {
					System.err.println("Error in interaction file line "
							+ nbLine + ", AND/OR not found");
					return null;
				}
				for (String s : conditions) {

					((RelationWithList) rel)
							.addRelation(makeUniqueFromCondition(s, nbLine));
				}

			} else {

				rel = makeUniqueFromCondition(conditions[0], nbLine);

			}
		}
		// if there are parentheses
		else {
			// list of all the expressions with parentheses (first level)
			List<String> ParenthesesExpressions = new ArrayList<String>();

			int indexStartExpr = 0;
			int indexEndExpr = 0;
			// gives the level of parentheses
			int level = 0;
			boolean inExpr = false;

			String copy = "";

			for (int i = 0; i < ifExpr.length(); i++) {

				if (ifExpr.charAt(i) == ')') {
					level--;
					if (level == -1) {
						System.err.println("Error in interaction file line "
								+ nbLine + ", parentheses error");
						return null;
					}

					if (inExpr && level == 0) {
						indexEndExpr = i;
						inExpr = false;
						ParenthesesExpressions.add(ifExpr.substring(
								indexStartExpr + 1, indexEndExpr));
					}

				}
				if (level == 0) {
					copy += ifExpr.charAt(i);
				}
				if (ifExpr.charAt(i) == '(') {
					level++;

					if (!inExpr && level == 1) {
						inExpr = true;
						indexStartExpr = i;
					}
				}
			}

			// if there as not as many ( and )
			if (level != 0
					|| StringUtils.countMatches(copy, "()") != ParenthesesExpressions
							.size()) {
				System.err.println("Error in interaction file line " + nbLine
						+ ", parentheses error");
				return null;
			}

			if (copy.contains(" AND ") && copy.contains(" OR ")) {

				System.err.println("Error in interaction file line " + nbLine
						+ ", AND and OR must be separated by parentheses");
				return null;
			}

			String[] OtherExpressions = copy.split(" AND | OR ");

			for (String expr : OtherExpressions) {
				if (!expr.replaceAll("\\s", "").equals("()")) {
					ParenthesesExpressions.add(expr);
				}
			}

			if (ParenthesesExpressions.size() == 1) {

				return makeRelationFromString(ParenthesesExpressions.get(0),
						nbLine);
			} else {

				if (copy.contains(" AND ")) {
					rel = (And) relationFactory.makeAnd();
				} else if (copy.contains(" OR ")) {
					rel = (Or) relationFactory.makeOr();
				} else {
					System.err.println("Error in interaction file line "
							+ nbLine + ", AND/OR not found");
					return null;
				}

				for (String expr : ParenthesesExpressions) {
					Relation r = makeRelationFromString(expr, nbLine);

					if (r != null) {
						((RelationWithList) rel)
								.addRelation(makeRelationFromString(expr,
										nbLine));
					} else {
						return null;
					}
				}

			}

		}

		return rel;
	}

	/**
	 * Creates a Unique from a string. Used when parsing the interaction file.
	 * 
	 * @param condition
	 *            The string to create the Unique from.
	 * @param nbLine
	 *            Line number of the interaction file.
	 * @return The created Unique.
	 * 
	 *         TODO : Manque de commentaires : a quoi ca sert ????
	 * 
	 */
	private Unique makeUniqueFromCondition(String condition, int nbLine) {

		String[] splitedCondition = condition.replaceAll("\\s", "").split(
				"<=|>=|=|<|>|\\*");

		String name = splitedCondition[0];

		// if the entity does not exist yet
		if (intNet.getEntity(name) == null) {

			System.err
					.println("Error : unknown variable in interaction file : "
							+ name + " line " + nbLine);
			System.exit(0);

		}

		if (splitedCondition.length == 1) {

			return (Unique) relationFactory.makeUnique(intNet.getEntity(name),
					operationFactory.makeNotEq(), 0);

		} else {
			Double value = 0.0;

			try {
				value = Double.parseDouble(splitedCondition[1]);
			} catch (NumberFormatException e) {

				System.err.println("Error in interaction file line " + nbLine);
				System.exit(0);

			}

			return (Unique) relationFactory.makeUnique(intNet.getEntity(name),
					operationFactory.makeOperationFromString(condition), value);

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

	public Map<Interaction, List<Constraint>> getIntToConstraint() {
		return intToConstraint;
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

	public Map<BioEntity, Constraint> getInteractionNetworkSimpleConstraints() {
		return interactionNetworkSimpleConstraints;
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

	public String statesFileName = "";

	/**
	 * 
	 * Find a steady state in the interaction network and return the constraints
	 * corresponding to this steady state
	 */

	public List<Constraint> findInteractionNetworkSteadyState() {

		Set<Interaction> toCheck = new HashSet<Interaction>();
		toCheck.addAll(intNet.getAddedInteractions());
		if (intNet.getTargetToInteractions().isEmpty()
				&& interactionNetworkSimpleConstraints.isEmpty()) {
			return new ArrayList<Constraint>();
		}

		// copy simpleConstraints
		Map<BioEntity, Constraint> thisStepSimpleConstraints = new HashMap<BioEntity, Constraint>();

		for (BioEntity b : simpleConstraints.keySet()) {

			thisStepSimpleConstraints.put(b, simpleConstraints.get(b));

			// if the entity is already set by a constraint, we remove te
			// interactions
			// that have this entity as a target
			if (simpleConstraints.get(b).getLb() == simpleConstraints.get(b)
					.getUb()) {

				if (intNet.getTargetToInteractions().containsKey(b)) {
					for (Interaction i : intNet.getTargetToInteractions()
							.get(b)) {
						toCheck.remove(i);
					}
				}
			}
		}
		//
		for (BioEntity b : interactionNetworkSimpleConstraints.keySet()) {

			if (!thisStepSimpleConstraints.containsKey(b)) {
				thisStepSimpleConstraints.put(b,
						interactionNetworkSimpleConstraints.get(b));
			}
			//if this entity had a simple constraint, but not fix (ub!=lb) we overwrite it
			else{
				if(simpleConstraints.get(b).getLb() != simpleConstraints.get(b)
						.getUb()){
					thisStepSimpleConstraints.put(b,
							interactionNetworkSimpleConstraints.get(b));
				}
			}
		}

		List<Map<BioEntity, Constraint>> allIterationsSimpleConstraints = new ArrayList<Map<BioEntity, Constraint>>();
		List<Map<BioEntity, Constraint>> attractorSimpleConstraints = new ArrayList<Map<BioEntity, Constraint>>();

		int attractorSize = 0;

		// ////////////////////////////////////////WRITE TO FILE

		Map<BioEntity, List<String>> toWrite = new HashMap<BioEntity, List<String>>();
		if (Vars.writeInteractionNetworkStates) {

			for (BioEntity ent : simpleConstraints.keySet()) {
				toWrite.put(ent, new ArrayList<String>());
			}

			for (BioEntity ent : interactionNetworkSimpleConstraints.keySet()) {

				if (!toWrite.containsKey(ent)) {
					toWrite.put(ent, new ArrayList<String>());

				}
			}

			for (BioEntity ent : intNet.getTargetToInteractions().keySet()) {

				if (!toWrite.containsKey(ent)) {
					toWrite.put(ent, new ArrayList<String>());

				}
			}
		}

		// ////////////////////////////////////////

		for (int it = 1; it < Vars.steadyStatesIterations; it++) {

			// ////////////////////////////////////////WRITE TO FILE
			if (Vars.writeInteractionNetworkStates) {
				for (BioEntity ent : toWrite.keySet()) {

					if (thisStepSimpleConstraints.get(ent) != null) {
						double lb = thisStepSimpleConstraints.get(ent).getLb();
						double ub = thisStepSimpleConstraints.get(ent).getUb();

						if (lb == ub) {

							toWrite.get(ent).add(String.valueOf(lb));
						} else {

							toWrite.get(ent).add(
									String.valueOf(lb) + ";"
											+ String.valueOf(ub));
						}

					} else {
						toWrite.get(ent).add("?");
					}
				}

			}

			if (thisStepSimpleConstraints.size() == 0) {
				System.out
						.println("Warning : all values of the interaction network are undetermined.");
				break;
			}

			// ////////////////////////////////////////

			// /////We check that this step has not already been achieved

			boolean areTheSame = false;
			for (Map<BioEntity, Constraint> previousStep : allIterationsSimpleConstraints) {
				areTheSame = true;
				// compare "thisStepSimpleConstraints" with "previousStep"
				// They have to be exactly the same

				// the same size
				if (thisStepSimpleConstraints.size() == previousStep.size()) {

					for (BioEntity b : thisStepSimpleConstraints.keySet()) {
						if (previousStep.containsKey(b)) {
							Constraint c1 = thisStepSimpleConstraints.get(b);
							Constraint c2 = previousStep.get(b);

							if (c1.getLb() != c2.getLb()
									|| c1.getUb() != c2.getUb()) {
								areTheSame = false;
							}
						} else {
							areTheSame = false;
						}
					}
				} else {
					areTheSame = false;
				}

				if (areTheSame) {
					attractorSize = it
							- allIterationsSimpleConstraints
									.indexOf(previousStep) - 1;

					for (int index = allIterationsSimpleConstraints
							.indexOf(previousStep); index < it - 1; index++) {
						attractorSimpleConstraints
								.add(allIterationsSimpleConstraints.get(index));
					}

					break;
				}

			}

			Set<BioEntity> setEntities = new HashSet<BioEntity>();
			Set<BioEntity> checkedEntities = new HashSet<BioEntity>();

			if (areTheSame) {
				System.err.println("Steady state found in " + (it - 1)
						+ " iterations.");
				System.err.println("Attractor size : " + attractorSize);
				break;
			}

			// /////
			allIterationsSimpleConstraints.add(thisStepSimpleConstraints);

			// we copy the previous state
			Map<BioEntity, Constraint> nextStepSimpleConstraints = new HashMap<BioEntity, Constraint>();
			for (BioEntity b : thisStepSimpleConstraints.keySet()) {
				nextStepSimpleConstraints.put(b,
						thisStepSimpleConstraints.get(b));
			}

			for (Interaction i : toCheck) {
				if (i.getCondition().isTrue(thisStepSimpleConstraints)) {

					// System.out.println(i);
					// we go through all the consequences (there should be only
					// one)
					if (intToConstraint.containsKey(i)) {
						for (Constraint consequence : this.intToConstraint
								.get(i)) {

							// we check it's a simple constraint
							if (consequence.getEntities().size() == 1) {
								for (BioEntity ent : consequence.getEntities()
										.keySet()) {

									if (consequence.getEntities().get(ent) == 1.0) {

										nextStepSimpleConstraints.put(ent,
												consequence);
										setEntities.add(ent);
										checkedEntities.add(ent);

									}
								}
							}
						}
					}
				}
			}

			// if it was not set, we put it's default value
			List<BioEntity> toRemove = new ArrayList<BioEntity>();
			for (BioEntity ent : nextStepSimpleConstraints.keySet()) {

				if (!setEntities.contains(ent)) {

					if (defaultValues.containsKey(ent)) {

						Map<BioEntity, Double> constMap = new HashMap<BioEntity, Double>();
						constMap.put(ent, 1.0);

						nextStepSimpleConstraints.put(
								ent,
								new Constraint(constMap,
										defaultValues.get(ent), defaultValues
												.get(ent)));
						checkedEntities.add(ent);
					} else {
						// we say it is undetermined only if it is a target
						if (intNet.getTargetToInteractions().containsKey(ent)) {

							// we remove only if one of the interaction is in
							// "toCheck"
							boolean hasInteractionToCheck = false;
							for (Interaction i : intNet
									.getTargetToInteractions().get(ent)) {
								if (toCheck.contains(i)) {
									hasInteractionToCheck = true;
									break;
								}
							}

							if (hasInteractionToCheck) {
								toRemove.add(ent);
							}
						}
					}
				}
			}
			for (BioEntity b : toRemove) {
				nextStepSimpleConstraints.remove(b);
			}

			thisStepSimpleConstraints = nextStepSimpleConstraints;

		}

		List<Constraint> steadyStateConstraints = new ArrayList<Constraint>();

		if (attractorSimpleConstraints.size() != 0) {

			for (BioEntity b : attractorSimpleConstraints.get(0).keySet()) {

				// If it is an external metab, we set a constraint
				boolean isExtMetab = false;

				if (b.getClass().getSimpleName().equals("BioPhysicalEntity")) {
					BioPhysicalEntity metab = (BioPhysicalEntity) b;
					// If it is external
					if (metab.getBoundaryCondition()) {
						isExtMetab = true;
					}
				}

				if (intNet.getTargetToInteractions().containsKey(b)
						|| isExtMetab) {

					System.out.println(b.getId());

					// We make the average of the values of all states of the
					// attractor
					double lb = 0;
					double ub = 0;
					for (int nb = 0; nb < attractorSimpleConstraints.size(); nb++) {
						lb += attractorSimpleConstraints.get(nb).get(b).getLb();
						ub += attractorSimpleConstraints.get(nb).get(b).getUb();
					}

					lb = lb / attractorSimpleConstraints.size();
					ub = ub / attractorSimpleConstraints.size();

					Map<BioEntity, Double> constMap = new HashMap<BioEntity, Double>();
					constMap.put(b, 1.0);
					steadyStateConstraints
							.add(new Constraint(constMap, lb, ub));
				}

				else {

				}
			}
		}

		if (Vars.writeInteractionNetworkStates) {

			PrintWriter out = null;

			try {
				out = new PrintWriter(new File(statesFileName));
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();

			}

			for (BioEntity ent : toWrite.keySet()) {

				out.print(ent.getId());

				for (String s : toWrite.get(ent)) {
					out.print("\t");
					out.print(s);
				}
				out.print("\n");

			}

			out.close();
		}

		return steadyStateConstraints;
	}

	public Map<Constraint, double[]> goToNextInteractionNetworkState(
			Map<BioEntity, Constraint> networkState) {

		Set<Interaction> toCheck = new HashSet<Interaction>();
		toCheck.addAll(intNet.getAddedInteractions());

		Map<Constraint, double[]> contToTimeInfos = new HashMap<Constraint, double[]>();

		Map<BioEntity, Constraint> nextStepState = new HashMap<BioEntity, Constraint>();
		// for (BioEntity b : networkState.keySet()) {
		// contToTimeInfos.put(networkState.get(b), new double[] { 0.0,
		// 0.0 });
		// nextStepState.put(b,networkState.get(b));
		// }

		// System.out.println(networkState.get(intNet.getEntity("M_3ohpame_b")));

		Set<BioEntity> setEntities = new HashSet<BioEntity>();

		for (Interaction i : toCheck) {

			if (i.getCondition().isTrue(networkState)) {

				// System.out.println(i);

				// we go through all the consequences (there should be only
				// one)
				if (intToConstraint.containsKey(i)) {
					for (Constraint consequence : this.intToConstraint.get(i)) {

						// we check it's a simple constraint
						if (consequence.getEntities().size() == 1) {
							for (BioEntity ent : consequence.getEntities()
									.keySet()) {
								if (consequence.getEntities().get(ent) == 1.0) {

									// System.out.println(i.getCondition()
									// + " donc " + consequence);

									contToTimeInfos.put(consequence,
											i.getTimeInfos());
									nextStepState.put(ent, consequence);

									setEntities.add(ent);
								}
							}
						}
					}
				}
			}
			// // if its not true, we remove the constraint associated if it was
			// not set in this step
			// else {
			// if (intToConstraint.containsKey(i)) {
			// for (Constraint consequence : this.intToConstraint.get(i)) {
			// // we check it's a simple constraint
			// if (consequence.getEntities().size() == 1) {
			// for (BioEntity ent : consequence.getEntities()
			// .keySet()) {
			// if (consequence.getEntities().get(ent) == 1.0) {
			// if (!setEntities.contains(ent)) {
			// nextStepState.remove(ent);
			// }
			// }
			// }
			// }
			// }
			// }
			// }
		}

		Map<Constraint, double[]> steadyStateConstraints = new HashMap<Constraint, double[]>();

		for (BioEntity ent : nextStepState.keySet()) {

			if (intNet.getTargetToInteractions().containsKey(ent)) {
				// System.out.println("oui "+ent.getId());
				steadyStateConstraints.put(nextStepState.get(ent),
						contToTimeInfos.get(nextStepState.get(ent)));
			} else {
				// System.out.println("non "+ent.getId());
			}

		}

		for (BioEntity ent : setEntities) {
			networkState.put(ent, nextStepState.get(ent));
		}

		return steadyStateConstraints;
	}

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
	 * @param e
	 * @param c
	 */
	public void addInteractionNetworkSimpleConstraint(BioEntity e, Constraint c) {
		this.interactionNetworkSimpleConstraints.put(e, c);
	}

	/**
	 * Computes reduced costs and shadow prices
	 * 
	 * @param fileName
	 */
	public abstract void sensitivityAnalysis(String fileName);

}
