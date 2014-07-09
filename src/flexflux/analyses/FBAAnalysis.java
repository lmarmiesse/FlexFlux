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
 * 5 avr. 2013 
 */
package flexflux.analyses;

import flexflux.analyses.result.FBAResult;
import flexflux.general.Bind;
import flexflux.general.Constraint;
import flexflux.general.DoubleResult;
import flexflux.general.Vars;

import java.util.ArrayList;

/**
 * 
 * Class to run a simple FBA analysis.
 * 
 * @author lmarmiesse 5 avr. 2013
 * 
 */
public class FBAAnalysis extends Analysis {

	public FBAAnalysis(Bind b) {
		super(b);
	}

	public FBAResult runAnalysis() {
		
		
		FBAResult result = new FBAResult(b);

		
		DoubleResult objValue = b.FBA(new ArrayList<Constraint>(),true,true);
		
		if (objValue.flag!=0){
			
			System.out.println(objValue.result);
			
			System.out.println("Unfeasible");
			System.exit(0);
			
		}
		
		result.setObjValue(objValue.result);
		
		return (FBAResult) result;
	}

}
