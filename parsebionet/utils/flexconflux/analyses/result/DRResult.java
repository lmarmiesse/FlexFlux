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
 * 8 avr. 2013 
 */
package parsebionet.utils.flexconflux.analyses.result;

import java.awt.BorderLayout;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;

import org.jfree.ui.RefineryUtilities;

import parsebionet.biodata.BioEntity;
import parsebionet.utils.flexconflux.Bind;

/**
 * 
 * Class representing the result of dead reactions analysis.
 * 
 * @author lmarmiesse 8 avr. 2013
 * 
 */
public class DRResult extends FVAResult {

	public DRResult(double objValue, Bind b) {
		super(objValue);
	}

	public void writeToFile(String path) {

		try {
			PrintWriter out = new PrintWriter(new File(path));
			out.println("Dead Reactions result\n");

			for (BioEntity entity : map.keySet()) {

				out.println(entity.getId());
			}

			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

		}
	}

	public void plot() {

		JList resultList = new JList(new DefaultListModel());

		for (BioEntity ent : map.keySet()) {

			((DefaultListModel) resultList.getModel()).addElement(ent.getId());
		}

		JFrame frame = new JFrame("Dead Reactions results");
		frame.add(new JLabel(resultList.getModel().getSize()
				+ " dead reactions"), BorderLayout.PAGE_START);
		frame.add(new JScrollPane(resultList), BorderLayout.CENTER);
		frame.pack();
		RefineryUtilities.centerFrameOnScreen(frame);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);

	}

	/**
	 * 
	 * @return The dead reactions.
	 */
	public List<BioEntity> getDeadReactions() {

		List<BioEntity> dead = new ArrayList<BioEntity>();

		dead.addAll(map.keySet());

		return dead;
	}

	/**
	 * 
	 * Removes all reaction that are not considered dead.
	 * 
	 * @param minValue
	 *            The minimal value to consider a reaction dead.
	 */
	public void clean(double minValue) {

		List<BioEntity> toRemove = new ArrayList<BioEntity>();

		for (BioEntity ent : map.keySet()) {

			if (Math.abs(map.get(ent)[0] - 0) > minValue
					|| Math.abs(map.get(ent)[1] - 0) > minValue) {
				toRemove.add(ent);
			}
		}

		for (BioEntity b : toRemove) {
			map.remove(b);
		}

	}

}
