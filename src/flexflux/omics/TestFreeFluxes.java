package flexflux.omics;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.kohsuke.args4j.Option;

import flexflux.applications.FFApplication;
import flexflux.condition.Condition;
import flexflux.condition.ListOfConditions;
import flexflux.general.Bind;
import flexflux.general.Constraint;
import flexflux.general.ConstraintType;
import flexflux.general.CplexBind;
import flexflux.general.GLPKBind;
import flexflux.general.Vars;
import flexflux.interaction.Interaction;
import flexflux.objective.Objective;
import parsebionet.biodata.BioChemicalReaction;
import parsebionet.biodata.BioEntity;

public class TestFreeFluxes extends FFApplication {

	public static String message = "Finds closest fba fluxes to experimental data by freeing every metabolic flux.";

	@Option(name = "-s", usage = "Metabolic network file path (SBML format)", metaVar = "File - in", required = true)
	public String metabolicNetworkPath = "";

	@Option(name = "-omics", usage = "Gene expression data file path", metaVar = "File - in", required = true)
	public String omicsDataPath = "";

	@Option(name = "-out", usage = "Path for the forlder containing the output", metaVar = "File - out", required = true)
	public String ResultsFolderPath = "";

	@Option(name = "-cond", usage = "[OPTIONAL] "
			+ ListOfConditions.fileFormat, metaVar = "File - in", required = false)
	public String conditionFile = "";

	/**
	 * 1 => And : sum ; or : mean <br/>
	 * 2 => all mean 3 => And : sum ; or : min <br/>
	 */
	@Option(name = "-calcGPR", usage = "GPR calculation method", metaVar = "Integer")
	public int gprCalculationMethod = 2;

	@Option(name = "-sol", usage = "Solver name", metaVar = "Solver")
	public String solver = "CPLEX";

