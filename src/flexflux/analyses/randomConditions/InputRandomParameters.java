package flexflux.analyses.randomConditions;

/**
 * 
 * 
 * 
 */
public class InputRandomParameters {

	/**
	 * id of the entity
	 */
	String id;

	/**
	 * Weight of the entity. Ex : if weight=2, the entity will appear twice in
	 * the randomised lists
	 */
	Integer weight;

	/**
	 * Value corresponding to the activation of the entity
	 */
	String activationValue;

	/**
	 * Value corresponding to the inhibition of the entity
	 */
	String inhibitionValue;

	public InputRandomParameters(String id, String inhibitionValue,
			String activationValue, int weight) {

		this.id = id;
		this.inhibitionValue = inhibitionValue;
		this.activationValue = activationValue;
		this.weight = weight;

	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Integer getWeight() {
		return weight;
	}

	public void setWeight(Integer weight) {
		this.weight = weight;
	}

	public String getActivationValue() {
		return activationValue;
	}

	public void setActivationValue(String activationValue) {
		this.activationValue = activationValue;
	}

	public String getInhibitionValue() {
		return inhibitionValue;
	}

	public void setInhibitionValue(String inhibitionValue) {
		this.inhibitionValue = inhibitionValue;
	}

}
