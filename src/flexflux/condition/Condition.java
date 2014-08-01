package flexflux.condition;

import java.util.ArrayList;
import java.util.List;

import flexflux.general.ConstraintType;
import flexflux.general.SimpleConstraint;

public class Condition {

	public String name = "NA";
	public String code = "NA";

	/**
	 * 
	 */

	public List<SimpleConstraint> constraints = new ArrayList<SimpleConstraint>();

	/**
	 * Constructor
	 * 
	 * @param bind
	 */
	public Condition(String code, String name) {

		this.name = name;
		this.code = code;

	}

	/**
	 * Add a constraint to the condition If the entity does not exist in the
	 * bind create it
	 * 
	 * @param entityId
	 *            : the identifier of the entity
	 * @param lb
	 *            : lower bound
	 * @param ub
	 *            : upper bound
	 * @param type
	 *            : type of the constraint
	 */
	public Boolean addConstraint(String entityId, double lb, double ub,
			ConstraintType type) {

		SimpleConstraint constraint = new SimpleConstraint(entityId, lb, ub, type);
		
		constraints.add(constraint);
		
		return true;

	}
}

