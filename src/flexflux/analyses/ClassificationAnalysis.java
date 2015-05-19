package flexflux.analyses;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import parsebionet.biodata.BioChemicalReaction;
import parsebionet.biodata.BioEntity;
import parsebionet.biodata.BioGene;
import parsebionet.biodata.BioNetwork;
import parsebionet.utils.graphe.ScopeCompounds;
import flexflux.analyses.result.ConcurrentReactionsResult;
import flexflux.analyses.result.FBAResult;
import flexflux.analyses.result.FVAResult;
import flexflux.analyses.result.KOResult;
import flexflux.analyses.result.ClassificationResult;
import flexflux.general.Bind;
import flexflux.general.Vars;
import flexflux.objective.Objective;

public class ClassificationAnalysis extends Analysis {
	
	ClassificationResult classificationResult;
	
	
	private HashMap<String, BioEntity> remainReactions;

	private Objective mainObjective;

	private FVAResult fvaWithNoConstraintResult;

	Boolean performGeneAnalysis = true;

	public ClassificationAnalysis(Bind b, Boolean performGeneAnalysis) {
		super(b);

		this.performGeneAnalysis = performGeneAnalysis;

		mainObjective = b.getObjective();
		remainReactions = new HashMap<String, BioEntity>();
		for (BioChemicalReaction reaction : b.getBioNetwork()
				.getBiochemicalReactionList().values()) {
			remainReactions.put(reaction.getId(), (BioEntity) reaction);
		}
		
		classificationResult = new ClassificationResult();

	}

	@Override
	public ClassificationResult runAnalysis() {

		for (BioChemicalReaction reaction : b.getDeadReactions()) {
			classificationResult.deadReactions.put(reaction.getId(), reaction);
			for (BioGene gene : reaction.getListOfGenes().values()) {
				classificationResult.genesInvolvedInDeadReactions.put(gene.getId(), gene);
			}
		}

		// It's important to keep the order
		double opt = this.fba();

		if (Vars.verbose) {
			System.err.println("[PFBA] Initial number of reactions : "
					+ remainReactions.size());
		}

		if (opt != 0 && ! Double.isNaN(opt)) {

			if (performGeneAnalysis) {
				this.geneKoAnalysis();
			}

			this.reactionDeletionAnalysis();

			this.fvaWithNoConstraint();

			this.fvaWithConstraint();

			this.getConcurrentReactions();

			this.fvaWithFluxSumConstraint();

			this.scopeTest();

			for (String reactionId : b.getBioNetwork()
					.getBiochemicalReactionList().keySet()) {
				if (!classificationResult.essentialReactions.containsKey(reactionId)
						&& !classificationResult.zeroFluxReactions.containsKey(reactionId)
						&& !classificationResult.mleReactions.containsKey(reactionId)
						&& !classificationResult.concurrentReactions.containsKey(reactionId)
						&& !classificationResult.eleReactions.containsKey(reactionId)
						&& !classificationResult.objectiveIndependentReactions
								.containsKey(reactionId)
						&& !classificationResult.optimaReactions.containsKey(reactionId)) {
					System.err.println("[PFBA Error] Reaction " + reactionId
							+ " is not classified");
				}
			}

			if (performGeneAnalysis) {
				this.classifyGenes();
			}
		}
		else {
			if(Vars.verbose)
			{
				System.err.println("opt = "+opt+" : classification not done");
			}
		}

		classificationResult.objectiveValue = opt;

		classificationResult.network = b.getBioNetwork();

		return classificationResult;

	}

	/**
	 * Runs a fba to see if the optimal equals to 0
	 */
	private double fba() {

		Bind newBind = null;

		try {
			newBind = b.copy();
		} catch (ClassNotFoundException | NoSuchMethodException
				| SecurityException | InstantiationException
				| IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			e.printStackTrace();
			System.exit(1);
		}

		newBind.prepareSolver();

		FBAAnalysis a = new FBAAnalysis(newBind);
		FBAResult r = a.runAnalysis();

		return r.getObjValue();

	}

