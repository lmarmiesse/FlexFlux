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

/**
 * A utility class that defines a small molecule structure. An instance of this
 * class can also define additional information about a small molecule, such as
 * its chemical formula, names, and synonyms. This information is stored in the
 * slot STRUCTURE-DATA, in one of two formats: the CML format (see URL
 * www.xml-cml.org) or the SMILES format (see URL
 * www.daylight.com/dayhtml/smiles/). The STRUCTURE-FORMAT slot specifies which
 * format used is used. An example is the following SMILES string, which
 * describes the structure of glucose-6-phosphate:
 * 
 * 'C(OP(=O)(O)O)[CH]1([CH](O)[CH](O)[CH](O)[CH](O)O1)'.
 */

public class BioChemicalStructure extends BioUtility {

	private String structureData;
	private String structureFormat;
	
	
	public BioChemicalStructure(BioChemicalStructure in) {
		super(in);
		this.setStructureData(in.getStructureData());
		this.setStructureFormat(in.getStructureFormat());
	}
	
	/**
	 * @return Returns the structureData.
	 */
	public String getStructureData() {
		return structureData;
	}

	/**
	 * @param structureData The structureData to set.
	 */
	public void setStructureData(String structureData) {
		this.structureData = structureData;
	}

	/**
	 * @return Returns the structureFormat.
	 */
	public String getStructureFormat() {
		return structureFormat;
	}

	/**
	 * @param structureFormat The structureFormat to set.
	 */
	public void setStructureFormat(String structureFormat) {
		this.structureFormat = structureFormat;
	}
	
	

}
