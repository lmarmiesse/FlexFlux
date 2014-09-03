package flexflux.analyses;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import parsebionet.biodata.BioEntity;
import parsebionet.biodata.BioNetwork;
import parsebionet.biodata.BioPhysicalEntity;
import parsebionet.io.Sbml2Bionetwork;
import flexflux.analyses.result.AnalysisResult;
import flexflux.analyses.result.FVAResult;
import flexflux.analyses.result.KOResult;
import flexflux.analyses.result.conditionComparison.ConditionComparisonResult;
import flexflux.condition.Condition;
import flexflux.general.Bind;
import flexflux.general.Constraint;
import flexflux.general.ConstraintType;
import flexflux.general.CplexBind;
import flexflux.general.DoubleResult;
import flexflux.general.GLPKBind;
import flexflux.general.Objective;
import flexflux.general.SimpleConstraint;
import flexflux.general.Vars;

public class ConditionComparisonAnalysis extends Analysis {

	String conditionFile = "";
	String objectiveFile = "";
	String interactionFile = "";
	String constraintFile = "";
	String reactionMetaDataFile = "";
	String geneMetaDataFile = "";
	String sbmlFile = "";
	Boolean extended = false;
	String solver = "GLPK";
	ConstraintType constraintType = null;
	Boolean flag = true;
	BioNetwork network = null;

	/**
	 * Separator for columns in metadata file
	 */
	String mdSep = ",";

	Objective obj = null;

	HashMap<String, HashMap<String, Double>> conditionConstraints;
	ArrayList<String> entities;

	public ArrayList<Condition> conditions = new ArrayList<Condition>();
	public HashMap<String, String> objectives = new HashMap<String, String>();

	public ConditionComparisonAnalysis(Bind bind, String sbmlFile,
			String interactionFile, String conditionFile,
			String constraintFile, String objectiveFile, ConstraintType type,
			Boolean extended, String solver, String reactionMetaDataFile,
			String geneMetaDataFile, String mdSep) {

		super(bind);

		this.sbmlFile = sbmlFile;
		this.extended = extended;
		this.solver = solver;
		this.conditionFile = conditionFile;
		this.interactionFile = interactionFile;
		this.objectiveFile = objectiveFile;
		this.constraintType = type;
		this.constraintFile = constraintFile;
		this.reactionMetaDataFile = reactionMetaDataFile;
		this.geneMetaDataFile = geneMetaDataFile;
		this.mdSep = mdSep;

		/**
		 * Reads the conditionFile
		 */
		Boolean flag = this.loadConditionFile();
		if (flag == false) {
			this.flag = false;
		} else {
			/**
			 * Reads the objective file
			 */
			flag = this.loadObjectiveFile();
			if (flag == false) {
				this.flag = false;
			} else {
				/**
				 * Reads the SBML file
				 */
				Sbml2Bionetwork parser = new Sbml2Bionetwork(this.sbmlFile,
						extended);
				this.network = parser.getBioNetwork();

			}
		}
	}

	/**
	 * Inits the bind
	 * 
	 * @return
	 */
	public Boolean init(String objName, Condition condition) {

		try {
			if (solver.equals("CPLEX")) {
				b = new CplexBind();
			} else if (solver.equals("GLPK")) {
				Vars.maxThread = 1;
				b = new GLPKBind();
			} else {
				System.err.println("Unknown solver name");
				return false;
			}
		} catch (UnsatisfiedLinkError e) {
			System.err
					.println("Error, the solver "
							+ solver
							+ " cannot be found. Check your solver installation and the configuration file, or choose a different solver (-sol).");
			return false;
		} catch (NoClassDefFoundError e) {
			System.err
					.println("Error, the solver "
							+ solver
							+ " cannot be found. There seems to be a problem with the .jar file of "
							+ solver + ".");
			return false;
		}

		b.setLoadObjective(false);

		Boolean integer = false;
		Boolean binary = false;

		if (constraintType.equals(ConstraintType.BINARY)) {
			integer = false;
			binary = true;
		} else if (constraintType.equals(ConstraintType.INTEGER)) {
			integer = true;
			binary = false;
		} else {
			integer = false;
			binary = false;
		}

		/**
		 * Loads the metabolic network
		 */
		b.setNetwork(this.network, this.extended);

		/**
		 * Loads the constraints applied on the metabolic network
		 */
		if (this.constraintFile != "")
			b.loadConditionsFile(this.constraintFile);

		/**
		 * Loads entities
		 */
		for (String id : entities) {

			if (b.getInteractionNetwork().getEntity(id) == null) {

				BioEntity bioEntity = new BioEntity(id, id);

				b.addRightEntityType(bioEntity, integer, binary);
			}
		}

		/**
		 * Loads interaction file
		 */
		if (this.interactionFile.compareTo("") != 0) {
			b.loadInteractionsFile(this.interactionFile);
		}

		String expr = objectives.get(objName);

		String objString = (String) expr.subSequence(expr.indexOf("(") + 1,
				expr.indexOf(")"));

		Boolean maximize = false;

		if (expr.contains("MIN(")) {
			maximize = false;
		} else if (expr.contains("MAX(")) {
			maximize = true;
		}

		obj = b.makeObjectiveFromString(objString, maximize, objName);

		b.setObjective(obj);
		b.setObjSense(obj.getMaximize());

		List<Constraint> constraints = new ArrayList<Constraint>();

		for (SimpleConstraint c : condition.constraints) {
			String id = c.entityId;
			BioEntity e = null;

			if (b.getInteractionNetwork().getEntity(id) == null) {
				e = new BioPhysicalEntity(id);
			} else {
				e = b.getInteractionNetwork().getEntity(id);
			}

			Map<BioEntity, Double> constraintMap = new HashMap<BioEntity, Double>();
			constraintMap.put(e, 1.0);

			Constraint constraint = new Constraint(constraintMap, c.lb, c.ub);

			constraints.add(constraint);

			b.addInteractionNetworkSimpleConstraint(e, constraint);
			// b.getConstraints().add(constraint);

		}

		b.prepareSolver();

		return true;

	}

