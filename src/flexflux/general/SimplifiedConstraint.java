package flexflux.general;

/**
 * 
 * @author lcottret Created to create a constraint without creating bioEntities
 *         and that accepts only one value
 * 
 */
public class SimplifiedConstraint {

	public String entityId;
	public Double value;
	public ConstraintType type;

	public SimplifiedConstraint(String e, Double value, ConstraintType t) {
		
		this.entityId = e;
		this.value = value;
		this.type = t;
	}

	/**
	 * for comparing to another simple constraint
	 * 
	 * @param c2
	 * @return
	 */
	@Override
	public boolean equals(Object c2) {

		if (c2 == null) {
			return false;
		}
		if (c2 == this) {
			return true;
		}

		if (!(c2 instanceof SimplifiedConstraint))
			return false;

		SimplifiedConstraint c2_2 = (SimplifiedConstraint) c2;

		if (c2_2.entityId.compareTo(this.entityId) != 0) {
			return false;
		}
		if (c2_2.value.doubleValue() != this.value.doubleValue()) {
			return false;
		}
		if (c2_2.type.compareTo(this.type) != 0) {
			return false;
		}

		return true;
	}

	/**
	 * must override hashcode if we override equals
	 */
	@Override
	public int hashCode() {
		return entityId.hashCode() + value.hashCode() + type.hashCode();
	}

	@Override
	public String toString() {
		return entityId + " : " + value + " - " + type.toString();
	}

	/**
	 * getter value
	 * @return
	 */
	public Double getValue() {
		return value;
	}
	
	
}
