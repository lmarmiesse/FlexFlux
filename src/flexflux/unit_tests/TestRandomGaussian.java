package flexflux.unit_tests;

import static org.junit.Assert.*;

import javax.swing.JFrame;

import org.junit.Test;
import org.math.plot.Plot2DPanel;

import flexflux.utils.maths.RandomGaussian;

public class TestRandomGaussian {

	public static void main(String[] args) {
		RandomGaussian r = new RandomGaussian();
		
		double[] numbers = new double[100000];
		
		double min = 1.0;
		double max = 100.0;
		
		
		for(int i=0;i < 100000; i++) {
			double val = r.getRandomDouble(10, 50);
			
//			if(val <min || val > max) {
//				i--;
//				
//			}
//			else {
				numbers[i] = val;
//			}
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
