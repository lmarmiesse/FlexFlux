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
 * 15 mai 2015
 */
package flexflux.analyses.result;

import java.util.HashMap;

import parsebionet.biodata.BioEntity;

public class ConcurrentReactionsResult extends AnalysisResult {

	protected HashMap<String, BioEntity> concurrentReactions;
	
	protected HashMap<String, BioEntity> otherReactions;
	
	
	public ConcurrentReactionsResult() {
		concurrentReactions = new HashMap<String, BioEntity>();
		otherReactions = new HashMap<String, BioEntity>();
	}
	
	
	public synchronized void addConcurrentReaction(String id, BioEntity reaction) {

		concurrentReactions.put(id, reaction);
	}
	
	public synchronized void addOtherReaction(String id, BioEntity reaction) {

		otherReactions.put(id, reaction);
	}
	

	public HashMap<String, BioEntity> getConcurrentReactions() {
		return concurrentReactions;
	}


	public HashMap<String, BioEntity> getOtherReactions() {
		return otherReactions;
	}


	@Override
	public void writeToFile(String path) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void plot() {
		// TODO Auto-generated method stub
		
	}
	
	
}
