package flexflux.utils.plot;

import java.awt.Color;
import java.awt.Font;
import java.util.Collection;
import java.util.Collections;

import javax.swing.JFrame;

import org.math.plot.Plot2DPanel;
import org.math.plot.plotObjects.BaseLabel;

public class Plot {
	
	
	/**
	 * Plots an histogram
	 * @param numArray
	 * @param titleString
	 * @param xLabel
	 * @param yLabel
	 */
	public static void plotHistogram(Collection<Integer> numArray, String titleString, String xLabel,String yLabel,int nbBins) 
	{
		
		double[] numbers = new double[numArray.size()+1];

		int i = 0;
		
		for (Integer nb : numArray) {
			numbers[i] = nb;
			i++;
		}
		
		int max = Collections.max(numArray);
		
		// trick to counteract the bug that does not show the last category
		numbers[i] = max + 1;
		
		
		
		Plot2DPanel plot = new Plot2DPanel();
		
		plot.addHistogramPlot(
				titleString,
				numbers, nbBins);
		
		
		 BaseLabel title = new BaseLabel(titleString, Color.BLUE, 0.5, 1.1);
         title.setFont(new Font("Courier", Font.BOLD, 20));
         plot.addPlotable(title);
		
         plot.setAxisLabel(0,  xLabel);
         plot.setAxisLabel(1,  yLabel);

		// put the PlotPanel in a JFrame like a JPanel
		JFrame frame = new JFrame(titleString);
		frame.setSize(600, 600);
		frame.setContentPane(plot);
		frame.setVisible(true);
	}

}
