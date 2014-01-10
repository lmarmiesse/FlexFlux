/**
 * 13 juin 2013 
 */
package src.applications;

import java.util.ArrayList;
import java.util.List;

import src.CplexBind;
import src.GLPKBind;

/**
 * 
 * Tests the solvers
 * 
 * 
 * @author lmarmiesse 13 juin 2013
 * 
 */
public class FlexfluxTest {

	public static List<String> okSolvers = new ArrayList<String>();
	
	public String message = "Sover test function";
	
	public static void main(String[] args) {

		
		
		boolean GLPKok = false;
		boolean CPLEXok = false;

		try {

			System.out.println(" --- GLPK SOLVER ---- ");
			new GLPKBind(false);
			GLPKok = true;
			System.out.println("GLPK solver : OK !");
			okSolvers.add("GLPK");

		} catch (UnsatisfiedLinkError e) {
			System.err
					.println("Error, the solver GLPK cannot be found. If you have installed this solver, check your solver installation and the configuration file.");
		} catch (NoClassDefFoundError e) {
			System.err
					.println("Error, the solver GLPK cannot be found. There seems to be a problem with the .jar file of GLPK");
		}
		System.out.println();

		try {

			System.out.println(" --- CPLEX SOLVER ---- ");
			new CplexBind(false);
			CPLEXok = true;
			System.out.println("CPLEX solver : OK !");
			okSolvers.add("CPLEX");
		} catch (UnsatisfiedLinkError e) {
			System.err
					.println("Error, the solver CPLEX cannot be found. If you have installed this solver, check your solver installation and the configuration file.");
		} catch (NoClassDefFoundError e) {
			System.err
					.println("Error, the solver CPLEX cannot be found. There seems to be a problem with the .jar file of CPLEX");
		} catch (Error e){
			System.err
			.println("Error, the solver CPLEX cannot be found. If you have installed this solver, check your solver installation and the configuration file.");
		}

		System.out.println();

		if (GLPKok) {
			System.out.println("You can use FlexFlux with the solver GLPK !");
		}
		if (CPLEXok) {
			System.out
					.println("You can use FlexFlux with the solver CPLEX ! (to use it, add \"-sol CPLEX\" to your command lines.)");
		}
		if (!GLPKok && !CPLEXok) {

			System.out
					.println("You can't use FlexFlux, no solver is well configured. Check the configuration file.");

		}

	}

}
