package flexflux.analyses;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import parsebionet.biodata.BioChemicalReaction;
import parsebionet.biodata.BioEntity;
import parsebionet.biodata.BioGene;
import parsebionet.biodata.BioNetwork;
import parsebionet.utils.graphe.ScopeCompounds;
import flexflux.analyses.result.FBAResult;
import flexflux.analyses.result.FVAResult;
import flexflux.analyses.result.KOResult;
import flexflux.analyses.result.PFBAResult;
import flexflux.general.Bind;
import flexflux.general.Constraint;
import flexflux.general.Objective;
import flexflux.general.Vars;

public class PFBAAnalysis extends Analysis {

	private HashMap<String, BioEntity> remainReactions;
	private HashMap<String, BioEntity> essentialReactions;
	private HashMap<String, BioEntity> zeroFluxReactions;
	private HashMap<String, BioEntity> optimaZeroFluxReactions;
	private HashMap<String, BioEntity> mleReactions;
	private HashMap<String, BioEntity> concurrentReactions;
	private HashMap<String, BioEntity> eleReactions;
	private HashMap<String, BioEntity> objectiveIndependantReactions;
	private HashMap<String, BioEntity> optimaReactions;
	private HashMap<String, BioEntity> minFluxZeroFluxReactions;

	private HashMap<String, BioEntity> essentialGenes;
	private HashMap<String, BioEntity> zeroFluxGenes;
	private HashMap<String, BioEntity> mleGenes;
	private HashMap<String, BioEntity> concurrentGenes;
	private HashMap<String, BioEntity> eleGenes;
	private HashMap<String, BioEntity> objectiveIndependantGenes;
	private HashMap<String, BioEntity> optimaGenes;

	private Objective mainObjective;

	private FVAResult fvaWithNoConstraintResult;

	Boolean performGeneAnalysis=true;
	

	public PFBAAnalysis(Bind b, Boolean performGeneAnalysis)  {
		super(b);
		
		this.performGeneAnalysis = performGeneAnalysis;

		mainObjective = b.getObjective();
		remainReactions = new HashMap<String, BioEntity>();
		for (BioChemicalReaction reaction : b.getBioNetwork()
				.getBiochemicalReactionList().values()) {
			remainReactions.put(reaction.getId(), (BioEntity) reaction);
		}

		essentialReactions = new HashMap<String, BioEntity>();
		zeroFluxReactions = new HashMap<String, BioEntity>();
		mleReactions = new HashMap<String, BioEntity>();
		concurrentReactions = new HashMap<String, BioEntity>();
		eleReactions = new HashMap<String, BioEntity>();
		objectiveIndependantReactions = new HashMap<String, BioEntity>();
		optimaReactions = new HashMap<String, BioEntity>();
		minFluxZeroFluxReactions = new HashMap<String, BioEntity>();

		essentialGenes = new HashMap<String, BioEntity>();
		zeroFluxGenes = new HashMap<String, BioEntity>();
		mleGenes = new HashMap<String, BioEntity>();
		eleGenes = new HashMap<String, BioEntity>();
		concurrentGenes = new HashMap<String, BioEntity>();
		objectiveIndependantGenes = new HashMap<String, BioEntity>();
		optimaGenes = new HashMap<String, BioEntity>();

	}

	@Override
	public PFBAResult runAnalysis() {

		// It's important to keep the order
		double opt = this.fba();
		if (opt != 0) {
			if(performGeneAnalysis) {
				this.geneKoAnalysis();
			}
			this.reactionDeletionAnalysis();
			this.fvaWithNoConstraint();
			this.fvaWithConstraint();
			this.getConcurrentReactions();
			this.fvaWithFluxSumConstraint();
			this.scopeTest();
			if(performGeneAnalysis) {
				this.classifyGenes();
			}
		}

		PFBAResult res = new PFBAResult();

		res.concurrentReactions = concurrentReactions;
		res.eleReactions = eleReactions;
		res.essentialReactions = essentialReactions;
		res.mleReactions = mleReactions;
		res.objectiveIndependentReactions = objectiveIndependantReactions;
		res.optimaReactions = optimaReactions;
		res.zeroFluxReactions = zeroFluxReactions;
		res.objectiveValue = opt;

		res.concurrentGenes = concurrentGenes;
		res.eleGenes = eleGenes;
		res.essentialGenes = essentialGenes;
		res.mleGenes = mleGenes;
		res.objectiveIndependentGenes = objectiveIndependantGenes;
		res.optimaGenes = optimaGenes;
		res.zeroFluxGenes = zeroFluxGenes;

		return res;

	}

