/**
 * 13 juin 2013 
 */
package flexflux.applications;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

import flexflux.general.CplexBind;
import flexflux.general.GLPKBind;
import flexflux.unit_tests.TestBECO;
import flexflux.unit_tests.TestBind;
import flexflux.unit_tests.TestClassification;
import flexflux.unit_tests.TestCondition;
import flexflux.unit_tests.TestExternalMetaboliteConstraints;
import flexflux.unit_tests.TestFVA_KO_DR;
import flexflux.unit_tests.TestInteraction;
import flexflux.unit_tests.TestKoWithInteractions;
import flexflux.unit_tests.TestListOfConditions;
import flexflux.unit_tests.TestROBA;
import flexflux.unit_tests.TestRandomConditions;
import flexflux.unit_tests.TestSBMLQual;
import flexflux.unit_tests.TestSimplifiedConstraint;
import flexflux.unit_tests.TestTDRFBA;

/**
 * 
 * Tests the solvers
 * 
 * 
 * @author lmarmiesse 13 juin 2013
 * 
 */
public class FlexfluxTest extends FFApplication {

	public static List<String> okSolvers = new ArrayList<String>();

	public static String message = "Solver test function";
	
	public static boolean doUnitTests = true;

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
		} catch (Error e) {
			System.err
					.println("Error, the solver CPLEX cannot be found. If you have installed this solver, check your solver installation and the configuration file.");
		}

		System.err.println();

		if (GLPKok) {
			System.err.println("You can use FlexFlux with the solver GLPK !");
		}
		if (CPLEXok) {
			System.err
					.println("You can use FlexFlux with the solver CPLEX ! (to use it, add \"-sol CPLEX\" to your command lines or choose it in the drop down list in the graphical version.)");
		}
		if (!GLPKok && !CPLEXok) {

			System.err
					.println("No solver is well configured. Check the configuration file.\nYou wont be able to use "
							+ "all of Flexflux functions");

		}

		if (okSolvers.size() > 0 && doUnitTests) {
			
			String solver = okSolvers.get(0);
			
			
			System.setProperty("solver", solver);

			// ////////////////////// Unit tests
			JUnitCore junit = new JUnitCore();

			List<Class> classes = new ArrayList<Class>();
			
			classes.add(TestBind.class);
			classes.add(TestBECO.class);
			classes.add(TestClassification.class);
			classes.add(TestCondition.class);
			classes.add(TestExternalMetaboliteConstraints.class);
			classes.add(TestFVA_KO_DR.class);
			classes.add(TestInteraction.class);
			classes.add(TestKoWithInteractions.class);
			classes.add(TestListOfConditions.class);
			classes.add(TestRandomConditions.class);
			classes.add(TestROBA.class);
			classes.add(TestSBMLQual.class);
			classes.add(TestSimplifiedConstraint.class);
			classes.add(TestTDRFBA.class);

			System.out.println("Running unit tests : ");

			for (Class cl : classes) {

				try{

					// TO STOP ALL THE PRINTS
					PrintStream oldOut = System.out;
					PrintStream oldErr = System.err;
					System.setOut(new PrintStream(new OutputStream() {
						public void write(int b) {
							// DO NOTHING
						}
					}));
					System.setErr(new PrintStream(new OutputStream() {
						public void write(int b) {
							// DO NOTHING
						}
					}));
					
					
					Result result = junit.run(cl);
					System.setOut(oldOut);
					System.setErr(oldErr);
	
					if (result.wasSuccessful()) {
						System.out.println(cl.getSimpleName() + " OK !");
					} else {
						System.out.println(cl.getSimpleName() + " ERROR");
						
						for (Failure f : result.getFailures()){
							System.out.println(f.getMessage());
//							System.out.println(f.getTrace());
						}
					}
				}catch(Exception e){
					
					System.out.println(cl.getSimpleName() + " ERROR");
					
				}

		

			}
		}

	}

	@Override
	public String getMessage() {
		return message;
	}

	@Override
	public String getExample() {
		return "";
	}

}