	/**
	 * find the essential reactions
	 */
	private void reactionDeletionAnalysis() {

		if (Vars.verbose) {
			System.err.println("[PFBA] Reaction Deletion Analysis");
		}

		Bind newBind = null;

		try {
			newBind = b.copy();
		} catch (ClassNotFoundException | NoSuchMethodException
				| SecurityException | InstantiationException
				| IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			e.printStackTrace();
			System.exit(1);
		}

		KOAnalysis koAnalysis = new KOAnalysis(newBind, 0,
				(HashMap<String, BioEntity>) remainReactions);

		KOResult koResult = koAnalysis.runAnalysis();

		classificationResult.essentialReactions = koResult.getEssentialEntities();
		
		classificationResult.unfeasibleKoReactions = koResult.getUnfeasibleKos();
		

		if (Vars.verbose) {
			System.err.println("essential Reactions : " + classificationResult.essentialReactions);
			System.err.println("unfeasible ko Reactions : " + classificationResult.unfeasibleKoReactions);
		}
		// We remove the essential and the unfeasible reactions from the remain reactions
		for (String id : classificationResult.essentialReactions.keySet()) {
			remainReactions.remove(id);
		}
		
		for (String id : classificationResult.unfeasibleKoReactions.keySet()) {
			remainReactions.remove(id);
		}

		if (Vars.verbose) {

			System.err.println("[PFBA] Number of essential reactions : "
					+ classificationResult.essentialReactions.size());
		}

	}

	/**
	 * Get the zero flux reactions
	 */
	private void fvaWithNoConstraint() {

		if (Vars.verbose) {
			System.err.println("[PFBA] FVA with no constraint");
		}

		Bind newBind = null;

		try {
			newBind = b.copy();
		} catch (ClassNotFoundException | NoSuchMethodException
				| SecurityException | InstantiationException
				| IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			e.printStackTrace();
			System.exit(1);
		}

		newBind.prepareSolver();

		double oldLibertyPercentage = Vars.libertyPercentage;

		// We remove the constraint on the objective
		Vars.libertyPercentage = 100;

		FVAAnalysis fvaAnalysis = new FVAAnalysis(newBind, remainReactions,
				null);

		fvaWithNoConstraintResult = fvaAnalysis.runAnalysis();
		
		classificationResult.zeroFluxReactions = fvaWithNoConstraintResult.getZeroFluxReactions();

		// We remove the zero flux reactions from the remain reactions
		for (String id : classificationResult.zeroFluxReactions.keySet()) {
			remainReactions.remove(id);
		}

		if (Vars.verbose) {

			System.err.println("[PFBA] Number of zero flux reactions : "
					+ classificationResult.zeroFluxReactions.size());
		}

		Vars.libertyPercentage = oldLibertyPercentage;

	}

	/**
	 * Get the optima Zero flux reactions
	 */
	private void fvaWithConstraint() {

		if (Vars.verbose) {
			System.err.println("[PFBA] FVA with constraint");
		}

		Bind newBind = null;

		try {
			newBind = b.copy();
		} catch (ClassNotFoundException | NoSuchMethodException
				| SecurityException | InstantiationException
				| IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			e.printStackTrace();
			System.exit(1);
		}

		newBind.prepareSolver();

		FVAAnalysis fvaAnalysis = new FVAAnalysis(newBind, remainReactions,
				null);

		FVAResult fvaResult = fvaAnalysis.runAnalysis();

		classificationResult.optimaZeroFluxReactions = fvaResult.getZeroFluxReactions();

		if (Vars.verbose) {
			System.err.println("[PFBA] Number of optimaZeroFluxReactions : "
					+ classificationResult.optimaZeroFluxReactions.size());
		}

		// We remove the zero flux reactions from the remain reactions
		for (String id : classificationResult.optimaZeroFluxReactions.keySet()) {
			remainReactions.remove(id);
		}

	}

	/**
	 * Two consecutive objectives : the main one and the flux sum minimisation
	 */
	private void fvaWithFluxSumConstraint() {

		if (Vars.verbose) {
			System.err.println("[PFBA] FVA with flux sum constraints");
		}

		Bind newBind = null;

		try {
			newBind = b.copy();
		} catch (ClassNotFoundException | NoSuchMethodException
				| SecurityException | InstantiationException
				| IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			e.printStackTrace();
			System.exit(1);
		}

		// Set minimum flux as objective
		BioEntity fluxSumEnt = newBind.createFluxesSummation();
		BioEntity fluxSumEntArray[] = { fluxSumEnt };
		double fluxSumCoeff[] = { 1.0 };

		Objective objMinFluxSum = new Objective(fluxSumEntArray, fluxSumCoeff,
				"fluxSum", false);

		newBind.constraintObjectives.add(newBind.getObjective());

		newBind.setObjective(objMinFluxSum);

		newBind.prepareSolver();

		FVAAnalysis fvaAnalysis = new FVAAnalysis(newBind, remainReactions,
				null);

		FVAResult fvaResult = fvaAnalysis.runAnalysis();

		classificationResult.minFluxZeroFluxReactions = fvaResult.getZeroFluxReactions();

		// fvaResult.plot();
		//
		// int i = 1;
		// while (i == 1) {
		// }

		// We remove the zero flux reactions from the remain reactions
		for (String id : classificationResult.minFluxZeroFluxReactions.keySet()) {
			remainReactions.remove(id);
		}

		if (Vars.verbose) {
			System.err.println("[PFBA] Number of minFluxZeroFluxReactions : "
					+ classificationResult.minFluxZeroFluxReactions.size());
		}

		classificationResult.optimaReactions = new HashMap<String, BioEntity>(remainReactions);

	}

