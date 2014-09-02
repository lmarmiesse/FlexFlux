package flexflux.analyses.result.conditionComparison;

import java.util.HashMap;

import parsebionet.biodata.BioChemicalReaction;
import parsebionet.biodata.BioEntity;
import parsebionet.biodata.BioGene;
import parsebionet.biodata.BioNetwork;
import flexflux.condition.Condition;
import flexflux.general.Objective;

public class ConditionComparisonGeneResult {
	
	public Objective objective;

	public Condition condition;
	
	public ConditionComparisonKoResult koResult;
	public ConditionComparisonFvaResult fvaResult;
	
	HashMap<String, BioEntity> essentialGenes;
	HashMap<String, BioEntity> dispensableGenes;
	HashMap<String, BioEntity> deadGenes;
	
	
	public ConditionComparisonGeneResult(Objective o, Condition c,
			ConditionComparisonKoResult koResult, ConditionComparisonFvaResult fvaResult, BioNetwork network) {
		this.objective = o;
		this.condition = c;
		this.koResult = koResult;
		this.fvaResult = fvaResult;
		
		HashMap<String, BioChemicalReaction> allReactions = network.getBiochemicalReactionList();
		
		dispensableGenes = new HashMap<String, BioEntity>();
		deadGenes = new HashMap<String, BioEntity>();
		/**
		 * Get essential genes
		 */
		essentialGenes = koResult.essentialGenes;
		
		/**
		 * Get potentially used genes
		 */
		HashMap<String, BioEntity> potentiallyUsedReactions = fvaResult.potentiallyUsedReactions;
		HashMap<String, BioGene> potentiallyUsedGenes = new HashMap<String, BioGene>();
		
		for(String reactionId : potentiallyUsedReactions.keySet()) {
			BioChemicalReaction reaction = allReactions.get(reactionId);
			HashMap<String, BioGene> genes = reaction.getListOfGenes();
			potentiallyUsedGenes.putAll(genes);
		}
		System.err.println(condition.code+"\t"+objective.getName());
		System.err.println("Potentially used genes : "+potentiallyUsedGenes);
		System.err.println("Essential genes :"+essentialGenes);
		
		/**
		 * Get dispensable genes (potentially used but not dispensable)
		 */
		for(String geneId : potentiallyUsedGenes.keySet())
		{
			if(! essentialGenes.containsKey(geneId)) 
			{
				dispensableGenes.put(geneId, potentiallyUsedGenes.get(geneId));
			}
		}
		
		/**
		 * Get dead genes : never used in any reactions
		 */
		for(String geneId : network.getGeneList().keySet()) {
			if(! potentiallyUsedGenes.containsKey(geneId)) {
				deadGenes.put(geneId, network.getGeneList().get(geneId));
			}
		}
		
	}
	
}
