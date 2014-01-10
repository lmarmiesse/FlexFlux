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
 * 15 mai 2013 
 */
package parsebionet.utils.flexconflux.analyses;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import parsebionet.biodata.BioEntity;
import parsebionet.utils.flexconflux.Bind;
import parsebionet.utils.flexconflux.Objective;
import parsebionet.utils.flexconflux.analyses.result.FBAResult;
import parsebionet.utils.flexconflux.analyses.result.ParetoAnalysisResult;
import parsebionet.utils.flexconflux.analyses.result.ReacAnalysisResult;
import parsebionet.utils.flexconflux.analyses.result.TwoReacsAnalysisResult;

/**
 * Class to run a pareto analysis.
 * 
 * @author lmarmiesse 15 mai 2013
 * 
 */
public class ParetoAnalysis extends Analysis {

	/**
	 * Decides if all 2D and 3D results are plot or only the best result.
	 */
	boolean plotAll;

	/**
	 * Path to the file containing the objectives and experimental values
	 */
	String filePath;

	/**
	 * List of objectives.
	 */
	List<Objective> objectives = new ArrayList<Objective>();

	/**
	 * key : an objective, value : its lower and upper bounds
	 */
	Map<Objective, double[]> objectivesBounds = new HashMap<Objective, double[]>();

	/**
	 * List containing all experimental values for each objective.
	 * 
	 */
	List<Map<Objective, Double>> experimentalValues = new ArrayList<Map<Objective, Double>>();

	public ParetoAnalysis(Bind b, String filePath, boolean plotAll) {
		super(b);
		this.filePath = filePath;
		this.plotAll = plotAll;
	}

