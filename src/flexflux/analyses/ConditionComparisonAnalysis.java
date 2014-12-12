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
package flexflux.analyses;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import parsebionet.biodata.BioChemicalReaction;
import parsebionet.biodata.BioEntity;
import parsebionet.biodata.BioNetwork;
import parsebionet.biodata.BioPhysicalEntity;
import parsebionet.io.Sbml2Bionetwork;
import flexflux.analyses.result.KOResult;
import flexflux.analyses.result.PFBAResult;
import flexflux.analyses.result.conditionComparison.ConditionComparisonResult;
import flexflux.condition.Condition;
import flexflux.condition.ListOfConditions;
import flexflux.general.Bind;
import flexflux.general.Constraint;
import flexflux.general.ConstraintType;
import flexflux.general.CplexBind;
import flexflux.general.DoubleResult;
import flexflux.general.GLPKBind;
import flexflux.general.SimplifiedConstraint;
import flexflux.general.Vars;
import flexflux.objective.ListOfObjectives;
import flexflux.objective.Objective;

public class ConditionComparisonAnalysis extends Analysis {

	String conditionFile = "";
	String objectiveFile = "";
	String regulationFile = "";
	String constraintFile = "";
	String reactionMetaDataFile = "";
	String geneMetaDataFile = "";
	String regulatorMetaDataFile = "";

	String sbmlFile = "";
	String inchlibPath = "";

	Boolean extended = false;
	String solver = "GLPK";
	ConstraintType constraintType = null;
	Boolean flag = true;
	BioNetwork network = null;

	Boolean minFlux = false;

	HashMap<String, BioEntity> regulators;

	HashMap<String, BioChemicalReaction> deadReactions;

	/**
	 * Separator for columns in metadata file
	 */
	String mdSep = ",";

	Objective obj = null;

	HashMap<String, HashMap<String, Double>> conditionConstraints;
	ArrayList<String> entities;

	public ListOfConditions conditions = new ListOfConditions();
	public ListOfObjectives objectives;

	public Boolean launchReactionAnalysis;
	public Boolean launchGeneAnalysis;
	public Boolean launchRegulatorAnalysis;

	public ConditionComparisonAnalysis(Bind bind, String sbmlFile,
			String regulationFile, String conditionFile,
			String constraintFile, ListOfObjectives objectives,
			ConstraintType type, Boolean extended, String solver,
			String reactionMetaDataFile, String geneMetaDataFile,
			String regulatorMetaDataFile, String mdSep, String inchlibPath,
			Boolean minFlux, Boolean noReactionAnalysis,
			Boolean noGeneAnalysis, Boolean noRegulatorAnalysis,
			Double liberty, int precision) {

		super(bind);

		this.sbmlFile = sbmlFile;
		this.extended = extended;
		this.solver = solver;
		this.conditionFile = conditionFile;
		this.regulationFile = regulationFile;
		this.constraintType = type;
		this.constraintFile = constraintFile;
		this.reactionMetaDataFile = reactionMetaDataFile;
		this.geneMetaDataFile = geneMetaDataFile;
		this.regulatorMetaDataFile = regulatorMetaDataFile;
		this.mdSep = mdSep;
		this.inchlibPath = inchlibPath;
		this.minFlux = minFlux;
		this.launchGeneAnalysis = !noGeneAnalysis;
		this.launchReactionAnalysis = !noReactionAnalysis;
		this.launchRegulatorAnalysis = !noRegulatorAnalysis;

		this.objectives = objectives;

		Vars.libertyPercentage = liberty;
		Vars.decimalPrecision = precision;
		
		
		conditions = new ListOfConditions();
		
		/**
		 * Reads the conditionFile
		 */
		Boolean flag = conditions.loadConditionFile(conditionFile, constraintType);
		if (flag == false) {
			this.flag = false;
		} else {
			
			entities = conditions.entities;
			
			/**
			 * Reads the SBML file
			 */
			Sbml2Bionetwork parser = new Sbml2Bionetwork(this.sbmlFile,
					extended);
			this.network = parser.getBioNetwork();

		}
	}

