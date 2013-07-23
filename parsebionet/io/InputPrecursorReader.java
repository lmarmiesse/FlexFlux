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
package parsebionet.io;

import java.util.HashSet;
import java.util.Set;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import parsebionet.utils.StringUtils;
import parsebionet.utils.XMLUtils;



public class InputPrecursorReader {

	Set<String> inputCompounds = new HashSet<String>();
	Set<String> bootstrapCompounds = new HashSet<String>();
	Set<String> targetCompounds = new HashSet<String>();
	Set<String> precursorCompounds = new HashSet<String>();
	
	
	public InputPrecursorReader(String inputFile) {
		this.readInputFile(inputFile);
	}
	
	private void readInputFile(String inputFile)	
	{
		try{
			Document document = XMLUtils.open(inputFile);
			parseInputCompounds(document);
			parseBootstrapCompounds(document);
			parsePrecursorCompounds(document);
			parseTargetCompounds(document);
		}
		catch(Exception e){
			System.out.println(e.getMessage());
		}
	}
	
	private void parseInputCompounds(Document document){
        // get the element "input-compounds"
        NodeList inputs = document.getElementsByTagName("input-compounds");
        
        // Check if everything is OK.
        if (inputs == null)
        	throw new RuntimeException("Incorrect Format: Can't find node [input-compounds]");

        if (inputs.getLength() > 1)
        	throw new RuntimeException("Incorrect Format: More than one node [input-compounds]");
        
        // print all child elements from input-compounds
        NodeList listOfCompounds = inputs.item(0).getChildNodes();
        int compoundCount = listOfCompounds.getLength();
        for (int i = 0; i < compoundCount; i++) {
        	if( listOfCompounds.item(i) instanceof Element )
        	{
	            Element compound = (Element)listOfCompounds.item(i);
	            if("species".equals(compound.getNodeName()))
	            {
	            	// Gets the input compound from the file and transform the "id" in an valid SBML-format
	            	// Then puts it into the inputCompounds set 
	            	String id = compound.getAttribute("id");
	                
	            	this.getInputCompounds().add(id);
	            }
        	}
        }
	}
	
	private void parseBootstrapCompounds(Document document){
        // get the element "bootstrap-compounds"
        NodeList bootstraps = document.getElementsByTagName("bootstrap-compounds");
        
        // Check if everything is OK.
        if (bootstraps == null)
        	throw new RuntimeException("Incorrect Format: Can't find node [bootstrap-compounds]");

        if (bootstraps.getLength() > 1)
        	throw new RuntimeException("Incorrect Format: More than one node [bootstrap-compounds]");
        
        // print all child elements from bootstrap-compounds
        NodeList listOfCompounds = bootstraps.item(0).getChildNodes();
        int compoundCount = listOfCompounds.getLength();
        for (int i = 0; i < compoundCount; i++) {
        	if( listOfCompounds.item(i) instanceof Element )
        	{
	            Element compound = (Element)listOfCompounds.item(i);
	            if("species".equals(compound.getNodeName()))
	            {
	            	// Gets the bootstrap compound from the file and puts it into the boostrapCompounds set
	            	String id = compound.getAttribute("id");
	            	
	            	this.getBootstrapCompounds().add(id);
	            }
        	}
        }		
	}

	private void parsePrecursorCompounds(Document document){
        // get the element "precursor-compounds"
        NodeList precursors = document.getElementsByTagName("precursor-compounds");
        
        // Check if everything is OK.
        if (precursors == null)
        	throw new RuntimeException("Incorrect Format: Can't find node [precursor-compounds]");

        if (precursors.getLength() > 1)
        	throw new RuntimeException("Incorrect Format: More than one node [precursor-compounds]");
        
        // print all child elements from precursor-compounds
        NodeList listOfCompounds = precursors.item(0).getChildNodes();
        int compoundCount = listOfCompounds.getLength();
        for (int i = 0; i < compoundCount; i++) {
        	if( listOfCompounds.item(i) instanceof Element )
        	{
	            Element compound = (Element)listOfCompounds.item(i);
	            if("species".equals(compound.getNodeName()))
	            {
	            	// Gets the precursor compound from the file and set's it's precursor flag
	            	String id = compound.getAttribute("id");
	            	
	            	this.getPrecursorCompounds().add(id);
	            }
        	}
        }		
	}
	
	private void parseTargetCompounds(Document document){
        // get the element "target-compounds"
        NodeList targets = document.getElementsByTagName("target-compounds");
        
        // Check if everything is OK.
        if (targets == null)
        	throw new RuntimeException("Incorrect Format: Can't find node [target-compounds]");

        if (targets.getLength() > 1)
        	throw new RuntimeException("Incorrect Format: More than one node [target-compounds]");
        
        // print all child elements from target-compounds
        NodeList listOfCompounds = targets.item(0).getChildNodes();
        int compoundCount = listOfCompounds.getLength();
        for (int i = 0; i < compoundCount; i++) {
        	if( listOfCompounds.item(i) instanceof Element )
        	{
	            Element compound = (Element)listOfCompounds.item(i);
	            if("species".equals(compound.getNodeName()))
	            {
	            	// Gets the target compound from the file and puts it into the targetCompounds set
	            	String id = compound.getAttribute("id");
	            	
	            	this.getTargetCompounds().add(id);
	            }
        	}
        }				
	}

	public Set<String> getBootstrapCompounds() {
		return bootstrapCompounds;
	}

	public void setBootstrapCompounds(Set<String> bootstrapCompounds) {
		this.bootstrapCompounds = bootstrapCompounds;
	}

	public Set<String> getInputCompounds() {
		return inputCompounds;
	}

	public void setInputCompounds(Set<String> inputCompounds) {
		this.inputCompounds = inputCompounds;
	}

	public Set<String> getTargetCompounds() {
		return targetCompounds;
	}

	public void setTargetCompounds(Set<String> targetCompounds) {
		this.targetCompounds = targetCompounds;
	}

	public Set<String> getPrecursorCompounds() {
		return precursorCompounds;
	}

	public void setPrecursorCompounds(Set<String> precursorCompounds) {
		this.precursorCompounds = precursorCompounds;
	}
	
	
}
