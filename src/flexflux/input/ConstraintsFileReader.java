package flexflux.input;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import parsebionet.biodata.BioEntity;
import parsebionet.biodata.BioNetwork;
import flexflux.general.Constraint;
import flexflux.general.Vars;
import flexflux.interaction.InteractionNetwork;
import flexflux.objective.Objective;

public class ConstraintsFileReader {

	public List<Constraint> constraints = new ArrayList<Constraint>();
	public Map<BioEntity, Constraint> simpleConstraints = new ConcurrentHashMap<BioEntity, Constraint>();

	public String path;
	public InteractionNetwork intNet;

	public boolean loadObjective = false;
	public Map<BioEntity, List<Constraint>> steadyStateConstraints = new ConcurrentHashMap<BioEntity, List<Constraint>>();
	public BioNetwork bioNet = new BioNetwork();
	/**
	 * Objective of the problem.
	 */
	public Objective obj;

	/**
	 * List used when several objectives are given in the constraints file.
	 */
	public List<Objective> constraintObjectives = new ArrayList<Objective>();

	// constructor for RSA
	public ConstraintsFileReader(String path, InteractionNetwork intNet) {

		this.path = path;
		this.intNet = intNet;

	}

	public ConstraintsFileReader(String path, InteractionNetwork intNet,
			List<Constraint> constraints,
			Map<BioEntity, Constraint> simpleConstraints,
			Map<BioEntity, List<Constraint>> steadyStateConstraints,
			BioNetwork bioNet, boolean loadObjective) {

		this.path = path;
		this.intNet = intNet;
		this.constraints = constraints;
		this.simpleConstraints = simpleConstraints;
		this.steadyStateConstraints = steadyStateConstraints;
		this.bioNet = bioNet;
		this.loadObjective = loadObjective;

	}

	public void readConstraintsFile() {

		try {

			// variables for the objective
			List<String> objString = new ArrayList<String>();
			Map<String, Boolean> objStringMap = new HashMap<String, Boolean>();

			boolean isError = false;
			boolean integer = false;
			boolean binary = false;
			boolean startingPoint = false;
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
				} else if (line.startsWith("STARTING POINT")) {
					startingPoint = true;
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

				else if (!line.replaceAll("\\s", "").equals("")) {

					// when it's not the equations
					if (!equations) {

						String expr[] = line.replaceAll("\t", " ").split(" ");

						if (binary) {

							if (expr.length < 1 || expr.length > 3) {

								System.err
										.println("Warning : Error in constraints file line "
												+ nbLine
												+ " , binary misformed");
								nbLine++;
								continue;
							}
						} else {

							if (expr.length < 2 || expr.length > 3) {

								System.err
										.println("Warning : Error in constraints file line "
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
							System.err
									.println("Error in constraints file line "
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
									// .println("Warning : constraints file line "
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
									.println("Warning : Error in constraints file line "
											+ nbLine);
							nbLine++;
							continue;
						}

						if (binary) {
							if ((lb != 1 && lb != 0) || (ub != 1 && ub != 0)) {
								System.err
										.println("Warning : Error in constraints file line "
												+ nbLine
												+ " , binary bounds must be 0 or 1");
								nbLine++;
								continue;
							}
						}

						if (lb > ub) {
							System.err
									.println("Warning : Error in constraints file line "
											+ nbLine
											+ " , lower bound is higher than upper bound");
							nbLine++;
							continue;
						}

						Map<BioEntity, Double> constraintMap = new HashMap<BioEntity, Double>();
						constraintMap.put(entity, 1.0);

						Constraint c = new Constraint(constraintMap, lb, ub);

						if (!startingPoint) {
							constraints.add(c);
							simpleConstraints.put(entity, c);
						}

						else {
							intNet.addInitialConstraint(entity, c);
						}

					}
					// when the "EQUATIONS" line was passed
					else {

						String[] equation = line.replaceAll("\\s", "").split(
								"<=|>=|=|<|>");

						if (equation.length != 2) {
							System.err
									.println("Warning : Error in constraints file line "
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
				System.err.println("constraints file not conform");
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
					System.err.println("Error : in constraints file variable "
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
									.println("Error : in constraints file variable "
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
										.println("Error : in constraints file variable "
												+ parts[1] + " unknown");
								System.exit(0);
							}

						}
						// and then on the right
						catch (Exception e) {

							System.err
									.println("Error :  in constraints file, coefficient must be on the left side");
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
	private void addRightEntityType(BioEntity b, boolean integer, boolean binary) {
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
	 * Changes the type of an entity to a new one.
	 * 
	 * @param entity
	 *            Entity to change.
	 * @param integer
	 *            If it is an integer entity.
	 * @param binary
	 *            If it is a binary entity.
	 */

	private void setRightEntityType(BioEntity entity, boolean integer,
			boolean binary) {

		intNet.removeNumEntity(entity);

		if (binary) {

			intNet.addBinaryEntity(entity);

		} else if (integer) {
			intNet.addIntEntity(entity);

		}

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
									.println("Error in constraints file line, objective coefficient must be a number");

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
							.println("Error in constraints file line, objective coefficient must be a number");

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
}
