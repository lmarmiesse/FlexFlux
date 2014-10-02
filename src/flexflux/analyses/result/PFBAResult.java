package flexflux.analyses.result;

import java.util.HashMap;

import parsebionet.biodata.BioEntity;

public class PFBAResult extends AnalysisResult {

	public HashMap<String, BioEntity> essentialReactions;
	public HashMap<String, BioEntity> zeroFluxReactions;
	public HashMap<String, BioEntity> mleReactions;
	public HashMap<String, BioEntity> concurrentReactions;
	public HashMap<String, BioEntity> eleReactions;
	public HashMap<String, BioEntity> objectiveIndependentReactions;
	public HashMap<String, BioEntity> optimaReactions;
	public HashMap<String, BioEntity> deadReactions;

	public HashMap<String, BioEntity> essentialGenes;
	public HashMap<String, BioEntity> zeroFluxGenes;
	public HashMap<String, BioEntity> mleGenes;
	public HashMap<String, BioEntity> concurrentGenes;
	public HashMap<String, BioEntity> eleGenes;
	public HashMap<String, BioEntity> objectiveIndependentGenes;
	public HashMap<String, BioEntity> optimaGenes;
	public HashMap<String, BioEntity> redundantGenesForEssentialReactions;
	public HashMap<String, BioEntity> deadGenes;

	public double objectiveValue = 0.0;

	@Override
	public void writeToFile(String path) {
		// TODO Auto-generated method stub

	}

	@Override
	public void plot() {
		// TODO Auto-generated method stub

	}

	public HashMap<String, BioEntity> get(String fieldName) {

		switch (fieldName) {
		case "essentialReactions":
			return essentialReactions;
		case "zeroFluxReactions":
			return zeroFluxReactions;
		case "mleReactions":
			return mleReactions;
		case "concurrentReactions":
			return concurrentReactions;
		case "eleReactions":
			return eleReactions;
		case "objectiveIndependentReactions":
			return objectiveIndependentReactions;
		case "optimaReactions":
			return optimaReactions;
		case "essentialGenes":
			return essentialGenes;
		case "zeroFluxGenes":
			return zeroFluxGenes;
		case "mleGenes":
			return mleGenes;
		case "concurrentGenes":
			return concurrentGenes;
		case "eleGenes":
			return eleGenes;
		case "objectiveIndependentGenes":
			return objectiveIndependentGenes;
		case "optimaGenes":
			return optimaGenes;
		case "redundantGenesForEssentialReactions":
			return redundantGenesForEssentialReactions;
		case "deadGenes":
			return deadGenes;
		case "deadReactions":
			return deadReactions;
		default:
			System.err.println("This field does not exist");
			return null;
		}

	}

}
