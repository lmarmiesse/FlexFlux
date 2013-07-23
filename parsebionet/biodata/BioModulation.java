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

import org.jdom.Element;

import parsebionet.sandbox.rdf.RdfQuery;



/**
 * A control interaction in which a physical entity modulates a catalysis
 * interaction. Biologically, most modulation interactions describe an
 * interaction in which a small molecule alters the ability of an enzyme to
 * catalyze a specific reaction. Instances of this class describe a pairing
 * between a modulating entity and a catalysis interaction. A separate
 * modulation instance should be created for each different catalysis that a
 * physical entity may modulate and for each different physical entity that may
 * modulate a catalysis instance. A typical modulation instance has a small
 * molecule as the controller entity and a catalysis instance as the controlled
 * entity. Examples of instances of this class include allosteric activation and
 * competitive inhibition of an enzyme's ability to catalyze a specific
 * reaction.
 */

public class BioModulation extends BioControl {
	
	/**
	 * @param e
	 * @param rdfQuery
	 */
	
	public BioModulation(BioModulation in) {
		super(in);
	}
	
}
