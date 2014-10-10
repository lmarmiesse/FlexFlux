package flexflux.thread;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

import parsebionet.biodata.BioEntity;
import flexflux.analyses.era.InputRandomParameters;
import flexflux.analyses.result.ERAResult;
import flexflux.general.Bind;
import flexflux.general.Constraint;
import flexflux.general.DoubleResult;
import flexflux.general.Objective;
import flexflux.general.Vars;
import flexflux.utils.maths.RandomGaussian;

public class ThreadEra extends ResolveThread {

	/**
	 * Number of simulations to treat.
	 */
	private int todo;

	/**
	 * Contains all the numbers of the simulations to treat
	 */
	private Queue<Integer> simulationNumbers;

	/**
	 * ERA result
	 */
	private ERAResult result;

	/**
	 * Map [name objective<->expression objective]
	 */
	private HashMap<String, String> objectives;

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
	 * to constraint the random choice (weight)
	 * In this array, each element can appear several times depending on 
	 * their weight
	 */
	ArrayList<InputRandomParameters> inputRandomParameterList;

	/**
	 * 
	 * Percentage of the analysis that is completed.
	 * 
	 */
	private static long percentage = 0;

	public ThreadEra(Bind b, Queue<Integer> simulationNumbers,
			HashMap<String, String> objectives, double gaussianMean,
			double gaussianStd, int minInputs, int maxInputs,
			ArrayList<InputRandomParameters>inputRandomParameterList,
			ERAResult result) {

		super(b);
		this.todo = simulationNumbers.size();
		this.simulationNumbers = simulationNumbers;
		this.result = result;
		this.objectives = objectives;
		this.gaussianMean = gaussianMean;
		this.gaussianStd = gaussianStd;
		this.minInputs = minInputs;
		this.maxInputs = maxInputs;
		this.inputRandomParameterList = inputRandomParameterList;
		percentage = 0;

	}

	/**
	 * Starts the thread.
	 */
	public void run() {

		
		while (simulationNumbers.poll() != null) {

			
			// We select a number of activated inputs in a truncated gaussian
			// distribution.
			int numberActivatedInputs = randomGaussian(this.gaussianMean,
					this.gaussianStd, this.minInputs, this.maxInputs);
			
			result.addNumberOfActivatedInputs(numberActivatedInputs);
			
			// We build a new input parameter array in this way : we duplicate the inputs according to
		 	// the weight value and we randomize the order of the rows.
			ArrayList<InputRandomParameters> randomInputRandomParameters = randomInputs();
			
			Set<String> activatedInputs = new HashSet<String>();
			
			int n=0;
			while(activatedInputs.size() < numberActivatedInputs) {
				InputRandomParameters entry = randomInputRandomParameters.get(n);
				
				if(! activatedInputs.contains(entry.getId())) {
					activatedInputs.add(entry.getId());
					result.incrementInputOccurences(entry.getId());
				}
				
				n++;
			}
			
			// In the new set of conditions, the value of selected inputs corresponds
			// to their activationValue, the value of the other ones corresponds
			// to their inhibitionValue
			Set<InputRandomParameters> inputSet = new HashSet<InputRandomParameters>(inputRandomParameterList);
			
			Set<String> inputsWithPositiveValue = new HashSet<String>();
			
			
			for(InputRandomParameters input : inputSet)
			{
				if (bind.getInteractionNetwork().getEntity(input.getId()) == null) {

					BioEntity bioEntity = new BioEntity(input.getId(), input.getId());

					bind.addRightEntityType(bioEntity, false, false);
				}
				
				BioEntity e = bind.getInteractionNetwork().getEntity(input.getId());
				
				Map<BioEntity, Double> constraintMap = new HashMap<BioEntity, Double>();
				constraintMap.put(e, 1.0);

				Constraint constraint = null;
				
				Double value;
				if(activatedInputs.contains(input.getId())) {
					value = input.getActivationValue();
				}
				else {
					value = input.getInhibitionValue();
				}
				
				constraint = new Constraint(constraintMap, value, value);
				
				if(value > 0) {
					inputsWithPositiveValue.add(input.getId());
				}
				
				bind.getInteractionNetwork().addInitialConstraint(e, constraint);
			}
			
			bind.prepareSolver();
			
			// the value in  ObjSimCount for an objective function and the value for the activated inputs
		 	// in ObjInputMatrix are incremented if the new set of conditions activates it
			for(String objName : objectives.keySet())
			{
				this.setObjective(objName);
				
				DoubleResult res = bind.FBA(new ArrayList<Constraint>(), false, true);
				
				double value = Vars.round(res.result);
				
				if(value > 0)
				{
					result.incrementObjSimCount(objName);
					for(String inputId : inputsWithPositiveValue)
					{
						result.incrementObjInputMatrix(objName, inputId);
					}
				}
			}
			
//			Double prop = ((double) todo - (double) simulationNumbers.size()) / (double) todo;
//			
//			long percent = Math.round(prop * 100);
			
			int percent = (int) Math.round(((double) todo - (double) simulationNumbers.size()) / (double)todo
					* 100);
			
			if (percent > percentage) {

				percentage = percent;
				if (Vars.verbose && percent % 2== 0) {
					System.err.print("*");
				}
			}
		}
	}
	
	/**
	 * 
	 * Set a new objective 
	 */
	private void setObjective(String objName) {
		String expr = objectives.get(objName);
		String objString = (String) expr.subSequence(expr.indexOf("(") + 1,
				expr.indexOf(")"));

		Boolean maximize = false;

		if (expr.contains("MIN(")) {
			maximize = false;
		} else if (expr.contains("MAX(")) {
			maximize = true;
		}

		Objective obj = bind.makeObjectiveFromString(objString, maximize, objName);
		bind.setObjective(obj);
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
	private ArrayList<InputRandomParameters> randomInputs() {
		
		ArrayList<InputRandomParameters> randomizedArray = new ArrayList<InputRandomParameters>();
		
		for(InputRandomParameters input : this.inputRandomParameterList) {
			int weight = input.getWeight();
			
			for(int i=0; i<weight;i++) {
				randomizedArray.add(input);
			}
		}
		
		long seed = System.nanoTime();
		Collections.shuffle(randomizedArray, new Random(seed));
		
		return randomizedArray;
		
	}
	

}
