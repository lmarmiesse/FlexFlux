package flexflux.omics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import parsebionet.biodata.BioEntity;

public class OmicsData {

	// private String name;
	// private String description;

	private List<Sample> samples = new ArrayList<Sample>();

	private List<BioEntity> variables = new ArrayList<BioEntity>();

	private HashMap<Sample, Map<BioEntity, Double>> data = new HashMap<Sample, Map<BioEntity, Double>>();

	public OmicsData() {

	}

	public void addSample(Sample sample) {

		for (Sample s : samples) {
			if (s.getName().equals(sample.getName())) {
				System.err.println("Error : duplicated sample name : " + sample.getName());
				System.exit(0);
			}
		}
		samples.add(sample);
	}

	public void addVariable(BioEntity ent) {

		for (BioEntity b : variables) {
			if (b.getId().equals(ent.getId())) {
				System.err.println("Error : duplicated variable name : " + ent.getId());
				System.exit(0);
			}
		}

		variables.add(ent);
	}

	public Sample getSample(String sampleName) {
		for (Sample s : samples){
			if (s.getName().equals(sampleName)){
				return s;
			}
		}
		return null;
	}
	
	public List<Sample> getSamples() {
		return samples;
	}

	public List<BioEntity> getVariables() {
		return variables;
	}
	
	public BioEntity getVariable(String varName) {
		for (BioEntity b : variables){
			if (b.getId().equals(varName)){
				return b;
			}
		}
		return null;
	}

	public void addDataValue(Sample sample, BioEntity variable, double val) {

		if (!data.containsKey(sample)) {
			data.put(sample, new HashMap<BioEntity, Double>());
		}

		data.get(sample).put(variable, val);

	}


	public double getDataValue(Sample sample, BioEntity variable) {

		return data.get(sample).get(variable);

	}
	
	public Map<BioEntity, Double> getDataValuesForSample(Sample sample) {

		return data.get(sample);

	}
	
	public void scaleVariable(BioEntity variable,int factor){
		
		for (Sample s : samples){
			data.get(s).put(variable,data.get(s).get(variable)/factor); 
		}
		
	}

}
