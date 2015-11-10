/*******************************************************************************
 * Copyright INRA
 * 
 *  Contact: ludovic.cottret@toulouse.inra.fr
 * 
 * 
 * This software is governed by the CeCILL license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 * 
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 *  In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 *  The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL license and that you accept its terms.
 ******************************************************************************/
/**
 * 7 mars 2013 
 */
package flexflux.interaction;



/**
 * 
 * Interaction is the base class to all interactions.
 * 
 * <p>
 * It contains two Relations :
 * </p>
 * <ul>
 * <li>The condition.
 * <li>The consequence.
 * </ul>
 * 
 * 
 * @author lmarmiesse 7 mars 2013
 * 
 */
public abstract class Interaction {

	/**
	 * 
	 * consequence Relation.
	 * 
	 */
	protected Unique consequence;
	/**
	 * condition Relation.
	 */
	protected Relation condition;
	
	protected String name;
	
	
	/**
	 * Links an interactions to when it begins and how long it lasts (only used
	 * in time-dependent alanyses)
	 */
	protected double[] timeInfos = new  double[] {0.0, 0.0};

	public Interaction(Unique consequence, Relation condition) {

		this.consequence = consequence;
		this.condition = condition;
		this.name = "";

	}
	
	public void setTimeInfos(double[] timeInfos){
		this.timeInfos = timeInfos;
	}
	
	public double[] getTimeInfos(){
		return timeInfos;
	}

	public Unique getConsequence() {
		return consequence;
	}

	public Relation getCondition() {
		return condition;
	}
	
	public void setName(String name){
		this.name = name;
	}
	
	public String getName(){
		return this.name;
	}

}