	@Override
	public AnalysisResult runAnalysis() {

		ConditionComparisonResult result = new ConditionComparisonResult(
				conditions, objectives, network);

		ArrayList<String> objectiveNames = new ArrayList<String>(
				objectives.keySet());

		for (String objName : objectiveNames) {

			for (Condition condition : conditions) {

				// We reinit the bind
				this.init(objName, condition);

				/**
				 * Computes FBA
				 */
				DoubleResult objValue = b.FBA(new ArrayList<Constraint>(),
						true, true);

				result.addFbaResult(obj, condition, objValue.result);

				/**
				 * Computes FVA
				 */
				// We reinit the bind
				// It does not work if don't reinit
				this.init(objName, condition);
				FVAAnalysis fvaAnalysis = new FVAAnalysis(b, null,
						new ArrayList<Constraint>());
				FVAResult resultFva = fvaAnalysis.runAnalysis();

				result.addFvaResult(obj, condition, resultFva);

				/**
				 * Computes gene KO
				 */
				// We reinit the bind
				this.init(objName, condition);
				KOAnalysis koAnalysis = new KOAnalysis(b, 1, null);
				KOResult resultKo = koAnalysis.runAnalysis();

				result.addKoResult(obj, condition, resultKo);

				System.err.println(result.koResults.get(condition.code)
						.keySet());

				/**
				 * Computes dead genes and dispensable genes from ko genes and
				 * FVa
				 */
				result.addGeneResult(
						obj,
						condition,
						result.koResults.get(condition.code).get(obj.getName()),
						result.fvaResults.get(condition.code)
								.get(obj.getName()));

				/**
				 * Reads the metaDataFile
				 */

				if (geneMetaDataFile != "") {
					HashMap<String, HashMap<String, String>> geneMetaData = this
							.readMetaDataFile(geneMetaDataFile, false,
									this.mdSep);
					result.geneMetaData = geneMetaData;
				}

				if (reactionMetaDataFile != "") {
					HashMap<String, HashMap<String, String>> reactionMetaData = this
							.readMetaDataFile(reactionMetaDataFile, true,
									this.mdSep);
					result.reactionMetaData = reactionMetaData;
				}
				
				
				for(BioEntity e : this.b.getInteractionNetwork().getTargetToInteractions().keySet()) {
					result.interactionTargets.add(e.getId());
				}
				
			}

		}
		return result;
	}

