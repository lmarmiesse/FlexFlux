package flexflux.analyses.randomConditions;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;


public class ListOfInputRandomParameters implements Iterable<InputRandomParameters>{

	
	public ArrayList<InputRandomParameters> inputRandomParameterList;
	
	public ListOfInputRandomParameters () {
		inputRandomParameterList = new ArrayList<InputRandomParameters>();
	}
	
	
	
	/**
	 * Reads the file containing for each input its value of inhibition, of
	 * activation and its weight during the selection of the activated inputs
	 * during the simulation
	 * 
	 * @param inputFile
	 * @return true or false if problem
	 */
	public Boolean loadInputRandomParameterFile(
			String inputFile) {

		ArrayList<InputRandomParameters> list = new ArrayList<InputRandomParameters>();

		BufferedReader in = null;

		try {
			in = new BufferedReader(new FileReader(inputFile));

			String line;

			int nbLine = 0;

			while ((line = in.readLine()) != null) {
				if (line.startsWith("#") || line.equals("")) {
					nbLine++;
					continue;
				}
				
				nbLine++;
				
				String tab[] = line.split("\t");

				if (tab.length != 4) {
					System.err.println("Error in the input file :  line "
							+ nbLine + " does not contain four columns");
					return false;
				}

				String inputId = tab[0];
				String inhibitionValueStr = tab[1];
				String activationValueStr = tab[2];
				String weightStr = tab[3];

				double inhibitionValue;
				double activationValue;
				int weight;

				try {
					inhibitionValue = Double.parseDouble(inhibitionValueStr);
				} catch (NumberFormatException e) {
					System.err.println("Inhibition value badly formatted line "
							+ nbLine);
					return false;
				}

				try {
					activationValue = Double.parseDouble(activationValueStr);
				} catch (NumberFormatException e) {
					System.err.println("Activation value badly formatted line "
							+ nbLine);
					return false;
				}

				try {
					weight = Integer.parseInt(weightStr);
				} catch (NumberFormatException e) {
					System.err.println("Weight value badly formatted line "
							+ nbLine);
					return false;
				}

				InputRandomParameters inputRandomParameters = new InputRandomParameters(
						inputId, inhibitionValue, activationValue, weight);

				list.add(inputRandomParameters);
				

			}
		} catch (FileNotFoundException e) {
			System.err.println(inputFile + " not found");
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			System.err.println("Error while reading " + inputFile);
			e.printStackTrace();
			return false;
		}

		finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					System.err.println("Error while closing the input file");
					e.printStackTrace();
					return false;
				}
			}
		}

		inputRandomParameterList = list;
		
		
		return true;

	}
	
	/**
	 * 
	 * @return the number of inputRandomParameters
	 */
	public int size() {
		return this.inputRandomParameterList.size();
	}
	
	@Override
	public Iterator<InputRandomParameters> iterator() {
		return inputRandomParameterList.iterator();
	}
	
}
