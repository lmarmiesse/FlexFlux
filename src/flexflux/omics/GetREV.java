package flexflux.omics;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import flexflux.interaction.Interaction;
import flexflux.interaction.InteractionNetwork;
import flexflux.interaction.RelationFactory;
import flexflux.operation.OperationFactory;
import parsebionet.biodata.BioChemicalReaction;
import parsebionet.biodata.BioNetwork;
import parsebionet.io.Sbml2Bionetwork;

public class GetREV {

	public static Map<Sample, Map<BioChemicalReaction, Double>> reactionExpressionValues = new HashMap<Sample, Map<BioChemicalReaction, Double>>();
	public static String metabolicNetworkPath = "Data/AraGEM_Cobra_modif.xml";
	public static String omicsDataPath = "Data/MYBSEQ_TIME.txt";
	public static String outputFile = "/home/lmarmiesse/Documents/revRes.txt";
	
	
	// rev = reaction expression value

	public static void main(String[] args) {

		/**
		 * 1 => And : sum ; or : mean <br/>
		 * 2 => all mean
		 * 3 => And : sum ; or : min <br/>
		 */
		int revCalculationMethod = 1;

		Sbml2Bionetwork parser = new Sbml2Bionetwork(metabolicNetworkPath, false);
		BioNetwork bioNet = parser.getBioNetwork();

		InteractionNetwork intNet = new InteractionNetwork();
		intNet.addNetworkEntities(bioNet);
		intNet.gprInteractions(bioNet, new RelationFactory(), new OperationFactory());

		
		OmicsData omicsData = OmicsDataReader.loadOmicsData(omicsDataPath, intNet.getEntities());

		List<Sample> samples = omicsData.getSamples();
		
		for (Sample s : samples) {
			reactionExpressionValues.put(s, new HashMap<BioChemicalReaction, Double>());
		}
		
		for (Interaction inter : intNet.getGPRInteractions()) {

			BioChemicalReaction reac = (BioChemicalReaction) inter.getConsequence().getEntity();
			for (Sample s : samples) {
				double expr = inter.getCondition()
						.calculateRelationQuantitativeValue(omicsData.getDataValuesForSample(s), revCalculationMethod);
				reactionExpressionValues.get(s).put(reac, expr);
			}
		}
		
		
//		writeResult(samples);

	}
	
	
	public static void writeResult(List<Sample> samples){
		
		try {
			PrintWriter out = new PrintWriter(new File(outputFile));
			
			Set<BioChemicalReaction> reacs = reactionExpressionValues.get(samples.get(0)).keySet();
			
			
			for (Sample sample:samples){
				out.print(sample.getName()+"\t");
			}
			out.print("\n");
			
			for (BioChemicalReaction reac:reacs){
				
				out.print(reac.getId());
				for (Sample s : samples) {
					out.print("\t"+reactionExpressionValues.get(s).get(reac));
				}
				out.print("\n");
			}
			
			out.close();
		} catch (IOException e) {
			System.out.println("path " + outputFile + " is not a valid path, or file could not be created.");
		}
		
	}

}
