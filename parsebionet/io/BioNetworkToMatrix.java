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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import parsebionet.biodata.BioChemicalReaction;
import parsebionet.biodata.BioNetwork;
import parsebionet.biodata.BioPhysicalEntityParticipant;


public class BioNetworkToMatrix {
	
	private BioNetwork network;
	private HashMap<String, HashMap<String, Double>> matrix;
	
	public BioNetworkToMatrix(BioNetwork network) {
		
		this.setNetwork(network);
		
		this.setMatrix(new HashMap<String, HashMap<String,Double>>());
		
	}
	
	public void createMatrix() {
		
		Set<String> cpds = this.getNetwork().getPhysicalEntityList().keySet();
		HashMap<String, HashMap<String, Double>> matrix = this.getMatrix();
		
		for(String cpd : cpds) {
			matrix.put(cpd, new HashMap<String, Double>());
		}
		
		for(BioChemicalReaction reaction : this.getNetwork().getBiochemicalReactionList().values()) {
			HashMap<String, BioPhysicalEntityParticipant> LP = reaction.getLeftParticipantList();
			
			for(BioPhysicalEntityParticipant L : LP.values()) {
				String id = L.getPhysicalEntity().getId();
				String coeff = L.getStoichiometricCoefficient();
				
				matrix.get(id).put(reaction.getId(), Double.parseDouble("-"+coeff));
				
			}
			
			HashMap<String, BioPhysicalEntityParticipant> RP = reaction.getRightParticipantList();
			
			for(BioPhysicalEntityParticipant R : RP.values()) {
				String id = R.getPhysicalEntity().getId();
				String coeff = R.getStoichiometricCoefficient();
				
				matrix.get(id).put(reaction.getId(), Double.parseDouble(coeff));
				
			}
			
		}
	}
	
	public void writeInFile(String filename) throws IOException {
		
		FileWriter fw = new FileWriter(filename);
		
		HashMap<String, BioChemicalReaction> reactions = this.getNetwork().getBiochemicalReactionList();
		
		ArrayList<String> tab = new ArrayList<String>(reactions.keySet());
		
		// header
		
		for(int i = 0; i<tab.size(); i++) {
			fw.write("\t");
			fw.write(tab.get(i));
			
		}
		fw.write("\n");
		
		for(String cpd : this.getMatrix().keySet()) {
			
			fw.write(cpd);
			
			for(int i = 0; i<tab.size(); i++) {
				
				String reac = tab.get(i);
				
				HashMap<String, Double> map = this.getMatrix().get(cpd);
				
				if(map.containsKey(reac)) {
					fw.write("\t"+map.get(reac));
				}
				else {
					fw.write("\t0");
				}
			}
			
			fw.write("\n");
			
		}
		
		fw.close();
		
	}
	
	

	public BioNetwork getNetwork() {
		return network;
	}

	public void setNetwork(BioNetwork network) {
		this.network = network;
	}



	public HashMap<String, HashMap<String, Double>> getMatrix() {
		return matrix;
	}



	public void setMatrix(HashMap<String, HashMap<String, Double>> matrix) {
		this.matrix = matrix;
	}
	
	/**
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException  {
		
		String sbmlFile = args[0];
		String matrixFile = args[1];
		
		Sbml2Bionetwork reader = new Sbml2Bionetwork(sbmlFile);
		
		BioNetwork bn = reader.getBioNetwork();

		BioNetworkToMatrix b2m = new BioNetworkToMatrix(bn);
		
		b2m.createMatrix();
		
		b2m.writeInFile(matrixFile);
		
	}
	
	
	
	
}
