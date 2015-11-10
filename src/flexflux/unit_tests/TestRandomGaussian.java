package flexflux.unit_tests;


import javax.swing.JFrame;

import org.math.plot.Plot2DPanel;

import flexflux.utils.maths.RandomGaussian;

public class TestRandomGaussian extends FFUnitTest{

	public static void main(String[] args) {
		RandomGaussian r = new RandomGaussian();
		
		double[] numbers = new double[100000];
		
		
		for(int i=0;i < 100000; i++) {
			double val = r.getRandomDouble(10, 50);
			
				numbers[i] = val;
		}
		
		Plot2DPanel plot = new Plot2DPanel();
		plot.addHistogramPlot("Log Normal population", numbers, 50);
		
		 // put the PlotPanel in a JFrame like a JPanel
        JFrame frame = new JFrame("a plot panel");
        frame.setSize(600, 600);
        frame.setContentPane(plot);
        frame.setVisible(true);
        
        return;
		
	}

}
