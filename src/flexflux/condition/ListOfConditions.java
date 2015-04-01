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
package flexflux.condition;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import flexflux.general.ConstraintType;
import flexflux.general.SimplifiedConstraint;
import flexflux.general.Vars;

public class ListOfConditions implements Iterable<Condition> {

	public ArrayList<Condition> conditions;
	public ArrayList<String> entities;

	public static final String fileFormat = "This must a tabulated network giving the state of each actor "
			+ "(0,1) in each condition: 1st column : the name of the condition 2nd column :"
			+ " the code of the condition following columns : the state of the actors (their names are in the header)";

	/**
	 * Constructor
	 */
	public ListOfConditions() {
		conditions = new ArrayList<Condition>();
		entities = new ArrayList<String>();
	}

	/**
	 * Read a file containing the description of the conditions.
	 * 
	 * All the constraints must belong to the same type : binary, double or
	 * integer
	 * 
	 * 
	 * @param conditionFile
	 *            : This must a tabulated network giving the state of each actor
	 *            (0,1) in each condition: 1st column : the name of the
	 *            condition 2nd column : the code of the condition following
	 *            columns : the state of the actors (their names are in the
	 *            header)
	 * 
	 * @param constraintType
	 *            : {@link ConstraintType} : type of the variables
	 * @param entities
	 *            : {@link ArrayList} of {@link String}. Will be filled during
	 *            the process : these are the entities found in the condition
	 *            file
	 * 
	 * @return false if there is a problem while reading the file
	 * 
	 */
	public boolean loadConditionFile(String conditionFile,
			ConstraintType constraintType) {

		Boolean flag = true;

		int ncol = 0;

		BufferedReader in = null;

		try {
			in = new BufferedReader(new FileReader(conditionFile));

			String line;

			int nbLine = 0;

			entities = new ArrayList<String>();

			while ((line = in.readLine()) != null) {
				if (line.startsWith("#") || line.equals("")) {
					nbLine++;
					continue;
				}

				String[] tab = line.split("\t");

				if (nbLine == 0) {
					/**
					 * Reads the header The first two columns are for the name
					 * and the code of the condition
					 */
					if (tab.length < 3) {
						System.err
								.println("Error in the header of the condition file : the number of columns\n"
										+ "must be greater than 3. The first two columns must correspond to the name and to\n"
										+ "the code of the conditions. The following columns must correspond to the identifier\n"
										+ "of the regulation network actors");
						return false;
					}

					/**
					 * Inits the number of columns to check the number of
					 * columns of the following lines
					 */
					ncol = tab.length;

					/**
					 * Fills the entity array
					 */
					for (int i = 2; i < ncol; i++) {
						entities.add(tab[i]);
					}

				} else {
					/**
					 * Following lines
					 */
					if (tab.length != ncol) {
						System.err.println("Bad number of columns line "
								+ nbLine);
						return false;
					}

					String conditionName = tab[0];
					String conditionCode = tab[1];

					Condition condition = new Condition(conditionCode,
							conditionName);

					/**
					 * Adds the constraints in the condition
					 */
					for (int i = 2; i < tab.length; i++) {

						String entityId = entities.get(i - 2);

						String valueStr = tab[i];

						if (valueStr.compareTo("NA") != 0) {

							Double value = null;

							try {
								value = Double.parseDouble(valueStr);

							} catch (NumberFormatException e) {
								System.err
										.println("Error in condition file line "
												+ nbLine
												+ " : the state value is not a number or NA");
								return false;
							}

							if (constraintType.equals(ConstraintType.BINARY)) {

								if (value != 1 && value != 0) {
									System.err
											.println("Error in condition file line "
													+ nbLine
													+ " : the state value is different than 0 or 1");
									return false;
								}
							}
							condition.addConstraint(entityId, value,
									constraintType);
						}

					}

					conditions.add(condition);
				}
				nbLine++;
			}
		} catch (FileNotFoundException e) {
			System.err.println(conditionFile + " not found");
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			System.err.println("Error while reading " + conditionFile);
			e.printStackTrace();
		}

		finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					System.err
							.println("Error while closing the condition file");
					e.printStackTrace();
				}
			}
		}

		return flag;

	}

	/**
	 * Adds a condition and adds entity ids in the list
	 * 
	 * @param condition
	 */
	public void add(Condition condition) {
		if (this.contains(condition)) {
			System.err.println("[FLEXFLUX WARNING] condition " + condition.code
					+ " - " + condition.name + " is duplicated");
		}
		for (SimplifiedConstraint c : condition.constraints.values()) {
			if (!entities.contains(c.entityId)) {
				entities.add(c.entityId);
			}
		}

		conditions.add(condition);
	}

	@Override
	public Iterator<Condition> iterator() {
		return conditions.iterator();
	}

	/**
	 * 
	 * @return the number of conditions
	 */
	public int size() {
		return conditions.size();
	}

	/**
	 * Test if a similar condition (same constraints) exist in conditions
	 * 
	 * @param c
	 * @return
	 */
	public Boolean contains(Condition c) {

		for (Condition cRef : conditions) {
			if (c.equals(cRef)) {
				
				if(Vars.verbose)
				{
					System.err.println(c.code+" equals to "+cRef.code);
				}
				
				return true;
			}
		}

		return false;

	}
	
	
	/**
	 * Check if a condition with the code code already exits in the conditions
	 * @param code
	 * @return
	 */
	public Boolean containsWithCode(String code)
	{
		for (Condition c : conditions) {
			if(c.code.compareTo(code)==0)
			{
				return true;
			}
		}
		
		return false;
		
	}
	

	/**
	 * Write conditions in a file
	 * 
	 * @param fileName
	 * @return
	 */
	public Boolean writeConditionFile(String fileName) {

		FileWriter fw = null;

		try {
			fw = new FileWriter(new File(fileName));

			/**
			 * To facilitate tests and reading
			 */
			Collections.sort(this.entities);
			
			// Header
			fw.write("conditionId\tconditionName");

			for (String entityId : this.entities) {
				fw.write("\t" + entityId);
			}

			fw.write("\n");

			// prints each condition
			for (Condition condition : this.conditions) {
				fw.write(condition.code + "\t" + condition.name);
				
				for (String entityId : this.entities) {
					String value = "0.0";
					if (condition.containsConstraint(entityId)) {
						value = condition.getConstraint(entityId).getValue()
								.toString();
					} else {
						value = "NA";
					}

					fw.write("\t" + value);
				}

				fw.write("\n");

			}

		} catch (IOException e) {
			e.printStackTrace();
			System.err
					.println("[FlexFlux Error] Problem while writing the file "
							+ fileName);
			return false;
		}

		finally {
			if (fw != null) {
				try {
					fw.close();
				} catch (IOException e) {
					e.printStackTrace();
					System.err.println("Problem while closing the file "
							+ fileName);
					return false;
				}
			}
		}

		return true;

	}
	
	
	/**
	 * @param code
	 * @return a condition with its code or null otherwise
	 */
	public Condition get(String code) {
		
		for(Condition c : this.conditions) 
		{
			if(c.code.compareTo(code)==0)
			{
				return c;
			}
		}
		
		return null;
		
	}
	

}
