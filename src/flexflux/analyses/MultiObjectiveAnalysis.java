package flexflux.analyses;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import flexflux.analyses.result.AnalysisResult;
import flexflux.general.Bind;

public class MultiObjectiveAnalysis extends Analysis {

	public HashMap<String, String> objectives;
	public String objectiveFile;
	
	
	public MultiObjectiveAnalysis(Bind bind, String objectiveFile) {
		super(bind);
		objectives = new HashMap<String, String>();
		
		this.objectiveFile = objectiveFile;
		
	}

	@Override
	public AnalysisResult runAnalysis() {
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
	 * Reads the objective file : each line corresponds to an objective First
	 * column : the name of the objective function Second column : its
	 * expression (ex : MAX(R_BIOMASS))
	 * 
	 * @return false if there is a problem while loading the file
	 */

	public Boolean loadObjectiveFile() {
		Boolean flag = true;

		BufferedReader in = null;

		try {
			in = new BufferedReader(new FileReader(this.objectiveFile));

			String line;

			int nbLine = 0;

			while ((line = in.readLine()) != null) {
				if (line.startsWith("#") || line.equals("")) {
					nbLine++;
					continue;
				}

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

				objectives.put(objName, objExpression);
			}
		} catch (FileNotFoundException e) {
			System.err.println(objectiveFile + " not found");
			e.printStackTrace();
			return false;
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

		return flag;

	}
	
	
}