	/**
	 * Runs a fba to see if the optimal equals to 0
	 */
	private double fba() {

		FBAAnalysis a = new FBAAnalysis(b);
		FBAResult r = a.runAnalysis();

		return r.getObjValue();

	}

	/**
	 * find the essential reactions
	 */
	private void reactionDeletionAnalysis() {

		KOAnalysis koAnalysis = new KOAnalysis(b, 0,
				(HashMap<String, BioEntity>) remainReactions);

		KOResult koResult = koAnalysis.runAnalysis();

		essentialReactions = koResult.getEssentialEntities();

		// We remove the reactions from the remain reactions
		for (String id : essentialReactions.keySet()) {
			remainReactions.remove(id);
		}
	}

	/**
	 * Get the zero flux reactions
	 */
	private void fvaWithNoConstraint() {

		double oldLibertyPercentage = Vars.libertyPercentage;

		// We remove the constraint on the objective
		Vars.libertyPercentage = 100;

		FVAAnalysis fvaAnalysis = new FVAAnalysis(b, remainReactions, null);

		fvaWithNoConstraintResult = fvaAnalysis.runAnalysis();

		zeroFluxReactions = fvaWithNoConstraintResult.getZeroFluxReactions();

		// We remove the zero flux reactions from the remain reactions
		for (String id : zeroFluxReactions.keySet()) {
			remainReactions.remove(id);
		}

		Vars.libertyPercentage = oldLibertyPercentage;

	}

	/**
	 * Get the mle reactions and the concurrent reactions
	 */
	private void fvaWithConstraint() {

		FVAAnalysis fvaAnalysis = new FVAAnalysis(b, remainReactions, null);

		FVAResult fvaResult = fvaAnalysis.runAnalysis();

		optimaZeroFluxReactions = fvaResult.getZeroFluxReactions();

		// We remove the zero flux reactions from the remain reactions
		for (String id : optimaZeroFluxReactions.keySet()) {
			remainReactions.remove(id);
		}

	}

	/**
	 * Two consecutive objectives : the main one and the flux sum minimisation
	 */
	private void fvaWithFluxSumConstraint() {

		
		// Set minimum flux as objective
		b.solverPrepared = false;
		BioEntity fluxSumEnt = b.createFluxesSummation();
		BioEntity fluxSumEntArray[] = { fluxSumEnt };
		double fluxSumCoeff[] = { 1.0 };

		Objective objMinFluxSum = new Objective(fluxSumEntArray, fluxSumCoeff,
				"fluxSum", false);


		b.setObjective(objMinFluxSum);

		b.constraintObjectives.add(this.mainObjective);

		b.prepareSolver();

		FVAAnalysis fvaAnalysis = new FVAAnalysis(b,
				remainReactions, null);

		FVAResult fvaResult = fvaAnalysis.runAnalysis();

		minFluxZeroFluxReactions = fvaResult.getZeroFluxReactions();

		// fvaResult.plot();
		//
		// int i = 1;
		// while (i == 1) {
		// }

		// We remove the zero flux reactions from the remain reactions
		for (String id : minFluxZeroFluxReactions.keySet()) {
			remainReactions.remove(id);
		}

		optimaReactions = new HashMap<String, BioEntity>(remainReactions);

	}