	/**
	 * Split minFluxZeroFluxReactions in ele reactions and objective Independent
	 * reactions
	 */
	private void scopeTest() {

		if (Vars.verbose) {
			System.err.println("[PFBA] Scope test");
		}

		BioNetwork network = b.getBioNetwork();

		// Get the reactions involved in the main objective function
		Set<String> reactionsInObj = new HashSet<String>();

		for (BioEntity e : mainObjective.getEntities()) {
			String id = e.getId();
			if (network.getBiochemicalReactionList().containsKey(id)) {
				reactionsInObj.add(id);
			}
		}

		for (String id : classificationResult.minFluxZeroFluxReactions.keySet()) {

			if (!classificationResult.objectiveIndependentReactions.containsKey(id)) {

				BioChemicalReaction reaction = network
						.getBiochemicalReactionList().get(id);

				Set<String> substrates = reaction.getListOfProducts().keySet();

				// We do the scope from the substrates of the reaction
				ScopeCompounds scope = new ScopeCompounds(b.getBioNetwork(),
						substrates, b.getBioNetwork().getPhysicalEntityList()
								.keySet(), new HashSet<String>(),
						new HashSet<String>(), false, true);

				scope.compute();

				BioNetwork scopeNetwork = scope.getScopeNetwork();

				Boolean flag = false;

				for (String reactionInObj : reactionsInObj) {
					if (scopeNetwork.getBiochemicalReactionList().containsKey(
							reactionInObj)) {
						flag = true;
						break;
					}
				}

				if (flag) {
					classificationResult.eleReactions.put(id, classificationResult.minFluxZeroFluxReactions.get(id));
				} else {
					// We put also all the reactions that are in the scope in
					// the
					// objective independent reactions
					classificationResult.objectiveIndependentReactions.put(id,
							classificationResult.minFluxZeroFluxReactions.get(id));
					for (String idScope : scopeNetwork
							.getBiochemicalReactionList().keySet()) {
						if (classificationResult.minFluxZeroFluxReactions.containsKey(idScope)) {
							classificationResult.objectiveIndependentReactions.put(idScope,
									classificationResult.minFluxZeroFluxReactions.get(idScope));
						}
					}
				}
			}
		}

		if (Vars.verbose) {
			System.err.println("[PFBA] Number of ele reactions : "
					+ classificationResult.eleReactions.size());
			System.err.println("[PFBA] Number of independent reactions : "
					+ classificationResult.objectiveIndependentReactions.size());
		}
	}

