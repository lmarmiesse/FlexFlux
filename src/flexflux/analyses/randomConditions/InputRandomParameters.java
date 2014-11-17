package flexflux.analyses.randomConditions;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

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
	Double activationValue;

	/**
	 * Value corresponding to the inhibition of the entity
	 */
	Double inhibitionValue;

	public InputRandomParameters(String id, double inhibitionValue,
			double activationValue, int weight) {

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

	public Double getActivationValue() {
		return activationValue;
	}

	public void setActivationValue(Double activationValue) {
		this.activationValue = activationValue;
	}

	public Double getInhibitionValue() {
		return inhibitionValue;
	}

	public void setInhibitionValue(Double inhibitionValue) {
		this.inhibitionValue = inhibitionValue;
	}

}
