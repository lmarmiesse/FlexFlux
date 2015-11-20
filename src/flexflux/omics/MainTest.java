package flexflux.omics;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import flexflux.analyses.FBAAnalysis;
import flexflux.general.Bind;
import flexflux.general.Constraint;
import flexflux.general.CplexBind;
import flexflux.general.GLPKBind;
import flexflux.general.Vars;
import flexflux.interaction.Interaction;
import flexflux.objective.Objective;
import parsebionet.biodata.BioChemicalReaction;
import parsebionet.biodata.BioEntity;

public class MainTest {

	public static void main(String[] args) {

		/**
		 * 1 => And : sum ; or : mean <br/>
		 * 2 => all mean
		 * 3 => And : sum ; or : min <br/>
		 */
		int gprCalculationMethod = 2;

		// SampleName => (fba1 => (ReactionName => value)
		// (fbaCorrected => (ReactionName => value)
		Map<String, Map<String, Map<String, Double>>> results = new HashMap<String, Map<String, Map<String, Double>>>();

//		String metabolicNetworkPath = "/run/user/91148/gvfs/sftp:host=147.100.166.10,user=lmarmiesse/home/users/GRP_DR/lmarmiesse/Documents/scripts/transcripto-metabolism/"
//				+ "correctFBA/Sclero_central_sbml_2015_09_15.xml";
//		String omicsDataPath = "/run/user/91148/gvfs/sftp:host=147.100.166.10,user=lmarmiesse/home/users/GRP_DR/lmarmiesse/Documents/scripts/transcripto-metabolism/"
//				+ "correctFBA/RNAseq_data.tab";
//		String constraintsFilePath = "/run/user/91148/gvfs/sftp:host=147.100.166.10,user=lmarmiesse/home/users/GRP_DR/lmarmiesse/Documents/scripts/transcripto-metabolism/"
//				+ "correctFBA/Environmental_condition_Glc.tab";
//		String resultsPath = "/run/user/91148/gvfs/sftp:host=147.100.166.10,user=lmarmiesse/home/users/GRP_DR/lmarmiesse/Documents/scripts/transcripto-metabolism/"
//				+ "correctFBA/resultsSclero.txt";
//		String[] sampleNames = new String[] { "RNAseq_MM", "RNAseq_PDB", "RNAseq_SCL", "RNAseq_MMGln", "RNAseq_IC","RNAseq_FRT","RNAseq_NEC" };

		String metabolicNetworkPath = "Data/AraGEM_Cobra_modif.xml";
		String omicsDataPath = "Data/MYBSEQ_TIME.txt";
		String constraintsFilePath = "Data/obj";
		String resultsPath ="/home/lmarmiesse/rocks1-compneur/work/lucas/Toy model/resultsAT.txt";
		String[] sampleNames = new String[] { "Col0Hpi", "Col1Hpi", "Col2Hpi"};
//		tring[] sampleNames = new String[] { "Col0Hpi", "Col1Hpi", "Col2Hpi", "Col4Hpi", "Col6Hpi" };

//		String metabolicNetworkPath = "/run/user/91148/gvfs/sftp:host=147.100.166.10,user=lmarmiesse/home/users/GRP_DR/lmarmiesse/Documents/scripts/transcripto-metabolism/"
//				+ "correctFBA/toyModel/Toy_model.xml";
//		String omicsDataPath = "/run/user/91148/gvfs/sftp:host=147.100.166.10,user=lmarmiesse/home/users/GRP_DR/lmarmiesse/Documents/scripts/transcripto-metabolism/"
//				+ "correctFBA/toyModel/Expression_data.tab";
//		String constraintsFilePath = "/run/user/91148/gvfs/sftp:host=147.100.166.10,user=lmarmiesse/home/users/GRP_DR/lmarmiesse/Documents/scripts/transcripto-metabolism/"
//				+ "correctFBA/toyModel/constraints.tab";
//		String resultsPath = "/run/user/91148/gvfs/sftp:host=147.100.166.10,user=lmarmiesse/home/users/GRP_DR/lmarmiesse/Documents/scripts/transcripto-metabolism/"
//				+ "correctFBA/toyModel/results.txt";
//		String[] sampleNames = new String[] { "Test_1","Test_2","Test_3" };
		
		
		
		
//		String metabolicNetworkPath = "/home/lmarmiesse/rocks1-compneur/work/lucas/Toy model/toyModel.xml";
//		String omicsDataPath = "/home/lmarmiesse/rocks1-compneur/work/lucas/Toy model/exprData.txt";
//		String constraintsFilePath = "/home/lmarmiesse/rocks1-compneur/work/lucas/Toy model/constraints.txt";
//		String resultsPath ="/home/lmarmiesse/rocks1-compneur/work/lucas/Toy model/results.txt";
//		String[] sampleNames = new String[] { "Cond1" , "Cond2" , "Cond3"};

		// TODO Auto-generated method stub

//		 Bind bind = new GLPKBind();
		Bind bind = new CplexBind();

		bind.loadSbmlNetwork(metabolicNetworkPath, false);

		bind.loadConstraintsFile(constraintsFilePath);

		OmicsData omicsData = OmicsDataReader.loadOmicsData(omicsDataPath, bind.getInteractionNetwork().getEntities());

		List<Sample> samples = new ArrayList<Sample>();

		for (String s : sampleNames) {
			samples.add(omicsData.getSample(s));
		}

		Map<Sample, Map<BioChemicalReaction, Double>> reactionExpressionValues = new HashMap<Sample, Map<BioChemicalReaction, Double>>();

		for (Sample s : samples) {
			reactionExpressionValues.put(s, new HashMap<BioChemicalReaction, Double>());
		}

		for (Interaction inter : bind.getInteractionNetwork().getGPRInteractions()) {

			BioChemicalReaction reac = (BioChemicalReaction) inter.getConsequence().getEntity();

			for (Sample s : samples) {
				
				
				double expr = inter.getCondition()
						.calculateRelationQuantitativeValue(omicsData.getDataValuesForSample(s), gprCalculationMethod);
				reactionExpressionValues.get(s).put(reac, expr);

			}
		}

		// System.out.println(samples.get(0).getName());
		// System.out.println(expReactionActivities.get(samples.get(0)));

		bind.prepareSolver();

		Objective initialObjective = bind.getObjective();
		BioEntity fluxSum = bind.createFluxesSummation();
		bind.prepareSolver();

		/////////////

		for (Sample sample : samples) {

			results.put(sample.getName(), new HashMap<String, Map<String, Double>>());

			bind.setObjective(initialObjective);

			FBAAnalysis fba = new FBAAnalysis(bind);
			double res = fba.runAnalysis().getObjValue();

			////////////// Constrain FBA result and minimize flux sum
			Map<BioEntity, Double> constraintMap = new HashMap<BioEntity, Double>();

			for (int i = 0; i < bind.getObjective().getCoeffs().length; i++) {
				constraintMap.put(bind.getObjective().getEntities()[i], bind.getObjective().getCoeffs()[i]);
			}

			Constraint c = new Constraint(constraintMap, res, res);
			Objective minFluxSumBoj = new Objective(new BioEntity[] { fluxSum }, new double[] { 1 }, "", false);
			bind.setObjective(minFluxSumBoj);

			List<Constraint> constraintsToAdd = new ArrayList<Constraint>();
			constraintsToAdd.add(c);

			bind.FBA(constraintsToAdd, true, false);

			// we save the results
			results.get(sample.getName()).put("fba1", new HashMap<String, Double>());
			for (String reacName : bind.getBioNetwork().getBiochemicalReactionList().keySet()) {
				BioChemicalReaction reac = bind.getBioNetwork().getBiochemicalReactionList().get(reacName);
				results.get(sample.getName()).get("fba1").put(reacName, bind.getSolvedValue(reac));
			}
			//

			///////////// We calculated the sum of the reaction expression
			///////////// values
			///////////// AND of the calculated fluxes
			Map<BioChemicalReaction, Double> reactionExpressionValues_MM = reactionExpressionValues.get(sample);

			double fluxSumNoNan = 0;
			double reactionExpressionValuesSum = 0;

			for (BioChemicalReaction reac : reactionExpressionValues_MM.keySet()) {
				if (!Double.isNaN(reactionExpressionValues_MM.get(reac))
						&& Math.abs(bind.getSolvedValue(reac)) > 0.0000001) {

					reactionExpressionValuesSum += Math.abs(reactionExpressionValues_MM.get(reac));
					fluxSumNoNan += Math.abs(bind.getSolvedValue(reac));

				}
			}

			/////////////

			///////////// Reaction expression values are scaled
			double scalingFactor = reactionExpressionValuesSum / fluxSumNoNan;
			scalingFactor *= 10;
			
//			scalingFactor = 1;
			System.out.println("Flux sum FBA1 : "+fluxSumNoNan);
			System.out.println(reactionExpressionValuesSum);
			System.out.println("sclaing factor : "+scalingFactor);

			Map<BioChemicalReaction, Double> scaledRactionExpressionValues_MM = new HashMap<BioChemicalReaction, Double>();

			double sumFba1 = 0;

			for (BioChemicalReaction reac : reactionExpressionValues_MM.keySet()) {
				if (!Double.isNaN(reactionExpressionValues_MM.get(reac))) {
					double scaledExpressionValue = reactionExpressionValues_MM.get(reac) / scalingFactor;
					scaledRactionExpressionValues_MM.put(reac, scaledExpressionValue);


					double ecart = Math
							.abs(scaledRactionExpressionValues_MM.get(reac) - Math.abs(bind.getSolvedValue(reac)));
					sumFba1 += ecart;

				}
			}

			System.out.println(sumFba1);
			/////////////

			///////////// We create FBA variables that correspond to the
			///////////// distance
			///////////// between scaled reaction expression values and flux
			///////////// values.
			///////////// We also create the objective function that minimises
			///////////// the
			///////////// distances

			BioEntity[] objectiveEntities = new BioEntity[scaledRactionExpressionValues_MM.size()];
			double[] objectiveCoeffs = new double[scaledRactionExpressionValues_MM.size()];
			int index = 0;

			constraintsToAdd = new ArrayList<Constraint>();

			for (BioChemicalReaction reac : scaledRactionExpressionValues_MM.keySet()) {

				BioEntity reacPrime = new BioEntity(reac.getId() + "_prime");
				bind.getInteractionNetwork().addNumEntity(reacPrime);
				objectiveEntities[index] = reacPrime;
				objectiveCoeffs[index] = 1.0;

				Constraint c1, c2;

				if (bind.getSimpleConstraints().get(reac).getLb() >= 0) {
					// we create the constraints : R1 - R1exp <= R'1
					// R1exp - R1 <= R'1

					// Const 1 : -inf < R1 - R1exp - R'1 <= 0
					// <=> -inf < R1 - R'1 <= R1exp
					Map<BioEntity, Double> cMap1 = new HashMap<BioEntity, Double>();
					cMap1.put(reac, 1.0);
					cMap1.put(reacPrime, -1.0);
					c1 = new Constraint(cMap1, -Double.MAX_VALUE, scaledRactionExpressionValues_MM.get(reac));
					// System.out.println(c1);

					// Const 2 : -inf < R1exp - R1 - R'1 <= 0
					// <=> -inf < -R1 - R'1 <= - R1exp
					Map<BioEntity, Double> cMap2 = new HashMap<BioEntity, Double>();
					cMap2.put(reac, -1.0);
					cMap2.put(reacPrime, -1.0);
					c2 = new Constraint(cMap2, -Double.MAX_VALUE, -scaledRactionExpressionValues_MM.get(reac));
					// System.out.println(c2);

				}
				// reversible reactions
				else {
					BioEntity irrevReac1 = bind.getInteractionNetwork().getEntity(reac.getId() + Vars.Irrev1);
					BioEntity irrevReac2 = bind.getInteractionNetwork().getEntity(reac.getId() + Vars.Irrev2);

					// we create the constraints : (R1irrev1-R1irrev2) - R1exp
					// <=
					// R'1
					// R1exp - (R1irrev1-R1irrev2) <= R'1

					// Const 1 : -inf < R1irrev1-R1irrev2 - R1exp - R'1 <= 0
					// <=> -inf < R1irrev1-R1irrev2 - R'1 <= R1exp
					Map<BioEntity, Double> cMap1 = new HashMap<BioEntity, Double>();
					cMap1.put(irrevReac1, 1.0);
					cMap1.put(irrevReac2, 1.0);
					cMap1.put(reacPrime, -1.0);
					c1 = new Constraint(cMap1, -Double.MAX_VALUE, scaledRactionExpressionValues_MM.get(reac));
					// System.out.println(c1);

					// Const 2 : -inf < R1exp - (R1irrev1-R1irrev2) - R'1 <= 0
					// <=> -inf < -(R1irrev1-R1irrev2) - R'1 <= - R1exp
					// <=> -inf < - R1irrev1 + R1irrev2 - R'1 <= - R1exp
					Map<BioEntity, Double> cMap2 = new HashMap<BioEntity, Double>();
					cMap2.put(irrevReac1, -1.0);
					cMap2.put(irrevReac2, -1.0);
					cMap2.put(reacPrime, -1.0);
					c2 = new Constraint(cMap2, -Double.MAX_VALUE, -scaledRactionExpressionValues_MM.get(reac));

					/////////////////// Then we make sure that the summ of the 2
					/////////////////// components
					/////////////////// of a reversible reaction is = to the
					/////////////////// absolute flux
					/////////////////// values of the reaction (not more)

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
					////////////// <=> Y-Raa-R1b = 0
					BioEntity y = new BioEntity("Y_" + reac.getId());
					bind.getInteractionNetwork().addNumEntity(y);
					Map<BioEntity, Double> yConstraintMap = new HashMap<BioEntity, Double>();
					yConstraintMap.put(y, 1.0);
					yConstraintMap.put(irrevReac1, -1.0);
					yConstraintMap.put(irrevReac2, -1.0);
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

			double sumFba2 = bind.FBA(constraintsToAdd, true, false).result;

			////////////// Constrain FBA result and minimize flux sum
			// constraintMap = new HashMap<BioEntity, Double>();
			//
			// for (int i = 0; i < bind.getObjective().getCoeffs().length; i++)
			// {
			// constraintMap.put(bind.getObjective().getEntities()[i],
			// bind.getObjective().getCoeffs()[i]);
			// }
			//
			// c = new Constraint(constraintMap, sumFba2, sumFba2);
			// minFluxSumBoj = new Objective(new BioEntity[] { fluxSum }, new
			// double[] { 1 }, "", false);
			// bind.setObjective(minFluxSumBoj);
			//
			// constraintsToAdd.add(c);
			//
			// bind.FBA(constraintsToAdd, true, false);

			System.out.println(sumFba2);
			System.out.println((1 - (sumFba2 / sumFba1)) * 100 + "%\n");

			results.get(sample.getName()).put("fba2", new HashMap<String, Double>());
			for (String reacName : bind.getBioNetwork().getBiochemicalReactionList().keySet()) {
				BioChemicalReaction reac = bind.getBioNetwork().getBiochemicalReactionList().get(reacName);
				results.get(sample.getName()).get("fba2").put(reacName, bind.getSolvedValue(reac));
			}
			
			
			
			fluxSumNoNan = 0;
			
			for (BioChemicalReaction reac : reactionExpressionValues_MM.keySet()) {
				if (!Double.isNaN(reactionExpressionValues_MM.get(reac))) {
//					System.out.println(reac.getId()+"\t"+Math.abs(scaledRactionExpressionValues_MM.get(reac))+"\t"+Math.abs(bind.getSolvedValue(reac)));
					fluxSumNoNan += Math.abs(bind.getSolvedValue(reac));
				}
			}
			
			System.out.println("Flux sum FBA2 : "+fluxSumNoNan);

		}

		writeResults(resultsPath, results, bind.getBioNetwork().getBiochemicalReactionList().keySet(), sampleNames);

	}

	public static void writeResults(String resultsPath, Map<String, Map<String, Map<String, Double>>> results,
			Set<String> reacsNames, String[] sampleNames) {

		try {
			PrintWriter out = new PrintWriter(new File(resultsPath));

			String firstLine = "";

			for (String sampleName : sampleNames) {
				for (String colName : results.get(sampleName).keySet()) {
					firstLine += sampleName + "_" + colName + "\t";
				}
			}
			firstLine = firstLine.substring(0, firstLine.length() - 1);
			out.println(firstLine);

			for (String reacName : reacsNames) {
				String line = reacName;
				for (String sampleName : sampleNames) {
					for (String colName : results.get(sampleName).keySet()) {

						line += "\t" + results.get(sampleName).get(colName).get(reacName);

					}
				}
				out.println(line);
			}

			out.close();
		} catch (IOException e) {
			System.out.println("path " + resultsPath + " is not a valid path, or file could not be created.");
		}

	}

}