	/**
	 * Inits the bind
	 * 
	 * @param minimizeFlux
	 *            : if false, considers only the main objective, if true,
	 *            considers the main objective + min(FluxSum) if the global
	 *            parameter minFlux is set to true
	 * 
	 * @return
	 */
	public Boolean init(String objName, Condition condition,
			Boolean minimizeFlux) {

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
		b.setNetworkAndConstraints(this.network);

		/**
		 * Loads the constraints applied on the metabolic network
		 */
		if (this.constraintFile != "")
			b.loadConstraintsFile(this.constraintFile);

		regulators = new HashMap<String, BioEntity>();

		/**
		 * Loads entities and fills up the regulator list
		 */
		for (String id : entities) {

			if (b.getInteractionNetwork().getEntity(id) == null) {

				BioEntity bioEntity = new BioEntity(id, id);

				b.addRightEntityType(bioEntity, integer, binary);

				// If the entity is neither a gene "GPR" or a metabolite or a
				// reaction , we put it in the regulator list
				if (!b.getBioNetwork().getGeneList().containsKey(id)
						&& !b.getBioNetwork().getBiochemicalReactionList()
								.containsKey(id)
						&& !b.getBioNetwork().getPhysicalEntityList()
								.containsKey(id)) {
					regulators.put(id, bioEntity);
				}
			}
		}

		/**
		 * Loads interaction file
		 */
		if (this.regulationFile.compareTo("") != 0) {
			b.loadRegulationFile(this.regulationFile);
		}

		/**
		 * Build objective
		 */
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

		if (minimizeFlux == true && this.minFlux == true) {
			BioEntity fluxSumEnt = b.createFluxesSummation();

			BioEntity fluxSumEntArray[] = { fluxSumEnt };
			double fluxSumCoeff[] = { 1.0 };

			Objective objMinFluxSum = new Objective(fluxSumEntArray,
					fluxSumCoeff, "fluxSum", false);

			b.setObjective(objMinFluxSum);

			b.constraintObjectives.add(obj);

		} else {
			b.setObjective(obj);
		}

		/**
		 * Build list of constraints depending on the condition
		 */

		List<Constraint> constraints = new ArrayList<Constraint>();
		for (SimplifiedConstraint c : condition.constraints.values()) {
			String id = c.entityId;
			BioEntity e = null;

			if (b.getInteractionNetwork().getEntity(id) == null) {
				e = new BioPhysicalEntity(id);
			} else {
				e = b.getInteractionNetwork().getEntity(id);
			}

			Map<BioEntity, Double> constraintMap = new HashMap<BioEntity, Double>();
			constraintMap.put(e, 1.0);

			Constraint constraint = new Constraint(constraintMap, c.getValue(), c.getValue());

			constraints.add(constraint);

			b.getInteractionNetwork().addInitialConstraint(e, constraint);
			// b.getConstraints().add(constraint);

		}

		b.prepareSolver();

		return true;

	}

	@Override
	public ConditionComparisonResult runAnalysis() {

		ConditionComparisonResult result = new ConditionComparisonResult(
				conditions, objectives, network, inchlibPath,
				launchReactionAnalysis, launchGeneAnalysis,
				launchRegulatorAnalysis);

		ArrayList<String> objectiveNames = new ArrayList<String>(
				objectives.objectives.keySet());

		for (String objName : objectiveNames) {

			for (Condition condition : conditions) {

				if (Vars.verbose) {
					System.err.println("\n**************\n" + condition.code
							+ "__" + objName);
				}

				// We reinit the bind
				this.init(objName, condition, false);

				if (result.getDeadReactions() == null) {

					deadReactions = new HashMap<String, BioChemicalReaction>();
					for (BioChemicalReaction deadReaction : b
							.getDeadReactions()) {
						deadReactions.put(deadReaction.getId(), deadReaction);
					}

					result.setDeadReactions(deadReactions);

				}

				/**
				 * Computes FBA (we don't minimize the fluxes)
				 */
				DoubleResult objValue = b.FBA(new ArrayList<Constraint>(),
						true, true);

				b.end();

				Double res = Vars.round(objValue.result);

				if (Vars.verbose) {
					System.err.println("Objective value : " + res);
				}

				result.addFbaResult(obj, condition, objValue.result);

				if (this.launchReactionAnalysis || this.launchGeneAnalysis) {

					if (Vars.verbose) {
						System.err
								.println("************\nPFBA analysis\n************");
					}

					PFBAResult resPFBA = null;
					if (!res.isNaN() && res != 0) {
						this.init(objName, condition, false);
						PFBAAnalysis a = new PFBAAnalysis(b, launchGeneAnalysis);
						a.addDeadReactions(deadReactions);
						resPFBA = a.runAnalysis();

						b.end();
					}

					result.addPFBAResult(obj, condition, resPFBA);
				}

				if (this.launchRegulatorAnalysis) {

					if (Vars.verbose) {
						System.err
								.println("************\nRegulator KO analysis\n************");
					}

					result.regulators = regulators;

					KOResult koResult = null;

					if (!res.isNaN() && res != 0) {
						this.init(objName, condition, false);
						KOAnalysis koAnalysis = new KOAnalysis(b, 1, regulators);
						koResult = koAnalysis.runAnalysis();

						b.end();
					}

					result.addKoResult(obj, condition, koResult);

				}
			}
		}

		/**
		 * Reads the metaDataFiles
		 */

		if (Vars.verbose) {
			System.err.println("************\nMetaData reading\n************");
		}

		if (geneMetaDataFile != "" && launchGeneAnalysis) {
			HashMap<String, HashMap<String, String>> geneMetaData = this
					.readMetaDataFile(geneMetaDataFile, false, this.mdSep);
			result.geneMetaData = geneMetaData;
		}

		if (reactionMetaDataFile != "" && launchReactionAnalysis) {
			HashMap<String, HashMap<String, String>> reactionMetaData = this
					.readMetaDataFile(reactionMetaDataFile, true, this.mdSep);
			result.reactionMetaData = reactionMetaData;
		}

		if (regulatorMetaDataFile != "" && launchRegulatorAnalysis) {
			HashMap<String, HashMap<String, String>> regulatorMetaData = this
					.readMetaDataFile(regulatorMetaDataFile, true, this.mdSep);
			result.regulatorMetaData = regulatorMetaData;
		}

		for (BioEntity e : this.b.getInteractionNetwork()
				.getTargetToInteractions().keySet()) {
			result.interactionTargets.add(e.getId());
		}

		return result;
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

						if (ids.contains(id)) {

							for (int i = 1; i < nbColumns; i++) {

								String colName = columns.get(i - 1);

								String value = tab[i];

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