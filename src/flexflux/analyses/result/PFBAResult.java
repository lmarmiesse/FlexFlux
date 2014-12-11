package flexflux.analyses.result;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.TreeMap;

import parsebionet.biodata.BioEntity;
import parsebionet.biodata.BioNetwork;
import parsebionet.io.BioNetwork2CytoscapeFile;
import parsebionet.io.BioNetworkToAttributeTable;
import parsebionet.utils.StringUtils;

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
	
	public BioNetwork network;
	

	@Override
	public void writeToFile(String path) {
		// TODO Auto-generated method stub

	}

	@Override
	public void plot() {
		// TODO Auto-generated method stub

	}
	
	/**
	 * Write sif file
	 * @return true if ok, false if problem
	 * @param prefix : the prefix of the filenames. Adds .sif for the network file and .attr for the attribute file
	 */
	public Boolean writeCytoscapeNetwork(String networkFile, Boolean sbmlEncode) {
		
		if(network == null)
		{
			return false;
		}
		
		/**
		 * writes the network
		 */
		BioNetwork2CytoscapeFile writer = new BioNetwork2CytoscapeFile(network, false, true, networkFile, sbmlEncode, false);
		try {
			writer.write();
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("[PFBA error]Error while writing the network File");
			return false;
		}
		
		
		return true;
	}
	
	/**
	 * Write classification attributes
	 * @return true if ok, false if problem
	 * @param prefix : the prefix of the filenames.
	 */
	public Boolean writeCytoscapeClassifAttribute(String attributeFile, Boolean sbmlEncode) {
		
		if(network == null)
		{
			return false;
		}
			
		FileWriter fw = null;
		
		try {
			fw = new FileWriter(new File(attributeFile));
			
			fw.write("classif (class=String)\n");
			
			/**
			 * ordering for easier testing
			 */
			TreeMap<String, String> classif = new TreeMap<String, String>(this.getReactionClassification());
			
			for(String id : classif.keySet()) {
				
				String codedId=id;
				if(sbmlEncode)
				{
					codedId = StringUtils.sbmlEncode(id);
				}
				
				String classe = classif.get(id);
				fw.write(codedId+" = "+classe+"\n");
			}
			
		} catch (IOException e) {
			
			e.printStackTrace();
			System.err.println("[PFBA error]Error while writing the attribute File");
			return false;
		}
		
		finally {
			if (fw != null) {
				try {
					fw.close();
				} catch (IOException e) {
					e.printStackTrace();
					System.err.println("[PFBA error]Error while closing the attribute File");
				}
			}
		}
		
		return true;
	}
	
	/**
	 * Write Cytoscape generic attributes
	 * @return true if ok, false if problem
	 */
	public Boolean writeCytoscapeGenericAttributes(String fileName, Boolean sbmlEncode) {
		
		if(network == null)
		{
			return false;
		}
			
		
		/**
		 * writes the general attributes
		 */
		BioNetworkToAttributeTable writerAttr = new BioNetworkToAttributeTable(network, fileName);
		
		try {
			writerAttr.writeAttributes(sbmlEncode);
		} catch (IOException e1) {
			e1.printStackTrace();
			System.err.println("[PFBA error]Error while writing the generic attribute file");
			return false;
		}
		
		return true;
	}
	
	
	/**
	 * Get reaction classification
	 * @return a HashMap for which the keys are reaction ids and the value the classification of the reaction
	 */
	public HashMap<String, String> getReactionClassification() {
		
		HashMap<String, String> classif = new HashMap<String, String>();
		
		for(String id : essentialReactions.keySet())
		{
			classif.put(id, "essential");
		}
		for(String id : zeroFluxReactions.keySet())
		{
			classif.put(id, "zeroFlux");
		}
		for(String id : mleReactions.keySet())
		{
			classif.put(id, "mle");
		}
		for(String id : concurrentReactions.keySet())
		{
			classif.put(id, "concurrent");
		}
		for(String id : eleReactions.keySet())
		{
			classif.put(id, "ele");
		}
		for(String id : objectiveIndependentReactions.keySet())
		{
			classif.put(id, "independent");
		}
		for(String id : optimaReactions.keySet())
		{
			classif.put(id, "optima");
		}
		for(String id : deadReactions.keySet())
		{
			classif.put(id, "dead");
		}
		
		return classif;
		
		
	}
	
	
	/**
	 * get a class of reaction of a class of genes
	 * @param fieldName
	 * @return a HashMap<String, BioEntity>
	 */
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