	public static void main(String[] args) {
		
		TestFreeFluxes f = new TestFreeFluxes();

		f.parseArguments(args);

		if (!new File(f.metabolicNetworkPath).isFile()) {
			System.err.println("Error : file " + f.metabolicNetworkPath + " not found");
			System.exit(0);
		}
		if (!new File(f.omicsDataPath).isFile()) {
			System.err.println("Error : file " + f.omicsDataPath + " not found");
			System.exit(0);
		}

		if (f.conditionFile != "") {
			if (!new File(f.conditionFile).isFile()) {
				System.err.println("Error : file " + f.conditionFile + " not found");
				System.exit(0);
			}
		}
		Bind bind = null;

		try {
			if (f.solver.equals("CPLEX")) {
				bind = new CplexBind();
			} else if (f.solver.equals("GLPK")) {
				bind = new GLPKBind();
			} else {
				System.err.println("Unknown solver name");
				f.parser.printUsage(System.err);
				System.exit(0);
			}
		} catch (UnsatisfiedLinkError e) {
			System.err.println("Error, the solver " + f.solver
					+ " cannot be found. Check your solver installation and the configuration file, or choose a different solver (-sol).");
			System.exit(0);
		} catch (NoClassDefFoundError e) {
			System.err.println("Error, the solver " + f.solver
					+ " cannot be found. There seems to be a problem with the .jar file of " + f.solver + ".");
			System.exit(0);
		}

		Map<String, Map<String, Double>> revResults = new HashMap<String, Map<String, Double>>();
		Map<String, Map<String, Double>> fbaResults = new HashMap<String, Map<String, Double>>();
		
		bind.loadSbmlNetwork(f.metabolicNetworkPath, false);

//		JSBMLToBionetwork parser = new JSBMLToBionetwork(f.metabolicNetworkPath);
		
//		BioNetwork allNetwork = parser.getBioNetwork();
		
//		BioNetwork reducedNetwork = ReduceNetwork.findCouples(allNetwork);
//		bind.setNetworkAndConstraints(reducedNetwork);
		
		
//		BioNetwork reducedNetwork = ReduceNetwork.incrementTechnique(allNetwork);
		
		
		
//		System.exit(0);
		

		// need to "free" every flux
		for (BioEntity bioEntity : bind.getInteractionNetwork().getEntities()) {
			
			if (!bind.getDeadReactions().contains(bioEntity)) {
				Constraint toDelete = null;
				for (Constraint c : bind.getConstraints()) {

					Map<BioEntity, Double> entities = c.getEntities();

					if (entities.size() == 1 && entities.containsKey(bioEntity)) {

						if (entities.get(bioEntity) == 1) {

							toDelete = c;
							break;
						}
					}
				}
				if (toDelete != null) {

					bind.getConstraints().remove(toDelete);
					bind.getSimpleConstraints().remove(bioEntity);

					double lb = -99999;
					double ub = 99999;

					Map<BioEntity, Double> constraintMap = new HashMap<BioEntity, Double>();
					constraintMap.put(bioEntity, 1.0);

					if (!((BioChemicalReaction) bioEntity).isReversible()) {
						lb = 0;
					}

					Constraint c = new Constraint(constraintMap, lb, ub);

					bind.getConstraints().add(c);
					bind.getSimpleConstraints().put(bioEntity, c);

				}
			}
		}

		OmicsData omicsData = OmicsDataReader.loadOmicsData(f.omicsDataPath,
				bind.getInteractionNetwork().getEntities());

		for (BioEntity gene : omicsData.getVariables()) {

			int nbReac = 0;

			for (Interaction interaction : bind.getInteractionNetwork().getGPRInteractions()) {
				for (BioEntity involvedEnt : interaction.getCondition().getInvolvedEntities()) {
					if (gene == involvedEnt) {
						nbReac++;
						break;
					}
				}
			}
			 omicsData.scaleVariable(gene,nbReac);

		}

		List<Sample> samples = omicsData.getSamples();

		if (f.conditionFile != "") {
			/**
			 * Load the condition file
			 */
			ListOfConditions conditions = new ListOfConditions();

			Boolean flag = conditions.loadConditionFile(f.conditionFile, ConstraintType.DOUBLE);

			if (flag == false) {
				System.err.println("Error in reading the condition file " + f.conditionFile);
				System.exit(0);
			}

			for (Condition cond : conditions.conditions) {

				Sample s = omicsData.getSample(cond.name);

				if (s != null) {
					s.setCondition(cond);
				} else {
					System.err.println("Warning: condition " + cond.name + " in the condition file is not recognised.");
				}

			}
		}

		Map<Sample, Map<BioChemicalReaction, Double>> reactionExpressionValues = new HashMap<Sample, Map<BioChemicalReaction, Double>>();

		double maxRev = 0;

		for (Sample s : samples) {

			reactionExpressionValues.put(s, new HashMap<BioChemicalReaction, Double>());
			revResults.put(s.getName(), new HashMap<String, Double>());

			for (Interaction inter : bind.getInteractionNetwork().getGPRInteractions()) {
				BioChemicalReaction reac = (BioChemicalReaction) inter.getConsequence().getEntity();

				double expr = inter.getCondition().calculateRelationQuantitativeValue(
						omicsData.getDataValuesForSample(s), f.gprCalculationMethod);
				
//				expr = Math.pow(expr, 1.55);
				

				if (!Double.isNaN(expr)) {

					revResults.get(s.getName()).put(reac.getId(), expr);
					reactionExpressionValues.get(s).put(reac, expr);

					if (expr > maxRev) {
						maxRev = expr;
					}

				}
			}
		}

		System.out.println("Max rev: " + maxRev);
		System.out.println("Scaling factor: " + maxRev / 25);
		double scalingFactor = maxRev / 25;
		// scaler pour etre < M

		writeRevResults(f.ResultsFolderPath + "/outRev.txt", revResults, samples);

		bind.createFluxesSummation();

		// to write the results into a file
		Map<String, Double> sampleToMinimisationRes = new HashMap<String, Double>();
		//

		for (Sample sample : samples) {

			System.out.println(sample.getName());

			fbaResults.put(sample.getName(), new HashMap<String, Double>());

			///////////// We create FBA variables that correspond to the
			///////////// distance
			///////////// between scaled reaction expression values and flux
			///////////// values.
			///////////// We also create the objective function that minimises
			///////////// the
			///////////// distances

			Map<BioChemicalReaction, Double> reactionExpressionValues_sample = reactionExpressionValues.get(sample);

			BioEntity[] objectiveEntities = new BioEntity[reactionExpressionValues_sample.size()];
			double[] objectiveCoeffs = new double[reactionExpressionValues_sample.size()];
			int index = 0;

			List<Constraint> constraintsToAdd = new ArrayList<Constraint>();

			for (BioChemicalReaction reac : reactionExpressionValues_sample.keySet()) {

				BioEntity reacPrime = new BioEntity(reac.getId() + "_prime");
				bind.getInteractionNetwork().addNumEntity(reacPrime);
				objectiveEntities[index] = reacPrime;
				objectiveCoeffs[index] = 1.0;

				Constraint c1, c2;

				double reactionExpressionValue = reactionExpressionValues_sample.get(reac) / scalingFactor;

				if (bind.getSimpleConstraints().get(reac).getLb() >= 0) {

					// we create the constraints : R1 - R1exp <= R'1
					// R1exp - R1 <= R'1

					// Const 1 : -inf < R1 - R1exp - R'1 <= 0
					// <=> -inf < R1 - R'1 <= R1exp
					Map<BioEntity, Double> cMap1 = new HashMap<BioEntity, Double>();
					cMap1.put(reac, 1.0);
					cMap1.put(reacPrime, -1.0);
					c1 = new Constraint(cMap1, -Double.MAX_VALUE, reactionExpressionValue);
					// System.out.println(c1);

					// Const 2 : -inf < R1exp - R1 - R'1 <= 0
					// <=> -inf < -R1 - R'1 <= - R1exp
					Map<BioEntity, Double> cMap2 = new HashMap<BioEntity, Double>();
					cMap2.put(reac, -1.0);
					cMap2.put(reacPrime, -1.0);
					c2 = new Constraint(cMap2, -Double.MAX_VALUE, -reactionExpressionValue);
					// System.out.println(c2);

				}
				// reversible reactions
				else {
					BioEntity reacAbs = bind.getInteractionNetwork().getEntity(reac.getId() + Vars.absolute);

					// we create the constraints :
					// reacAbs - R1exp <= R'1
					// R1exp - reacAbs <= R'1

					// Const 1 : -inf < reacAbs - R1exp - R'1 <= 0
					// <=> -inf < reacAbs - R'1 <= R1exp
					Map<BioEntity, Double> cMap1 = new HashMap<BioEntity, Double>();
					cMap1.put(reacAbs, 1.0);
					cMap1.put(reacPrime, -1.0);
					c1 = new Constraint(cMap1, -Double.MAX_VALUE, reactionExpressionValue);
					// System.out.println(c1);

					// Const 2 : -inf < R1exp - reacAbs - R'1 <= 0
					// <=> -inf < - reacAbs - R'1 <= - R1exp
					Map<BioEntity, Double> cMap2 = new HashMap<BioEntity, Double>();
					cMap2.put(reacAbs, -1.0);
					cMap2.put(reacPrime, -1.0);
					c2 = new Constraint(cMap2, -Double.MAX_VALUE, -reactionExpressionValue);

					/////////////////// Then we make sure that reacAbs is not >
					/////////////////// than absolute value of reaction

					///////////// Need to add two boolean variables : b and a =
					///////////// 1-b
					BioEntity b = new BioEntity("interger_mip_b_" + reac.getId());
					bind.getInteractionNetwork().addBinaryEntity(b);
					BioEntity a = new BioEntity("interger_mip_a_" + reac.getId());
					bind.getInteractionNetwork().addBinaryEntity(a);

					/// constraint a = 1-b <=> a-1+b = 0 <=> a+b=1
					Map<BioEntity, Double> intergerConstraintMap = new HashMap<BioEntity, Double>();
					intergerConstraintMap.put(a, 1.0);
					intergerConstraintMap.put(b, 1.0);
					Constraint integerConstraint = new Constraint(intergerConstraintMap, 1.0, 1.0);
					constraintsToAdd.add(integerConstraint);

					////////////// We add an entity Y that is R1a + R1b
					////////////// <=> Y-R1a-R1b = 0
					BioEntity y = new BioEntity("Y_" + reac.getId());
					bind.getInteractionNetwork().addNumEntity(y);
					Map<BioEntity, Double> yConstraintMap = new HashMap<BioEntity, Double>();
					yConstraintMap.put(y, 1.0);
					yConstraintMap.put(reacAbs, -1.0);
					Constraint yConstraint = new Constraint(yConstraintMap, 0.0, 0.0);
					constraintsToAdd.add(yConstraint);

					////////////// http://lpsolve.sourceforge.net/5.1/absolute.htm
					////////////// We add these two constrains:
					// R1 + M * B >= Y
					// -R1 + M * A >= Y
					// <=>
					// R1 + M*B - Y >= 0
					// -R1 + M*A - Y >= 0

					double M = 100;
					// first const
					Map<BioEntity, Double> intergerSumConstraintMap1 = new HashMap<BioEntity, Double>();
					intergerSumConstraintMap1.put(reac, 1.0);
					intergerSumConstraintMap1.put(b, M);
					intergerSumConstraintMap1.put(y, -1.0);

					Constraint integerSumConstraint1 = new Constraint(intergerSumConstraintMap1, 0.0, Double.MAX_VALUE);

					constraintsToAdd.add(integerSumConstraint1);

					// second const
					Map<BioEntity, Double> intergerSumConstraintMap2 = new HashMap<BioEntity, Double>();
					intergerSumConstraintMap2.put(reac, -1.0);
					intergerSumConstraintMap2.put(a, M);
					intergerSumConstraintMap2.put(y, -1.0);
					Constraint integerSumConstraint2 = new Constraint(intergerSumConstraintMap2, 0.0, Double.MAX_VALUE);

					constraintsToAdd.add(integerSumConstraint2);

					// System.out.println(c2);
				}
				constraintsToAdd.add(c1);
				constraintsToAdd.add(c2);
				index++;
			}

			bind.prepareSolver();

			Objective p = new Objective(objectiveEntities, objectiveCoeffs, "", false);
			bind.setObjective(p);

			if (sample.getHasCondtition()) {
				for (String constraintName : sample.getCondition().constraints.keySet()) {

					if (sample.getCondition().constraints.get(constraintName).value == 0.0) {
						
						BioEntity ent = bind.getInteractionNetwork()
								.getEntity(sample.getCondition().constraints.get(constraintName).entityId);

						if (ent == null) {
							System.err.println(
									"Warning, entity " + sample.getCondition().constraints.get(constraintName).entityId
											+ " is unknown. It might belong to a dead reaction.");
						} else {
							Map<BioEntity, Double> newConstraintMap = new HashMap<BioEntity, Double>();
							newConstraintMap.put(ent, 1.0);
							constraintsToAdd.add(new Constraint(newConstraintMap, 0.0, 0.0));

						}
					}
				}
			}

			double resFBA = bind.FBA(constraintsToAdd, true, false).result;

			System.out.println(resFBA);
			sampleToMinimisationRes.put(sample.getName(), resFBA);

			// write results
			for (String reacName : bind.getBioNetwork().getBiochemicalReactionList().keySet()) {
				BioChemicalReaction reac = bind.getBioNetwork().getBiochemicalReactionList().get(reacName);
				fbaResults.get(sample.getName()).put(reacName, bind.getSolvedValue(reac));
			}

		}

		writeFbaResults(f.ResultsFolderPath + "/outFBA.txt", fbaResults, samples);

		writeStatsResults(f.ResultsFolderPath + "/stats.txt", scalingFactor, sampleToMinimisationRes);
		
		
//		generatePlots(f.ResultsFolderPath,scalingFactor,revResults,fbaResults);

	}

