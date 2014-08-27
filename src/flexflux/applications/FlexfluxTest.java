/**
 * 13 juin 2013 
 */
package flexflux.applications;

import flexflux.general.CplexBind;
import flexflux.general.GLPKBind;

import java.util.ArrayList;
import java.util.List;

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
	
	public static String message = "Solver test function";
	
	public static void main(String[] args) {

		
		
		boolean GLPKok = false;
		boolean CPLEXok = false;

		try {

			System.err.println(" --- GLPK SOLVER ---- ");
			new GLPKBind();
			GLPKok = true;
			System.err.println("GLPK solver : OK !");
			okSolvers.add("GLPK");

		} catch (UnsatisfiedLinkError e) {
			System.err
					.println("Error, the solver GLPK cannot be found. If you have installed this solver, check your solver installation and the configuration file.");
		} catch (NoClassDefFoundError e) {
			System.err
					.println("Error, the solver GLPK cannot be found. There seems to be a problem with the .jar file of GLPK");
		}
		System.err.println();

		try {

			System.err.println(" --- CPLEX SOLVER ---- ");
			new CplexBind();
			CPLEXok = true;
			System.err.println("CPLEX solver : OK !");
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

		System.err.println();

		if (GLPKok) {
			System.err.println("You can use FlexFlux with the solver GLPK !");
		}
		if (CPLEXok) {
			System.err
					.println("You can use FlexFlux with the solver CPLEX ! (to use it, add \"-sol CPLEX\" to your command lines.)");
		}
		if (!GLPKok && !CPLEXok) {

			System.err
					.println("You can't use FlexFlux, no solver is well configured. Check the configuration file.");

		}

	}

}
