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
 * 18 avr. 2013 
 */
package flexflux.analyses;

import flexflux.analyses.result.CompFVAResult;
import flexflux.analyses.result.FVAResult;
import flexflux.general.Bind;

import java.util.HashMap;
import java.util.Map;

import parsebionet.biodata.BioEntity;

/**
 * 
 * Class to perform a comparison of two FVA's.
 * 
 * 
 * @author lmarmiesse 18 avr. 2013
 * 
 */
public class CompFVAAnalysis extends Analysis {

	/**
	 * 
	 * First FVA result.
	 * 
	 */
	private FVAResult res1;
	/**
	 * Second FVA result.
	 */
	private FVAResult res2;

	/**
	 * It is constructed with two FVA results and then compares them.
	 * 
	 * @param b
	 *            Bind to get the data from.
	 * @param res1
	 *            First FVA result.
	 * @param res2
	 *            Second FVA result.
	 */
	public CompFVAAnalysis(Bind b, FVAResult res1, FVAResult res2) {
		super(b);
		this.res1 = res1;
		this.res2 = res2;
	}

	public CompFVAResult runAnalysis() {

		Map<String, double[]> resultsMap = new HashMap<String, double[]>();

		CompFVAResult result = new CompFVAResult(resultsMap,
				res1.getObjValue(), res2.getObjValue());

		for (BioEntity entity : res1.getMap().keySet()) {

			resultsMap.put(entity.getId(),
					new double[] { res1.getMap().get(entity)[0],
							res1.getMap().get(entity)[1], 0.0, 0.0 });

		}

		for (BioEntity entity : res2.getMap().keySet()) {

			resultsMap.get(entity.getId())[2] = res2.getMap().get(entity)[0];
			resultsMap.get(entity.getId())[3] = res2.getMap().get(entity)[1];

		}

		return result;
	}
}
