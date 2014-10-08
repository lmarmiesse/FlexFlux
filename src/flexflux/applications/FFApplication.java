package flexflux.applications;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

public class FFApplication {

	
	public static boolean requiresSolver = true;
	
	/**
	 * Reads the objective file : each line corresponds to an objective First
	 * column : the name of the objective function Second column : its
	 * expression (ex : MAX(R_BIOMASS))
	 * 
	 * @return null if there is a problem while loading the file
	 * or a hashmap with key = objective name and value its expression
	 */

	public HashMap<String, String> loadObjectiveFile(String objectiveFile) {

		BufferedReader in = null;
		
		HashMap<String, String> objectives = new HashMap<String, String>();

		try {
			in = new BufferedReader(new FileReader(objectiveFile));

			String line;

			int nbLine = 0;

			while ((line = in.readLine()) != null) {
				if (line.startsWith("#") || line.equals("")) {
					nbLine++;
					continue;
				}
				
				nbLine++;

				String tab[] = line.split("\t");

				if (tab.length != 2) {
					System.err.println("Error line " + nbLine
							+ " does not contain two columns");
					return null;
				}

				String objExpression = tab[1];

				if (!objExpression.contains("MIN(")
						&& !objExpression.contains("MAX(")) {
					System.err
							.println("Objective function badly formatted line "
									+ nbLine + " (" + objExpression + ")");
					return null;
				}

				String objName = tab[0];

				objectives.put(objName, objExpression);
				
			}
		} catch (FileNotFoundException e) {
			System.err.println(objectiveFile + " not found");
			e.printStackTrace();
			return null;
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

		return objectives;

	}
	
	
	
	
}