	/**
	 * Browse optimaZeroFluxReactions, check if there are concurrent reactions
	 * We do this one in the last step because we change all the lower and upper
	 * bound of the internal reactions For each reaction set the lb and the ub
	 * to the fva min max found above and compute the optimal value of the main
	 * objective If the main objective equals to 0, then the reaction is a
	 * concurrent reaction
	 * 
	 */
	private void getConcurrentReactions() {

		if (Vars.verbose) {
			System.err.println("[PFBA] Get concurrent reactions");
			System.err.println("[PFBA] Number of reactions to treat : "
					+ classificationResult.optimaZeroFluxReactions.size());
			System.err.print("[");
		}

		Bind newBind = null;

		try {
			newBind = b.copy();
		} catch (ClassNotFoundException | NoSuchMethodException
				| SecurityException | InstantiationException
				| IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			e.printStackTrace();
			System.exit(1);
		}

		newBind.prepareSolver();

		ConcurrentReactionsAnalysis a = new ConcurrentReactionsAnalysis(
				newBind, classificationResult.optimaZeroFluxReactions, fvaWithNoConstraintResult);
		ConcurrentReactionsResult r = a.runAnalysis();

		classificationResult.concurrentReactions = r.getConcurrentReactions();
		classificationResult.mleReactions = r.getOtherReactions();

		// int n = 0;
		//
		// // Change all the internal reaction bounds to extreme values
		// for (BioEntity e : newBind.getSimpleConstraints().keySet()) {
		//
		// String id = e.getId();
		//
		// if
		// (newBind.getBioNetwork().getBiochemicalReactionList().containsKey(id))
		// {
		//
		// BioChemicalReaction reaction = newBind.getBioNetwork()
		// .getBiochemicalReactionList().get(id);
		//
		// // We limit the exchange reaction identification to one to
		// // one
		// // reactions
		// if (!(reaction.isExchangeReaction()
		// && reaction.getLeftParticipantList().size() == 1 && reaction
		// .getRightParticipantList().size() == 1)) {
		// // if the reaction is not an exchange reaction we change
		// // its
		// // lower and upper bound to FVA max values.
		// Constraint constraint = newBind.getSimpleConstraints().get(e);
		// constraint.setUb(Vars.maxUpperBound);
		//
		// if (reaction.isReversible()) {
		// constraint.setLb(Vars.minLowerBound);
		// } else {
		// constraint.setLb(0);
		// }
		// }
		// }
		//
		// }
		//
		// double sumFBA1 = 0.0;
		// double sumFBA2 = 0.0;
		// int n2 = 0;
		//
		// for (BioEntity e : optimaZeroFluxReactions.values()) {
		//
		// long startTime = System.currentTimeMillis();
		//
		// n++;
		//
		// String id = e.getId();
		//
		// BioChemicalReaction reaction = newBind.getBioNetwork()
		// .getBiochemicalReactionList().get(id);
		//
		// Double min = 0.0;
		// Double max = 0.0;
		//
		// Constraint constraint = newBind.getSimpleConstraints().get(e);
		//
		// double oldLb = constraint.getLb();
		// double oldUb = constraint.getUb();
		//
		// max = fvaWithNoConstraintResult.getMap().get(e)[1];
		// min = fvaWithNoConstraintResult.getMap().get(e)[0];
		//
		// constraint.setUb(max);
		// constraint.setLb(max);
		//
		// // We compute the optimal value of the main objective. If the
		// // optimal value equals to 0, the reaction is concurrent
		// newBind.prepareSolver();
		//
		// FBAAnalysis fbaAnalysis = new FBAAnalysis(newBind);
		// FBAResult res = fbaAnalysis.runAnalysis();
		//
		// long stopTime = System.currentTimeMillis();
		// long elapsedTime = stopTime - startTime;
		//
		// sumFBA1 += elapsedTime;
		//
		// // System.err.println("First FBA : "+elapsedTime);
		//
		// if (res.getObjValue().isNaN() || res.getObjValue() == 0) {
		// concurrentReactions.put(e.getId(), e);
		// } else {
		// if (reaction.isReversible()) {
		//
		// n2++;
		//
		// startTime = System.currentTimeMillis();
		//
		// constraint.setUb(min);
		// constraint.setLb(min);
		//
		// newBind.prepareSolver();
		//
		// fbaAnalysis = new FBAAnalysis(newBind);
		// res = fbaAnalysis.runAnalysis();
		//
		// stopTime = System.currentTimeMillis();
		// elapsedTime = stopTime - startTime;
		//
		// sumFBA2 += elapsedTime;
		//
		// // System.err.println("Second FBA : "+elapsedTime);
		//
		// if (res.getObjValue().isNaN() || res.getObjValue() == 0) {
		// concurrentReactions.put(e.getId(), e);
		// } else {
		// mleReactions.put(e.getId(), e);
		// }
		// } else {
		// mleReactions.put(e.getId(), e);
		// }
		// }
		//
		// constraint.setUb(oldUb);
		// constraint.setLb(oldLb);
		//
		// if (Vars.verbose) {
		// if (n % 10 == 0) {
		// System.err.print("*");
		// }
		// }
		//
		// }
		// if (Vars.verbose) {
		// System.err.println("First FBA mean time : " + sumFBA1 / n);
		// System.err.println("First FBA 2 mean time : " + sumFBA2 / n2);
		// }
		//
		// if (Vars.verbose) {
		// System.err.println("]");
		// System.err.println("[PFBA] Number of mle reactions : "
		// + mleReactions.size());
		// System.err.println("[PFBA] Number of concurrent reactions : "
		// + concurrentReactions.size());
		// }

	}