	public ParetoAnalysisResult runAnalysis() {

		Map<Objective, List<Double>> oneDResults = new HashMap<Objective, List<Double>>();
		Map<ReacAnalysisResult, Double> twoDResults = new HashMap<ReacAnalysisResult, Double>();
		Map<TwoReacsAnalysisResult, Double> threeDResults = new HashMap<TwoReacsAnalysisResult, Double>();

		parseFile();
		//

		// 1D

		//

		for (Objective obj : objectives) {

			objectivesBounds.put(obj, new double[] { 0, 0 });

			b.setObjective(obj);

			FBAAnalysis fba = new FBAAnalysis(b);

			FBAResult result = fba.runAnalysis();

			List<Double> objResults = new ArrayList<Double>();

			for (Map<Objective, Double> m : experimentalValues) {
				objResults.add(m.get(obj));
			}

			oneDResults.put(obj, objResults);

			// we set the lower and upper values of the objectives
			if (obj.getMaximize()) {
				objectivesBounds.get(obj)[1] = result.getObjValue();
			} else {
				objectivesBounds.get(obj)[0] = result.getObjValue();
			}
			

			b.setObjSense(!obj.getMaximize());

			FBAAnalysis fba2 = new FBAAnalysis(b);

			FBAResult result2 = fba2.runAnalysis();

			if (!obj.getMaximize()) {
				objectivesBounds.get(obj)[1] = result2.getObjValue();
			} else {
				objectivesBounds.get(obj)[0] = result2.getObjValue();
			}
			
			System.out.println(obj.getName()+" min : "+objectivesBounds.get(obj)[0]+" max : "+objectivesBounds.get(obj)[1]);

			// we normalize the values
			List<Double> normalizedResults = new ArrayList<Double>();

			for (Double val : oneDResults.get(obj)) {

				double normalizedValue = (val - objectivesBounds.get(obj)[0])
						/ (objectivesBounds.get(obj)[1] - objectivesBounds
								.get(obj)[0]);

				if (!obj.getMaximize()) {
					normalizedValue = 1 - normalizedValue;
				}

				normalizedResults.add(normalizedValue);

			}
			oneDResults.put(obj, normalizedResults);

		}

		//

		// 2D

		//

		List<Objective[]> pairsToTest = makeObjsPairs();

		// to store the scores
		Entry<ReacAnalysisResult, Double> bestResult2D = new SimpleEntry<ReacAnalysisResult, Double>(
				null, 0.0);

		int nb=0;
		for (Objective[] toTest : pairsToTest) {
			nb++;
			System.out.println("2D analysis "+nb+"/"+pairsToTest.size());
			b.setObjective(toTest[0]);

			double lb = objectivesBounds.get(toTest[1])[0];
			double ub = objectivesBounds.get(toTest[1])[1];

			double deltaF = (ub - lb) / 200;

			Map<BioEntity, Double> entitiesValues = new HashMap<BioEntity, Double>();
			int i = 0;
			for (BioEntity ent : toTest[1].getEntities()) {

				entitiesValues.put(ent, toTest[1].getCoeffs()[i]);
				i++;
			}

			ReacAnalysis analysis = new ReacAnalysis(b, entitiesValues,
					toTest[1].getName(), lb, ub, deltaF, false);

			ReacAnalysisResult result = analysis.runAnalysis();

			// comparison between result and exp values

			for (Map<Objective, Double> m : experimentalValues) {

				double val1 = m.get(toTest[0]);
				double val2 = m.get(toTest[1]);

				result.addExpValue(val2, val1);

			}
			
			
			result.normalizeValues(lb, ub, !toTest[1].getMaximize(),
					objectivesBounds.get(toTest[0])[0],
					objectivesBounds.get(toTest[0])[1],
					!toTest[0].getMaximize());

			
			result.calculateScore();

			twoDResults.put(result, result.getScore());

			if (bestResult2D.getKey() == null) {
				bestResult2D = new SimpleEntry<ReacAnalysisResult, Double>(
						result, result.getScore());
			}

			else if (result.getScore() < bestResult2D.getValue()) {

				bestResult2D = new SimpleEntry<ReacAnalysisResult, Double>(
						result, result.getScore());
			}

			
		}

		//

		// 3D

		//

		List<Objective[]> tripletsToTest = makeObjsTriplets();

		// to store the scores
		Entry<TwoReacsAnalysisResult, Double> bestResult3D = new SimpleEntry<TwoReacsAnalysisResult, Double>(
				null, 0.0);

		nb=0;
		for (Objective[] toTest : tripletsToTest) {
			nb++;
			System.out.println("3D analysis "+nb+"/"+tripletsToTest.size());

			b.setObjective(toTest[0]);

			double lb1 = objectivesBounds.get(toTest[1])[0];
			double ub1 = objectivesBounds.get(toTest[1])[1];

			double deltaF1 = (ub1 - lb1) / 20;

			double lb2 = objectivesBounds.get(toTest[2])[0];
			double ub2 = objectivesBounds.get(toTest[2])[1];

			double deltaF2 = (ub2 - lb2) / 20;

			Map<BioEntity, Double> entitiesValues1 = new HashMap<BioEntity, Double>();
			int i = 0;
			for (BioEntity ent : toTest[1].getEntities()) {

				entitiesValues1.put(ent, toTest[1].getCoeffs()[i]);
				i++;
			}

			Map<BioEntity, Double> entitiesValues2 = new HashMap<BioEntity, Double>();
			i = 0;
			for (BioEntity ent : toTest[2].getEntities()) {

				entitiesValues2.put(ent, toTest[2].getCoeffs()[i]);
				i++;
			}

			TwoReacsAnalysis analysis = new TwoReacsAnalysis(b,
					toTest[1].getName(), entitiesValues1, entitiesValues2, lb1,
					ub1, deltaF1, toTest[2].getName(), lb2, ub2, deltaF2, false);

			TwoReacsAnalysisResult result = analysis.runAnalysis();

			// comparison between result and exp values

			for (Map<Objective, Double> m : experimentalValues) {

				double val1 = m.get(toTest[0]);
				double val2 = m.get(toTest[1]);
				double val3 = m.get(toTest[2]);

				result.addExpValue(val1, val2, val3);

			}

			result.normalizeValues(lb1, ub1, !toTest[1].getMaximize(), lb2,
					ub2, !toTest[2].getMaximize(),
					objectivesBounds.get(toTest[0])[0],
					objectivesBounds.get(toTest[0])[1],
					!toTest[0].getMaximize());
			
			result.calculateScore();
			
			threeDResults.put(result, result.getScore());

			if (bestResult3D.getKey() == null) {
				bestResult3D = new SimpleEntry<TwoReacsAnalysisResult, Double>(
						result, result.getScore());
			}

			else if (result.getScore() < bestResult3D.getValue()) {

				bestResult3D = new SimpleEntry<TwoReacsAnalysisResult, Double>(
						result, result.getScore());
			}

			

		}

		if (plotAll) {

			return new ParetoAnalysisResult(oneDResults, twoDResults,
					threeDResults);
		}
		else {
			
			Map<ReacAnalysisResult,Double> best2D = new HashMap<ReacAnalysisResult,Double>();
			best2D.put(bestResult2D.getKey(),bestResult2D.getValue());
			
			Map<TwoReacsAnalysisResult,Double> best3D = new HashMap<TwoReacsAnalysisResult,Double>();
			best3D.put(bestResult3D.getKey(),bestResult3D.getValue());
			
			return new ParetoAnalysisResult(oneDResults, best2D,
					best3D);
		}
	}

