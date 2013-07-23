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
 * Baobab Team
 * 01 d�c 06
 * Project : parseBioNet
 * Package : baobab.parseBioNet.applications
 * File : Cyc2File.java
 * 
 * Purpose :
 * Creates a sbml or a tabulated file from a local biocyc database
 * The user can choose 
 * - the code of the organism in the pathway tools.
 * - the type of reactionNodes in the network ha wants to deal with :
 * 		"metab-all"		= metab-smm rxns plus all other enzyme-catalyzed reactionNodes.
 *      "metab-smm"    	= metab-pathways reactionNodes plus all other reactionNodes not in pathways whose
 *         					substrates are all small molecules.
 *      "metab-pathways" = All reactionNodes found within metabolic pathways. May include a handfull 
 *       				 	of reactionNodes whose substrates are macromolecules, e.g., ACP.  Excludes transport reactionNodes.
 *      "enzyme"         = All enzyme-catalyzed reactionNodes of Species (instances of classes
 *        					EC-Reactions or Unclassified-Reactions)
 *       "small-molecule" = All reactionNodes whose substrates are all small molecules, as opposed to 
 *       					macromolecules. Excludes transport.
 *       "transport"      = All transport reactionNodes of Species
 *       "dna"            = All DNA Binding Reactions
 *       "sigtrans"       = Signal-transduction reactionNodes
 *       "protein-small-molecule-reaction"	= One substrate is a protein and one is a small 
 *       										molecule
 *       "protein-binds-dna"	= One substrate is a protein and one is a DNA-binding-site
 *       "protein-reaction"		= All substrates are proteins
 *       "trna-reaction"		= One substrate is a tRNA
 *       "spontaneous"			= Spontaneous reactionNodes
 *       "non-spontaneous"		= Non-spontaneous reactionNodes
 * - to keep only the reactionNodes which occurs in the metabolic pathways
 * and only the primary compounds (onlyPrimaries = 1) or not (onlyPrimaries = 0)
 * - to avoid all the generic compounds (ex : an aldehyde) (keepClassCompounds=0) or not 
 * 		(keepClassCompounds=1)
 * - the type of file : "sbml" or "tab" to save the network in a sbml file or in a tabulated file
 * - the filename where the network must be saved
 * 
 * Needs a local installation of the pathway tools and the libunixdomainsocket.so dynamic library
 * (available in the lib directory and which can be compiled with the MakeFile in the baoabcyc 
 * directory
 * 
 * Usage :
 * 
 * 
 */

package parsebionet.io;

import java.io.IOException;

import parsebionet.biodata.BioNetwork;

import baobab.hypercyc.connection.IServerPathwayToolsServices;
import baobab.hypercyc.connection.JavacycPlus;
import baobab.hypercyc.connection.ServerPathwayToolsServicesSingleton;

/**
 * @author ludo
 * 
 */
public class Cyc2File {

	private static BioNetwork bioNetwork;

	/**
	 * @param args
	 */
	public Cyc2File(String organism, String metabType, Boolean filter) {

		// org.apache.log4j.BasicConfigurator.configure();

		IServerPathwayToolsServices myConnection = ServerPathwayToolsServicesSingleton
				.getInstance();
		myConnection.start();

		// Connexion
		JavacycPlus cyc = new JavacycPlus(organism);
		
		// Loading the pgdb in a BioNetwork instance
		BioNetwork bioNetwork = new BioNetwork(cyc, metabType);
		
		if(filter) {
			bioNetwork.removeInfeasibleReactions();
		}
		
		setBioNetwork(bioNetwork);

		myConnection.stop();

	}

	/**
	 * @return Returns the bioNetwork.
	 */
	public BioNetwork getBioNetwork() {
		return bioNetwork;
	}

	/**
	 * @param bioNetwork
	 *            The bioNetwork to set.
	 */
	public void setBioNetwork(BioNetwork bn) {
		bioNetwork = bn;
	}

	public void writeToTabulatedFile(Boolean onlyPrimaries,
			Boolean keepHolderClassCompounds, String fileName) {
		BioNetwork2TabulatedFile writer = new BioNetwork2TabulatedFile(
				getBioNetwork(), onlyPrimaries, keepHolderClassCompounds,
				fileName);
		try {
			writer.write();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void writeToSbmlFile(Boolean onlyPrimaries,
			Boolean keepHolderClassCompounds, String fileName, Boolean extended) {
		BioNetwork2SbmlFile writer = new BioNetwork2SbmlFile(getBioNetwork(),
				onlyPrimaries, keepHolderClassCompounds, fileName, extended);
		try {
			writer.write();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws IOException {

		String organism = "BUCAIREVISED";
		String metabType = "metab-all";
		Boolean onlyPrimaries = false;
			
		Boolean keepHolderClassCompounds = true;
		Boolean filter = false;

		String fileType = "sbml";

		String fileName = "/home/ludo/symbiocyc/essai.xml";
		
		// Initializes the network
		System.err.println("Loading...");
		Cyc2File bn = new Cyc2File(organism, metabType, filter);
		
		String fileCof = "/home/ludo/Data/listeCofacteurs.txt";
		bn.getBioNetwork().markCofactors(fileCof);

		if (fileType.compareTo("sbml") == 0) {
			
			bn.writeToSbmlFile(onlyPrimaries, keepHolderClassCompounds,
					fileName, true);
		} else if (fileType.compareTo("tab") == 0) {
			bn.writeToTabulatedFile(onlyPrimaries, keepHolderClassCompounds,
					fileName);
		} else {
			System.err.println("Format not recognized");
			System.exit(0);
		}
		
	}

}