	/**
	 * Gene Ko analysis to identify essential genes
	 */
	private void geneKoAnalysis() {

		if (Vars.verbose) {
			System.err.println("[PFBA] Gene Ko Analysis");
		}
		Bind newBind = null;

		try {
			newBind = b.copy();
		} catch (ClassNotFoundException | NoSuchMethodException
				| SecurityException | InstantiationException
				| IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			e.printStackTrace();
			System.exit(1);
		}

		KOAnalysis koAnalysis = new KOAnalysis(newBind, 1, null);

		KOResult res = koAnalysis.runAnalysis();

		classificationResult.essentialGenes = res.getEssentialEntities();
		
		classificationResult.unfeasibleKoGenes = res.getUnfeasibleKos();

		return;

	}

	/**
	 * Classify the genes from the reaction classification and the KO results
	 */
	private void classifyGenes() {

		if (Vars.verbose) {
			System.err.println("[PFBA] Classify genes");
		}

		BioNetwork network = b.getBioNetwork();

		for (BioGene gene : network.getGeneList().values()) {

			String geneId = gene.getId();

			if (!classificationResult.essentialGenes.containsKey(geneId) && !classificationResult.unfeasibleKoGenes.containsKey(geneId)) {

				Set<String> reactionIds = network.getReactionsFromGene(geneId);

				Set<String> essentialReactionIds = classificationResult.essentialReactions.keySet();

				Set<String> intersection = new HashSet<String>(reactionIds);
				intersection.retainAll(essentialReactionIds);

				if (intersection.size() != 0) {
					classificationResult.redundantGenesForEssentialReactions.put(geneId, gene);
				} else {

					Set<String> optimaReactionIds = classificationResult.optimaReactions.keySet();

					intersection = new HashSet<String>(reactionIds);
					intersection.retainAll(optimaReactionIds);

					if (intersection.size() != 0) {
						classificationResult.optimaGenes.put(geneId, gene);
					} else {
						Set<String> concurrentReactionIds = classificationResult.concurrentReactions
								.keySet();
						intersection = new HashSet<String>(reactionIds);
						intersection.retainAll(concurrentReactionIds);

						if (intersection.size() != 0) {
							classificationResult.concurrentGenes.put(geneId, gene);
						} else {
							Set<String> mleReactionIds = classificationResult.mleReactions.keySet();
							intersection = new HashSet<String>(reactionIds);
							intersection.retainAll(mleReactionIds);
							if (intersection.size() != 0) {
								classificationResult.mleGenes.put(geneId, gene);
							} else {
								Set<String> eleReactionIds = classificationResult.eleReactions
										.keySet();
								intersection = new HashSet<String>(reactionIds);
								intersection.retainAll(eleReactionIds);
								if (intersection.size() != 0) {
									classificationResult.eleGenes.put(geneId, gene);
								} else {
									Set<String> oirReactionIds = classificationResult.objectiveIndependentReactions
											.keySet();
									intersection = new HashSet<String>(
											reactionIds);
									intersection.retainAll(oirReactionIds);
									if (intersection.size() != 0) {
										classificationResult.objectiveIndependentGenes.put(geneId,
												gene);
									} else {
										Set<String> zfReactionIds = classificationResult.zeroFluxReactions
												.keySet();
										intersection = new HashSet<String>(
												reactionIds);
										intersection.retainAll(zfReactionIds);
										if (intersection.size() != 0) {
											classificationResult.zeroFluxGenes.put(geneId, gene);
										} else if (classificationResult.genesInvolvedInDeadReactions
												.containsKey(geneId)) {
											classificationResult.deadGenes.put(geneId, gene);
										} else {
											System.err
													.println("[ERROR] PFBA: The gene "
															+ geneId
															+ " didn't find its category !!!");
										}
									}
								}
							}
						}
					}
				}

			}

		}

	}

	/**
	 * 
	 * @param deadReactions
	 */
	public void addDeadReactions(
			HashMap<String, BioChemicalReaction> deadReactions) {
		classificationResult.deadReactions.putAll(deadReactions);

		for (BioChemicalReaction reaction : deadReactions.values()) {
			for (BioGene gene : reaction.getListOfGenes().values()) {
				classificationResult.genesInvolvedInDeadReactions.put(gene.getId(), gene);
			}
		}
	}

}