	private static void writeStatsResults(String path, double scalingFactor,
			Map<String, Double> sampleToMinimisationRes) {
		try {
			PrintWriter out = new PrintWriter(new File(path));

			out.println("Scaling factor\t"+scalingFactor);
			
			out.println("");
			
			for (String sampleName : sampleToMinimisationRes.keySet()){
				out.println(sampleName+"\t"+sampleToMinimisationRes.get(sampleName));
			}
			

			out.close();
		} catch (IOException e) {
			System.out.println("path " + path + " is not a valid path, or file could not be created.");
		}

	}

	public static void writeFbaResults(String resultsPath, Map<String, Map<String, Double>> fbaResults,
			List<Sample> samples) {

		try {
			PrintWriter out = new PrintWriter(new File(resultsPath));

			String firstLine = "";

			List<String> sampleNames = new ArrayList<>();
			List<String> reacNames = new ArrayList<>();

			for (Sample sample : samples) {
				sampleNames.add(sample.getName());
				firstLine += sample.getName() + "\t";
			}

			for (String reacName : fbaResults.get(sampleNames.get(0)).keySet()) {
				reacNames.add(reacName);
			}

			firstLine = firstLine.substring(0, firstLine.length() - 1);
			out.println(firstLine);

			for (String reacName : reacNames) {
				String line = reacName;
				for (String sampleName : sampleNames) {
					line += "\t" + fbaResults.get(sampleName).get(reacName);
				}
				out.println(line);
			}

			out.close();
		} catch (IOException e) {
			System.out.println("path " + resultsPath + " is not a valid path, or file could not be created.");
		}

	}

	public static void writeRevResults(String resultsPath, Map<String, Map<String, Double>> revResults,
			List<Sample> samples) {

		try {
			PrintWriter out = new PrintWriter(new File(resultsPath));

			String firstLine = "";

			List<String> sampleNames = new ArrayList<>();
			List<String> reacNames = new ArrayList<>();

			for (Sample sample : samples) {
				sampleNames.add(sample.getName());
				firstLine += sample.getName() + "\t";
			}

			for (String reacName : revResults.get(sampleNames.get(0)).keySet()) {
				reacNames.add(reacName);
			}

			firstLine = firstLine.substring(0, firstLine.length() - 1);
			out.println(firstLine);

			for (String reacName : reacNames) {
				String line = reacName;
				for (String sampleName : sampleNames) {
					line += "\t" + revResults.get(sampleName).get(reacName);
				}
				out.println(line);
			}

			out.close();
		} catch (IOException e) {
			System.out.println("path " + resultsPath + " is not a valid path, or file could not be created.");
		}

	}

	public String getMessage() {
		return message;
	}

}
