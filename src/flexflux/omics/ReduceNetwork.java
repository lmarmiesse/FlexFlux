package flexflux.omics;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import flexflux.analyses.Analysis;
import flexflux.analyses.FVAAnalysis;
import flexflux.analyses.result.AnalysisResult;
import flexflux.general.Bind;
import flexflux.general.Constraint;
import flexflux.general.CplexBind;
import flexflux.objective.Objective;
import parsebionet.biodata.BioChemicalReaction;
import parsebionet.biodata.BioEntity;
import parsebionet.biodata.BioNetwork;
import parsebionet.biodata.Flux;

public class ReduceNetwork {

	public static BioNetwork findCouples(BioNetwork metabNetwork) {

		Bind bind = new CplexBind();

		bind.setNetworkAndConstraints(metabNetwork);

		int nb = 0;

		for (String reacName : metabNetwork.getBiochemicalReactionList().keySet()) {

			BioChemicalReaction reac = metabNetwork.getBiochemicalReactionList().get(reacName);

			BioEntity[] entities = new BioEntity[1];
			double[] coeffs = new double[1];
			entities[0] = reac;
			coeffs[0] = 1;
			Objective obj = new Objective(entities, coeffs, "", true);

			bind.setObjective(obj);
			bind.prepareSolver();

			double max = bind.FBA(new ArrayList<Constraint>(), false, false).result;
			bind.setObjSense(false);
			double min = bind.FBA(new ArrayList<Constraint>(), false, false).result;

			System.out.println(reac.getId());
			System.out.println(min);
			System.out.println(max);

			bind.changeObjVarValue(reac, 0.0);

			Set<BioChemicalReaction> neighbors = getNeighbors(metabNetwork, reac);

			nb++;

			if (nb == 10) {
				break;
			}

		}

		return metabNetwork;

	}

	private static Set<BioChemicalReaction> getNeighbors(BioNetwork metabNetwork, BioChemicalReaction reac) {

		Set<BioChemicalReaction> neighbors = new HashSet<BioChemicalReaction>();

		for (String entName : reac.getListOfSubstrates().keySet()) {
			for (String reacName : reac.getListOfSubstrates().get(entName).getReactionsAsProduct().keySet()) {
				neighbors.add(reac.getListOfSubstrates().get(entName).getReactionsAsProduct().get(reacName));
			}
		}

		for (String entName : reac.getListOfProducts().keySet()) {
			for (String reacName : reac.getListOfProducts().get(entName).getReactionsAsSubstrate().keySet()) {
				neighbors.add(reac.getListOfProducts().get(entName).getReactionsAsSubstrate().get(reacName));
			}
		}

		System.out.println(neighbors.size());

		return null;
	}

	public static BioNetwork incrementTechnique(BioNetwork metabNetwork) {

		double bound = 100;

		for (String reacName : metabNetwork.getBiochemicalReactionList().keySet()) {

			BioChemicalReaction reac = metabNetwork.getBiochemicalReactionList().get(reacName);

			if (reac.isReversible()) {

				Flux flux = new Flux();
				flux.value = String.valueOf(bound * -1);
				bound++;

				reac.setLowerBound(flux);

				Flux flux2 = new Flux();
				flux2.value = String.valueOf(bound);
				bound++;

				reac.setUpperBound(flux2);
			} else {

				Flux flux = new Flux();
				flux.value = String.valueOf(bound);
				bound++;

				reac.setUpperBound(flux);

			}
		}

		Bind bind = new CplexBind();

		bind.setNetworkAndConstraints(metabNetwork);
		bind.prepareSolver();
		
		Analysis analysis = new FVAAnalysis(bind, null, null);
		AnalysisResult result = analysis.runAnalysis();
		
		
		
		result.plot();
		
		int i =5;
		
		while (i==5){
			
		}
		
		
		return null;

	}

}
