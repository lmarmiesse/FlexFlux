package flexflux.thread;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

import flexflux.analyses.randomConditions.InputRandomParameters;
import flexflux.analyses.randomConditions.ListOfInputRandomParameters;
import flexflux.analyses.result.RandomConditionsResult;
import flexflux.condition.Condition;
import flexflux.general.ConstraintType;
import flexflux.general.Vars;
import flexflux.utils.maths.RandomGaussian;

public class ThreadRandomConditions extends Thread {

	/**
	 * Number of simulations to treat.
	 */
	private int todo;

	/**
	 * Contains all the numbers of the simulations to treat
	 */
	private Queue<Integer> simulationNumbers;
	
	/**
	 * parameters of the Gaussian distribution built to generate random number
	 * of activated inputs
	 */
	private double gaussianMean;
	private double gaussianStd;

	/**
	 * minimum number and the maximum number of activated inputs at each step of
	 * the simulation
	 */
	private int minInputs;
	private int maxInputs;

	/**
	 * Array indicating for inputs in InteractionNetwork one value of activation
	 * (activationValue), one value of inhibition (inhibitionValue) and a weight
	 * to constraint the random choice (weight) In this array, each element can
	 * appear several times depending on their weight
	 */
	private ListOfInputRandomParameters inputRandomParameterList;

	/**
	 * 
	 * Percentage of the analysis that is completed.
	 * 
	 */
	private static long percentage = 0;
	
	private RandomConditionsResult result;
	
	private ConstraintType type;
	
	
	/**
	 * 
	 * @param simulationNumbers
	 * @param gaussianMean
	 * @param gaussianStd
	 * @param minInputs
	 * @param maxInputs
	 * @param result
	 */
	public ThreadRandomConditions(Queue<Integer> simulationNumbers, double gaussianMean,
			double gaussianStd, int minInputs, int maxInputs,
			ListOfInputRandomParameters inputRandomParameterList, ConstraintType type,
			RandomConditionsResult result) {
		
		this.todo = simulationNumbers.size();
		this.simulationNumbers = simulationNumbers;
		this.result = result;
		this.gaussianMean = gaussianMean;
		this.gaussianStd = gaussianStd;
		this.minInputs = minInputs;
		this.maxInputs = maxInputs;
		this.type = type;
		this.inputRandomParameterList = inputRandomParameterList;
		percentage = 0;
		
	}
	
	
	/**
	 * Starts the thread.
	 */
	public void run() {

		Integer i;
		
		while ((i=simulationNumbers.poll()) != null) {

			Condition condition = new Condition(Integer.toString(i),
					"simu_condition_" + i);

			int n = 0;

			Set<String> activatedInputs = new HashSet<String>();

			Boolean flag = false;

			/**
			 * check the redondances of the conditions Be careful, the checking
			 * of the number of permutations / nb simulations must be done
			 * before to avoid infinite loops !!!
			 */
			while (!flag) {
				// We select a number of activated inputs in a truncated
				// gaussian
				// distribution.
				int numberActivatedInputs = randomGaussian(gaussianMean,
						gaussianStd, minInputs, maxInputs);

				// We build a new input parameter array in this way : we
				// duplicate
				// the inputs according to
				// the weight value and we randomize the order of the rows.
				ArrayList<InputRandomParameters> randomInputRandomParameters = randomInputs(inputRandomParameterList);

				n = 0;
				activatedInputs.clear();
				while (activatedInputs.size() < numberActivatedInputs) {
					InputRandomParameters entry = randomInputRandomParameters
							.get(n);

					if (!activatedInputs.contains(entry.getId())) {
						activatedInputs.add(entry.getId());
					}

					n++;
				}

				// In the new set of conditions, the value of selected inputs
				// corresponds
				// to their activationValue, the value of the other ones
				// corresponds
				// to their inhibitionValue
				Set<InputRandomParameters> inputSet = new HashSet<InputRandomParameters>(
						inputRandomParameterList.inputRandomParameterList);

				Set<String> inputsWithPositiveValue = new HashSet<String>();

				for (InputRandomParameters input : inputSet) {
					// if (bind.getInteractionNetwork().getEntity(input.getId())
					// == null) {
					//
					// BioEntity bioEntity = new BioEntity(input.getId(),
					// input.getId());
					//
					// bind.addRightEntityType(bioEntity, false, false);
					// }
					//
					// BioEntity e = bind.getInteractionNetwork().getEntity(
					// input.getId());
					//
					// Map<BioEntity, Double> constraintMap = new
					// HashMap<BioEntity, Double>();
					// constraintMap.put(e, 1.0);
					//
					// Constraint constraint = null;

					String valueStr;
					if (activatedInputs.contains(input.getId())) {
						valueStr = input.getActivationValue();
					} else {
						valueStr = input.getInhibitionValue();
					}

					if(valueStr.compareTo("NA") != 0)
					{
						Double value = null;
						try {
							value = Double
									.parseDouble(valueStr);
						} catch (NumberFormatException e) {
							System.err
									.println("Activation or inhibition value must be a double or NA");
							System.exit(1);
						}
					
					// constraint = new Constraint(constraintMap, value, value);

					if (value > 0) {
						inputsWithPositiveValue.add(input.getId());
					}

					condition.addConstraint(input.getId(), value,
							this.type);
					
					}

					// bind.addSimpleConstraint(e, constraint);
				}
				
				if(! result.containsCondition(condition)) {
					flag = true;
					result.addCondition(condition);
					
					result.addActivatedInputSet(activatedInputs);
					result.addNumberOfActivatedInputs(numberActivatedInputs);
					
					for (String inputId : activatedInputs) {
						result.incrementInputOccurences(inputId);
					}
				}
			}
			
			int percent = (int) Math
					.round(((double) todo - (double) simulationNumbers.size())
							/ (double) todo * 100);

			if (percent > percentage) {

				percentage = percent;
				if (Vars.verbose && percent % 2 == 0) {
					System.err.print("*");
				}
			}
			
		}
		
	}
	
	/**
	 * 
	 * @param gaussianMean
	 * @param gaussianStd
	 * @param min
	 *            : minimum for the random number
	 * @param max
	 *            : max for the random number
	 * @return a random integer selected in a truncated gaussian distribution
	 */
	private int randomGaussian(double gaussianMean, double gaussianStd,
			int min, int max) {

		if (min > max) {
			System.err.println("min > max");
			return -1;
		}

		if (min < 1) {
			System.err
					.println("the minimum number of inputs must be at least 1");
			return 1;
		}

		Boolean flag = false;

		RandomGaussian rg = new RandomGaussian();

		long val = -1;

		while (!flag) {
			val = rg.getRandomInteger(gaussianMean, Math.pow(gaussianStd, 2));
			if (val >= min && val <= max) {
				flag = true;
			}
		}

		return (int) val;

	}

	/**
	 * @return a new randomised array of input random parameters
	 */
	private ArrayList<InputRandomParameters> randomInputs(
			ListOfInputRandomParameters inputRandomParameterList) {

		ArrayList<InputRandomParameters> randomizedArray = new ArrayList<InputRandomParameters>();

		for (InputRandomParameters input : inputRandomParameterList) {
			int weight = input.getWeight();

			for (int i = 0; i < weight; i++) {
				randomizedArray.add(input);
			}
		}

		long seed = System.nanoTime();
		Collections.shuffle(randomizedArray, new Random(seed));

		return randomizedArray;

	}


	
	
}
