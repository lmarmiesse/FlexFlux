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
 * 7 mai 2013 
 */
package flexflux.analyses;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;

import flexflux.analyses.result.FVAResult;
import parsebionet.biodata.BioEntity;

/**
 * 
 * Class used to compare two phenotype phases.
 * 
 * @author lmarmiesse 7 mai 2013
 * 
 */
public class PhenotypicPhaseComparator {

	private Map<Integer, List<BioEntity>> newEssentialEntities = new HashMap<Integer, List<BioEntity>>();
	private Map<Integer, List<BioEntity>> noLongerEssentialEntities = new HashMap<Integer, List<BioEntity>>();

	private Map<Integer, JPanel> newEssentialEntitiesPanel = new HashMap<Integer, JPanel>();
	private Map<Integer, JPanel> noLongerEssentialEntitiesPanel = new HashMap<Integer, JPanel>();

	public PhenotypicPhaseComparator(Map<Integer, FVAResult> fvaResults) {

		Map<Integer, List<BioEntity>> essentialReactionsMap = new HashMap<Integer, List<BioEntity>>();

		for (Integer index : fvaResults.keySet()) {

			List<BioEntity> essentialReactions = fvaResults.get(index)
					.getEssentialReactions();

			essentialReactionsMap.put(index, essentialReactions);

		}

		// maps to display differences between phases

		for (int groupIndex = 1; groupIndex <= essentialReactionsMap.size(); groupIndex++) {

			newEssentialEntities.put(groupIndex, new ArrayList<BioEntity>());

			noLongerEssentialEntities.put(groupIndex,
					new ArrayList<BioEntity>());

			List<BioEntity> essentialReactions = essentialReactionsMap
					.get(groupIndex);

			if (groupIndex > 1) {
				List<BioEntity> essentialReactionsPreviousStep = essentialReactionsMap
						.get(groupIndex - 1);

				// we only add reactions not present in the previous step
				for (BioEntity ent : essentialReactions) {
					if (!essentialReactionsPreviousStep.contains(ent)) {
						newEssentialEntities.get(groupIndex).add(ent);
					} else {

						double val1 = fvaResults.get(groupIndex)
								.getValuesForEntity(ent)[0];
						double val2 = fvaResults.get(groupIndex - 1)
								.getValuesForEntity(ent)[0];

						// if they dont have the same sign
						if (val1 * val2 < 0) {
							newEssentialEntities.get(groupIndex).add(ent);
						}
					}
				}

				for (BioEntity ent : essentialReactionsPreviousStep) {
					if (!essentialReactions.contains(ent)) {
						noLongerEssentialEntities.get(groupIndex).add(ent);
					}
				}

			} else {
				newEssentialEntities.get(groupIndex).addAll(essentialReactions);
			}

			newEssentialEntitiesPanel.put(
					groupIndex,
					fvaResults.get(groupIndex).getReactionsPanel(
							newEssentialEntities.get(groupIndex)));

			noLongerEssentialEntitiesPanel.put(
					groupIndex,
					fvaResults.get(groupIndex).getReactionsPanel(
							noLongerEssentialEntities.get(groupIndex)));

		}
	}

	public Map<Integer, JPanel> getNewEssentialEntitiesPanel() {
		return newEssentialEntitiesPanel;
	}

	public Map<Integer, JPanel> getNoLongerEssentialEntitiesPanel() {
		return noLongerEssentialEntitiesPanel;
	}

	public Map<Integer, List<BioEntity>> getNewEssentialEntities() {
		return newEssentialEntities;

	}

	public Map<Integer, List<BioEntity>> getNoLongerEssentialEntities() {
		return noLongerEssentialEntities;

	}

}
