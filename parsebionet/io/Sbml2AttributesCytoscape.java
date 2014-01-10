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

import java.io.FileWriter;
import java.io.IOException;

import parsebionet.biodata.BioNetwork;
import parsebionet.utils.StringUtils;


public class Sbml2AttributesCytoscape {
	
	public Sbml2AttributesCytoscape(String sbmlFile, String fileOut, Boolean encodeSbml) throws IOException {
		Sbml2Bionetwork reader = new Sbml2Bionetwork(sbmlFile);
		
		BioNetwork bn = reader.getBioNetwork();
		
		FileWriter fw = new FileWriter(fileOut);
		
		fw.write("Equation\n");
		
		if(encodeSbml)
			fw.write(bn.networkAsString(encodeSbml));
		else
			fw.write(bn.networkAsString());
		
		fw.close();
		
	}
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		
		String file = "/home/ludo/Data/BioCyc12.5/WithClasses/TRYPANO/allCpdsMetabSmmReactionsCompounds.xml";
		
		String fileOut = "/home/ludo/work/trypano_Equations.attr";
		
		new Sbml2AttributesCytoscape(file, fileOut, true);
		

	}

}
