package flexflux.interaction;

import flexflux.general.Constraint;

import java.util.Map;

import parsebionet.biodata.BioEntity;

public class UndeterminedUnique extends Unique {

	public UndeterminedUnique(BioEntity entity) {
		super(entity);
	
	}

	public boolean isTrue(Map<BioEntity, Constraint> simpleConstraints) {

		return false;

	}
	
	public String toString() {
		String s = "Undetermined";
		return s;
	}
	
}
