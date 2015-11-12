package flexflux.utils.maths;

import java.util.Random;

public final class RandomGaussian {

	private Random fRandom;

	public RandomGaussian() {
		fRandom = new Random();
	}

	public double getRandomDouble(double aMean, double aVariance) {
		return aMean + fRandom.nextGaussian() * aVariance;
	}

	public long getRandomInteger(double aMean, double aVariance) {
		
		double num = getRandomDouble(aMean, aVariance);
		
		return Math.round(num);
		
	}
}