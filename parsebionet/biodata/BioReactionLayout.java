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
package parsebionet.biodata;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;

import baobab.hypercyc.connection.JavacycPlus;


public class BioReactionLayout extends BioUtility {

	private String reactionId = null;
	private String direction = null;
	private ArrayList<String> left = new ArrayList<String>();
	private ArrayList<String> right = new ArrayList<String>();
	private String reactionLayout;
	private JavacycPlus cyc;
	
	public BioReactionLayout(BioReactionLayout in) {
		super(in);
		this.setReactionId(in.getReactionId());
		this.setDirection(in.getDirection());
		this.setLeft(new ArrayList<String>());
		this.getLeft().addAll(in.getLeft());
		this.setRight(new ArrayList<String>());
		this.getRight().addAll(in.getRight());
		this.setReactionLayout(in.getReactionLayout());
		this.setCyc(null);
	}

	public BioReactionLayout(String reactionLayoutIn, JavacycPlus cycIn) { // Build from the reactionLayout slot of Biocyc
//		 ex : (PGLUCISOM-RXN( :LEFT-PRIMARIES GLC-6-P)( :DIRECTION :L2R)( :RIGHT-PRIMARIES FRUCTOSE-6P))
		
		super();

		setCyc(cycIn);
		setReactionLayout(reactionLayoutIn);
		
		String REGEX = "^\\([# < ]*([^\\(]*)\\(";
		
		Pattern pattern = Pattern.compile(REGEX);
        Matcher matcher = pattern.matcher(reactionLayout);
        
        if(matcher.find()) {
        	String reactionId = matcher.group(1);
        	
        	if(cyc.instanceAllInstanceOfP("|Reactions|", reactionId))
        		this.setReactionId(reactionId);	
        	else {
        		return;
        	}
        }
        
        addCpd("left");
        addCpd("right");
        
        // Set Direction
        
        REGEX = "DIRECTION\\s(.+)\\)\\(\\s:RIGHT";
        
        pattern = Pattern.compile(REGEX);
        matcher = pattern.matcher(reactionLayout);
        
        if(matcher.find()){
        
        	String direction = matcher.group(1);
        	
        	this.setDirection(direction);
        }
        
	}
	
	/**
	 * Adds the primary compounds in the list.
	 * @param side : indicates if the compounds are on the left side or
	 */
	
	private void addCpd (String side) {
		
		String REGEX = "";
		ArrayList<String> compounds;
		
		if(side.compareTo("left") == 0) {
			compounds = this.getLeft();
			REGEX = "LEFT-PRIMARIES[# < ]*(\\S+) .*\\)\\(\\s:DIRECTION";
		}
		else {
			compounds = this.getRight();
			REGEX = "RIGHT-PRIMARIES[# < ]*(\\S+) .*\\)\\)$";
		}
		
		Pattern pattern = Pattern.compile(REGEX);
        Matcher matcher = pattern.matcher(reactionLayout);
        
        while(matcher.find()) {
        	String str = matcher.group(1);
        	
        	// if the list of compounds contains any generic compound 
        	
        	REGEX = ".*(\\|.*\\|).*";
        	
        	pattern = Pattern.compile(REGEX);
            Matcher matcher2 = pattern.matcher(str);
            
            while(matcher2.find()) {
            	String genericCompound = matcher2.group(1);
            	if(! left.contains(genericCompound)) {
        			this.addLeft(genericCompound);
        		}
            	
            }
            
            str.replaceAll(REGEX, "");
            
            // If the list of compounds contains any name of compound surrounded by ""
            
            REGEX = ".*(\\\".*\\\").*";
        	
        	pattern = Pattern.compile(REGEX);
            matcher2 = pattern.matcher(str);
            
            while(matcher2.find()) {
            	String genericCompound = matcher2.group(1);
            	
            	if(! compounds.contains(genericCompound)) {
        			compounds.add(genericCompound);
        		}
            	
            }
            
            str.replaceAll(REGEX, "");
            
        	String[] tab = str.split("\\s");
        	for (int j=0; j < tab.length; j++) {
        		
        		String cpdId = tab[j];
        		
        		if(! compounds.contains(cpdId)) {
        			compounds.add(cpdId);
        		}
        	}
        }
	}
	
	/**
	 * @return Returns the direction.
	 */
	public String getDirection() {
		return direction;
	}
	/**
	 * @param direction The direction to set.
	 */
	public void setDirection(String direction) {
		this.direction = direction;
	}
	/**
	 * @return Returns the left.
	 */
	public  ArrayList<String> getLeft() {
		return left;
	}
	/**
	 * @param left The left to set.
	 */
	public void setLeft(ArrayList<String>left) {
		this.left = left;
	}
	/**
	 * @return Returns the reactionId.
	 */
	public String getReactionId() {
		return reactionId;
	}
	/**
	 * @param reactionId The reactionId to set.
	 */
	public void setReactionId(String reactionId) {
		this.reactionId = reactionId;
	}
	/**
	 * @return Returns the right.
	 */
	public ArrayList<String> getRight() {
		return right;
	}
	/**
	 * @param right The right to set.
	 */
	public void setRight(ArrayList<String> right) {
		this.right = right;
	}
	
	public void addLeft(String cpd) {
		this.left.add(cpd);
	}
	
	public void addRight(String cpd) {
		this.right.add(cpd);
	}
	
	/**
	 * @return the cyc
	 */
	public JavacycPlus getCyc() {
		return cyc;
	}

	/**
	 * @param cyc the cyc to set
	 */
	public void setCyc(JavacycPlus cyc) {
		this.cyc = cyc;
	}

	/**
	 * @return the reactionLayout
	 */
	public String getReactionLayout() {
		return reactionLayout;
	}

	/**
	 * @param reactionLayout the reactionLayout to set
	 */
	public void setReactionLayout(String reactionLayout) {
		this.reactionLayout = reactionLayout;
	}
	
}
