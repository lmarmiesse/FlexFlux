package flexflux.omics;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import flexflux.general.Bind;
import flexflux.general.Constraint;
import flexflux.general.CplexBind;
import flexflux.general.DoubleResult;
import flexflux.interaction.Interaction;
import flexflux.objective.Objective;
import parsebionet.biodata.BioChemicalReaction;
import parsebionet.biodata.BioEntity;

public class TestBounds {

	public static void main(String[] args) {
		/**
		 * 1 => And : sum ; or : mean <br/>
		 * 2 => all mean 3 => And : sum ; or : min <br/>
		 */
		int gprCalculationMethod = 3;

		Map<String, Map<String, Double>> revResults = new HashMap<String, Map<String, Double>>();
		Map<String, Map<String, Double>> fbaResults = new HashMap<String, Map<String, Double>>();

		String metabolicNetworkPath = "/home/lmarmiesse/rocks1-compneur/FBA-stemcells/transcriptome/NaivePrimed/Recon2.v04-sbml-jsbml.xml";
		String omicsDataPath = "/home/lmarmiesse/rocks1-compneur/FBA-stemcells/transcriptome/NaivePrimed/primed-naive-rpkm-mapped-to-recon2-reduced-FF.txt";
		String constraintsFilePath = "/home/lmarmiesse/rocks1-compneur/FBA-stemcells/transcriptome/NaivePrimed/FlexFlux/cons.txt";
		
		String revResultsPath = "/home/lmarmiesse/rocks1-compneur/FBA-stemcells/transcriptome/NaivePrimed/FlexFlux/rev_3.txt";
		String fbaResultsPath = "/home/lmarmiesse/rocks1-compneur/FBA-stemcells/transcriptome/NaivePrimed/FlexFlux/results_recon2_fba.txt";
		
		// String metabolicNetworkPath =
		// "/home/lmarmiesse/rocks1-compneur/work/lucas/Toy
		// model/toyModel2.xml";
		// String omicsDataPath =
		// "/home/lmarmiesse/rocks1-compneur/work/lucas/Toy
		// model/exprData2.txt";
		// String constraintsFilePath =
		// "/home/lmarmiesse/rocks1-compneur/work/lucas/Toy
		// model/constraints2.txt";
		//
		// String revResultsPath =
		// "/home/lmarmiesse/rocks1-compneur/work/lucas/Toy
		// model/resultsRev2.txt";
		// String fbaResultsPath =
		// "/home/lmarmiesse/rocks1-compneur/work/lucas/Toy
		// model/resultsFBA2.txt";

		Bind bind = new CplexBind();

		bind.loadSbmlNetwork(metabolicNetworkPath, false);
		bind.loadConstraintsFile(constraintsFilePath);

		OmicsData omicsData = OmicsDataReader.loadOmicsData(omicsDataPath, bind.getInteractionNetwork().getEntities());

		List<Sample> samples = omicsData.getSamples();

		Map<Sample, Map<BioChemicalReaction, Double>> reactionExpressionValues = new HashMap<Sample, Map<BioChemicalReaction, Double>>();

		double maxRev = 0;

		for (Sample s : samples) {
			reactionExpressionValues.put(s, new HashMap<BioChemicalReaction, Double>());
			revResults.put(s.getName(), new HashMap<String, Double>());

			for (Interaction inter : bind.getInteractionNetwork().getGPRInteractions()) {
				BioChemicalReaction reac = (BioChemicalReaction) inter.getConsequence().getEntity();

				double expr = inter.getCondition()
						.calculateRelationQuantitativeValue(omicsData.getDataValuesForSample(s), gprCalculationMethod);

				if (!Double.isNaN(expr)) {

					revResults.get(s.getName()).put(reac.getId(), expr);
					reactionExpressionValues.get(s).put(reac, expr);

					if (expr > maxRev) {
						maxRev = expr;
					}

				}
			}
		}

		writeResults(revResultsPath, revResults, samples);

		for (Sample sample : samples) {
			System.out.println(sample.getName());

			Map<BioChemicalReaction, Double> reactionExpressionValues_sample = reactionExpressionValues.get(sample);

			fbaResults.put(sample.getName(), new HashMap<String, Double>());

			int flag = 1;
			double relaxFlux = 0;

			while (flag != 0) {

				List<Constraint> constraintsToAdd = new ArrayList<Constraint>();

				for (BioChemicalReaction reac : reactionExpressionValues_sample.keySet()) {

					double reactionExpressionValue = reactionExpressionValues_sample.get(reac);

					if (bind.getSimpleConstraints().containsKey(reac)) {
						// System.out.println(bind.getSimpleConstraints().get(reac));
						// removedConst = true;
						//
						// toRemove = bind.getSimpleConstraints().get(entity);
						// bind.getSimpleConstraints().remove(entity);
						// bind.getConstraints().remove(toRemove);
						// bind.prepareSolver();
					}

					Map<BioEntity, Double> entityMap = new HashMap<BioEntity, Double>();
					entityMap.put(reac, 1.0);

					Constraint newConstraint = new Constraint(entityMap, reactionExpressionValue - relaxFlux,
							reactionExpressionValue + relaxFlux);
					constraintsToAdd.add(newConstraint);

				}

				bind.prepareSolver();

				DoubleResult result = bind.FBA(constraintsToAdd, true, false);
				double resFBA = result.result;
				flag = result.flag;

				if (flag == 0) {

					for (Constraint c : constraintsToAdd) {
						System.out.println(c);
					}

					System.out.println(resFBA);
				} else {
					System.out.println("Unfeasible");
				}

				relaxFlux += 10;

			}

			// write results
			for (String reacName : bind.getBioNetwork().getBiochemicalReactionList().keySet()) {
				BioChemicalReaction reac = bind.getBioNetwork().getBiochemicalReactionList().get(reacName);
				fbaResults.get(sample.getName()).put(reacName, bind.getSolvedValue(reac));
			}

		}

		writeResults(fbaResultsPath, fbaResults, samples);

	}

	public static void writeResults(String resultsPath, Map<String, Map<String, Double>> revResults,
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

}
