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
package flexflux.objective;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

public class ListOfObjectives {

	public HashMap<String, String> objectives;

	public final String fileFormat = "Each line corresponds to an objective. First column : "
			+ "name of the objective function. 2nd column : the expression of the objective function (ex : MAX(R_BIOMASS)";

	public ListOfObjectives() {
		objectives = new HashMap<String, String>();
	}

	/**
	 * Fills the objectives with an objective file : each line corresponds to an
	 * objective First column : the name of the objective function Second column
	 * : its expression (ex : MAX(R_BIOMASS))
	 * 
	 * @return false if there is a problem while loading the file or a hashmap
	 *         with key = objective name and value its expression
	 */
	public Boolean loadObjectiveFile(String objectiveFile) {
		BufferedReader in = null;

		HashMap<String, String> objs = new HashMap<String, String>();
		

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

				objs.put(objName, objExpression);

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
		
		objectives.putAll(objs);
		
		return true;
	}
	
	/**
	 * Returns the expression of an objective from its name
	 * @param objName
	 * @return an objective expression
	 */
	public String get(String objName) {
		
		return objectives.get(objName);
		
	}
	

}
