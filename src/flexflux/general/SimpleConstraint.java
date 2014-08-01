package flexflux.general;

/**
 * 
 * @author lcottret
 * Created to create a constraint without creating bioEntities
 *
 */
public class SimpleConstraint {
	
	public String entityId;
	public Double lb;
	public Double ub;
	public ConstraintType type;
	
	public SimpleConstraint(String e, Double lb, Double ub, ConstraintType t) {
		this.entityId = e;
		this.lb = lb;
		this.ub = ub;
		this.type = t;
	}
	
	
}