	/**
	 * Split minFluxZeroFluxReactions in ele reactions and objective independant
	 * reactions
	 */
	private void scopeTest() {

		BioNetwork network = b.getBioNetwork();

		// Get the reactions involved in the main objective function
		Set<String> reactionsInObj = new HashSet<String>();

		for (BioEntity e : mainObjective.getEntities()) {
			String id = e.getId();
			if (network.getBiochemicalReactionList().containsKey(id)) {
				reactionsInObj.add(id);
			}
		}

		for (String id : minFluxZeroFluxReactions.keySet()) {

			BioChemicalReaction reaction = network.getBiochemicalReactionList()
					.get(id);

			Set<String> substrates = reaction.getListOfSubstrates().keySet();

			// We do the scope from the substrates of the reaction
			ScopeCompounds scope = new ScopeCompounds(b.getBioNetwork(),
					substrates, new HashSet<String>(), new HashSet<String>(),
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
				eleReactions.put(id, minFluxZeroFluxReactions.get(id));
			} else {
				objectiveIndependantReactions.put(id,
						minFluxZeroFluxReactions.get(id));
			}
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

		// Change all the internal reaction bounds to extreme values
		for (BioEntity e : b.getSimpleConstraints().keySet()) {

			String id = e.getId();

			if (b.getBioNetwork().getBiochemicalReactionList().containsKey(id)) {

				BioChemicalReaction reaction = b.getBioNetwork()
						.getBiochemicalReactionList().get(id);

				// We limit the exchange reaction identification to one to one
				// reactions
				if (!(reaction.isExchangeReaction()
						&& reaction.getLeftParticipantList().size() == 1 && reaction
						.getRightParticipantList().size() == 1)) {
					// if the reaction is not an exchange reaction we change its
					// lower and upper bound to FVA max values.
					Constraint constraint = b.getSimpleConstraints().get(e);
					constraint.setUb(Vars.maxUpperBound);

					if (reaction.isReversible()) {
						constraint.setLb(Vars.minLowerBound);
					} else {
						constraint.setLb(0);
					}
				}
			}
		}

		for (BioEntity e : optimaZeroFluxReactions.values()) {

			String id = e.getId();

			BioChemicalReaction reaction = b.getBioNetwork()
					.getBiochemicalReactionList().get(id);

			Double min = 0.0;
			Double max = 0.0;

			Constraint constraint = b.getSimpleConstraints().get(e);

			double oldLb = constraint.getLb();
			double oldUb = constraint.getUb();

			/**
			 * The bioentities are not the same in the two analyses
			 */
			for (BioEntity iterEnt : fvaWithNoConstraintResult.getMap()
					.keySet()) {
				String idEnt = iterEnt.getId();
				if (idEnt.compareTo(id) == 0) {
					max = fvaWithNoConstraintResult.getMap().get(iterEnt)[1];
					min = fvaWithNoConstraintResult.getMap().get(iterEnt)[0];
					break;
				}
			}

			constraint.setUb(max);
			constraint.setLb(max);

			// We compute the optimal value of the main objective. If the
			// optimal value equals to 0, the reaction is concurrent
			b.prepareSolver();

			FBAAnalysis fbaAnalysis = new FBAAnalysis(b);
			FBAResult res = fbaAnalysis.runAnalysis();

			if (res.getObjValue() == 0) {
				concurrentReactions.put(e.getId(), e);
			} else {
				if (reaction.isReversible()) {
					constraint.setUb(min);
					constraint.setLb(min);

					b.prepareSolver();

					fbaAnalysis = new FBAAnalysis(b);
					res = fbaAnalysis.runAnalysis();

					if (res.getObjValue() == 0) {
						concurrentReactions.put(e.getId(), e);
					}
					else {
						mleReactions.put(e.getId(), e);
					}
				} else {
					mleReactions.put(e.getId(), e);
				}
			}

			constraint.setUb(oldUb);
			constraint.setLb(oldLb);

		}

	}

	/**
	 * Gene Ko analysis to identify essential genes
	 */
	private void geneKoAnalysis() {
		KOAnalysis koAnalysis = new KOAnalysis(b, 1, null);

		KOResult res = koAnalysis.runAnalysis();

		essentialGenes = res.getEssentialEntities();
		
		return;

	}

	/**
	 * Classify the genes from the reaction classification and the KO results
	 */
	private void classifyGenes() {

		BioNetwork network = b.getBioNetwork();

		for (BioGene gene : network.getGeneList().values()) {

			String geneId = gene.getId();

			if (!essentialGenes.containsKey(geneId)) {

				Set<String> reactionIds = network.getReactionsFromGene(geneId);

				Set<String> optimaReactionIds = optimaReactions.keySet();

				Set<String> intersection = new HashSet<String>(reactionIds);
				intersection.retainAll(optimaReactionIds);

				if (intersection.size() != 0) {
					optimaGenes.put(geneId, gene);
				} else {
					Set<String> concurrentReactionIds = concurrentReactions
							.keySet();
					intersection = new HashSet<String>(reactionIds);
					intersection.retainAll(concurrentReactionIds);

					if (intersection.size() != 0) {
						concurrentGenes.put(geneId, gene);
					} else {
						Set<String> mleReactionIds = mleReactions.keySet();
						intersection = new HashSet<String>(reactionIds);
						intersection.retainAll(mleReactionIds);
						if (intersection.size() != 0) {
							mleGenes.put(geneId, gene);
						} else {
							Set<String> eleReactionIds = eleReactions.keySet();
							intersection = new HashSet<String>(reactionIds);
							intersection.retainAll(eleReactionIds);
							if (intersection.size() != 0) {
								eleGenes.put(geneId, gene);
							} else {
								Set<String> oirReactionIds = objectiveIndependantReactions
										.keySet();
								intersection = new HashSet<String>(reactionIds);
								intersection.retainAll(oirReactionIds);
								if (intersection.size() != 0) {
									objectiveIndependantGenes.put(geneId, gene);
								} else {
									Set<String> zfReactionIds = zeroFluxReactions
											.keySet();
									intersection = new HashSet<String>(
											reactionIds);
									intersection.retainAll(zfReactionIds);
									if (intersection.size() != 0) {
										zeroFluxGenes.put(geneId, gene);
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
