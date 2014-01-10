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

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import parsebionet.biodata.BioChemicalReaction;
import parsebionet.biodata.BioCompartment;
import parsebionet.biodata.BioComplex;
import parsebionet.biodata.BioGene;
import parsebionet.biodata.BioNetwork;
import parsebionet.biodata.BioPathway;
import parsebionet.biodata.BioPhysicalEntity;
import parsebionet.biodata.BioPhysicalEntityParticipant;
import parsebionet.biodata.BioProtein;
import parsebionet.biodata.Flux;
import parsebionet.biodata.UnitDefinition;
import parsebionet.biodata.UnitSbml;
import parsebionet.utils.StringUtils;



public class BioNetwork2CytoscapeFile extends BioNetwork2File {

	// To encode SBML identifiers
	private Boolean encode = false;
	
	// Create reverse links when the reaction is reversible
	private Boolean rev = false;
	
	public BioNetwork2CytoscapeFile() {
		super(null, null, null, null);
	}


	public BioNetwork2CytoscapeFile(BioNetwork bioNetwork, Boolean onlyPrimaries, Boolean keepHolderClassCompounds, String fileName, Boolean encode, Boolean rev) {
		super(bioNetwork, onlyPrimaries, keepHolderClassCompounds, fileName);

		this.setEncode(encode);
		
		this.setRev(rev);
		
	}


	public void write() throws IOException{

		for(BioChemicalReaction reaction : this.getBioNetwork().getBiochemicalReactionList().values()) {

			Boolean isRev = false;
			if(reaction.getReversiblity().equalsIgnoreCase("reversible")) {
				isRev = true;
			}
			
			String reactionId = reaction.getId();
			
			if(this.getEncode())
				reactionId = StringUtils.sbmlEncode(reaction.getId());
			
			for(String left : reaction.getLeftList().keySet()) {
				
				
				if(this.getEncode())
					left=StringUtils.sbmlEncode(left);

				this.getWriter().write(left+"\treaction-reactant\t"+reactionId+"\n");

			}

			for(String right : reaction.getRightList().keySet()) {
				
				if(this.getEncode())
					right=StringUtils.sbmlEncode(right);

				this.getWriter().write(reactionId+"\treaction-product\t"+right+"\n");

			}
			
			if(isRev && rev) {
				for(String left : reaction.getLeftList().keySet()) {
					
					if(this.getEncode())
						left=StringUtils.sbmlEncode(left);
					
					this.getWriter().write(reactionId+"\treaction-product\t"+left+"\n");

				}

				for(String right : reaction.getRightList().keySet()) {
					
					if(this.getEncode())
						right=StringUtils.sbmlEncode(right);

					this.getWriter().write(right+"\treaction-reactant\t"+reactionId+"\n");

				}
			}
			

		}
		this.getWriter().close();
	}


	public Boolean getEncode() {
		return encode;
	}

	public void setEncode(Boolean encode) {
		this.encode = encode;
	}


	/**
	 * @return the rev
	 */
	public Boolean getRev() {
		return rev;
	}


	/**
	 * @param rev the rev to set
	 */
	public void setRev(Boolean rev) {
		this.rev = rev;
	}

	

}

