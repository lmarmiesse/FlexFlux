package flexflux.analyses.result.conditionComparison;

import java.util.HashMap;

import parsebionet.biodata.BioEntity;
import flexflux.analyses.result.KOResult;
import flexflux.condition.Condition;
import flexflux.general.Objective;

public class ConditionComparisonKoResult {

	public Objective objective;

	public Condition condition;

	public KOResult koResult;

	HashMap<String, BioEntity> essentialGenes;

	public ConditionComparisonKoResult(Objective o, Condition c, KOResult result) {
		this.objective = o;
		this.condition = c;
		this.koResult = result;

		if (result != null) {
			essentialGenes = result.getEssentialEntities();
		} else {
			essentialGenes = new HashMap<String, BioEntity>();
		}
	}

}
