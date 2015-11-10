package flexflux.omics;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import parsebionet.biodata.BioEntity;

public class OmicsDataReader {

	public static OmicsData loadOmicsData(String path, List<BioEntity> existingEntities) {

		OmicsData omicsData = new OmicsData();

		try {
			BufferedReader in = new BufferedReader(new FileReader(path));

			String line;
			// read the sample names
			line = in.readLine();
			String[] sampleNames = line.split("\t");

			if (sampleNames.length == 0) {
				System.err.println("Error : no sample names in first line of omics data file");
				System.exit(0);
			}

			for (String sampleName : sampleNames) {
				omicsData.addSample(new Sample(sampleName));
			}

			int nbLine = 2;
			while ((line = in.readLine()) != null) {
				if (line.equals("")) {
					nbLine++;
					continue;
				}

				String[] elements = line.split("\t");

				if (elements.length != omicsData.getSamples().size() + 1) {
					System.err.println("Error : there should be " + (omicsData.getSamples().size() + 1)
							+ " columns on line " + nbLine + ". " + elements.length + " were found.");
					System.exit(0);
				}

				String varName = elements[0];

				///// Check entity
				boolean alreadyExists = false;
				for (BioEntity ent : existingEntities) {
					if (ent.getId().equals(varName)) {
						alreadyExists = true;
						omicsData.addVariable(ent);
						break;
					}
				}
				if (!alreadyExists) {
					omicsData.addVariable(new BioEntity(varName));
				}

				/////

				String[] values = Arrays.copyOfRange(elements, 1, elements.length);
				BioEntity variable = omicsData.getVariable(varName);

				int index = 0;
				for (String strVal : values) {
					double val = Double.parseDouble(strVal);

					omicsData.addDataValue(omicsData.getSamples().get(index), variable, val);
					index++;
				}

				// System.out.println(line);

				nbLine++;
			}

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return omicsData;

	}

}