	/**
	 * 
	 * @return A list of all possible pairs of objectives.
	 */
	private List<Objective[]> makeObjsPairs() {

		List<Objective[]> pairsToTest = new ArrayList<Objective[]>();

		for (int i = 0; i < objectives.size() - 1; i++) {
			for (int j = i + 1; j < objectives.size(); j++) {
				pairsToTest.add(new Objective[] { objectives.get(i),
						objectives.get(j) });

			}
		}

		return pairsToTest;
	}

	/**
	 * 
	 * @return A list of all possible triplets of objectives.
	 */
	private List<Objective[]> makeObjsTriplets() {
		List<Objective[]> TripletsToTest = new ArrayList<Objective[]>();

		for (int i = 0; i < objectives.size() - 2; i++) {
			for (int j = i + 1; j < objectives.size() - 1; j++) {
				for (int k = j + 1; k < objectives.size(); k++) {
					TripletsToTest.add(new Objective[] { objectives.get(i),
							objectives.get(j), objectives.get(k) });

				}
			}
		}

		return TripletsToTest;
	}

	/**
	 * Parses the experimental file and gets the objective and experimental
	 * values.
	 */
	private void parseFile() {

		List<String[]> objStrings = new ArrayList<String[]>();
		List<double[]> expValues = new ArrayList<double[]>();

		BufferedReader in;
		try {
			in = new BufferedReader(new FileReader(filePath));

			String line;
			int nbLine = 0;
			while ((line = in.readLine()) != null) {
				nbLine++;
				if (line.startsWith("#") || line.equals("")) {
					continue;
				}

				if (!line.contains("MAX") && !line.contains("MIN")) {

					String[] stringValues = line.split("\\s");

					if (stringValues.length != objStrings.size()) {

						System.err
								.println("Error in experimental file line "
										+ nbLine
										+ ", there must be as many experimental values as there are objective functions ("
										+ objStrings.size() + ")");

						System.exit(0);

					}

					double[] values = new double[stringValues.length];

					int i = 0;

					for (String s : stringValues) {

						try {
							values[i] = Double.parseDouble(s);
						} catch (Exception e) {
							System.err
									.println("Error in experimental file line "
											+ nbLine
											+ ", experimental values must be numbers ...");

							System.exit(0);
						}
						i++;
					}
					expValues.add(values);

				} else {

					if (!objStrings.isEmpty()) {
						System.err.println("Error in experimental file line "
								+ nbLine);
						System.exit(0);
					}
					// we get the objectives

					Pattern pattern = Pattern
							.compile("(MAX|MIN)\\(([_a-zA-Z0-9\\+\\*\\-\\. ]+)\\)");

					Matcher matcher = pattern.matcher(line);

					while (matcher.find()) {

						objStrings.add(new String[] { matcher.group(1),
								matcher.group(2) });

					}

				}

			}

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// we add the objectives to the list
		for (String[] objString : objStrings) {

			boolean maximize = true;

			if (objString[0].equals("MIN")) {
				maximize = false;
			}

			String expr = objString[1];

			Objective obj = b.makeObjectiveFromString(expr, maximize,
					"Normalized " + expr);

			objectives.add(obj);
		}

		// we add the experimental values
		for (double[] expValue : expValues) {

			Map<Objective, Double> exp = new HashMap<Objective, Double>();
			for (int i = 0; i < expValue.length; i++) {

				exp.put(objectives.get(i), expValue[i]);

			}
			experimentalValues.add(exp);

		}

	}
}
