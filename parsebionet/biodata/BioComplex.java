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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.jdom.Attribute;
import org.jdom.Element;

import parsebionet.sandbox.rdf.RdfConstants;
import parsebionet.sandbox.rdf.RdfQuery;



/**
 * A physical entity whose structure is comprised of other physical entities
 * bound to each other non-covalently, at least one of which is a macromolecule
 * (protein or RNA). Complexes must be stable enough to function as a biological
 * unit; in general, the temporary association of an enzyme with its
 * substrate(s) should not be considered or represented as a complex. A complex
 * is the physical product of an interaction (complex assembly), thus is not an
 * interaction itself. Examples of this class include complexes of multiple
 * protein monomers and complexes of proteins and small molecules.
 * 
 * NOTE: Complexes can be defined recursively to describe smaller complexes
 * within larger complexes, e.g., a participant in a complex can itself be a
 * complex.
 * 
 * NOTE: The boundaries on the size of complexes described by this class are not
 * defined here, although elements of the cell as large and dynamic as, e.g., a
 * mitochondrion would typically not be described using this class (later
 * versions of this ontology may include a cellularComponent class to represent
 * these). The strength of binding of the components is also not described.
 *  
 */

public class BioComplex extends BioPhysicalEntity {

	private HashMap<String, BioPhysicalEntityParticipant> componentList = new HashMap<String, BioPhysicalEntityParticipant>();
	private HashMap<String, BioPhysicalEntity> allComponentList = null;
	
	
	private Object organism;
	
	private Boolean isGeneticallyPossible = null;
	
	private HashMap <String, BioGene> listOfGenes = null;
	
	
	public BioComplex(String id) {
		super(id);
	}
	
	public BioComplex(String id,String name) {
		super(id, name);
	}

	/**
	 * @return Returns the organism
	 */
	public Object getOrganism() {
		return organism;
	}

	/**
	 * @param bioSource The organism to set : can be a string or a bioSource
	 */
	public void setOrganism(Object org) {
		this.organism = org;
	}

	/**
	 * @return Returns the component list
	 */
	public HashMap<String, BioPhysicalEntityParticipant> getComponentList() {
		return componentList;
	}
	
	/**
	 * Add a Component in the list
	 * @param a BioPhysicaélEntityParticipant to add
	 */
	public void addComponent(BioPhysicalEntityParticipant o) {
		this.componentList.put(o.getId(), o);
	}

	/**
	 * @return Returns the isGeneticallyPossible.
	 */
	public Boolean getIsGeneticallyPossible() {
		if(isGeneticallyPossible == null) {
			setIsGeneticallyPossible();
		}
		return isGeneticallyPossible;
	}
	
	/**
	 * 
	 */
	public void setIsGeneticallyPossible() {
		if(isGeneticallyPossible != null) {
			return;
		}
		else {
			
			if(componentList.size() == 0) {
				isGeneticallyPossible = false;
				return;
			}
			
			ArrayList<BioPhysicalEntityParticipant> liste = new ArrayList<BioPhysicalEntityParticipant>(componentList.values());
			
			for(int i = 0; i < liste.size(); i ++) {
				BioPhysicalEntity component = liste.get(i).getPhysicalEntity();
				String classe = component.getClass().getSimpleName();
				
				if(classe.compareTo("BioProtein") == 0) {
					if(((BioProtein)component).getGeneList().size() == 0) {
						isGeneticallyPossible = false;
						return;
					}
				}
				else if (classe.compareTo("BioComplex") == 0) {
					((BioComplex)component).setIsGeneticallyPossible();
					if(((BioComplex)component).getIsGeneticallyPossible() == false) {
						isGeneticallyPossible = false;
						return;
					}
				}
			}
			isGeneticallyPossible = true;
			return;
		}
	}

	/**
	 * @return Returns the listOfGenes.
	 */
	public HashMap<String, BioGene> getGeneList() {
		
		setGeneList();
		
		return listOfGenes;
	}

	/**
	 * Sets the list of the genes necessary to build the complex  
	 */
	public void setGeneList() {
		
		listOfGenes = new HashMap<String, BioGene>();
		
		for(Iterator iter = componentList.keySet().iterator() ; iter.hasNext(); ) {
			BioPhysicalEntity component = componentList.get(iter.next()).getPhysicalEntity();
			
			String classe = component.getClass().getSimpleName();
			
			if(classe.compareTo("BioProtein") == 0) {
				listOfGenes.putAll(((BioProtein)component).getGeneList());
			}
			else if(classe.compareTo("BioComplex") == 0) {
				if(component.getId().compareTo(this.getId()) != 0)
					listOfGenes.putAll(((BioComplex)component).getGeneList());
			}
		}
	
	}

	/**
	 * @return Returns the list of all components included in the complex : if the complex contains 
	 * another complex, it returns also thelist of components of the sub complex
	 */
	public HashMap<String, BioPhysicalEntity> getAllComponentList() {
		if(allComponentList == null) {
			this.setAllComponentList();
		}
		return allComponentList;
	}

	/**
	 * 
	 */
	public void setAllComponentList() {
		allComponentList = new HashMap<String, BioPhysicalEntity>();
		
		for(Iterator iter = componentList.keySet().iterator(); iter.hasNext(); ) {
			BioPhysicalEntity component = componentList.get(iter.next()).getPhysicalEntity();
			
			String classe = component.getClass().getSimpleName();
			
			if(classe.compareTo("BioComplex") == 0) {
				allComponentList.putAll(((BioComplex)component).getAllComponentList());
			}
			else {
				allComponentList.put(component.getId(), component);
			}
		}
	}
}