	/**
	 * Read a file containing the description of the conditions This must a
	 * tabulated network giving the state of each actor (0,1) in each condition:
	 * 1st column : the name of the condition 2nd column : the code of the
	 * condition following columns : the state of the actors (their names are in
	 * the header)
	 * 
	 * All the constraints must belong to the same type : binary, double or
	 * integer
	 * 
	 * @return false if there is a problem while reading the file
	 */
	public Boolean loadConditionFile() {

		Boolean flag = true;

		int ncol = 0;

		BufferedReader in = null;

		try {
			in = new BufferedReader(new FileReader(this.conditionFile));

			String line;

			int nbLine = 0;

			conditionConstraints = new HashMap<String, HashMap<String, Double>>();
			entities = new ArrayList<String>();

			while ((line = in.readLine()) != null) {
				if (line.startsWith("#") || line.equals("")) {
					nbLine++;
					continue;
				}

				String[] tab = line.split("\t");

				if (nbLine == 0) {
					/**
					 * Reads the header The first two columns are for the name
					 * and the code of the condition
					 */
					if (tab.length < 3) {
						System.err
								.println("Error in the header of the condition file : the number of columns\n"
										+ "must be greater than 3. The first two columns must correspond to the name and to\n"
										+ "the code of the conditions. The following columns must correspond to the identifier\n"
										+ "of the regulation network actors");
						return false;
					}

					/**
					 * Inits the number of columns to check the number of
					 * columns of the following lines
					 */
					ncol = tab.length;

					/**
					 * Fills the entity array and adds the entities in the model
					 */
					for (int i = 2; i < ncol; i++) {
						entities.add(tab[i]);
						conditionConstraints.put(tab[i],
								new HashMap<String, Double>());
					}

				} else {
					/**
					 * Following lines
					 */
					if (tab.length != ncol) {
						System.err.println("Bad number of columns line "
								+ nbLine);
						return false;
					}

					String conditionName = tab[0];
					String conditionCode = tab[1];

					Condition condition = new Condition(conditionCode,
							conditionName);

					/**
					 * Adds the constraints in the condition
					 */
					for (int i = 2; i < tab.length; i++) {

						String entityId = entities.get(i - 2);

						String valueStr = tab[i];

						Double value = null;

						try {

							value = Double.parseDouble(valueStr);

						} catch (NumberFormatException e) {
							System.err.println("Error in condition file line "
									+ nbLine
									+ " : the state value is not a number");
							return false;
						}

						if (constraintType.equals(ConstraintType.BINARY)) {

							if (value != 1 && value != 0) {
								System.err
										.println("Error in condition file line "
												+ nbLine
												+ " : the state value is different than 0 or 1");
								return false;
							}
						}

						condition.addConstraint(entityId, value, value,
								constraintType);

					}

					conditions.add(condition);
				}
				nbLine++;
			}
		} catch (FileNotFoundException e) {
			System.err.println(conditionFile + " not found");
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			System.err.println("Error while reading " + conditionFile);
			e.printStackTrace();
		}

		finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					System.err
							.println("Error while closing the condition file");
					e.printStackTrace();
				}
			}
		}

		return flag;

	}

	/**
	 * Reads the objective file : each line corresponds to an objective First
	 * column : the name of the objective function Second column : its
	 * expression (ex : MAX(R_BIOMASS))
	 * 
	 * @return false if there is a problem while loading the file
	 */

	public Boolean loadObjectiveFile() {
		Boolean flag = true;

		BufferedReader in = null;

		try {
			in = new BufferedReader(new FileReader(this.objectiveFile));

			String line;

			int nbLine = 0;

			while ((line = in.readLine()) != null) {
				if (line.startsWith("#") || line.equals("")) {
					nbLine++;
					continue;
				}

				String tab[] = line.split("\t");

				if (tab.length != 2) {
					System.err.println("Error line " + nbLine
							+ " does not contain two columns");
					return false;
				}

				String objExpression = tab[1];

				if (!objExpression.contains("MIN(")
						&& !objExpression.contains("MAX(")) {
					System.err
							.println("Objective function badly formatted line "
									+ nbLine + " (" + objExpression + ")");
					return false;
				}

				String objName = tab[0];

				objectives.put(objName, objExpression);
			}
		} catch (FileNotFoundException e) {
			System.err.println(objectiveFile + " not found");
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			System.err.println("Error while reading " + objectiveFile);
			e.printStackTrace();
		}

		finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					System.err
							.println("Error while closing the objective file");
					e.printStackTrace();
				}
			}
		}

		return flag;

	}

	/**
	 * 
	 * @param isReaction
	 *            : if true, considers ids as reaction ids, otherwise considers
	 *            ids as gene ids
	 * @return
	 */
	public HashMap<String, HashMap<String, String>> readMetaDataFile(
			String metaDataFile, Boolean isReaction, String sep) {

		HashMap<String, HashMap<String, String>> metaData = new HashMap<String, HashMap<String, String>>();

		Set<String> ids;

		if (isReaction) {
			ids = network.getBiochemicalReactionList().keySet();
		} else {
			ids = network.getGeneList().keySet();
		}

		BufferedReader in = null;

		try {
			in = new BufferedReader(new FileReader(metaDataFile));

			String line;

			int nbLine = 0;

			int nbColumns = 0;

			ArrayList<String> columns = new ArrayList<>();

			while ((line = in.readLine()) != null) {
				if (line.startsWith("#") || line.equals("")) {
					nbLine++;
					continue;
				}

				String[] tab = line.split(this.mdSep);

				if (tab.length > 1) {

					if (nbLine == 0) {
						nbColumns = tab.length;
						// reads the header
						for (int i = 1; i < nbColumns; i++) {
							String colName = tab[i];

							columns.add(colName);

							metaData.put(colName, new HashMap<String, String>());

						}
					} else {

						if (tab.length != nbColumns) {
							System.err
									.println("[ERROR] Bad number of columns line "
											+ nbLine);
							return null;
						}

						String id = tab[0];
						
						System.err.println(ids);
						System.err.println(id);

						if (ids.contains(id)) {
							
							System.err.println("match");

							for (int i = 1; i < nbColumns; i++) {
								
								String colName = columns.get(i-1);

								String value = tab[i];
								
								
								System.err.println(id+"__"+colName+"__"+value);

								metaData.get(colName).put(id, value);

							}

						}

					}
				} else {
					System.err
							.println("[ERROR] Not enough columns in the metadata file "
									+ metaDataFile);
					return null;
				}

				nbLine++;

			}
		} catch (IOException e) {
			System.err.println("[ERROR] Error reading the metadata file "
					+ metaDataFile);
			return null;
		}

		finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					System.err
							.println("Error while closing the objective file");
					e.printStackTrace();
				}
			}
		}

		return metaData;

	}

}